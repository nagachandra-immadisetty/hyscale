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

import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.handler.ResourceLifeCycleHandler;
import com.github.srujankujmar.deployer.services.model.DeployerActivity;
import com.github.srujankujmar.deployer.services.util.ExceptionHelper;
import com.github.srujankujmar.deployer.services.util.K8sResourcePatchUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.ActivityContext;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.AnnotationKey;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.core.model.ResourceOperation;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Secret;
import io.kubernetes.client.openapi.models.V1SecretList;
import io.kubernetes.client.custom.V1Patch;

// TODO Integrate logging with AOP
public class V1SecretHandler implements ResourceLifeCycleHandler<V1Secret> {

	private static final Logger LOGGER = LoggerFactory.getLogger(V1SecretHandler.class);

	@Override
	public V1Secret create(ApiClient apiClient, V1Secret resource, String namespace) throws HyscaleException {
		if (resource == null) {
			LOGGER.debug("Cannot create null Secret");
			return resource;
		}
		WorkflowLogger.startActivity(DeployerActivity.DEPLOYING_SECRETS);
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);
		String name = resource.getMetadata().getName();
		V1Secret v1Secret = null;
		try {
			resource.getMetadata().putAnnotationsItem(
					AnnotationKey.K8S_HYSCALE_LAST_APPLIED_CONFIGURATION.getAnnotation(), gson.toJson(resource));
			v1Secret = coreV1Api.createNamespacedSecret(namespace, resource, TRUE, null, null);
		} catch (ApiException e) {
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_CREATE_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.CREATE));
			LOGGER.error("Error while creating Secret {} in namespace {}, error {}", name, namespace, ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		}
		WorkflowLogger.endActivity(Status.DONE);
		return v1Secret;
	}

	@Override
	public boolean update(ApiClient apiClient, V1Secret resource, String namespace) throws HyscaleException {
		if (resource == null) {
			LOGGER.debug("Cannot update null Secret");
			return false;
		}
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);
		String name = resource.getMetadata().getName();
		V1Secret existingSecret = null;
		try {
			existingSecret = get(apiClient, name, namespace);
		} catch (HyscaleException ex) {
			LOGGER.debug("Error while getting Secret {} in namespace {} for Update, creating new", name, namespace);
			V1Secret secret = create(apiClient, resource, namespace);
			return secret != null ? true : false;
		}
		WorkflowLogger.startActivity(DeployerActivity.DEPLOYING_SECRETS);
		try {

			String resourceVersion = existingSecret.getMetadata().getResourceVersion();
			resource.getMetadata().setResourceVersion(resourceVersion);
			coreV1Api.replaceNamespacedSecret(name, namespace, resource, TRUE, null,null);
		} catch (ApiException e) {
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_UPDATE_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.UPDATE));
			LOGGER.error("Error while updating Secret {} in namespace {}, error {}", name, namespace, ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		}
		WorkflowLogger.endActivity(Status.DONE);
		return true;
	}

	@Override
	public V1Secret get(ApiClient apiClient, String name, String namespace) throws HyscaleException {
		V1Secret v1Secret = null;
		CoreV1Api apiInstance = new CoreV1Api(apiClient);
		try {
			v1Secret = apiInstance.readNamespacedSecret(name, namespace, TRUE, null, null);
		} catch (ApiException e) {
			HyscaleException ex = ExceptionHelper.buildGetException(getKind(), e, ResourceOperation.GET);
			LOGGER.error("Error while fetching Secret {} in namespace {}, error {}", name, namespace, ex.toString());
			throw ex;
		}
		return v1Secret;
	}

	@Override
	public List<V1Secret> getBySelector(ApiClient apiClient, String selector, boolean label, String namespace)
			throws HyscaleException {
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);
		List<V1Secret> v1Secrets = null;
		try {
			String labelSelector = label ? selector : null;
			String fieldSelector = label ? null : selector;
			V1SecretList v1SecretList = coreV1Api.listNamespacedSecret(namespace, TRUE, null, null,fieldSelector,
					labelSelector, null, null, null, null);
			v1Secrets = v1SecretList != null ? v1SecretList.getItems() : null;
		} catch (ApiException e) {
			HyscaleException ex = ExceptionHelper.buildGetException(getKind(), e, ResourceOperation.GET_BY_SELECTOR);
			LOGGER.error("Error while listing Secrets in namespace {}, with selectors {}, error {} ", namespace,
					selector, ex.toString());
			throw ex;
		}
		return v1Secrets;
	}

	@Override
	public boolean patch(ApiClient apiClient, String name, String namespace, V1Secret target) throws HyscaleException {
		if (target == null) {
			LOGGER.debug("Cannot patch null Secret");
			return false;
		}
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);
		target.getMetadata().putAnnotationsItem(AnnotationKey.K8S_HYSCALE_LAST_APPLIED_CONFIGURATION.getAnnotation(),
				gson.toJson(target));
		V1Secret sourceSecret = null;
		try {
			sourceSecret = get(apiClient, name, namespace);
		} catch (HyscaleException e) {
			LOGGER.debug("Error while getting Secret {} in namespace {} for Patch, creating new", name, namespace);
			V1Secret secret = create(apiClient, target, namespace);
			return secret != null ? true : false;
		}
		WorkflowLogger.startActivity(DeployerActivity.DEPLOYING_SECRETS);
		Object patchObject = null;
		String lastAppliedConfig = sourceSecret.getMetadata().getAnnotations()
				.get(AnnotationKey.K8S_HYSCALE_LAST_APPLIED_CONFIGURATION.getAnnotation());
		try {
			patchObject = K8sResourcePatchUtil.getJsonPatch(gson.fromJson(lastAppliedConfig, V1Secret.class), target,
					V1Secret.class);
			V1Patch v1Patch = new V1Patch(patchObject.toString());
			coreV1Api.patchNamespacedSecret(name, namespace, v1Patch, TRUE, null, null,null);
		} catch (HyscaleException ex) {
			LOGGER.error("Error while creating patch for Secret {}, source {}, target {}, error {}", name, sourceSecret,
					target, ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		} catch (ApiException e) {
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_PATCH_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.PATCH));
			LOGGER.error("Error while patching Secret {} in namespace {} , error {}", name, namespace, ex.toString());
			WorkflowLogger.endActivity(Status.FAILED);
			throw ex;
		}
		WorkflowLogger.endActivity(Status.DONE);
		return true;
	}

	@Override
	public boolean delete(ApiClient apiClient, String name, String namespace, boolean wait) throws HyscaleException {
		CoreV1Api coreV1Api = new CoreV1Api(apiClient);
		V1DeleteOptions deleteOptions = getDeleteOptions();
		ActivityContext activityContext = new ActivityContext(DeployerActivity.DELETING_SECRETS);
		WorkflowLogger.startActivity(activityContext);
		try {
		    try {
				coreV1Api.deleteNamespacedSecret(name, namespace, TRUE, null, null, null, null, deleteOptions);
		    } catch (JsonSyntaxException e) {
			// K8s end exception ignore
		    }
			List<String> secretList = Lists.newArrayList();
			secretList.add(name);
			if (wait) {
				waitForResourceDeletion(apiClient, secretList, namespace, activityContext);
			}
		} catch (ApiException e) {
			if (e.getCode() == 404) {
				WorkflowLogger.endActivity(activityContext, Status.NOT_FOUND);
				return false;
			}
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_DELETE_RESOURCE,
					ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.DELETE));
			LOGGER.error("Error while deleting Secret {} in namespace {}, error {}", name, namespace, ex.toString());
			WorkflowLogger.endActivity(activityContext, Status.FAILED);
			throw ex;
		}
		WorkflowLogger.endActivity(activityContext, Status.DONE);
		return true;
	}

	@Override
	public boolean deleteBySelector(ApiClient apiClient, String selector, boolean label, String namespace, boolean wait)
			throws HyscaleException {
		try {
			List<V1Secret> V1SecretList = getBySelector(apiClient, selector, label, namespace);
			if (V1SecretList == null || V1SecretList.isEmpty()) {
			    return false;
			}
			for (V1Secret V1Secret : V1SecretList) {
				delete(apiClient, V1Secret.getMetadata().getName(), namespace, wait);
			}
		} catch (HyscaleException e) {
			if (DeployerErrorCodes.RESOURCE_NOT_FOUND.equals(e.getHyscaleErrorCode())) {
				LOGGER.error("Error while deleting Secrets for selector {} in namespace {}, error {}", selector,
						namespace, e.toString());
				return false;
			}
			throw e;
		}
		return true;
	}

	@Override
	public String getKind() {
		return ResourceKind.SECRET.getKind();
	}

	@Override
	public boolean cleanUp() {
		return true;
	}

	@Override
	public int getWeight() {
	    return ResourceKind.SECRET.getWeight();
	}

}
