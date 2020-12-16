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
package com.github.srujankujmar.commons.models;

import com.github.srujankujmar.commons.constants.ToolConstants;

public class ClusterVersionInfo {

    private String major;
    private String minor;

    public String getMajor() {
        return major;
    }

    public void setMajor(String majorVersion) {
        this.major = majorVersion;
    }

    public String getMinor() {
        return minor;
    }

    public void setMinor(String minorVersion) {
        this.minor = minorVersion;
    }

    public String getVersion() {
        return major + ToolConstants.DOT + minor;
    }
}