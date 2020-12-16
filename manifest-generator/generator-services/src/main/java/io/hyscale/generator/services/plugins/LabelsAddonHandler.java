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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.commons.models.ServiceMetadata;
import com.github.srujankujmar.generator.services.generator.MetadataManifestSnippetGenerator;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.plugin.framework.annotation.ManifestPlugin;
import com.github.srujankujmar.plugin.framework.handler.ManifestHandler;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.plugin.framework.util.JsonSnippetConvertor;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

@Component
@ManifestPlugin(name = "LabelsAddonHandler")
public class LabelsAddonHandler implements ManifestHandler {

    private static final Logger logger = LoggerFactory.getLogger(LabelsAddonHandler.class);

    @Override
    public List<ManifestSnippet> handle(ServiceSpec serviceSpec, ManifestContext manifestContext)
            throws HyscaleException {
        Map<String, String> addOnLabels = manifestContext.getCustomLabels();

        if (addOnLabels == null || addOnLabels.isEmpty()) {
            return Collections.emptyList();
        }
        logger.debug("Started LabelsAddonHandler");
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setAppName(manifestContext.getAppName());
        serviceMetadata.setEnvName(manifestContext.getEnvName());
        serviceMetadata.setServiceName(serviceSpec.get(HyscaleSpecFields.name, String.class));
        List<ManifestSnippet> snippetList = new ArrayList<>();
        try {
            for (ManifestResource manifestResource : ManifestResource.values()) {
                if (manifestResource.getPredicate().test(serviceSpec)) {
                    // Get existing metadata and update labels in it
                    ManifestSnippet snippet = MetadataManifestSnippetGenerator.getMetaData(manifestResource, serviceMetadata);
                    V1ObjectMeta v1ObjectMeta = JsonSnippetConvertor.deserialize(snippet.getSnippet(), V1ObjectMeta.class);
                    Map<String, String> labels = v1ObjectMeta.getLabels();
                    if (labels == null) {
                        labels = new HashMap<String, String>();
                    }
                    labels.putAll(addOnLabels);
                    v1ObjectMeta.setLabels(labels);
                    snippet.setSnippet(JsonSnippetConvertor.serialize(v1ObjectMeta));
                    snippetList.add(snippet);
                }
            }
        } catch (IOException e) {
            logger.error("Error while processing metadata labels snippet.", e);
        }
        logger.debug("Completed LabelsAddonHandler");
        return snippetList;
    }
}
