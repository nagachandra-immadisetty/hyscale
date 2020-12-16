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

import java.util.List;
import java.util.stream.Collectors;

import com.github.srujankujmar.commons.io.StructuredOutputHandler;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.LoggerTags;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.validator.Validator;
import com.github.srujankujmar.controller.activity.ValidatorActivity;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;
import com.github.srujankujmar.servicespec.commons.model.service.Volume;

@Component
public class ManifestValidator implements Validator<WorkflowContext> {

    private static final Logger logger = LoggerFactory.getLogger(ManifestValidator.class);

    @Autowired
    private StructuredOutputHandler outputHandler;

    @Override
    public boolean validate(WorkflowContext context) throws HyscaleException {
        logger.debug("Executing Manifest Validator Hook");
        ServiceSpec serviceSpec = context.getServiceSpec();
        if (serviceSpec == null) {
            return false;
        }
        
        TypeReference<List<Volume>> volumeTypeReference = new TypeReference<List<Volume>>() {
        };
        List<Volume> volumeList = serviceSpec.get(HyscaleSpecFields.volumes, volumeTypeReference);
        List<String> invalidVolumes = null;
        if (volumeList != null && !volumeList.isEmpty()) {
            invalidVolumes = volumeList.stream()
                    .filter(volume -> volume != null && StringUtils.isBlank(volume.getPath()))
                    .map(each -> each.getName()).collect(Collectors.toList());

        }
        
        if (invalidVolumes == null || invalidVolumes.isEmpty()) {
            return true;
        }
        StringBuilder messageBuilder = new StringBuilder(" Invalid volumes ").append(invalidVolumes);
        WorkflowLogger.persist(ValidatorActivity.MANIFEST_VALIDATION_FAILED, LoggerTags.ERROR, messageBuilder.toString());
        if(WorkflowLogger.isDisabled()){
            outputHandler.addErrorMessage(ControllerErrorCodes.MANIFEST_VALIDATION_FAILED.getMessage(),messageBuilder.toString());
        }
        return false;
        
    }

}
