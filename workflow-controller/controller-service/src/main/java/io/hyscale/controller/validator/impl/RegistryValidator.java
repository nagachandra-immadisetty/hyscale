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

import java.util.HashSet;
import java.util.Set;

import com.github.srujankujmar.builder.services.exception.ImageBuilderErrorCodes;
import com.github.srujankujmar.commons.io.StructuredOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.LoggerTags;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.validator.Validator;
import com.github.srujankujmar.controller.activity.ValidatorActivity;
import com.github.srujankujmar.controller.manager.RegistryManager;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Image;
import com.github.srujankujmar.servicespec.commons.util.ImageUtil;

/**
 * Validates registry related details
 * Returns true if registry details are not required
 * i.e. image build or push not required
 * 
 */
@Component
public class RegistryValidator implements Validator<WorkflowContext> {
	private static final Logger logger = LoggerFactory.getLogger(RegistryValidator.class);

	@Autowired
	private RegistryManager registryManager;

	@Autowired
	private StructuredOutputHandler outputHandler;
	
	private Set<String> validRegistries = new HashSet<>();
	
	private Set<String> inValidRegistries = new HashSet<>();

	/**
	 * 1. It will check that spec has buildspec or dockerfile 
	 * 2. If both is not then it will return true
	 * 3. If any one is there then 
	 *    3.1  It will fetch registry details
	 *    3.2  Then it will verify the registry
	 *    3.3  if registry is exist which is provided by user then return true else false
	 */
	@Override
	public boolean validate(WorkflowContext context) throws HyscaleException {
	    logger.debug("Starting registry validation");
	    Image image = context.getServiceSpec().get(HyscaleSpecFields.image, Image.class);
	    String registry = image.getRegistry();

	    if (validRegistries.contains(registry)) {
	        return true;
	    }
	    
	    if (inValidRegistries.contains(registry)) {
	        return false;
	    }
		if (!ImageUtil.isImageBuildPushRequired(context.getServiceSpec())) {
			return true;
		}
		boolean isRegistryAvailable = registryManager.getImageRegistry(registry) != null ? true : false;
		if (isRegistryAvailable) {
			validRegistries.add(registry);
			return true;
		} else {
		    inValidRegistries.add(registry);
		}
		registry=registry!=null?registry:"";
		WorkflowLogger.persist(ValidatorActivity.MISSING_DOCKER_REGISTRY_CREDENTIALS, LoggerTags.ERROR, registry, registry);
		if(WorkflowLogger.isDisabled()){
			outputHandler.addErrorMessage(ImageBuilderErrorCodes.MISSING_DOCKER_REGISTRY_CREDENTIALS.getMessage(),registry, registry);
		}
		return false;
	}
}
