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
package com.github.srujankujmar.controller.manager.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.DockerConfig;
import com.github.srujankujmar.commons.models.DockerCredHelper;
import com.github.srujankujmar.commons.models.DockerHubAliases;
import com.github.srujankujmar.commons.models.ImageRegistry;
import com.github.srujankujmar.commons.utils.ObjectMapperFactory;
import com.github.srujankujmar.commons.utils.WindowsUtil;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.builder.ImageRegistryBuilder;
import com.github.srujankujmar.controller.config.ControllerConfig;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import com.github.srujankujmar.controller.manager.RegistryManager;

/**
 * Provides registry credentials.
 * Reads local docker registry config.
 * Takes registry name as input,tries to find the matching registry in credential helpers if present and return credentials,else searches in
 * credStore if specified in config file,else tries to get from the auths.
 */
@Component
public class LocalRegistryManagerImpl implements RegistryManager {

    private static final Logger logger = LoggerFactory.getLogger(LocalRegistryManagerImpl.class);

    private LocalDockerConfigBuilder dockerConfigBuilder;

    private DockerConfig externalRegistryConf;

    @Autowired
    private ControllerConfig controllerConfig;

    @PostConstruct
    public void init() {
        this.dockerConfigBuilder = new LocalDockerConfigBuilder();
    }

    /**
     * returns image registry credentials if found in docker config in the credHelpers,credsStore or Auths else returns null.
     *
     * @param registry
     * @return ImageRegistry object if found in credHelpers,credsStore or Auths else returns null.
     */
    @Override
    public ImageRegistry getImageRegistry(String registry) throws HyscaleException {
        if (!SetupConfig.hasExternalRegistryConf()) {
            return getImageRegistry(dockerConfigBuilder.getDockerConfig(), registry);
        }
        logger.debug("Found External registry Conf");
        buildExternalRegistryConf();
        return getImageRegistry(externalRegistryConf, registry);
    }

    public ImageRegistry getImageRegistry(DockerConfig dockerConfig, String registry) {

        if (dockerConfig == null) {
            return null;
        }

        List<String> dockerRegistryAliases = null;
        if (registry != null) {
            dockerRegistryAliases = DockerHubAliases.getDockerRegistryAliases(registry);
        } else {
            dockerRegistryAliases = DockerHubAliases.getDefaultDockerRegistryAlias();
        }

        List<String> registryPatterns = new ArrayList<>();
        for (String registryAlias : dockerRegistryAliases) {
            registryPatterns.addAll(getRegistryPatterns(registryAlias));
        }
        for (String pattern : registryPatterns) {
            ImageRegistryBuilder builder = new ImageRegistryBuilder(pattern);
            DockerCredHelper dockerCredHelper = getDockerCredHelper(dockerConfig, pattern);
            ImageRegistry imageRegistry = dockerCredHelper != null ? builder.from(dockerCredHelper) : builder.from(dockerConfig.getAuths());
            if (imageRegistry != null) {
                return imageRegistry;
            }
        }
        return null;

    }

    /**
     * Returns credential helper if registry pattern found  in credHelpers if specified
     * or directly credsStore if specified else returns null.
     *
     * @param pattern
     * @return docker credential helper else returns null
     */
    private DockerCredHelper getDockerCredHelper(DockerConfig dockerConfig, String pattern) {
        String helperFunc = dockerConfig.getCredHelpers() != null ? getHelperFunction(dockerConfig, pattern) : null;
        if (helperFunc != null) {
            return new DockerCredHelper(helperFunc);
        } else if (dockerConfig.getCredsStore() != null) {
            return new DockerCredHelper(dockerConfig.getCredsStore());
        }
        return null;
    }

    private static String getHelperFunction(DockerConfig dockerConfig, String registry) {
        return dockerConfig.getCredHelpers().containsKey(registry) ? dockerConfig.getCredHelpers().get(registry) : null;
    }

    private static List<String> getRegistryPatterns(String registry) {
        String exactMatch = registry;
        String withHttps = "https://" + registry;
        String withSuffix = registry + "/";
        String withHttpsAndSuffix = "https://" + registry + "/";
        return Arrays.asList(exactMatch, withHttps, withSuffix, withHttpsAndSuffix);
    }


    private void validate(String path) throws HyscaleException {
        File confFile = new File(path);
        if (confFile != null && !confFile.exists()) {
            String confPath = SetupConfig.getMountOfDockerConf(path);
            confPath = WindowsUtil.updateToHostFileSeparator(confPath);
            WorkflowLogger.error(ControllerActivity.CANNOT_FIND_FILE,
                    confPath);
            throw new HyscaleException(ControllerErrorCodes.DOCKER_CONFIG_NOT_FOUND, confPath);
        }
    }

    public class LocalDockerConfigBuilder {

        private LocalDockerConfigBuilder() {
        }

        private DockerConfig dockerConfig;

        public DockerConfig getDockerConfig() throws HyscaleException {
            if (dockerConfig != null) {
                return dockerConfig;
            } else {
                build();
                return dockerConfig;
            }
        }

        /**
         * Reads local docker config
         */
        private void build() throws HyscaleException {
            if (dockerConfig != null) {
                return;
            }
            validate(controllerConfig.getDefaultRegistryConf());
            ObjectMapper mapper = ObjectMapperFactory.jsonMapper();
            try {
                TypeReference<DockerConfig> dockerConfigTypeReference = new TypeReference<DockerConfig>() {
                };

                this.dockerConfig = mapper.readValue(new File(controllerConfig.getDefaultRegistryConf()),
                        dockerConfigTypeReference);

            } catch (IOException e) {
                String dockerConfPath = SetupConfig.getMountOfDockerConf(controllerConfig.getDefaultRegistryConf());
                WorkflowLogger.error(ControllerActivity.ERROR_WHILE_READING, dockerConfPath, e.getMessage());
                HyscaleException ex = new HyscaleException(e, ControllerErrorCodes.DOCKER_CONFIG_NOT_FOUND, dockerConfPath);
                logger.error("Error while deserializing image registries {}", ex.toString());
                throw ex;
            }
        }
    }

    /*
     *   Builds docker config from Standard Input of the process
     */
    private void buildExternalRegistryConf() throws HyscaleException {
        if (externalRegistryConf == null) {
            logger.debug("Reading the external registry conf input");
            try (Scanner scanner = new Scanner(System.in)){
                if (scanner.hasNextLine()) {
                    String json = scanner.nextLine();
                    ObjectMapper mapper = ObjectMapperFactory.jsonMapper();
                    try {
                        TypeReference<DockerConfig> dockerConfigTypeReference = new TypeReference<DockerConfig>() {
                        };
                        this.externalRegistryConf = mapper.readValue(json,
                                dockerConfigTypeReference);
                        logger.debug("Initialized the external registry conf input");
                    } catch (IOException e) {
                        logger.error("Error while building external registry config", e);
                        String dockerConfPath = SetupConfig.getMountOfDockerConf(controllerConfig.getDefaultRegistryConf());
                        WorkflowLogger.error(ControllerActivity.ERROR_WHILE_READING, dockerConfPath, e.getMessage());
                        HyscaleException ex = new HyscaleException(e, ControllerErrorCodes.DOCKER_CONFIG_NOT_FOUND, dockerConfPath);
                        throw ex;
                    }
                }
            }
        }
    }
}