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
package com.github.srujankujmar.builder.services.service;

import com.github.srujankujmar.builder.core.models.BuildContext;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

public interface ImagePushService {

	/**
	 * Check docker exists, pull(if required), tag, push(if required)
	 * 
	 * @param serviceSpec
	 * @param buildContext
	 * @throws HyscaleException
	 */
	public void pushImage(ServiceSpec serviceSpec, BuildContext buildContext) throws HyscaleException;
}