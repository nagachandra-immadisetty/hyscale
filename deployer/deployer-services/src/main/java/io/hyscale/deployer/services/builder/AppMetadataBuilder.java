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
package com.github.srujankujmar.deployer.services.builder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.utils.ResourceLabelUtil;
import com.github.srujankujmar.deployer.core.model.AppMetadata;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.services.model.PodParent;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1StatefulSet;

@Component
public class AppMetadataBuilder {

    /**
     * Gets app and service name from labels of pod parent generated by hyscale
     * For pod controller not deployed with hyscale AppMetadata with only namespace will be returned
     * @param podParentList
     * @return List of {@link AppMetadata} containing details of deployed apps
     */
    public List<AppMetadata> build(List<PodParent> podParentList) {
        Map<String, AppMetadata> mapping = new HashMap<>();

        podParentList.forEach(podParent -> {
            V1ObjectMeta metadata = null;
            
            if (ResourceKind.DEPLOYMENT.getKind().equalsIgnoreCase(podParent.getKind())) {
                metadata = ((V1Deployment)podParent.getParent()).getMetadata();
            }
            if (ResourceKind.STATEFUL_SET.getKind().equalsIgnoreCase(podParent.getKind())) {
                metadata = ((V1StatefulSet)podParent.getParent()).getMetadata();
            }
            String namespace = metadata.getNamespace();
            String appName = ResourceLabelUtil.getAppName(metadata.getLabels());
            String serviceName = ResourceLabelUtil.getServiceName(metadata.getLabels());
            String envName = ResourceLabelUtil.getEnvName(metadata.getLabels());

            if (mapping.get(namespace) == null) {
                AppMetadata appData = new AppMetadata();
                appData.setNamespace(namespace);
                mapping.put(namespace, appData);
            }
            if (StringUtils.isBlank(appName) || StringUtils.isBlank(serviceName)) {
                return;
            }
            // One namespace can have only one app
            mapping.get(namespace).setAppName(appName);
            if (StringUtils.isNotBlank(envName)) {
                mapping.get(namespace).setEnvName(envName);
            }
            if (mapping.get(namespace).getServices() == null
                    || !mapping.get(namespace).getServices().contains(serviceName)) {
                mapping.get(namespace).addServices(serviceName);
            }
        });
        
        return mapping.values().stream().collect(Collectors.toList());
    }

}
