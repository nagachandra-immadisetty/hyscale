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
package com.github.srujankujmar.deployer.services.handler.impl;

import java.util.List;

import com.github.srujankujmar.deployer.services.model.DeployerActivity;
import com.github.srujankujmar.deployer.services.constants.DeployerConstants;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.handler.ResourceLifeCycleHandler;
import com.github.srujankujmar.deployer.services.util.ExceptionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.ActivityContext;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.core.model.ResourceOperation;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1DeleteOptions;
import io.kubernetes.client.openapi.models.V1Namespace;

/**
 * V1Namespace resource operations
 *
 */
public class NamespaceHandler implements ResourceLifeCycleHandler<V1Namespace> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceHandler.class);

    @Override
    public V1Namespace create(ApiClient apiClient, V1Namespace resource, String namespace) throws HyscaleException {
        if (resource == null) {
            return resource;
        }
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        String name = resource.getMetadata().getName();
        V1Namespace createdNamespace = null;
        try {
            createdNamespace = coreV1Api.createNamespace(resource, DeployerConstants.TRUE, null, null);
        } catch (ApiException e) {
            if (e.getCode() != 409) {
                HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_CREATE_RESOURCE,
                        ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.CREATE));
                LOGGER.error("Error while creating namespace {}", name, ex);
                throw ex;
            }
        }
        return createdNamespace;
    }

    @Override
    public V1Namespace get(ApiClient apiClient, String name, String namespace) throws HyscaleException {
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        V1Namespace v1Namespace = null;
        try {
            v1Namespace = coreV1Api.readNamespace(name, DeployerConstants.TRUE, true, true);
        } catch (ApiException e) {
            HyscaleException ex = null;
            if (e.getCode() != 404) {
                ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_GET_RESOURCE,
                        ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.GET));
            } else {
                ex = new HyscaleException(e, DeployerErrorCodes.RESOURCE_NOT_FOUND,
                        ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.GET));
            }
            LOGGER.error("Error while fetching namespace {}, error {}", name, ex.toString());
            throw ex;
        }
        return v1Namespace;
    }

    @Override
    public boolean delete(ApiClient apiClient, String name, String namespace, boolean wait) throws HyscaleException {
        ActivityContext activityContext = new ActivityContext(DeployerActivity.DELETING_NAMESPACE);
        try {
            WorkflowLogger.startActivity(activityContext);
            delete(apiClient, namespace);
            List<String> namespaceList = Lists.newArrayList();
            namespaceList.add(name);
            if (wait) {
                waitForResourceDeletion(apiClient, namespaceList, namespace, activityContext);
            }
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                WorkflowLogger.endActivity(activityContext, Status.NOT_FOUND);
                return false;
            }
            HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.FAILED_TO_DELETE_RESOURCE,
                    ExceptionHelper.getExceptionMessage(getKind(), e, ResourceOperation.DELETE));
            LOGGER.error("Error while deleting namespace {}, error {}", name, ex.toString());
            WorkflowLogger.endActivity(activityContext, Status.FAILED);
            throw ex;
        }
        WorkflowLogger.endActivity(activityContext, Status.DONE);
        return true;
    }
    
    private void delete(ApiClient apiClient, String name) throws ApiException {
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        V1DeleteOptions deleteOptions = getDeleteOptions();
        try {
            coreV1Api.deleteNamespace(name, DeployerConstants.TRUE, null, null, null, null, deleteOptions);
        } catch (JsonSyntaxException e) {
            // K8s end exception ignore
        }
    }

    @Override
    public boolean update(ApiClient apiClient, V1Namespace resource, String namespace) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.UPDATE.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public List<V1Namespace> getBySelector(ApiClient apiClient, String selector, boolean label, String namespace)
            throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.GET_BY_SELECTOR.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public boolean patch(ApiClient apiClient, String name, String namespace, V1Namespace target)
            throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.PATCH.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public boolean deleteBySelector(ApiClient apiClient, String selector, boolean label, String namespace, boolean wait)
            throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.DELETE_BY_SELECTOR.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public String getKind() {
        return ResourceKind.NAMESPACE.getKind();
    }

    @Override
    public boolean cleanUp() {
        return false;
    }

    @Override
    public int getWeight() {
        return ResourceKind.NAMESPACE.getWeight();
    }

}
