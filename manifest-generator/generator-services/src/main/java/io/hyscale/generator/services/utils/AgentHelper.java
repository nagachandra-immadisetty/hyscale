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

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Agent;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public abstract class AgentHelper {

    private static final Logger logger = LoggerFactory.getLogger(AgentHelper.class);

    public List<Agent> getAgents(ServiceSpec serviceSpec){
        TypeReference<List<Agent>> agentsList = new TypeReference<List<Agent>>() {
        };
        try {
            List<Agent> agents = serviceSpec.get(HyscaleSpecFields.agents, agentsList);
            return  agents;
        } catch (HyscaleException e) {
            logger.error("Error while fetching agents from service spec, returning null.",e);
            return null;
        }
    }
}
