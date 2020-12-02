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
package com.github.srujankujmar.generator.services.plugins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.srujankujmar.generator.services.utils.ConfigMapDataUtil;
import com.github.srujankujmar.plugin.framework.annotation.ManifestPlugin;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.model.ServiceMetadata;
import com.github.srujankujmar.generator.services.predicates.ManifestPredicates;
import com.github.srujankujmar.generator.services.provider.PropsProvider;
import com.github.srujankujmar.plugin.framework.handler.ManifestHandler;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Props;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ManifestPlugin(name = "ConfigMapDataHandler")
public class ConfigMapDataHandler implements ManifestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConfigMapDataHandler.class);

    @Override
    public List<ManifestSnippet> handle(ServiceSpec serviceSpec, ManifestContext manifestContext) throws HyscaleException {
        Props props = PropsProvider.getProps(serviceSpec);
        if (!ManifestPredicates.getPropsPredicate().test(serviceSpec)) {
            logger.debug("Props found to be empty while processing ConfigMap data.");
            return null;
        }
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setAppName(manifestContext.getAppName());
        serviceMetadata.setEnvName(manifestContext.getEnvName());
        serviceMetadata.setServiceName(serviceSpec.get(HyscaleSpecFields.name, String.class));

        String propsVolumePath = serviceSpec.get(HyscaleSpecFields.propsVolumePath, String.class);

        List<ManifestSnippet> manifestSnippetList = new ArrayList<>();
        try {
            manifestSnippetList.addAll(ConfigMapDataUtil.build(props,propsVolumePath));
            logger.debug("Added ConfigMap map data to the manifest snippet list");
        } catch (JsonProcessingException e) {
            logger.error("Error while generating manifest for props of service {}", serviceMetadata.getServiceName(), e);
        }
        return manifestSnippetList;
    }
}
