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
package com.github.srujankujmar.controller.validator.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import com.github.srujankujmar.commons.constants.K8SRuntimeConstants;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.LoggerTags;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.AnnotationKey;
import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.commons.models.StorageClassAnnotation;
import com.github.srujankujmar.commons.utils.HyscaleStringUtil;
import com.github.srujankujmar.commons.utils.ResourceSelectorUtil;
import com.github.srujankujmar.commons.validator.Validator;
import com.github.srujankujmar.controller.activity.ValidatorActivity;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.handler.ResourceHandlers;
import com.github.srujankujmar.deployer.services.handler.impl.V1PersistentVolumeClaimHandler;
import com.github.srujankujmar.deployer.services.handler.impl.V1StorageClassHandler;
import com.github.srujankujmar.deployer.services.model.DeployerActivity;
import com.github.srujankujmar.deployer.services.provider.K8sClientProvider;
import com.github.srujankujmar.deployer.services.util.KubernetesVolumeUtil;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import com.github.srujankujmar.servicespec.commons.model.service.Volume;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1StorageClass;

/**
 * Validate Volumes:
 * Storage Class should be valid
 *      persist error message and returns false
 * Size and Storage class modification 
 *      persist warn message and returns true
 * 
 * @author tushar
 *
 */
@Component
public class VolumeValidator implements Validator<WorkflowContext>{
	private static final Logger logger = LoggerFactory.getLogger(VolumeValidator.class);
	
	private static final String STORAGE = "storage";

	@Autowired
	private K8sClientProvider clientProvider;

	private List<V1StorageClass> storageClassList = new ArrayList<>();

	@Override
	public boolean validate(WorkflowContext context) throws HyscaleException {
		logger.debug("Validating volumes from the service spec");
		ServiceSpec serviceSpec = context.getServiceSpec();
		if (serviceSpec == null) {
			return false;
		}
		long startTime = System.currentTimeMillis();
		TypeReference<List<Volume>> volTypeRef = new TypeReference<List<Volume>>() {
		};
		List<Volume> volumeList = serviceSpec.get(HyscaleSpecFields.volumes, volTypeRef);

		if (volumeList == null || volumeList.isEmpty()) {
			return true;
		}
		ApiClient apiClient = clientProvider.get((K8sAuthorisation) context.getAuthConfig());
		
		try {
		    if (!initStorageClass(apiClient)) {
		        return printMsg(true, startTime);
		    }
		} catch (HyscaleException ex) {
		    throw ex;
		}
		
		// Validate Storage class
		if (!validateStorageClass(volumeList)) {
		    return printMsg(true, startTime);
		}

		logger.debug("Storage class provided are valid");

		// Validate volume edit
		if (!validateVolumeEdit(apiClient, context, volumeList)) {
            return printMsg(true, startTime);
        }
		
		return printMsg(false, startTime);
	}
	
	private boolean printMsg(boolean isFailed, long startTime) {
	    logger.debug("Time taken: {}", System.currentTimeMillis() - startTime);
	    if (isFailed) {
            return false;
	    }
        return true;
	}
	

