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
/**
 *
 */
package com.github.srujankujmar.deployer.services.handler.impl;

import java.util.List;

import com.github.srujankujmar.deployer.services.constants.DeployerConstants;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.handler.ResourceHandlers;
import com.github.srujankujmar.deployer.services.handler.ResourceLifeCycleHandler;
import com.github.srujankujmar.deployer.services.model.DeployerActivity;
import com.github.srujankujmar.deployer.services.model.PodCondition;
import com.github.srujankujmar.deployer.services.model.ResourceStatus;
import com.github.srujankujmar.deployer.services.util.ExceptionHelper;
import com.github.srujankujmar.deployer.services.util.K8sPodUtil;
import com.github.srujankujmar.deployer.services.util.K8sResourcePatchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import com.github.srujankujmar.commons.constants.K8SRuntimeConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.ActivityContext;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.AnnotationKey;
import com.github.srujankujmar.commons.models.ResourceLabelKey;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.commons.utils.GsonProviderUtil;
import com.github.srujankujmar.commons.utils.ResourceSelectorUtil;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.core.model.ResourceOperation;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1beta2Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1beta2StatefulSet;
import io.kubernetes.client.openapi.models.V1beta2StatefulSetList;
import io.kubernetes.client.openapi.models.V1beta2StatefulSetStatus;
import io.kubernetes.client.custom.V1Patch;

/**
 * @author tushart
 *
 */
public class V1beta2StatefulSetHandler implements ResourceLifeCycleHandler<V1beta2StatefulSet> {

	private static final Logger LOGGER = LoggerFactory.getLogger(V1beta2StatefulSetHandler.class);

