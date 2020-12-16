/**
 * Copyright 2019 Pramati Prism, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.srujankujmar.deployer.services.util;

import com.github.srujankujmar.commons.constants.K8SRuntimeConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.AnnotationKey;
import com.github.srujankujmar.commons.models.KubernetesResource;
import com.github.srujankujmar.commons.models.Manifest;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.commons.utils.ResourceSelectorUtil;
import com.github.srujankujmar.deployer.core.model.CustomResourceKind;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.services.broker.K8sResourceBroker;
import com.github.srujankujmar.deployer.services.builder.NamespaceBuilder;
import com.github.srujankujmar.deployer.services.client.GenericK8sClient;
import com.github.srujankujmar.deployer.services.client.K8sResourceClient;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.handler.ResourceHandlers;
import com.github.srujankujmar.deployer.services.handler.ResourceLifeCycleHandler;
import com.github.srujankujmar.deployer.services.handler.impl.NamespaceHandler;
import com.github.srujankujmar.deployer.services.manager.AnnotationsUpdateManager;
import com.github.srujankujmar.deployer.services.model.CustomObject;
import com.github.srujankujmar.deployer.services.model.DeployerActivity;
import com.github.srujankujmar.deployer.services.model.PodParent;
import com.github.srujankujmar.deployer.services.processor.PodParentUtil;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Namespace;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles generic resource level operation such as apply, undeploy among others
 */

