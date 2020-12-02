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
package com.github.srujankujmar.deployer.services.manager;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.deployer.services.model.ScaleOperation;
import com.github.srujankujmar.deployer.services.model.ScaleSpec;
import com.github.srujankujmar.deployer.services.model.ScaleStatus;
import io.kubernetes.client.openapi.ApiClient;

public interface ScaleServiceManager {

    public ScaleStatus scale(ApiClient apiClient, String appName, String service, String namespace, ScaleSpec scaleSpec) throws HyscaleException;
}
