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
package com.github.srujankujmar.generator.services.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.constants.ManifestGenConstants;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.generator.services.model.PodChecksum;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.plugin.framework.util.JsonSnippetConvertor;
import com.github.srujankujmar.servicespec.commons.model.service.MapBasedSecrets;
import com.github.srujankujmar.servicespec.commons.model.service.Secrets;
import com.github.srujankujmar.servicespec.commons.model.service.SecretType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;

public class SecretsDataUtil {

    private static final Logger logger = LoggerFactory.getLogger(SecretsDataUtil.class);
    
    private SecretsDataUtil() {}

    public static ManifestSnippet build(Secrets secrets, String secretsVolumePath, String fileName) throws JsonProcessingException{
        ManifestSnippet snippet = new ManifestSnippet();
        if(secrets == null) {
            return null;
        }
        if(secrets.getType() == SecretType.MAP){
            MapBasedSecrets mapBasedSecrets = (MapBasedSecrets) secrets;
            Map<String, String> modifiedMap = mapBasedSecrets.entrySet().stream().collect(
                    Collectors.toMap(key -> key.getKey(), value -> Base64.encodeBase64String(value.getValue().getBytes())));

            if (StringUtils.isNotBlank(secretsVolumePath)) {
                logger.debug("Writing secrets into file {}.",secretsVolumePath);
                StringBuilder stringBuilder = new StringBuilder();
                mapBasedSecrets.entrySet().stream().forEach(each -> {
                    stringBuilder.append(each.getKey()).append("=").append(each.getValue()).append("\n");
                });
                modifiedMap.put(fileName,
                        Base64.encodeBase64String(stringBuilder.toString().getBytes()));
            }
            snippet.setSnippet(JsonSnippetConvertor.serialize(modifiedMap));
            snippet.setKind(ManifestResource.SECRET.getKind());
            snippet.setPath("data");
            return snippet;
        }
        return null;
    }
    
    public static void updatePodChecksum(ManifestSnippet secretsSnippet, ManifestContext manifestContext,
            String agentName) {
        if (secretsSnippet == null || StringUtils.isBlank(secretsSnippet.getSnippet())) {
            return;
        }
        Object podChecksumObj = manifestContext.getGenerationAttribute(ManifestGenConstants.POD_CHECKSUM);
        PodChecksum podChecksum = podChecksumObj == null ? new PodChecksum() : (PodChecksum) podChecksumObj;

        if (StringUtils.isBlank(agentName)) {
            podChecksum.setSecret(secretsSnippet.getSnippet());
        } else {
            podChecksum.addAgentSecret(agentName, secretsSnippet.getSnippet());
        }
        manifestContext.addGenerationAttribute(ManifestGenConstants.POD_CHECKSUM, podChecksum);
    }
}
