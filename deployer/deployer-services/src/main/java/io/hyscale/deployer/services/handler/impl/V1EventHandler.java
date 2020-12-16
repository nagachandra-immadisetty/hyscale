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

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.ResourceFieldSelectorKey;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.core.model.ResourceOperation;
import com.github.srujankujmar.deployer.services.constants.DeployerConstants;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.handler.ResourceLifeCycleHandler;
import com.github.srujankujmar.deployer.services.util.ExceptionHelper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Event;
import io.kubernetes.client.openapi.models.V1EventList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Kubernetes events life cycle handler
 */

public class V1EventHandler implements ResourceLifeCycleHandler<V1Event> {

    private static final Logger LOGGER = LoggerFactory.getLogger(V1EventHandler.class);

    @Override
    public V1Event create(ApiClient apiClient, V1Event resource, String namespace) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.CREATE.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public boolean update(ApiClient apiClient, V1Event resource, String namespace) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.UPDATE.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public V1Event get(ApiClient apiClient, String name, String namespace) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.GET.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public List<V1Event> getBySelector(ApiClient apiClient, String selector, boolean label, String namespace) throws HyscaleException {
        CoreV1Api coreV1Api = new CoreV1Api(apiClient);
        List<V1Event> events = null;
        try {
            String labelSelector = label ? selector : null;
            String fieldSelector = label ? null : selector;

            V1EventList v1EventList = coreV1Api.listNamespacedEvent(namespace, DeployerConstants.TRUE, null, 
                    null, fieldSelector, labelSelector, null, null, null, null);

            events = v1EventList != null ? v1EventList.getItems() : null;
        } catch (ApiException e) {
            HyscaleException ex = ExceptionHelper.buildGetException(getKind(), e, ResourceOperation.GET_BY_SELECTOR);
            LOGGER.error("Error while listing events in namespace {}, with selectors {}, error {} ", namespace,
                    selector, ex.toString());
            throw ex;
        }
        return events;
    }

    @Override
    public boolean patch(ApiClient apiClient, String name, String namespace, V1Event body) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.PATCH.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public boolean delete(ApiClient apiClient, String name, String namespace, boolean wait) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.DELETE.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public boolean deleteBySelector(ApiClient apiClient, String selector, boolean label, String namespace, boolean wait) throws HyscaleException {
        HyscaleException hyscaleException = new HyscaleException(DeployerErrorCodes.OPERATION_NOT_SUPPORTED,
                ResourceOperation.DELETE_BY_SELECTOR.getOperation(), getKind());
        LOGGER.error(hyscaleException.getMessage());
        throw hyscaleException;
    }

    @Override
    public String getKind() {
        return ResourceKind.EVENT.getKind();
    }

    @Override
    public int getWeight() {
        return ResourceKind.EVENT.getWeight();
    }

    @Override
    public boolean cleanUp() {
        return false;
    }

    public enum EventFieldKey implements ResourceFieldSelectorKey {
        INVOLVED_OBJECT_NAME("involvedObject.name"),
        INVOLVED_OBJECT_NAMESPACE("involvedObject.namespace"),
        INVOLVED_OBJECT_UID("involvedObject.uid"),
        ;

        private String fieldName;

        EventFieldKey(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }
}
