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

import javax.annotation.PostConstruct;

import com.github.srujankujmar.builder.services.exception.ImageBuilderErrorCodes;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import com.github.srujankujmar.controller.manager.RegistryManager;
import com.github.srujankujmar.controller.model.WorkflowContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.builder.core.models.BuildContext;
import com.github.srujankujmar.commons.component.ComponentInvoker;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.DockerfileEntity;
import com.github.srujankujmar.controller.hooks.ImageCleanUpHook;
import com.github.srujankujmar.builder.services.service.ImageBuildPushService;
import com.github.srujankujmar.servicespec.commons.exception.ServiceSpecErrorCodes;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

/**
 *	Image builder component acts as a bridge between workflow controller and image-builder
 *	for image build and push operation provides link between
 *	{@link WorkflowContext} and {@link BuildContext}
 */
@Component
public class ImageBuildComponentInvoker extends ComponentInvoker<WorkflowContext> {

    @Autowired
    private ImageBuildPushService imageBuildService;

    @Autowired
    private RegistryManager registryManager;

    @Autowired
    private ImageCleanUpHook imageCleanUpHook;

    @PostConstruct
    public void init() {
        super.addHook(imageCleanUpHook);
    }

    private static final Logger logger = LoggerFactory.getLogger(ImageBuildComponentInvoker.class);

    @Override
    protected void doExecute(WorkflowContext context) throws HyscaleException {
        if (context == null || context.isFailed()) {
            return;
        }
        ServiceSpec serviceSpec = context.getServiceSpec();
        if (serviceSpec == null) {
            context.setFailed(true);
            logger.error(" Cannot build image for empty service spec");
            throw new HyscaleException(ServiceSpecErrorCodes.SERVICE_SPEC_REQUIRED);
        }
        String serviceName;
        try {
            serviceName = serviceSpec.get(HyscaleSpecFields.name, String.class);
        } catch (HyscaleException e) {
            logger.error("Failed to get service name, error {}", e.toString());
            throw e;
        }

        String appName = context.getAppName();
        WorkflowLogger.header(ControllerActivity.BUILD_AND_PUSH);

        // BuildContext according to imagebuilder
        BuildContext buildContext = new BuildContext();
        buildContext.setAppName(appName);
        buildContext.setServiceName(serviceName);
        Boolean stackAsServiceImage = (Boolean) context.getAttribute(WorkflowConstants.STACK_AS_SERVICE_IMAGE);
        buildContext.setStackAsServiceImage(stackAsServiceImage == null ? false : stackAsServiceImage);
        buildContext.setVerbose((Boolean) context.getAttribute(WorkflowConstants.VERBOSE));
        buildContext.setImageRegistry(registryManager.getImageRegistry(serviceSpec
                .get(HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.registry), String.class)));

        DockerfileEntity dockerfileEntity = (DockerfileEntity) context
                .getAttribute(WorkflowConstants.DOCKERFILE_ENTITY);
        buildContext.setDockerfileEntity(dockerfileEntity);

        try {
            imageBuildService.buildAndPush(serviceSpec, buildContext);
        } catch (HyscaleException e) {
            logger.error("Error while build and push for service: {}", serviceName, e);
            context.setFailed(true);
            throw e;
        } finally {
            context.addAttribute(WorkflowConstants.IMAGE_SHA_SUM,
                    buildContext.getImageShaSum());
            context.addAttribute(WorkflowConstants.BUILD_LOGS,
                    buildContext.getBuildLogs());
            context.addAttribute(WorkflowConstants.PUSH_LOGS,
                    buildContext.getPushLogs());
        }
    }

    @Override
    protected void onError(WorkflowContext context, HyscaleException he) throws HyscaleException {
        WorkflowLogger.header(ControllerActivity.ERROR);
        WorkflowLogger.error(ControllerActivity.CAUSE, he != null ? he.getMessage() : ImageBuilderErrorCodes.FAILED_TO_BUILD_AND_PUSH_IMAGE.getErrorMessage());
        context.setFailed(true);
        if (he != null) {
            throw he;
        }
    }
}
