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
import com.github.srujankujmar.generator.services.utils.SecretsDataUtil;
import com.github.srujankujmar.plugin.framework.annotation.ManifestPlugin;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.io.HyscaleFilesUtil;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.generator.services.model.ServiceMetadata;
import com.github.srujankujmar.plugin.framework.handler.ManifestHandler;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Secrets;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ManifestPlugin(name = "SecretsDataHandler")
public class SecretsDataHandler implements ManifestHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecretsDataHandler.class);

    @Override
    public List<ManifestSnippet> handle(ServiceSpec serviceSpec, ManifestContext manifestContext) throws HyscaleException {
        Secrets secrets = serviceSpec.get(HyscaleSpecFields.secrets, Secrets.class);
        if (!ManifestResource.SECRET.getPredicate().test(serviceSpec)) {
            return null;
        }

        String secretsVolumePath = serviceSpec.get(HyscaleSpecFields.secretsVolumePath, String.class);
        ServiceMetadata serviceMetadata = new ServiceMetadata();
        serviceMetadata.setAppName(manifestContext.getAppName());
        serviceMetadata.setEnvName(manifestContext.getEnvName());
        serviceMetadata.setServiceName(serviceSpec.get(HyscaleSpecFields.name, String.class));

        List<ManifestSnippet> manifestSnippetList = new ArrayList<>();
        try {

            manifestSnippetList.add(getSecretsData(secrets, secretsVolumePath));
        } catch (JsonProcessingException e) {
            logger.error("Error while generating manifest for secrets of service {}", serviceMetadata.getServiceName(), e);
        }
        return manifestSnippetList;

    }


    private ManifestSnippet getSecretsData(Secrets secrets, String secretsVolumePath)
            throws JsonProcessingException {
        ManifestSnippet snippet = SecretsDataUtil.build(secrets, secretsVolumePath, ManifestGenConstants.DEFAULT_SECRETS_FILE);
        return snippet;
    }
}
