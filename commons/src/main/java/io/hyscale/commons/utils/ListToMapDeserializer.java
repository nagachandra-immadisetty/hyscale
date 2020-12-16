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

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

public class ListToMapDeserializer extends JsonDeserializer<HashMap<String, String>> {

	private static final String KEY_FIELD = "key";
	private static final String VALUE_FIELD = "value";

	@Override
	public HashMap<String, String> deserialize(JsonParser parser, DeserializationContext context)
			throws IOException {

		HashMap<String, String> ret = new HashMap<>();

		ObjectCodec codec = parser.getCodec();
		TreeNode node = codec.readTree(parser);

		if (node.isArray()) {
			for (JsonNode n : (ArrayNode) node) {
				JsonNode id = n.get(KEY_FIELD);
				if (id != null) {
					JsonNode name = n.get(VALUE_FIELD);
					ret.put(id.asText(), name.asText());
				}
			}
		}
		return ret;
	}

}
