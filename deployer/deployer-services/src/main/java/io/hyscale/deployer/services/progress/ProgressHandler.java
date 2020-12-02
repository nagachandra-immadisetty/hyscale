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
package com.github.srujankujmar.deployer.services.progress;

/**
 * Progress Handler for cluster resources
 * Updates progress based on resource status
 *
 */
public interface ProgressHandler {

	void onPodLaunch(String podName);

	void onPodCompletion(String podName, boolean isSuccess);

	void onContainerLaunch(String containerName, String podName);

	void onContainerCompletion(String containerName, String podName, boolean isSuccess);

}