public class K8sResourceDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(K8sResourceDispatcher.class);

    private K8sResourceBroker resourceBroker;
    private ApiClient apiClient;
    private String namespace;
    private boolean waitForReadiness;

    public K8sResourceDispatcher(ApiClient apiClient) {
        this.apiClient = apiClient;
        this.namespace = K8SRuntimeConstants.DEFAULT_NAMESPACE;
        this.waitForReadiness = true;
        this.resourceBroker = new K8sResourceBroker(apiClient);
    }

    public K8sResourceDispatcher withNamespace(String namespace) {
        this.namespace = namespace;
        this.resourceBroker.withNamespace(namespace);
        return this;
    }

    public void create(List<Manifest> manifests) throws HyscaleException {
        apply(manifests);
    }

    public boolean isWaitForReadiness() {
        return waitForReadiness;
    }

    public void waitForReadiness(boolean waitForReadiness) {
        this.waitForReadiness = waitForReadiness;
    }

    /**
     * Applies manifest to cluster
     * Use update policy if resource found on cluster otherwise create
     *
     * @param manifests
     * @throws HyscaleException
     */
    public void apply(List<Manifest> manifests) throws HyscaleException {
        if (manifests == null || manifests.isEmpty()) {
            logger.error("Found empty manifests to deploy ");
            throw new HyscaleException(DeployerErrorCodes.MANIFEST_REQUIRED);
        }
        createNamespaceIfNotExists();

        MultiValueMap<String,CustomObject> kindVsCustomObjects = getCustomObjects(manifests);
        List<KubernetesResource> k8sResources = getSortedResources(manifests);
        List<String> appliedKinds = buildAppliedKindsAnnotation(kindVsCustomObjects);

        for (KubernetesResource k8sResource : k8sResources) {
            AnnotationsUpdateManager.update(k8sResource, AnnotationKey.LAST_UPDATED_AT,
                    DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));

            if(k8sResource.getKind().equalsIgnoreCase("deployment") || k8sResource.getKind().equalsIgnoreCase("statefulset")){
                AnnotationsUpdateManager.update(k8sResource,AnnotationKey.HYSCALE_APPLIED_KINDS,appliedKinds.toString());
            }
            ResourceLifeCycleHandler lifeCycleHandler = ResourceHandlers.getHandlerOf(k8sResource.getKind());
            if (lifeCycleHandler != null && k8sResource.getResource() != null && k8sResource.getV1ObjectMeta() != null) {
                try {
                    String name = k8sResource.getV1ObjectMeta().getName();
                    if (resourceBroker.get(lifeCycleHandler, name) != null) {
                        resourceBroker.update(lifeCycleHandler, k8sResource, lifeCycleHandler.getUpdatePolicy());
                    } else {
                        resourceBroker.create(lifeCycleHandler, k8sResource.getResource());
                    }
                    kindVsCustomObjects.remove(k8sResource.getKind());
                } catch (HyscaleException ex) {
                    logger.error("Failed to apply resource :{} Reason :: {}", k8sResource.getKind(), ex.getMessage(),ex);
                }
            }
        }
        applyCustomResources(kindVsCustomObjects);
    }

    /**
     * Undeploy resources from cluster
     * Deletes all resources belonging to all services in an app environment
     * @param appName
     * @throws HyscaleException
     */
    public void undeployApp(String appName) throws HyscaleException {
        logger.info("Undeploy initiated for application - {}",appName);
        if (StringUtils.isBlank(appName)) {
            logger.error("No applicaton found for undeployment");
            throw new HyscaleException(DeployerErrorCodes.APPLICATION_REQUIRED);
        }
        PodParentUtil podParentUtil = new PodParentUtil(apiClient,namespace);
        Map<String,PodParent> serviceVsPodParents = podParentUtil.getServiceVsPodParentMap(appName);
        if(serviceVsPodParents != null && !serviceVsPodParents.isEmpty()){
            for (Map.Entry<String, PodParent> entry : serviceVsPodParents.entrySet()) {
                PodParent podParent = entry.getValue();
                List<CustomResourceKind> appliedKindsList = podParentUtil.getAppliedKindsList(podParent);
                String selector = ResourceSelectorUtil.getServiceSelector(appName,null);
                deleteResources(selector,appliedKindsList);
            }
        }
    }

    /**
     * Undeploy resources from cluster
     * Deletes all resources in a service
     *
     * @param appName
     * @param serviceName
     * @throws HyscaleException if failed to delete any resource
     */
    public void undeployService(String appName, String serviceName) throws HyscaleException {
        logger.info("Undeploy initiated for service - {}",serviceName);
        if (StringUtils.isBlank(appName)) {
            logger.error("No applicaton found for undeployment");
            throw new HyscaleException(DeployerErrorCodes.APPLICATION_REQUIRED);
        }
        PodParentUtil podParentUtil = new PodParentUtil(apiClient,namespace);
        PodParent podParent = podParentUtil.getPodParentForService(serviceName);
        List<CustomResourceKind> appliedKindsList = podParentUtil.getAppliedKindsList(podParent);
        String selector = ResourceSelectorUtil.getServiceSelector(appName,serviceName);
        deleteResources(selector,appliedKindsList);
    }

    private List<String> buildAppliedKindsAnnotation(MultiValueMap<String, CustomObject> kindVsCustomObject){
        if(kindVsCustomObject == null || kindVsCustomObject.isEmpty()){
            return Collections.emptyList();
        }
        List<String> appliedKinds = new ArrayList<>();
        kindVsCustomObject.forEach((kind,customObjects)->{
            String kindVsApiVersion = kind+":"+customObjects.get(0).getApiVersion();
            appliedKinds.add(kindVsApiVersion);
        });
        return appliedKinds;
    }

    private void applyCustomResources(MultiValueMap<String, CustomObject> kindVsCustomObject){
        if(kindVsCustomObject != null && !kindVsCustomObject.isEmpty()){
            kindVsCustomObject.forEach((kind,customObjects)->{
                for(CustomObject object : customObjects){
                    // Using Generic K8s Client
                    GenericK8sClient genericK8sClient = new K8sResourceClient(apiClient).
                            withNamespace(namespace).forKind(new CustomResourceKind(kind,object.getApiVersion()));
                    if(genericK8sClient != null){
                        try{
                            WorkflowLogger.startActivity(DeployerActivity.DEPLOYING,kind);
                            if(genericK8sClient.get(object) != null && !genericK8sClient.patch(object)){
                                logger.debug("Updating resource with Generic client for Kind - {}",kind);
                                // Delete and Create if failed to Patch
                                logger.info("Deleting & Creating resource : {}",object.getKind());
                                genericK8sClient.delete(object);
                                genericK8sClient.create(object);
                                WorkflowLogger.endActivity(Status.DONE);
                            }else{
                                logger.debug("Creating resource with Generic client for Kind - {}",kind);
                                genericK8sClient.create(object);
                                WorkflowLogger.endActivity(Status.DONE);
                            }
                        }catch (HyscaleException ex){
                            WorkflowLogger.endActivity(Status.FAILED);
                            logger.error("Failed to apply resource :{} Reason :: {}", kind, ex.getMessage());
                        }
                    }
                }
            });
        }
    }

    private void deleteResources(String labelSelector, List<CustomResourceKind> appliedKindsList) throws HyscaleException {
        boolean resourcesDeleted = true;

        List<String> failedResources = new ArrayList<>();
        if(appliedKindsList!=null && !appliedKindsList.isEmpty()){
            for(CustomResourceKind customResource : appliedKindsList){
                logger.info("Cleaning up - {}",customResource.getKind());
                GenericK8sClient genericK8sClient = new K8sResourceClient(apiClient).
                        withNamespace(namespace).forKind(customResource);
                List<CustomObject> resources =  genericK8sClient.getBySelector(labelSelector);
                if(resources == null || resources.isEmpty()){
                    continue;
                }
                WorkflowLogger.startActivity(DeployerActivity.DELETING,customResource.getKind());
                for(CustomObject resource : resources){
                    resource.put("kind",customResource.getKind());
                    boolean result = genericK8sClient.delete(resource);
                    logger.debug("Undeployment status for resource {} is {}", customResource.getKind(), result);
                    resourcesDeleted = resourcesDeleted && result;
                }
                if(resourcesDeleted){ WorkflowLogger.endActivity(Status.DONE); }
                else{
                    failedResources.add(customResource.getKind());
                    WorkflowLogger.endActivity(Status.FAILED);
                }
            }
        }else if(!failedResources.isEmpty()){
            String[] args = new String[failedResources.size()];
            failedResources.toArray(args);
            throw new HyscaleException(DeployerErrorCodes.FAILED_TO_DELETE_RESOURCE, args);
        }else{
            WorkflowLogger.info(DeployerActivity.NO_RESOURCES_TO_UNDEPLOY);
        }
    }

    private MultiValueMap<String,CustomObject> getCustomObjects(List<Manifest> manifests) throws HyscaleException {
        MultiValueMap<String,CustomObject> kindVsObjects = new LinkedMultiValueMap<>();
        for (Manifest manifest : manifests) {
            try {
                CustomObject object = KubernetesResourceUtil.getK8sCustomObjectResource(manifest,namespace);
                if(object != null){
                    logger.debug("Adding kind - {}",object.getKind());
                    kindVsObjects.add(object.getKind(),object);
                }
            } catch (Exception e) {
                HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_APPLY_MANIFEST);
                logger.error("Error while applying manifests to kubernetes", ex);
                throw ex;
            }
        }
        return kindVsObjects;
    }
    
    private List<KubernetesResource> getSortedResources(List<Manifest> manifests) throws HyscaleException{
        List<KubernetesResource> k8sResources = new ArrayList<>();
        
        for (Manifest manifest : manifests) {
            try {
                KubernetesResource kubernetesResource = KubernetesResourceUtil.getKubernetesResource(manifest, namespace);
                if(kubernetesResource != null){
                        k8sResources.add(kubernetesResource);
                }
            } catch (Exception e) {
                HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_APPLY_MANIFEST);
                logger.error("Error while applying manifests to kubernetes", ex);
                throw ex;
            }
        }
        // Sort resources to deploy secrets and configmaps before Pod Controller
        k8sResources.sort((resource1, resource2) -> ResourceKind.fromString(resource1.getKind()).getWeight()
                - ResourceKind.fromString(resource2.getKind()).getWeight());
        
        return k8sResources;
    }

    /**
     * Creates namespace if it doesnot exist on the cluster
     *
     * @throws HyscaleException
     */
    private void createNamespaceIfNotExists() throws HyscaleException {
        NamespaceHandler namespaceHandler = (NamespaceHandler) ResourceHandlers.getHandlerOf(ResourceKind.NAMESPACE.getKind());
        V1Namespace v1Namespace = null;
        try {
            v1Namespace = namespaceHandler.get(apiClient, namespace, null);
        } catch (HyscaleException ex) {
            logger.error("Error while getting namespace: {}, error: {}", namespace, ex.getMessage());
        }
        if (v1Namespace == null) {
            logger.debug("Namespace: {}, does not exist, creating", namespace);
            namespaceHandler.create(apiClient, NamespaceBuilder.build(namespace), namespace);
        }
    }

}
