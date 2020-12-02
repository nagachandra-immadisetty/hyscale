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
package com.github.srujankujmar.generator.services.builder;

import com.github.srujankujmar.commons.models.ResourceLabelKey;
import com.github.srujankujmar.commons.utils.ResourceLabelBuilder;
import com.github.srujankujmar.generator.services.model.ServiceMetadata;

import java.util.Map;
import java.util.stream.Collectors;

//TODO Normaliza label as per regex (([A-Za-z0-9][-A-Za-z0-9_.]*)?[A-Za-z0-9])?
public class DefaultLabelBuilder {

    public static Map<String, String> build(ServiceMetadata serviceMetadata) {
        Map<ResourceLabelKey, String> resourceLabelMap = ResourceLabelBuilder.build(serviceMetadata.getAppName(), serviceMetadata.getEnvName(),
                serviceMetadata.getServiceName());
        if (resourceLabelMap != null && !resourceLabelMap.isEmpty()) {
            Map<String, String> defaultLabels = resourceLabelMap.entrySet().stream().filter(each -> {
                return each != null;
            }).collect(Collectors.toMap(k -> k.getKey().getLabel(), v -> v.getValue()));
            return defaultLabels;
        }
        return null;
    }

    public static Map<String, String> build(String appName, String envName, String serviceName) {
        Map<ResourceLabelKey, String> resourceLabelMap = ResourceLabelBuilder.build(appName, envName,
                serviceName);
        if (resourceLabelMap != null && !resourceLabelMap.isEmpty()) {
            Map<String, String> defaultLabels = resourceLabelMap.entrySet().stream().filter(each -> {
                return each != null;
            }).collect(Collectors.toMap(k -> k.getKey().getLabel(), v -> v.getValue()));
            return defaultLabels;
        }
        return null;
    }

    public static Map<String, String> build(String appName, String envName) {
        Map<ResourceLabelKey, String> resourceLabelMap = ResourceLabelBuilder.build(appName, envName);
        if (resourceLabelMap != null && !resourceLabelMap.isEmpty()) {
            Map<String, String> defaultLabels = resourceLabelMap.entrySet().stream().filter(each -> {
                return each != null;
            }).collect(Collectors.toMap(k -> k.getKey().getLabel(), v -> v.getValue()));
            return defaultLabels;
        }
        return null;
    }

}
