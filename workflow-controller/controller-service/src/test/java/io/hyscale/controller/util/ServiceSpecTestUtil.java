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
package com.github.srujankujmar.controller.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.utils.ObjectMapperFactory;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

public class ServiceSpecTestUtil {

    public static JsonNode getServiceSpecJsonNode(String filepath) throws IOException {
        if (StringUtils.isBlank(filepath)) {
            return null;
        }
        ObjectMapper objectMapper = ObjectMapperFactory.yamlMapper();
        InputStream resourceAsStream = ServiceSpecTestUtil.class.getResourceAsStream(filepath);
        String testData = IOUtils.toString(resourceAsStream, StandardCharsets.UTF_8);
        return objectMapper.readTree(testData);
    }
    
    public static ServiceSpec getServiceSpec(String serviceSpecPath) throws HyscaleException {
        if (StringUtils.isBlank(serviceSpecPath)) {
            return null;
        }
        return new ServiceSpec(getServiceSpecFile(serviceSpecPath));
    }
    
    private static File getServiceSpecFile(String serviceSpecPath) {
        URL urlPath = ServiceSpecTestUtil.class.getResource(serviceSpecPath);
        return new File(urlPath.getFile());
    }

}
