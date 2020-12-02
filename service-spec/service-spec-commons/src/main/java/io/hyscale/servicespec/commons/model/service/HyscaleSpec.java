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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.srujankujmar.commons.exception.HyscaleException;

public interface HyscaleSpec {
    /**
     * Get JsonNode for field defined by path from the root
     *
     * @param path
     * @return JsonNode of field at path
     */
    public JsonNode get(String path);

    /**
     * Get Object for field defined by path from the root
     *
     * @param <T>   class object to be returned
     * @param path
     * @param klass
     * @return object of class T
     * @throws HyscaleException
     */
    public <T> T get(String path, Class<T> klass) throws HyscaleException;

    /**
     * Get Object for field defined by path from the root
     *
     * @param <T>
     * @param path
     * @param typeReference - defines class object (T) to be returned
     * @return object of class T
     * @throws HyscaleException
     */
    public <T> T get(String path, TypeReference<T> typeReference) throws HyscaleException;

}
