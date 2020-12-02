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
package com.github.srujankujmar.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.exception.CommonErrorCode;
import com.github.srujankujmar.commons.exception.HyscaleException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class DataFormatConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataFormatConverter.class);

    /**
     * Converts the given String to Json.
     *
     * @param yaml String input
     * @return Json String
     * @throws HyscaleException if any IO Exception occurs while conversion.
     */
    public static String yamlToJson(String yaml) throws HyscaleException {
        if(StringUtils.isBlank(yaml)){
            return yaml;
        }
        ObjectMapper yamlReader = ObjectMapperFactory.yamlMapper();
        Object obj;
        try {
            obj = yamlReader.readValue(yaml, Object.class);
            ObjectMapper jsonWriter = ObjectMapperFactory.jsonMapper();
            return jsonWriter.writeValueAsString(obj);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            HyscaleException ex = new HyscaleException(e, CommonErrorCode.YAML_TO_JSON_CONVERSION_FAILURE,e.getMessage());
            throw ex;
        }
    }

    /**
     * Converts the given file to Json.
     *
     * @param file
     * @return Json String
     * @throws HyscaleException if any IO Exception occurs while conversion.
     */
    public static String yamlToJson(File file) throws HyscaleException{
        try {
            if(file==null){
               return null;
            }
            String data = FileUtils.readFileToString(file,ToolConstants.CHARACTER_ENCODING);
            return yamlToJson(data);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            HyscaleException ex = new HyscaleException(e, CommonErrorCode.FAILED_TO_READ_FILE,file.getPath());
            throw ex;
        }
    }
}