	@Override
	public V1beta2StatefulSet create(ApiClient apiClient, V1beta2StatefulSet resource, String namespace)
			throws HyscaleException {
		if (resource == null) {
			LOGGER.debug("Cannot create null StatefulSet");
			return resource;
		}
		WorkflowLogger.startActivity(DeployerActivity.DEPLOYING_STATEFULSET);
		AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);
		String name = resource.getMetadata().getName();
		V1beta2StatefulSet statefulSet = null;
		try {
			resource.getMetadata().putAnnotationsItem(
					AnnotationKey.K8S_HYSCALE_LAST_APPLIED_CONFIGURATION.getAnnotation(), GsonProviderUtil.getPrettyGsonBuilder().toJson(resource));
			statefulSet = appsV1beta2Api.createNamespacedStatefulSet(namespace, resource, DeployerConstants.TRUE, null, null);
		} catch (ApiException e) {
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_CREATE_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.CREATE));
			LOGGER.error("Error while creating statefulset {} in namespace {}, error {}", name, namespace,
					ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		}
		WorkflowLogger.endActivity(Status.DONE);
		return statefulSet;
	}

	@Override
	public boolean update(ApiClient apiClient, V1beta2StatefulSet resource, String namespace) throws HyscaleException {
		if (resource == null) {
			LOGGER.debug("Cannot update null StatefulSet");
			return false;
		}
		AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);
		String name = resource.getMetadata().getName();
		V1beta2StatefulSet existingStatefulSet = null;
		try {
			existingStatefulSet = get(apiClient, name, namespace);
		} catch (HyscaleException ex) {
			LOGGER.debug("Error while getting StatefulSet {} in namespace {} for Update, creating new", name,
					namespace);
			V1beta2StatefulSet statefulSet = create(apiClient, resource, namespace);
			return statefulSet != null;
		}

		WorkflowLogger.startActivity(DeployerActivity.DEPLOYING_STATEFULSET);
		try {
			String resourceVersion = existingStatefulSet.getMetadata().getResourceVersion();
			resource.getMetadata().setResourceVersion(resourceVersion);
			appsV1beta2Api.replaceNamespacedStatefulSet(name, namespace, resource, DeployerConstants.TRUE, null, null);
		} catch (ApiException e) {
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_UPDATE_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.UPDATE));
			LOGGER.error("Error while updating SatefulSet {} in namespace {}, error {}", name, namespace,
					ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		}
		WorkflowLogger.endActivity(Status.DONE);
		return true;
	}

	@Override
	public V1beta2StatefulSet get(ApiClient apiClient, String name, String namespace) throws HyscaleException {
		AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);
		V1beta2StatefulSet v1StatefulSet = null;
		try {
			v1StatefulSet = appsV1beta2Api.readNamespacedStatefulSet(name, namespace, DeployerConstants.TRUE, null, null);
		} catch (ApiException e) {
			HyscaleException ex = ExceptionHelper.buildGetException(getKind(), e, ResourceOperation.GET);
			LOGGER.error("Error while fetching StatefulSet {} in namespace {}, error {}", name, namespace,
					ex.toString());
			throw ex;
		}
		return v1StatefulSet;
	}

	@Override
	public List<V1beta2StatefulSet> getBySelector(ApiClient apiClient, String selector, boolean label, String namespace)
			throws HyscaleException {
		AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);
		String labelSelector = label ? selector : null;
		String fieldSelector = label ? null : selector;
		List<V1beta2StatefulSet> statefulSets = null;
		try {
			V1beta2StatefulSetList statefulSetList = appsV1beta2Api.listNamespacedStatefulSet(namespace, DeployerConstants.TRUE, null, 
					null, fieldSelector, labelSelector, null, null, null, null);
			statefulSets = statefulSetList != null ? statefulSetList.getItems() : null;
		} catch (ApiException e) {
			HyscaleException ex = ExceptionHelper.buildGetException(getKind(), e, ResourceOperation.GET_BY_SELECTOR);
			LOGGER.error("Error while listing StatefulSets in namespace {}, with selectors {} , error {}", namespace,
					selector, ex.toString());
			throw ex;
		}
		return statefulSets;
	}

	/**
	 * Also deletes previous pods if they are in error state
	 */
	@Override
	public boolean patch(ApiClient apiClient, String name, String namespace, V1beta2StatefulSet target)
			throws HyscaleException {
		if (target == null) {
			LOGGER.debug("Cannot patch null StatefulSet");
			return false;
		}
		AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);
		target.getMetadata().putAnnotationsItem(AnnotationKey.K8S_HYSCALE_LAST_APPLIED_CONFIGURATION.getAnnotation(),
				GsonProviderUtil.getPrettyGsonBuilder().toJson(target));
		V1beta2StatefulSet sourceStatefulSet = null;
		try {
			sourceStatefulSet = get(apiClient, name, namespace);
		} catch (HyscaleException e) {
			LOGGER.debug("Error while getting StatefulSet {} in namespace {} for Patch, creating new", name, namespace);
			V1beta2StatefulSet statefulSet = create(apiClient, target, namespace);
			return statefulSet != null;
		}
		WorkflowLogger.startActivity(DeployerActivity.DEPLOYING_STATEFULSET);
		Object patchObject = null;
		String lastAppliedConfig = sourceStatefulSet.getMetadata().getAnnotations()
				.get(AnnotationKey.K8S_HYSCALE_LAST_APPLIED_CONFIGURATION.getAnnotation());
		boolean deleteRequired = false;
		String serviceName = sourceStatefulSet.getMetadata().getLabels().get(ResourceLabelKey.SERVICE_NAME.getLabel());
		try {
			patchObject = K8sResourcePatchUtil.getJsonPatch(GsonProviderUtil.getPrettyGsonBuilder().fromJson(lastAppliedConfig, V1beta2StatefulSet.class),
					target, V1beta2StatefulSet.class);
			deleteRequired = isDeletePodRequired(apiClient, serviceName, namespace);
			LOGGER.debug("Deleting existing pods for updating StatefulSet patch required :{}", deleteRequired);
			V1Patch v1Patch = new V1Patch(patchObject.toString());
			appsV1beta2Api.patchNamespacedStatefulSet(name, namespace, v1Patch, DeployerConstants.TRUE, null, null, null);
		} catch (HyscaleException ex) {
			LOGGER.error("Error while creating patch for StatefulSet {}, source {}, target {}, error {}", name,
					sourceStatefulSet, target, ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		} catch (ApiException e) {
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_PATCH_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.PATCH));
			LOGGER.error("Error while patching StatefulSet {} in namespace {} , error {}", name, namespace,
					ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		} finally {
			if (deleteRequired) {
				V1PodHandler podHandler = (V1PodHandler) ResourceHandlers.getHandlerOf(ResourceKind.POD.getKind());
				podHandler.deleteBySelector(apiClient, getPodSelector(serviceName), true, namespace, false);
			}
		}
		WorkflowLogger.endActivity(Status.DONE);
		return true;
	}

	/**
	 * 
	 * @param apiClient
	 * @param name
	 * @param namespace
	 * @return true if pods needs to be deleted
	 * @throws HyscaleException
	 */
	private boolean isDeletePodRequired(ApiClient apiClient, String name, String namespace) throws HyscaleException {
		V1PodHandler podHandler = (V1PodHandler) ResourceHandlers.getHandlerOf(ResourceKind.POD.getKind());
		List<V1Pod> v1PodList = podHandler.getBySelector(apiClient, getPodSelector(name), true, namespace);
		boolean isPodInErrorState = false;
		if (v1PodList != null && !v1PodList.isEmpty()) {
            isPodInErrorState = v1PodList.stream()
                    .anyMatch(each -> !K8sPodUtil.getAggregatedStatusOfContainersForPod(each)
                            .equalsIgnoreCase(K8SRuntimeConstants.POD_RUNING_STATE_CONDITION)
                            || !K8sPodUtil.checkForPodCondition(each, PodCondition.READY));
		}
		return isPodInErrorState;
	}

	@Override
	public boolean delete(ApiClient apiClient, String name, String namespace, boolean wait) throws HyscaleException {
		ActivityContext activityContext = new ActivityContext(DeployerActivity.DELETING_STATEFULSET);
		WorkflowLogger.startActivity(activityContext);
		try {
			delete(apiClient, name, namespace);
			if (wait) {
			    List<String> pendingStatefulSets = Lists.newArrayList();
			    pendingStatefulSets.add(name);
			    waitForResourceDeletion(apiClient, pendingStatefulSets, namespace, activityContext);
			}
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				WorkflowLogger.endActivity(activityContext, Status.NOT_FOUND);
				return false;
			}
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_DELETE_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.DELETE));
			LOGGER.error("Error while deleting StatefulSet {} in namespace {} , error {}", name, namespace,
					ex.toString());
			WorkflowLogger.endActivity(activityContext, Status.FAILED);
			throw ex;
		}
		LOGGER.debug("Deleting StatefulSet done");
		WorkflowLogger.endActivity(activityContext, Status.DONE);
		return true;
	}

    private void delete(ApiClient apiClient, String name, String namespace) throws ApiException {
        AppsV1beta2Api appsV1beta2Api = new AppsV1beta2Api(apiClient);
        V1DeleteOptions deleteOptions = getDeleteOptions();
        deleteOptions.setApiVersion("apps/v1beta2");
        try {
            appsV1beta2Api.deleteNamespacedStatefulSet(name, namespace, DeployerConstants.TRUE, null, null, null, null,
                    deleteOptions);
        } catch (JsonSyntaxException e) {
            // K8s end exception ignore
        }
    }
	

	@Override
	public boolean deleteBySelector(ApiClient apiClient, String selector, boolean label, String namespace, boolean wait)
			throws HyscaleException {

		try {
			List<V1beta2StatefulSet> statefulSets = getBySelector(apiClient, selector, label, namespace);

			if (statefulSets == null || statefulSets.isEmpty()) {
				return false;
			}
			for (V1beta2StatefulSet statefulSet : statefulSets) {
				delete(apiClient, statefulSet.getMetadata().getName(), namespace, wait);
			}
		} catch (HyscaleException e) {
			if (DeployerErrorCodes.RESOURCE_NOT_FOUND.equals(e.getHyscaleError())) {
				return false;
			}
			throw e;
		}
		return true;

	}

	@Override
	public String getKind() {
		return ResourceKind.STATEFUL_SET.getKind();
	}

	private String getPodSelector(String serviceName) {
		return ResourceSelectorUtil.getServiceSelector(null, serviceName);
	}

	@Override
	public boolean cleanUp() {
		return true;
	}

	@Override
	public ResourceStatus status(V1beta2StatefulSet statefulSet) {
		if (statefulSet.getStatus() == null) {
			return ResourceStatus.FAILED;
		} else {
			V1beta2StatefulSetStatus stsStatus = statefulSet.getStatus();
			String currentRevision = stsStatus.getCurrentRevision();
			String updateRevision = stsStatus.getUpdateRevision();
			// stsStatus.getConditions()
			Integer currentReplicas = stsStatus.getCurrentReplicas();
			Integer readyReplicas = stsStatus.getReadyReplicas();
			Integer intendedReplicas = statefulSet.getSpec().getReplicas();
			// Success case update remaining pods status and return
			if (updateRevision != null && updateRevision.equals(currentRevision) && intendedReplicas != null
					&& intendedReplicas.equals(currentReplicas) && intendedReplicas.equals(readyReplicas)) {
				return ResourceStatus.STABLE;
			}
			return ResourceStatus.PENDING;
		}
	}

	@Override
	public int getWeight() {
	    return ResourceKind.STATEFUL_SET.getWeight();
	}
}