	/**
	 * Validate Storage class
	 * Checks for the following conditions in the cluster and fails when
	 * <ol>
	 * <li>There are no storage classes</li>
	 * <li>No storage class is defined & default storage class does not exist in the cluster</li>
	 * <li>Provided storage class does not exist in cluster</li>
	 * </ol>
	 *
	 * @param volumeList
	 */
	private boolean validateStorageClass(List<Volume> volumeList) {
		List<String> defaultStorageClass = getDefaultStorageClass();
		Set<String> storageClassAllowed = storageClassList.stream().map(each -> each.getMetadata().getName())
				.collect(Collectors.toSet());
		logger.debug("Allowed Storage classes are : {}", storageClassAllowed);
		boolean isFailed = false;
		DeployerErrorCodes errorCode = null;
		StringBuilder failMsgBuilder = new StringBuilder();
		for (Volume volume : volumeList) {
			String storageClass = volume.getStorageClass();
			if (storageClass == null && isInvalidDefaultStorageClass(defaultStorageClass)) {
			    isFailed = true;
			    if (defaultStorageClass == null || defaultStorageClass.isEmpty()) {
			        errorCode = DeployerErrorCodes.MISSING_DEFAULT_STORAGE_CLASS;
			    } else {
			        errorCode = DeployerErrorCodes.MISSING_UNIQUE_DEFAULT_STORAGE_CLASS;
			    }
			    failMsgBuilder.append(volume.getName());
			    failMsgBuilder.append(ToolConstants.COMMA);
			}
			if (storageClass != null && !storageClassAllowed.contains(storageClass)) {
				isFailed = true;
				errorCode = DeployerErrorCodes.INVALID_STORAGE_CLASS_FOR_VOLUME;
				failMsgBuilder.append(storageClass).append(" in volume ").append(volume.getName());
				failMsgBuilder.append(ToolConstants.COMMA);
			}
		}
		if (isFailed) {
			String failMsg = HyscaleStringUtil.removeSuffixStr(failMsgBuilder, ToolConstants.COMMA);
			failMsg = new HyscaleException(errorCode, failMsg, storageClassAllowed.toString()).getMessage();
			WorkflowLogger.persist(ValidatorActivity.VOLUME_VALIDATION_FAILED, LoggerTags.ERROR, failMsg);
            logger.info("StroageClass validation failed. Message : {}", failMsg);
            return false;
		}
		return true;
	}

	private boolean isInvalidDefaultStorageClass(List<String> defaultStorageClass) {
	    if (defaultStorageClass == null || defaultStorageClass.isEmpty() || defaultStorageClass.size() > 1) {
	        return true;
	    }
        return false;
    }

    /**
	 * Validate volume edit is supported,
	 * <p> print warn message for size and storage class changes
	 * Get all pvc for this service and app
	 * Create map of volume name to pvc
	 * For volumes check existing values through pvcs
	 * @param apiClient
	 * @param context
	 * @param volumeList
	 */
	private boolean validateVolumeEdit(ApiClient apiClient, WorkflowContext context, List<Volume> volumeList) {

	    V1PersistentVolumeClaimHandler pvcHandler = (V1PersistentVolumeClaimHandler) ResourceHandlers
				.getHandlerOf(ResourceKind.PERSISTENT_VOLUME_CLAIM.getKind());
		String appName = context.getAppName();
		String envName = context.getEnvName();
		String serviceName = context.getServiceName();
		String namespace = context.getNamespace();

		String selector = ResourceSelectorUtil.getSelector(appName, envName, serviceName);
		List<V1PersistentVolumeClaim> pvcList = null;
		try {
			pvcList = pvcHandler.getBySelector(apiClient, selector, true, namespace);
		} catch (HyscaleException ex) {
			logger.debug("Error while fetching PVCs with selector: {}, error: {}", selector, ex.getMessage());
		}

		if (pvcList == null || pvcList.isEmpty()) {
			return true;
		}
		Map<String, V1PersistentVolumeClaim> volumeVsPVC = pvcList.stream().collect(
				Collectors.toMap(pvc -> KubernetesVolumeUtil.getVolumeName(pvc), pvc -> pvc, (key1, key2) -> {
					return key1;
				}));

		StringBuilder warnMsgBuilder = new StringBuilder();
		volumeList.stream().forEach( volume -> {
		    V1PersistentVolumeClaim pvc = volumeVsPVC.get(volume.getName());

            if (pvc == null) {
                // new volume does not exist on cluster
                return;
            }
            String storageClass = pvc.getSpec().getStorageClassName();
            if (StringUtils.isBlank(storageClass)) {
                logger.debug("Storage class not found in spec, getting from annotation");
                storageClass = pvc.getMetadata().getAnnotations().get(AnnotationKey.K8S_STORAGE_CLASS.getAnnotation());
            }
            Quantity existingSize = pvc.getStatus().getCapacity() != null ? pvc.getStatus().getCapacity().get(STORAGE)
                    : null;
            if (existingSize == null) {
                logger.debug("Size not found in status, getting from spec");
                V1ResourceRequirements resources = pvc.getSpec().getResources();
                existingSize = resources != null
                        ? (resources.getRequests() != null ? resources.getRequests().get(STORAGE) : null)
                        : null;
            }
            Quantity newSize = Quantity.fromString(StringUtils.isNotBlank(volume.getSize()) ? volume.getSize()
                    : K8SRuntimeConstants.DEFAULT_VOLUME_SIZE);
            boolean isStorageClassSame = matchStorageClass(storageClass, volume.getStorageClass());
            boolean isSizeSame = newSize.equals(existingSize);
            if (!isStorageClassSame || !isSizeSame) {
                warnMsgBuilder.append(volume.getName()).append(ToolConstants.COMMA).append(ToolConstants.SPACE);
            }
		});
		String warnMsg = warnMsgBuilder.toString();
		if (StringUtils.isNotBlank(warnMsg)) {
			warnMsg = HyscaleStringUtil.removeSuffixStr(warnMsg, ToolConstants.COMMA + ToolConstants.SPACE);
			logger.debug(DeployerActivity.IGNORING_VOLUME_MODIFICATION.getActivityMessage(), serviceName, warnMsg);
			WorkflowLogger.persist(DeployerActivity.IGNORING_VOLUME_MODIFICATION, serviceName, warnMsg);
		}
		return true;
	}

