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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.AnnotationKey;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.constants.ManifestGenConstants;
import com.github.srujankujmar.generator.services.model.PodChecksum;
import com.github.srujankujmar.generator.services.utils.ChecksumProvider;
import com.github.srujankujmar.plugin.framework.annotation.ManifestPlugin;
import com.github.srujankujmar.plugin.framework.handler.ManifestHandler;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.plugin.framework.util.JsonSnippetConvertor;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

@Component
@ManifestPlugin(name = "PodAnnotationHandler")
public class PodAnnotationHandler implements ManifestHandler {

    private static final Logger logger = LoggerFactory.getLogger(PodAnnotationHandler.class);

    @Override
    public List<ManifestSnippet> handle(ServiceSpec serviceSpec, ManifestContext manifestContext)
            throws HyscaleException {
        logger.debug("Executing annotation handler");
        Object podChecksumObj = manifestContext.getGenerationAttribute(ManifestGenConstants.POD_CHECKSUM);
        if (podChecksumObj == null) {
            return Collections.emptyList();
        }
        PodChecksum podChecksum = (PodChecksum) podChecksumObj;
        ChecksumProvider<PodChecksum> checkSumProvider = new ChecksumProvider<PodChecksum>(podChecksum);
        Map<String, String> annotations = new HashMap<String, String>();
        annotations.put(AnnotationKey.CHECKSUM.getAnnotation(), checkSumProvider.getChecksum());
        String podSpecOwner = ((String) manifestContext.getGenerationAttribute(ManifestGenConstants.POD_SPEC_OWNER));
        ManifestSnippet manifestSnippet = new ManifestSnippet();
        try {
            manifestSnippet.setKind(podSpecOwner);
            manifestSnippet.setPath("spec.template.metadata.annotations");
            manifestSnippet.setSnippet(JsonSnippetConvertor.serialize(annotations));
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing pod annotations", e);
        }
        
        return Arrays.asList(manifestSnippet);
    }

}
