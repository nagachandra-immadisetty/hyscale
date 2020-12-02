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

import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class KubernetesResource {

	private Object resource;
	private V1ObjectMeta v1ObjectMeta;
	private String kind;

	public Object getResource() {
		return resource;
	}

	public void setResource(Object resource) {
		this.resource = resource;
	}

	public V1ObjectMeta getV1ObjectMeta() {
		return v1ObjectMeta;
	}

	public void setV1ObjectMeta(V1ObjectMeta v1ObjectMeta) {
		this.v1ObjectMeta = v1ObjectMeta;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

}