	private Predicate<V1StorageClass> isDefaultStorageClass() {
		return v1StorageClass -> {
			Map<String, String> annotations = v1StorageClass.getMetadata().getAnnotations();
			String annotationValue = StorageClassAnnotation.getDefaultAnnotaionValue(annotations);
			if (StringUtils.isNotBlank(annotationValue)) {
				return Boolean.valueOf(annotationValue);
			}
			return false;
		};
	}

    private List<String> getDefaultStorageClass() {
        List<String> storageClasses = new ArrayList<String>();
        if (storageClassList != null && !storageClassList.isEmpty()) {
            storageClasses = storageClassList.stream().filter(isDefaultStorageClass())
                    .map(each -> each.getMetadata().getName()).collect(Collectors.toList());

        }
        return storageClasses;
    }

	private boolean initStorageClass(ApiClient apiClient) throws HyscaleException {
	    if (storageClassList != null && !storageClassList.isEmpty()) {
	        return true;
	    }
        V1StorageClassHandler storageClassHandler = (V1StorageClassHandler) ResourceHandlers
                .getHandlerOf(ResourceKind.STORAGE_CLASS.getKind());

		// Storage class are cluster based no need of selector and namespace
		try {
			storageClassList = storageClassHandler.getAll(apiClient);
		} catch (HyscaleException ex) {
			logger.error("Error while getting storage class list, error {}", ex.getMessage());
			WorkflowLogger.persist(ValidatorActivity.VOLUME_VALIDATION_FAILED, LoggerTags.ERROR,
			        DeployerErrorCodes.NO_STORAGE_CLASS_IN_K8S.getErrorMessage());
			throw new HyscaleException(DeployerErrorCodes.NO_STORAGE_CLASS_IN_K8S);
		}

		if (storageClassList == null || storageClassList.isEmpty()) {
			return false;
		}
		return true;
	}

    private boolean matchStorageClass(String existing, String modified) {
        if (StringUtils.isBlank(modified)) {
            List<String> defaultStorageClassList = getDefaultStorageClass();
            return (existing != null && defaultStorageClassList != null && defaultStorageClassList.size() == 1)
                    ? existing.equals(defaultStorageClassList.get(0))
                    : false;
        }
        if (StringUtils.isNotBlank(existing) && StringUtils.isNotBlank(modified)) {
            return existing.equals(modified);
        }
        return false;
    }

}
