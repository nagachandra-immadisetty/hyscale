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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.builder.core.models.ImageBuilderActivity;
import com.github.srujankujmar.commons.commands.CommandExecutor;
import com.github.srujankujmar.commons.commands.provider.ImageCommandProvider;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.LoggerTags;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.commons.validator.Validator;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.servicespec.commons.util.ImageUtil;

/**
 * Validates if docker is installed as well as running
 * In case docker is not required,
 * skips validation and returns true
 */
@Component
public class DockerDaemonValidator implements Validator<WorkflowContext> {
    private static final Logger logger = LoggerFactory.getLogger(DockerDaemonValidator.class);

    @Autowired
    private ImageCommandProvider commandProvider;

    private boolean isDockerAvailable = false;
    
    private boolean isDockerUnavailable = false;

    /**
     * 1. It will check that spec has buildspec or dockerfile 
     * 2. If both is not then it will return true
     * 3. If any one is there then 
     *    3.1  It will verify that docker is installed or not
     *    3.2  It will run docker command 
     *    3.3  if command executed successfully then return true else false
     */
    @Override
    public boolean validate(WorkflowContext context) throws HyscaleException {
        if (isDockerAvailable) {
            return isDockerAvailable;
        }
        if (isDockerUnavailable) {
            return false;
        }

        if (!ImageUtil.isImageBuildPushRequired(context.getServiceSpec())) {
            return true;
        }
        String command = commandProvider.dockerVersion();
        logger.debug("Docker Installed check command: {}", command);
        boolean success = CommandExecutor.execute(command);
        if (!success) {
            WorkflowLogger.persist(ImageBuilderActivity.DOCKER_NOT_INSTALLED, LoggerTags.ERROR);
            isDockerUnavailable = true;
            return false;
        }
        command = commandProvider.dockerImages();
        logger.debug("Docker Daemon running check command: {}", command);
        success = CommandExecutor.execute(command);
        if (!success) {
            WorkflowLogger.persist(ImageBuilderActivity.DOCKER_DAEMON_NOT_RUNNING, LoggerTags.ERROR);
            isDockerUnavailable = true;
            return false;
        }
        isDockerAvailable = true;
        return isDockerAvailable;
    }
}
