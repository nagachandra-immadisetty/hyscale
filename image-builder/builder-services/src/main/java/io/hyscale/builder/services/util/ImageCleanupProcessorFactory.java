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
package com.github.srujankujmar.builder.services.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.builder.cleanup.services.ImageCleanupProcessor;
import com.github.srujankujmar.builder.cleanup.services.impl.DeleteAfterBuild;
import com.github.srujankujmar.builder.cleanup.services.impl.DeleteAllImages;
import com.github.srujankujmar.builder.cleanup.services.impl.PreserveAll;
import com.github.srujankujmar.builder.cleanup.services.impl.PreserveLastNUsed;
import com.github.srujankujmar.builder.core.models.ImageCleanUpPolicy;

@Component
public class ImageCleanupProcessorFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageCleanupProcessorFactory.class);

    @Autowired
    private DeleteAllImages deleteAllImages;
    
    @Autowired
    private DeleteAfterBuild deleteAfterBuild;
    
    @Autowired
    private PreserveLastNUsed preserveLastNUsed;
    
    @Autowired
    private PreserveAll preserveAll;

    public ImageCleanupProcessor getImageCleanupProcessor(String imageCleanUpPolicy) {
        ImageCleanUpPolicy cleanUpPolicy = ImageCleanUpPolicy.fromString(imageCleanUpPolicy);
        
        logger.debug("Image cleanup Policy {}", cleanUpPolicy);
        
        if (cleanUpPolicy == null) {
            cleanUpPolicy = ImageCleanUpPolicy.PRESERVE_N_RECENTLY_USED;
        }
        return getImageCleanupProcessor(cleanUpPolicy);
    }

    private ImageCleanupProcessor getImageCleanupProcessor(ImageCleanUpPolicy policy) {

        switch (policy) {

            case DELETE_AFTER_BUILD:
                return deleteAfterBuild;

            case PRESERVE_N_RECENTLY_USED:
                return preserveLastNUsed;

            case PRESERVE_ALL:
                return preserveAll;

            case DELETE_ALL:
                return deleteAllImages;

            default:
                return deleteAfterBuild;
        }
    }
}
