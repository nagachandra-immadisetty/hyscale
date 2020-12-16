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
package com.github.srujankujmar.controller.model;

import java.util.HashMap;
import java.util.Map;

import com.github.srujankujmar.commons.component.ComponentInvokerContext;
import com.github.srujankujmar.commons.models.AuthConfig;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

/**
 * Context information for workflow controller
 */
public class WorkflowContext extends ComponentInvokerContext {

    private ServiceSpec serviceSpec;
    private String namespace;
    private String appName;
    private String serviceName;
    private String envName;
    private AuthConfig authConfig;
    private Map<String, Object> attributes;

    WorkflowContext(String appName) {
        this.appName = appName;
        attributes = new HashMap<>();
    }

    public String getNamespace() {
        return namespace;
    }

    protected void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getAppName() {
        return appName;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public String getServiceName() {
        return serviceName;
    }

    protected void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEnvName() {
        return envName;
    }

    public ServiceSpec getServiceSpec() {
        return serviceSpec;
    }

    protected void setServiceSpec(ServiceSpec serviceSpec) {
        this.serviceSpec = serviceSpec;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    protected void setAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    protected void setEnvName(String envName) {
        this.envName = envName;
    }
}
