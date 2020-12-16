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
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.constants.ManifestGenConstants;
import com.github.srujankujmar.generator.services.generator.K8sResourceNameGenerator;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.generator.services.predicates.ManifestPredicates;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.plugin.framework.util.JsonSnippetConvertor;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Agent;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import io.kubernetes.client.openapi.models.V1ConfigMapVolumeSource;
import io.kubernetes.client.openapi.models.V1SecretVolumeSource;
import io.kubernetes.client.openapi.models.V1Volume;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Volumes manifest snippet builder for agents.
 * <p>
 * This class is responsible for building manifest snippets based on volumes
 * belonging to agents.
 * </p>
 *
 */
@Component
public class AgentVolumesBuilder extends AgentHelper implements AgentBuilder {
    @Autowired
    AgentManifestNameGenerator agentManifestNameGenerator;
    
    @Override
    public List<ManifestSnippet> build(ManifestContext manifestContext, ServiceSpec serviceSpec) throws JsonProcessingException, HyscaleException {
        String podSpecOwner = ((String) manifestContext.getGenerationAttribute(ManifestGenConstants.POD_SPEC_OWNER));
        List<ManifestSnippet> volumeSnippets = new ArrayList<ManifestSnippet>();
        List<Agent> agents = getAgents(serviceSpec);
        String serviceName = serviceSpec.get(HyscaleSpecFields.name,String.class);
        if(agents == null){
            return volumeSnippets;
        }
        ManifestSnippet volumeSnippet = new ManifestSnippet();
        volumeSnippet.setKind(podSpecOwner);
        volumeSnippet.setPath("spec.template.spec.volumes");
        List<V1Volume> volumeList = new ArrayList<V1Volume>();
        for (Agent agent : agents) {
            if (agent.getProps() != null && !agent.getProps().isEmpty()) {
                V1Volume volume = new V1Volume();
                String configMapName = agentManifestNameGenerator.generateConfigMapName(agent.getName(),serviceName);
                volume.setName(K8sResourceNameGenerator.getResourceVolumeName(configMapName, ManifestResource.CONFIG_MAP.getKind()));
                V1ConfigMapVolumeSource v1ConfigMapVolumeSource = new V1ConfigMapVolumeSource();
                v1ConfigMapVolumeSource.setName(configMapName);
                volume.setConfigMap(v1ConfigMapVolumeSource);
                volumeList.add(volume);
            }
            if (agent.getSecrets() != null) {
                V1Volume volume = new V1Volume();
                String secretName = agentManifestNameGenerator.generateSecretName(agent.getName(),serviceName);
                volume.setName(K8sResourceNameGenerator.getResourceVolumeName(secretName, ManifestResource.SECRET.getKind()));
                V1SecretVolumeSource v1SecretVolumeSource = new V1SecretVolumeSource();
                v1SecretVolumeSource.secretName(secretName);
                volume.setSecret(v1SecretVolumeSource);
                volumeList.add(volume);
            }
        }
        volumeSnippet.setSnippet(JsonSnippetConvertor.serialize(volumeList));
        volumeSnippets.add(volumeSnippet);
        return volumeSnippets;
    }
}
