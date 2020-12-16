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
package com.github.srujankujmar.deployer.services.provider.impl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.deployer.services.model.K8sKubeConfigAuth;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.commons.models.K8sBasicAuth;
import com.github.srujankujmar.commons.models.K8sConfigFileAuth;
import com.github.srujankujmar.commons.models.K8sConfigReaderAuth;
import com.github.srujankujmar.deployer.services.provider.K8sClientProvider;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;

@Component
public class K8sClientProviderImpl implements K8sClientProvider {

    private static final Logger logger = LoggerFactory.getLogger(K8sClientProviderImpl.class);
    
    private static final String FAILED_TO_INITIALIZE_K8S_CLIENT = "Failed to initialize k8s client ";
    
    /*
     * Get client from K8s config file
     */
    private ApiClient from(K8sConfigFileAuth authConfig) throws HyscaleException {
        String mountedKubeConfigName = authConfig.getK8sConfigFile() != null ? SetupConfig.getMountPathOfKubeConf(authConfig.getK8sConfigFile().getName()) : ToolConstants.EMPTY_STRING;
        try (FileInputStream fis = new FileInputStream(authConfig.getK8sConfigFile())) {
            return Config.fromConfig(fis);
        } catch (FileNotFoundException e) {
            HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.KUBE_CONFIG_NOT_FOUND, mountedKubeConfigName);
            logger.error("Failed to find kube config ", ex);
            throw ex;
        } catch (IOException e) {
            HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.UNABLE_TO_READ_KUBE_CONFIG);
            logger.error(FAILED_TO_INITIALIZE_K8S_CLIENT, ex);
            throw ex;
        } catch (Exception e) {
            // yaml parsing exception
            HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.INVALID_KUBE_CONFIG, mountedKubeConfigName);
            logger.error(FAILED_TO_INITIALIZE_K8S_CLIENT, ex);
            throw ex;
        }
    }

    /*
     * Get client from K8s config reader
     */
    private ApiClient from(K8sConfigReaderAuth authConfig) throws HyscaleException {
        try {
            return Config.fromConfig(authConfig.getK8sConfigReader());
        } catch (IOException e) {
            HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.UNABLE_TO_READ_KUBE_CONFIG);
            logger.error(FAILED_TO_INITIALIZE_K8S_CLIENT, ex);
            throw ex;
        } catch (Exception e) {
            // yaml parsing exception
            HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.INVALID_KUBE_CONFIG, ToolConstants.EMPTY_STRING);
            logger.error(FAILED_TO_INITIALIZE_K8S_CLIENT, ex);
            throw ex;
        }
    }

    /*
     * Get client from K8s {@link K8sBasicAuth} object
     */
    private ApiClient from(K8sBasicAuth authConfig){
        if (authConfig.getToken() == null) {
            return Config.fromUserPassword(authConfig.getMasterURL(), authConfig.getUserName(),
                    authConfig.getPassword(), authConfig.getCaCert() != null);
        } else {
            return Config.fromToken(authConfig.getMasterURL(), authConfig.getToken(),
                    authConfig.getCaCert() != null);
        }
    }

	private ApiClient from(K8sKubeConfigAuth authConfig) throws HyscaleException{
		try{
			return Config.fromConfig(authConfig.getKubeConfig());
		}catch (IOException e){
			HyscaleException ex = new HyscaleException(e, DeployerErrorCodes.UNABLE_TO_READ_KUBE_CONFIG);
			logger.error(FAILED_TO_INITIALIZE_K8S_CLIENT, ex);
			throw ex;
		}
	}

	@Override
	public ApiClient get(K8sAuthorisation authConfig) throws HyscaleException {
		ApiClient apiClient = null;
		switch (authConfig.getK8sAuthType()) {
		case KUBE_CONFIG_FILE:
			apiClient = from((K8sConfigFileAuth) authConfig);
			break;
		case KUBE_CONFIG_READER:
			apiClient = from((K8sConfigReaderAuth) authConfig);
			break;
		case BASIC_AUTH:
			apiClient = from((K8sBasicAuth) authConfig);
			break;
		case KUBE_CONFIG_OBJECT:
			apiClient = from((K8sKubeConfigAuth) authConfig);
			break;
		}
		return apiClient;
	}

}
