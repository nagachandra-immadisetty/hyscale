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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.Activity;
import com.github.srujankujmar.commons.models.HyscaleSpecType;
import com.github.srujankujmar.controller.activity.ValidatorActivity;
import com.github.srujankujmar.controller.util.ServiceProfileUtil;
import com.github.srujankujmar.controller.validator.SpecSchemaValidator;
import com.github.srujankujmar.servicespec.commons.activity.ServiceSpecActivity;

/**
 * Provides Profile spec schema related implementation to {@link SpecSchemaValidator}
 *
 */
@Component
public class HprofSchemaValidator extends SpecSchemaValidator {

    private static final Logger logger = LoggerFactory.getLogger(HprofSchemaValidator.class);

    @Override
    public HyscaleSpecType getReferenceSchemaType() {
        return HyscaleSpecType.PROFILE;
    }

    @Override
    public boolean validateData(File profileFile) throws HyscaleException {
        String profileFileName = profileFile.getName().split("\\.")[0];
        int dashIndex = profileFileName.indexOf(ToolConstants.DASH);
        if (dashIndex < 0) {
            logger.warn(ServiceSpecActivity.PROFILE_NAME_MISMATCH.getActivityMessage(), profileFile.getName());
            WorkflowLogger.persist(ServiceSpecActivity.PROFILE_NAME_MISMATCH, profileFile.getName());
        }
        StringBuilder profileNameBuilder = new StringBuilder();
        profileNameBuilder.append(ServiceProfileUtil.getProfileName(profileFile)).append(ToolConstants.DASH)
                .append(ServiceProfileUtil.getServiceNameFromProfile(profileFile));
        if (!profileFileName.equals(profileNameBuilder.toString())) {
            logger.warn(ServiceSpecActivity.PROFILE_NAME_MISMATCH.getActivityMessage(), profileFile.getName());
            WorkflowLogger.persist(ServiceSpecActivity.PROFILE_NAME_MISMATCH, profileFile.getName());
        }
        return true;
    }
    
    @Override
    protected Activity getActivity() {
        return ValidatorActivity.PROFILE_VALIDATION_FAILED;
    }

}
