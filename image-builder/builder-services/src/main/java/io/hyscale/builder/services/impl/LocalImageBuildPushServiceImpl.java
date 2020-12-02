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
package com.github.srujankujmar.builder.services.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.builder.cleanup.services.ImageCleanupProcessor;
import com.github.srujankujmar.builder.core.models.BuildContext;
import com.github.srujankujmar.builder.core.models.ImageBuilderActivity;
import com.github.srujankujmar.builder.services.config.ImageBuilderConfig;
import com.github.srujankujmar.builder.services.exception.ImageBuilderErrorCodes;
import com.github.srujankujmar.builder.services.service.ImageBuildPushService;
import com.github.srujankujmar.builder.services.util.ImageCleanupProcessorFactory;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Dockerfile;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

@Component
public class LocalImageBuildPushServiceImpl implements ImageBuildPushService {

    private static final Logger logger = LoggerFactory.getLogger(LocalImageBuildPushServiceImpl.class);

    @Autowired
    private LocalImageBuildServiceImpl buildService;

    @Autowired
    private ImageCleanupProcessorFactory imageCleanupProcessorFactory;

    @Autowired
    private LocalImagePushServiceImpl pushService;
    
    @Autowired
    private ImageBuilderConfig imageBuilderConfig;

    @Override
    public void buildAndPush(ServiceSpec serviceSpec, BuildContext context) throws HyscaleException {
        if (validate(serviceSpec) && isImageBuildPushRequired(serviceSpec, context)) {
            context = buildService.build(serviceSpec, context);
            pushService.pushImage(serviceSpec, context);
            String imageCleanUpPolicy = imageBuilderConfig.getImageCleanUpPolicy();
            ImageCleanupProcessor imageCleanupProcessor = imageCleanupProcessorFactory.getImageCleanupProcessor(imageCleanUpPolicy);
            logger.debug("Image clean up processor used {}", imageCleanupProcessor.getClass());
            if (imageCleanupProcessor != null) {
                imageCleanupProcessor.clean(serviceSpec);
            }

        } else {
            WorkflowLogger.startActivity(ImageBuilderActivity.IMAGE_BUILD_PUSH);
            WorkflowLogger.endActivity(Status.SKIPPING);
        }
    }

    /**
     * Not required if dockerSpec and dockerfile are not available.
     * In case its just a stack image, need to push only
     *
     * @param serviceSpec
     * @param context
     * @return boolean
     * @throws HyscaleException
     */
    private boolean isImageBuildPushRequired(ServiceSpec serviceSpec, BuildContext context) throws HyscaleException {

        if (context.isStackAsServiceImage()) {
            return true;
        }
        // No dockerfile
        if ((context.getDockerfileEntity() == null || context.getDockerfileEntity().getDockerfile() == null)
                && (serviceSpec.get(HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.dockerfile),
                Dockerfile.class) == null)) {
            return false;
        }

        return true;
    }

    private boolean validate(ServiceSpec serviceSpec) throws HyscaleException {
        String imageName = serviceSpec.get(HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.name), String.class);
        if (StringUtils.isBlank(imageName)) {
            throw new HyscaleException(ImageBuilderErrorCodes.CANNOT_RESOLVE_IMAGE_NAME);
        }
        return true;
    }

}
