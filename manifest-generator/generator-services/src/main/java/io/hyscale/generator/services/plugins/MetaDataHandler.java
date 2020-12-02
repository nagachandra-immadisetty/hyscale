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
import com.github.srujankujmar.generator.services.constants.ManifestGenConstants;
import com.github.srujankujmar.generator.services.predicates.ManifestPredicates;
import com.github.srujankujmar.plugin.framework.annotation.ManifestPlugin;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.generator.services.model.ServiceMetadata;
import com.github.srujankujmar.generator.services.generator.MetadataManifestSnippetGenerator;
import com.github.srujankujmar.plugin.framework.handler.ManifestHandler;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ManifestPlugin(name = "MetaDataHandler")
public class MetaDataHandler implements ManifestHandler {

    private static final Logger logger = LoggerFactory.getLogger(MetaDataHandler.class);

    @Override
    public List<ManifestSnippet> handle(ServiceSpec serviceSpec, ManifestContext context) throws HyscaleException {
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setAppName(context.getAppName());
        serviceMetadata.setEnvName(context.getEnvName());
        serviceMetadata.setServiceName(serviceSpec.get(HyscaleSpecFields.name, String.class));
        List<ManifestSnippet> snippetList = new ArrayList<>();
        try {
            for (ManifestResource manifestResource : ManifestResource.values()) {

                if (manifestResource.getPredicate().test(serviceSpec)) {
                    logger.debug("Creating metadata for resource {}.",manifestResource.getKind());
                    /* Snippet for kind for each manifest */
                    snippetList.add(MetadataManifestSnippetGenerator.getKind(manifestResource));

                    /* Snippet for apiVersion for each manifest */
                    snippetList.add(MetadataManifestSnippetGenerator.getApiVersion(manifestResource, serviceMetadata));

                    /* Snippet for metadata for each manifest */
                    snippetList.add(MetadataManifestSnippetGenerator.getMetaData(manifestResource, serviceMetadata));

                }

            }
            context.addGenerationAttribute(ManifestGenConstants.POD_SPEC_OWNER, ManifestPredicates.getVolumesPredicate().test(serviceSpec) ? ManifestResource.STATEFUL_SET.getKind() :
                    ManifestResource.DEPLOYMENT.getKind());
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing metadata snippet.", e);
        }
        return snippetList;
    }


}
