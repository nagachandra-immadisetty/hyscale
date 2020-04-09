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
package io.hyscale.controller.processor;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.hyscale.commons.exception.HyscaleException;
import io.hyscale.commons.io.HyscaleFilesUtil;
import io.hyscale.commons.logger.WorkflowLogger;
import io.hyscale.commons.models.Status;
import io.hyscale.commons.utils.ObjectMapperFactory;
import io.hyscale.controller.activity.ControllerActivity;
import io.hyscale.controller.exception.ControllerErrorCodes;
import io.hyscale.controller.model.EffectiveServiceSpec;
import io.hyscale.controller.util.ServiceProfileUtil;
import io.hyscale.controller.util.ServiceSpecUtil;
import io.hyscale.generator.services.model.ServiceMetadata;
import io.hyscale.servicespec.commons.builder.EffectiveServiceSpecBuilder;
import io.hyscale.servicespec.commons.builder.MapFieldDataProvider;
import io.hyscale.servicespec.commons.builder.ServiceInputType;
import io.hyscale.servicespec.commons.model.service.ServiceSpec;

@Component
public class ServiceSpecProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ServiceSpecProcessor.class);

    public List<EffectiveServiceSpec> getEffectiveServiceSpec(List<File> serviceSpecFiles, List<File> profileFiles)
            throws HyscaleException {

        Map<String, Entry<String, File>> serviceVsProfile = validateAndGetDependencyMap(serviceSpecFiles, profileFiles);
        return mergeServiceSpec(serviceSpecFiles, serviceVsProfile);

    }

    private List<EffectiveServiceSpec> mergeServiceSpec(List<File> serviceSpecFiles,
            Map<String, Entry<String, File>> serviceVsProfile) throws HyscaleException {

        List<EffectiveServiceSpec> effectiveServiceSpecList = new ArrayList<EffectiveServiceSpec>();
        ObjectMapper mapper = null;

        for (File serviceSpecFile : serviceSpecFiles) {
            EffectiveServiceSpec effectiveServiceSpec = new EffectiveServiceSpec();
            effectiveServiceSpec.setServiceSpecFile(serviceSpecFile);
            ServiceMetadata serviceMetadata = new ServiceMetadata();
            effectiveServiceSpec.setServiceMetadata(serviceMetadata);
            mapper = ObjectMapperFactory.yamlMapper();
            String serviceName = ServiceSpecUtil.getServiceName(serviceSpecFile);
            serviceMetadata.setServiceName(serviceName);
            Entry<String, File> profileDetail = serviceVsProfile.remove(serviceName);
            String serviceSpecData = HyscaleFilesUtil.readFileData(serviceSpecFile);
            if (profileDetail != null) {
                String profileName = profileDetail.getKey();
                serviceMetadata.setEnvName(profileName);
                File profileFile = profileDetail.getValue();
                WorkflowLogger.startActivity(ControllerActivity.APPLYING_PROFILE_FOR_SERVICE, profileName, serviceName);
                mapper = ObjectMapperFactory.jsonMapper();
                try {
                    MapFieldDataProvider mapFieldDataProvider = new MapFieldDataProvider();
                    // Merge
                    serviceSpecData = new EffectiveServiceSpecBuilder().type(ServiceInputType.YAML)
                            .withServiceSpec(serviceSpecData).withProfile(HyscaleFilesUtil.readFileData(profileFile))
                            .withFieldMetaDataProvider(mapFieldDataProvider).build();
                    WorkflowLogger.endActivity(Status.DONE);
                } catch (HyscaleException e) {
                    logger.error("Error while applying profile {} for service {}", profileName, serviceName, e);
                    WorkflowLogger.endActivity(Status.FAILED);
                    throw e;
                }
            }
            try {
                effectiveServiceSpec.setServiceSpec(new ServiceSpec(mapper.readTree(serviceSpecData)));
                effectiveServiceSpecList.add(effectiveServiceSpec);
            } catch (IOException e) {
                logger.error("Error while processing service spec ", e);
                throw new HyscaleException(ControllerErrorCodes.SERVICE_SPEC_PROCESSING_FAILED, e.getMessage());
            }
        }
        return effectiveServiceSpecList;
    }

    private Map<String, Map.Entry<String, File>> validateAndGetDependencyMap(List<File> serviceSpecFiles,
            List<File> profileFiles) throws HyscaleException {
        Map<String, Entry<String, File>> serviceVsProfile = new HashMap<String, Map.Entry<String, File>>();
        List<String> invalidServiceList = new ArrayList<String>();
        if (profileFiles != null && !profileFiles.isEmpty()) {
            for (File profileFile : profileFiles) {
                String profileName = ServiceProfileUtil.getProfileName(profileFile);
                String serviceName = ServiceProfileUtil.getServiceNameFromProfile(profileFile);
                if (serviceVsProfile.get(serviceName) != null) {
                    // Multiple profiles for a single service
                    invalidServiceList.add(serviceName);
                }
                serviceVsProfile.put(serviceName, new SimpleEntry<String, File>(profileName, profileFile));
            }
        }

        if (!invalidServiceList.isEmpty()) {
            String invalidServices = invalidServiceList.toString();
            logger.error("Multiple profiles found for services {}", invalidServices);
            WorkflowLogger.error(ControllerActivity.MULIPLE_PROFILES_FOUND, invalidServices);
            throw new HyscaleException(ControllerErrorCodes.UNIQUE_PROFILE_REQUIRED, invalidServices);
        }

        Map<String, File> serviceVsSpecFile = new HashMap<String, File>();

        for (File serviceSpecFile : serviceSpecFiles) {
            serviceVsSpecFile.put(ServiceSpecUtil.getServiceName(serviceSpecFile), serviceSpecFile);
        }

        // Services specified in profile not found
        invalidServiceList = serviceVsProfile.entrySet().stream().map(entrySet -> entrySet.getKey())
                .filter(service -> !serviceVsSpecFile.containsKey(service)).collect(Collectors.toList());

        if (invalidServiceList != null && !invalidServiceList.isEmpty()) {
            String invalidServices = invalidServiceList.toString();
            logger.error("Services {} mentioned in profiles not available in deployment", invalidServices);
            WorkflowLogger.error(ControllerActivity.NO_SERVICE_FOUND_FOR_PROFILE, invalidServices);
            throw new HyscaleException(ControllerErrorCodes.SERVICE_NOT_PROVIDED_FOR_PROFILE, invalidServices);
        }
        return serviceVsProfile;
    }

}