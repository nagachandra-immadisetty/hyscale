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
package com.github.srujankujmar.deployer.services.handler.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.CommonErrorCode;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.AuthConfig;
import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.deployer.core.model.ResourceOperation;
import com.github.srujankujmar.deployer.services.handler.AuthenticationHandler;
import com.github.srujankujmar.deployer.services.provider.K8sClientProvider;
import com.github.srujankujmar.deployer.services.util.ExceptionHelper;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AuthenticationV1Api;
import io.kubernetes.client.openapi.models.V1APIResourceList;

/**
 * Kubernetes cluster authorisation handler
 * Checks if cluster access is allowed for given {@link AuthConfig}
 *
 */
@Component
public class K8sAuthenticationHandler implements AuthenticationHandler<K8sAuthorisation> {
    
	private static final Logger logger = LoggerFactory.getLogger(K8sAuthenticationHandler.class);

	private static final String KUBERNETES_AUTHENTICATION = "Kubernetes authentication";
	
	private static final int UNAUTHORISED_ERROR_CODE = 401;
	
	@Autowired
	private K8sClientProvider clientProvider;

	public boolean authenticate(K8sAuthorisation authConfig) throws HyscaleException {
	    if (authConfig == null) {
	        return false;
	    }
		ApiClient apiClient = clientProvider.get(authConfig);
		AuthenticationV1Api apiInstance = new AuthenticationV1Api(apiClient);
		try {
			V1APIResourceList result = apiInstance.getAPIResources();
			return result != null;
		} catch (ApiException e) {
			logger.error("Exception when calling k8s authentication {} {} ", e.getCode(), e.getResponseBody(), e);
		    if (UNAUTHORISED_ERROR_CODE == e.getCode()) {
		        return false;
		    }
			throw new HyscaleException(e, CommonErrorCode.FAILED_TO_CONNECT_TO_CLUSTER,
					ExceptionHelper.getExceptionMessage(KUBERNETES_AUTHENTICATION, e, ResourceOperation.GET));
		}
	}

}
