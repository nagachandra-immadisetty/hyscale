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
package com.github.srujankujmar.builder.cleanup.services.impl;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.builder.cleanup.services.ImageCleanupProcessor;
import com.github.srujankujmar.builder.services.config.ImageBuilderConfig;
import com.github.srujankujmar.commons.commands.CommandExecutor;
import com.github.srujankujmar.commons.commands.provider.ImageCommandProvider;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import com.github.srujankujmar.servicespec.commons.util.ImageUtil;

/**
 * This class preserves the last 'n' service images built by hyscale, where
 * (n = {@link com.github.srujankujmar.builder.services.config.ImageBuilderConfig#getNoOfPreservedImages() }).
 * Hyscale adds a label to the image as imageowner = hyscale. This clean up happends on all
 * those images which are tagged with the label imageowner=hyscale
 * <p>
 * docker rmi $(docker images <serviceimage> --filter label=imageowner=hyscale -q)
 */

@Component
public class PreserveLastNUsed implements ImageCleanupProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(PreserveLastNUsed.class);

    @Autowired
    private ImageCommandProvider imageCommandProvider;

    @Autowired
    private ImageBuilderConfig imageBuilderConfig;

    @Override
    public void clean(ServiceSpec serviceSpec) {
        logger.debug("Cleaning up least recently used images");
        String image = null;
        try {
            image = ImageUtil.getImageWithoutTag(serviceSpec);
        } catch (HyscaleException e) {
            logger.error("Error while fetching image from service spec to clean up service images", e);
        }
        if (StringUtils.isNotBlank(image)) {
            // Fetch the image id's to be deleted of the service image which are labelled by imageowner=hyscale
            String existingImageIds = CommandExecutor.executeAndGetResults(imageCommandProvider.dockerImageByNameFilterByImageOwner(image)).
                    getCommandOutput();
            String[] imgIds = StringUtils.isNotBlank(existingImageIds)? existingImageIds.split("\\s+") : null;
            if (imgIds == null || imgIds.length == 0) {
                logger.debug("No images found to clean from the host machine");
                return;
            }
            // Need to preserve the order of output ,hence a LinkedHashset
            Set<String> imageIds = new LinkedHashSet<>(Arrays.asList(imgIds));
            logger.debug("Removing images: {}", imageIds);
            // delete those image id's which are older than 'n' (imageBuilderConfig.getNoOfPreservedImages())
            if (imageIds.size() > imageBuilderConfig.getNoOfPreservedImages()) {
                CommandExecutor.execute(imageCommandProvider.removeDockerImages(
                        imageIds.stream().skip(imageBuilderConfig.getNoOfPreservedImages()).collect(Collectors.toSet())));
            }
        }
    }
}
