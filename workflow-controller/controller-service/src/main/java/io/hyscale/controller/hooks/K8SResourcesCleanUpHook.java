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
package com.github.srujankujmar.controller.hooks;

import java.io.IOException;
import java.util.*;

import com.github.srujankujmar.commons.component.InvokerHook;
import com.github.srujankujmar.deployer.core.model.CustomResourceKind;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.services.client.GenericK8sClient;
import com.github.srujankujmar.deployer.services.client.K8sResourceClient;
import com.github.srujankujmar.deployer.services.model.CustomObject;
import com.github.srujankujmar.deployer.services.model.PodParent;
import com.github.srujankujmar.deployer.services.processor.PodParentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.commons.models.Manifest;
import com.github.srujankujmar.commons.utils.ResourceSelectorUtil;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.provider.K8sClientProvider;
import com.github.srujankujmar.deployer.services.util.KubernetesResourceUtil;
import io.kubernetes.client.openapi.ApiClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Hook to remove stale resources from K8s cluster
 *
 */
@Component
public class K8SResourcesCleanUpHook implements InvokerHook<WorkflowContext> {

	private static final Logger logger = LoggerFactory.getLogger(K8SResourcesCleanUpHook.class);

	@Autowired
	private K8sClientProvider clientProvider;

	/**
	 * Clean up old resources
	 * 1.	Create map of resources in manifest
	 * 2.	For each Resource where clean up is enabled except PVC:
	 * 		1. Fetch resource from K8s based on selector
	 * 		2. if does not exist in map delete
	 */
	@Override
	public void preHook(WorkflowContext context) throws HyscaleException {
		logger.info("Starting stale kubernetes resource cleanup");
		ApiClient apiClient = clientProvider.get((K8sAuthorisation) context.getAuthConfig());
		String serviceName = context.getServiceName();
		String appName = context.getAppName();
		String namespace = context.getNamespace();
		String envName = context.getEnvName();
		List<Manifest> manifestList = (List<Manifest>) context.getAttribute(WorkflowConstants.GENERATED_MANIFESTS);

		if (manifestList == null || manifestList.isEmpty()) {
			logger.debug("No resources to cleanup");
			return;
		}
		boolean isMsgPrinted = false;
		String selector = ResourceSelectorUtil.getSelector(appName, envName, serviceName);
		try{
			MultiValueMap<String,String> kindVsResourcesManifestMap = getResourcesMap(manifestList);
			PodParentUtil podParentUtil = new PodParentUtil(apiClient,namespace);
			PodParent podParent = podParentUtil.getPodParentForService(serviceName);
			if(podParent == null){
				return;
			}
			List<CustomResourceKind> appliedKindsList = podParentUtil.getAppliedKindsList(podParent);
			for(CustomResourceKind customResourceKind : appliedKindsList){
				if(ResourceKind.PERSISTENT_VOLUME_CLAIM.getKind().equalsIgnoreCase(customResourceKind.getKind())){
					continue;
				}
				GenericK8sClient genericK8sClient = new K8sResourceClient(apiClient).withNamespace(namespace)
						.forKind(customResourceKind);
				List<CustomObject> existingResources = genericK8sClient.getBySelector(selector);
				if(existingResources == null || existingResources.isEmpty()){
					continue;
				}
				List<String> newResources = kindVsResourcesManifestMap.get(customResourceKind.getKind()) != null
						? kindVsResourcesManifestMap.get(customResourceKind.getKind())
						: new ArrayList<String>();

				//TODO implement Lazy Deletion
				for(CustomObject existingResource : existingResources){
					try{
						String name;
						name = existingResource.getMetadata().getName();
						if (!newResources.contains(name)) {
							if (!isMsgPrinted) {
								WorkflowLogger.header(ControllerActivity.CLEANING_UP_RESOURCES);
								isMsgPrinted = true;
							}
							genericK8sClient.delete(existingResource);
						}
					}catch (Exception e){
						logger.error("Error while cleaning up stale resource: {}, error: {}", customResourceKind.getKind(),
								e.getMessage());
					}
				}
			}
			if(isMsgPrinted){
				WorkflowLogger.footer();
			}
		}catch (Exception e){
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_READ_MANIFEST);
			logger.error("Error while cleaning stale kubernetes resources, error: {}", ex.getMessage());
			return;
		}
	}

	@Override
	public void postHook(WorkflowContext context) throws HyscaleException {

	}

	@Override
	public void onError(WorkflowContext context, Throwable th) {
		logger.error("Error while cleaning up stale resources, error {}", th.getMessage());
	}

	private MultiValueMap<String,String> getResourcesMap(List<Manifest> manifestList) throws IOException {
		MultiValueMap<String,String> kindVsResourceNames = new LinkedMultiValueMap<>();
		for(Manifest manifest : manifestList){
			CustomObject resource = KubernetesResourceUtil.getK8sCustomObjectResource(manifest,null);
			if(resource == null || resource.getMetadata() == null ){
				continue;
			}
			CustomResourceKind resourceKind = new CustomResourceKind(resource.getKind(),resource.getApiVersion());
			kindVsResourceNames.add(resourceKind.getKind(),resource.getMetadata().getName());
		}
		return kindVsResourceNames;
	}

}
