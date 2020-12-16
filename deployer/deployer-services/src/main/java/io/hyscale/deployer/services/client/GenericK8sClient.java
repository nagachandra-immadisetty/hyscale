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
package com.github.srujankujmar.deployer.services.client;

import com.github.srujankujmar.commons.constants.K8SRuntimeConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.deployer.core.model.CustomResourceKind;
import com.github.srujankujmar.deployer.services.model.CustomListObject;
import com.github.srujankujmar.deployer.services.model.CustomObject;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.bouncycastle.util.Strings;

import java.util.List;


public abstract class GenericK8sClient {

    private ApiClient apiClient;
    protected String namespace;
    protected GenericKubernetesApi<CustomObject, CustomListObject> genericClient;

    protected GenericK8sClient(ApiClient apiClient){
        this.apiClient = apiClient;
        this.namespace = K8SRuntimeConstants.DEFAULT_NAMESPACE;
    }

    public GenericK8sClient withNamespace(String namespace){
        this.namespace = namespace;
        return this;
    }

    public GenericK8sClient forKind(CustomResourceKind resourceKind){
        String apiVersion = resourceKind.getApiVersion();
        this.genericClient = new GenericKubernetesApi<>(
                CustomObject.class, CustomListObject.class, getApiGroup(apiVersion),
                getApiVersion(apiVersion),
                resourceKind.getKind().toLowerCase() + "s",apiClient);

        return this;
    }

    public abstract void create(CustomObject resource) throws HyscaleException;

    public abstract void update(CustomObject resource) throws HyscaleException;

    public abstract boolean patch(CustomObject resource) throws HyscaleException;

    public abstract boolean delete(CustomObject resource);

    public abstract CustomObject get(CustomObject resource);

    public abstract CustomObject getResourceByName(String name);

    public abstract List<CustomObject> getAll();

    public abstract List<CustomObject> getBySelector(String selector);

    private String getApiGroup(String apiVersion) {
        if (apiVersion == null || apiVersion.equalsIgnoreCase("")) {
            return apiVersion;
        }
        String[] groups = Strings.split(apiVersion, '/');
        return groups.length == 2 ? groups[0] : "";
    }

    private static String getApiVersion(String apiVersion) {
        if (apiVersion == null || apiVersion.equalsIgnoreCase("")) {
            return apiVersion;
        }
        String[] groups = Strings.split(apiVersion, '/');
        return groups.length == 2 ? groups[1] : groups[0];
    }
}
