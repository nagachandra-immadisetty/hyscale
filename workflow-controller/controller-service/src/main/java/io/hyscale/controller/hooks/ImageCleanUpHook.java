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
package com.github.srujankujmar.controller.hooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.commands.provider.ImageCommandProvider;
import com.github.srujankujmar.commons.component.InvokerHook;
import com.github.srujankujmar.commons.commands.CommandExecutor;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.servicespec.commons.exception.ServiceSpecErrorCodes;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

/**
 * Hook to clean up local images which are no longer in use
 *
 */
@Component
public class ImageCleanUpHook implements InvokerHook<WorkflowContext> {

	private static final Logger logger = LoggerFactory.getLogger(ImageCleanUpHook.class);

	@Autowired
	private ImageCommandProvider imageCommandProvider;

	@Override
	public void preHook(WorkflowContext context) {

	}

	@Override
	public void postHook(WorkflowContext context) throws HyscaleException {
		ServiceSpec serviceSpec = context.getServiceSpec();
		if (serviceSpec == null) {
			logger.error(" Cannot clean up image without service specs ");
			throw new HyscaleException(ServiceSpecErrorCodes.SERVICE_SPEC_REQUIRED);
		}

		String serviceName = serviceSpec.get(HyscaleSpecFields.name, String.class);

		String tag = serviceSpec.get(HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.tag),
				String.class);
		String cleanUpCommand = imageCommandProvider.getImageCleanUpCommand(context.getAppName(), serviceName, tag);
		logger.debug("Starting image cleanup, command {}", cleanUpCommand);
		boolean success = CommandExecutor.execute(cleanUpCommand);

		logger.debug("Image clean up {}", success ? Status.DONE.getMessage() : Status.FAILED.getMessage());

	}

	@Override
	public void onError(WorkflowContext context, Throwable th) {
		context.setFailed(true);
	}

}
