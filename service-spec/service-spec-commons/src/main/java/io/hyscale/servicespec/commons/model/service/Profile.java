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
package com.github.srujankujmar.servicespec.commons.model.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.utils.ObjectMapperFactory;
import com.github.srujankujmar.servicespec.commons.exception.ServiceSpecErrorCodes;
import com.github.srujankujmar.servicespec.commons.json.parser.JsonTreeParser;

import java.io.IOException;

public class Profile implements HyscaleSpec {
    private JsonNode root;

    public Profile(JsonNode root) {
        this.root = root;
    }

    public Profile(String profilePath) throws HyscaleException {
        ObjectMapper mapper = ObjectMapperFactory.yamlMapper();
        try {
            this.root = mapper.readTree(profilePath);
        } catch (IOException e) {
            throw new HyscaleException(ServiceSpecErrorCodes.SERVICE_PROFILE_PARSE_ERROR);
        }
    }

    @Override
    public JsonNode get(String path) {
        return JsonTreeParser.get(root, path);
    }

    @Override
    public <T> T get(String path, Class<T> klass) throws HyscaleException {
        return JsonTreeParser.get(root, path, klass);
    }

    @Override
    public <T> T get(String path, TypeReference<T> typeReference) throws HyscaleException {
        return JsonTreeParser.get(root, path, typeReference);
    }

    @Override
    public String toString() {
        ObjectMapper mapper = ObjectMapperFactory.jsonMapper();
        try {
            return mapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
