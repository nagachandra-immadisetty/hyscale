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
package com.github.srujankujmar.controller.validator.impl;

import com.github.srujankujmar.commons.exception.CommonErrorCode;
import com.github.srujankujmar.commons.io.StructuredOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.LoggerTags;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.AuthConfig;
import com.github.srujankujmar.commons.validator.Validator;
import com.github.srujankujmar.controller.activity.ValidatorActivity;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.deployer.services.deployer.Deployer;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Validate cluster information using {@link AuthConfig}
 * provided by {@link WorkflowContext}
 * Checks if access to cluster is allowed.
 */
@Component
public class ClusterValidator implements Validator<WorkflowContext> {

    private static final Logger logger = LoggerFactory.getLogger(ClusterValidator.class);

    @Autowired
    private Deployer deployer;

    @Autowired
    private StructuredOutputHandler outputHandler;

    private Map<AuthConfig, Boolean> clusterValidationMap;

    @PostConstruct
    public void init() {
        clusterValidationMap = new HashMap<>();
    }

    @Override
    public boolean validate(WorkflowContext context) throws HyscaleException {

        if (clusterValidationMap.containsKey(context.getAuthConfig())) {
            return clusterValidationMap.get(context.getAuthConfig());
        }
        long startTime = System.currentTimeMillis();
        boolean isClusterValid = false;
        logger.debug("Starting K8s cluster validation");
        try {
            isClusterValid = deployer.authenticate(context.getAuthConfig());
            clusterValidationMap.put(context.getAuthConfig(), isClusterValid);
        } catch (HyscaleException ex) {
            logger.error("Error while validating cluster", ex);
            clusterValidationMap.put(context.getAuthConfig(), false);
            throw ex;
        } finally {
            if (!isClusterValid) {
                WorkflowLogger.persist(ValidatorActivity.CLUSTER_AUTHENTICATION_FAILED, LoggerTags.ERROR);
                if(WorkflowLogger.isDisabled()){
                    outputHandler.addErrorMessage(CommonErrorCode.FAILED_TO_CONNECT_TO_CLUSTER.getMessage());
                }
            }
        }
        logger.debug("Is K8s cluster valid: {}. Time taken: {}", isClusterValid, System.currentTimeMillis() - startTime);
        return isClusterValid;
    }
}
