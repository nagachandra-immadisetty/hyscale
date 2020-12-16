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
package com.github.srujankujmar.deployer.services.constants;

public class DeployerConstants {
    
    private DeployerConstants() {}

    public static final String LB_READY_TIMEOUT = "HYS_LB_READY_TIMEOUT";
    
    public static final String POD_RESTART_COUNT="HYS_POD_RESTART_COUNT";

    public static final String TRUE = "true";
    
    public static final long DEFAULT_LB_READY_TIMEOUT = 90000;
    
    public static final long DEFAULT_POD_RESTART_COUNT = 3;
    
    public static final long DELETE_SLEEP_INTERVAL_IN_MILLIS = 3000;
    
    public static final long MAX_WAIT_TIME_IN_MILLISECONDS = 120000;

}
