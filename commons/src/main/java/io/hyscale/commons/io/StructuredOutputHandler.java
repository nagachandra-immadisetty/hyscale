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
package com.github.srujankujmar.commons.io;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.utils.GsonProviderUtil;
import com.github.srujankujmar.commons.utils.HyscaleStringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to handle output acivity in structured format like JSON/YAML.
 */
@SuppressWarnings("java:S106")
@Component
public class StructuredOutputHandler {

    private List<String> errorMessages = new ArrayList<>();

    public void addErrorMessage(String message, String... args) {
        if (StringUtils.isEmpty(message)) {
            return;
        }
        if (args != null && args.length > 0) {
            errorMessages.add(String.format(message.replaceAll("\\{\\}", "%s"), args));
        } else {
            errorMessages.add(message);
        }
    }

    public static void prepareOutput(String key, JsonElement jsonElement) {
        JsonObject outputJson = new JsonObject();
        outputJson.add(key, jsonElement);
        System.out.println(GsonProviderUtil.getPrettyGsonBuilder().toJson(outputJson));
    }

    public static void prepareOutput(String key, String message) {
        JsonObject outputJson = new JsonObject();
        outputJson.addProperty(key, message);
        System.out.println(GsonProviderUtil.getPrettyGsonBuilder().toJson(outputJson));
    }

    public void generateErrorMessage(String key) {
        StringBuilder sb = new StringBuilder();
        errorMessages.forEach(each -> sb.append(each).append(ToolConstants.COMMA));
        JsonObject outputJson = new JsonObject();
        outputJson.addProperty(key, HyscaleStringUtil.removeSuffixStr(sb,ToolConstants.COMMA));
        System.out.println(GsonProviderUtil.getPrettyGsonBuilder().toJson(outputJson));
    }
}