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
package com.github.srujankujmar.controller.invoker;

import com.github.srujankujmar.dockerfile.gen.services.exception.DockerfileErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.component.ComponentInvoker;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.DockerfileEntity;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.hooks.ServiceDirCleanUpHook;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.dockerfile.gen.services.model.DockerfileGenContext;
import com.github.srujankujmar.dockerfile.gen.services.generator.DockerfileGenerator;
import com.github.srujankujmar.servicespec.commons.exception.ServiceSpecErrorCodes;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

import javax.annotation.PostConstruct;

/**
 * This class invokes the @see {@link DockerfileGenerator} to generate the Dockerfile
 * It acts as a bridge between workflow controller and docker file generator
 * provides link between {@link WorkflowContext} and {@link DockerfileGenContext}
 * <p>
 * The registered hooks are executed as a part of component invocation
 */
@Component
public class DockerfileGeneratorComponentInvoker extends ComponentInvoker<WorkflowContext> {

    private static final Logger logger = LoggerFactory.getLogger(DockerfileGeneratorComponentInvoker.class);

    @Autowired
    private DockerfileGenerator dockerfileGenerator;

    @Autowired
    private ServiceDirCleanUpHook serviceDirCleanUpHook;

    @PostConstruct
    public void init() {
        super.addHook(serviceDirCleanUpHook);
    }

    @Override
    protected void doExecute(WorkflowContext context) throws HyscaleException {
        ServiceSpec serviceSpec = context.getServiceSpec();
        if (serviceSpec == null) {
            throw new HyscaleException(ServiceSpecErrorCodes.SERVICE_SPEC_REQUIRED);
        }
        WorkflowLogger.header(ControllerActivity.DOCKERFILE_GENERATION);
        DockerfileGenContext dockerfileContext = new DockerfileGenContext();

        dockerfileContext.setAppName(context.getAppName());
        try {
            dockerfileContext.setServiceName(serviceSpec.get(HyscaleSpecFields.name, String.class));
        } catch (HyscaleException e) {
            logger.error("Failed to get service name, error {}", e.toString());
            throw e;
        }
        try {
            DockerfileEntity dockerfileEntity = dockerfileGenerator.generateDockerfile(serviceSpec, dockerfileContext);
            context.addAttribute(WorkflowConstants.DOCKERFILE_ENTITY, dockerfileEntity);
            context.addAttribute(WorkflowConstants.STACK_AS_SERVICE_IMAGE,
                    dockerfileContext.isStackAsServiceImage());
            if (dockerfileEntity != null && dockerfileEntity.getDockerfile() != null) {
                context.addAttribute(WorkflowConstants.DOCKERFILE_INPUT,
                        dockerfileEntity.getDockerfile().getAbsolutePath());
            }
        } catch (HyscaleException e) {
            WorkflowLogger.error(ControllerActivity.DOCKERFILE_GENERATION_FAILED, e.getMessage());
            logger.error("Failed to generate dockerfile, error {}", e.toString());
            context.setFailed(true);
            throw e;
        }
    }

    @Override
    protected void onError(WorkflowContext context, HyscaleException he) throws HyscaleException {
        WorkflowLogger.header(ControllerActivity.ERROR);
        WorkflowLogger.error(ControllerActivity.CAUSE, he != null ?
                he.getMessage() : DockerfileErrorCodes.FAILED_TO_GENERATE_DOCKERFILE.getMessage());
        context.setFailed(true);
        context.addAttribute(WorkflowConstants.ERROR_MESSAGE, (he != null) ? he.getMessage() : DockerfileErrorCodes.FAILED_TO_GENERATE_DOCKERFILE.getMessage());
        if (he != null) {
            throw he;
        }
    }

}
