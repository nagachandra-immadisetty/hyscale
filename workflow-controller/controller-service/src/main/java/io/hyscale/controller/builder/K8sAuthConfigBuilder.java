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
package com.github.srujankujmar.controller.builder;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.AuthConfig;
import com.github.srujankujmar.commons.models.K8sConfigFileAuth;
import com.github.srujankujmar.commons.utils.WindowsUtil;
import com.github.srujankujmar.controller.config.ControllerConfig;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;

/**
 *
 *  Prepares the authorisation config @see {@link AuthConfig }
 *  for kubernetes cluster.
 *
 */

@Component
public class K8sAuthConfigBuilder {

    @Autowired
    private ControllerConfig controllerConfig;

    private AuthConfig defaultAuthConfig;

    /**
     * Gets {@link K8sConfigFileAuth} from {@link ControllerConfig} default config
     * @return {@link K8sConfigFileAuth}
     */
    public AuthConfig getAuthConfig() throws HyscaleException {
        if (defaultAuthConfig == null) {
            defaultAuthConfig = getAuthConfig(controllerConfig.getDefaultKubeConf());
        }
        return defaultAuthConfig;
    }

    public AuthConfig getAuthConfig(String kubeConfigPath) throws HyscaleException{
    	validate(kubeConfigPath);
        K8sConfigFileAuth k8sAuth = new K8sConfigFileAuth();
        k8sAuth.setK8sConfigFile(new File(kubeConfigPath));
        return k8sAuth;
    }

    private void validate(String path) throws HyscaleException {
    	if(path==null) {
    		throw new HyscaleException(ControllerErrorCodes.KUBE_CONFIG_PATH_EMPTY);
    	}
        File confFile = new File(path);
        if (confFile != null && !confFile.exists()) {
            String confPath = SetupConfig.getMountPathOfKubeConf(path) ;
            confPath = WindowsUtil.updateToHostFileSeparator(confPath);
            throw new HyscaleException(ControllerErrorCodes.KUBE_CONFIG_NOT_FOUND, confPath);
        }
    }

}
