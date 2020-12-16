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
package com.github.srujankujmar.generator.services.generator;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.github.srujankujmar.commons.models.ServiceMetadata;
import com.github.srujankujmar.generator.services.builder.DefaultLabelBuilder;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.plugin.framework.util.JsonSnippetConvertor;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class MetadataManifestSnippetGenerator {
    
    private MetadataManifestSnippetGenerator() {}

	public static ManifestSnippet getMetaData(ManifestResource manifestResource, ServiceMetadata serviceMetadata)
			throws JsonProcessingException {
		V1ObjectMeta v1ObjectMeta = new V1ObjectMeta();
		v1ObjectMeta.setLabels(DefaultLabelBuilder.build(serviceMetadata));
		v1ObjectMeta.setName(manifestResource.getName(serviceMetadata));

		ManifestSnippet snippet = new ManifestSnippet();
		snippet.setSnippet(JsonSnippetConvertor.serialize(v1ObjectMeta));
		snippet.setPath("metadata");
		snippet.setKind(manifestResource.getKind());
		return snippet;
	}

	public static ManifestSnippet getApiVersion(ManifestResource manifestResource) {
		ManifestSnippet apiVersionSnippet = new ManifestSnippet();
		apiVersionSnippet.setPath("apiVersion");
		apiVersionSnippet.setKind(manifestResource.getKind());
		apiVersionSnippet.setSnippet(manifestResource.getApiVersion());
		return apiVersionSnippet;
	}

	public static ManifestSnippet getKind(ManifestResource manifestResource) {
		ManifestSnippet kindSnippet = new ManifestSnippet();
		kindSnippet.setPath("kind");
		kindSnippet.setKind(manifestResource.getKind());
		kindSnippet.setSnippet(manifestResource.getKind());
		return kindSnippet;
	}

}
