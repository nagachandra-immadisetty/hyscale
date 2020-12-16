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
package com.github.srujankujmar.controller.profile;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.io.HyscaleFilesUtil;
import com.github.srujankujmar.controller.commands.args.ProfileLocator;
import com.github.srujankujmar.controller.commands.input.ProfileArg;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import com.github.srujankujmar.controller.model.HyscaleInputSpec;
import com.github.srujankujmar.controller.util.ServiceProfileUtil;
import com.github.srujankujmar.controller.util.ServiceSpecUtil;
import com.github.srujankujmar.controller.validator.impl.ProfileSpecInputValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Processes the profile arg , validates the profile schema
 * and validates service dependency on profiles like
 * multiple profiles or missing profiles
 */
@Component
public class ProfileSpecProcessor {

    @Autowired
    private ProfileSpecInputValidator profileSpecInputValidator;

    @Autowired
    private ProfileLocator profileLocator;

    public HyscaleInputSpec process(ProfileArg profileArg, List<File> serviceSpecFiles) throws HyscaleException {
        List<File> profiles = null;
        boolean strict = false;
        if (profileArg != null) {
            if (StringUtils.isNotBlank(profileArg.getProfileName())) {
                profiles = profileLocator.locateProfiles(profileArg.getProfileName(), serviceSpecFiles);
                strict = true;
            } else {
                profiles = profileArg.getProfiles();
            }
        }
        if (!profileSpecInputValidator.validate(profiles)) {
            throw new HyscaleException(ControllerErrorCodes.PROFILE_VALIDATION_FAILED);
        }
        checkForServiceDependency(serviceSpecFiles, profiles, strict);
        HyscaleInputSpec hyscaleInputSpec = new HyscaleInputSpec();
        hyscaleInputSpec.setServiceSpecFiles(serviceSpecFiles);
        hyscaleInputSpec.setProfileFiles(profiles);
        return hyscaleInputSpec;
    }

    private void checkForServiceDependency(List<File> serviceSpecFiles,
                                           List<File> profileFiles, boolean strictProfile) throws HyscaleException {
        Set<String> serviceFromSpec = new HashSet<>();
        Set<String> serviceFromProfiles = new HashSet<String>();

        serviceFromSpec = serviceSpecFiles.stream().map(eachFile -> {
            try {
                return ServiceSpecUtil.getServiceName(eachFile);
            } catch (HyscaleException e) {
                return null;
            }
        }).collect(toSet());

        boolean multipleProfileOfSameService = false;
        Set<String> mismatchedFileAndProfileNameSet = new HashSet<>();
        Set<String> multipleProfilesServices = new HashSet<String>();
        for (File eachProfile : profileFiles) {
            String serviceName = ServiceProfileUtil.getServiceNameFromProfile(eachProfile);
            if (strictProfile) {
                String profileName = ServiceProfileUtil.getProfileName(eachProfile);
                String fileName = eachProfile.getName();
                if (!fileName.matches(profileLocator.getProfileNamePattern(profileName))) {
                    mismatchedFileAndProfileNameSet.add(fileName);
                }
            }
            multipleProfileOfSameService = serviceFromProfiles.add(serviceName);
            if (!multipleProfileOfSameService) {
                multipleProfilesServices.add(serviceName);
            }
        }

        if (!mismatchedFileAndProfileNameSet.isEmpty()) {
            throw new HyscaleException(ControllerErrorCodes.PROFILE_NAMES_MISMATCHED_WITH_FILES, mismatchedFileAndProfileNameSet.stream().collect(Collectors.joining(",")));
        }

        if (!multipleProfilesServices.isEmpty()) {
            throw new HyscaleException(ControllerErrorCodes.UNIQUE_PROFILE_REQUIRED, multipleProfilesServices.stream().collect(Collectors.joining(",")));
        }

        if (strictProfile) {
            serviceFromSpec.removeAll(serviceFromProfiles);
            if (!serviceFromSpec.isEmpty()) {
                throw new HyscaleException(ControllerErrorCodes.PROFILE_NOT_PROVIDED_FOR_SERVICES, serviceFromSpec.stream().collect(Collectors.joining(",")));
            }
        }
    }
}