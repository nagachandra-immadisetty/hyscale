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
package com.github.srujankujmar.deployer.services.manager;

import com.github.srujankujmar.commons.models.AnnotationKey;
import com.github.srujankujmar.commons.models.KubernetesResource;
import io.kubernetes.client.openapi.models.V1ObjectMeta;

import java.util.HashMap;

public class AnnotationsUpdateManager {
    
    private AnnotationsUpdateManager() {}

    public static void update(KubernetesResource kubernetesResource, AnnotationKey annotationKey, String value) {
        if (kubernetesResource == null) {
            return;
        }

        V1ObjectMeta v1ObjectMeta = kubernetesResource.getV1ObjectMeta();
        if (v1ObjectMeta == null) {
            v1ObjectMeta = new V1ObjectMeta();
        }
        if (v1ObjectMeta.getAnnotations() == null) {
            v1ObjectMeta.setAnnotations(new HashMap<>());
        }
        v1ObjectMeta.getAnnotations().put(annotationKey.getAnnotation(), value);
    }
}
