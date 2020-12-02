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
package com.github.srujankujmar.troubleshooting.integration.util;

import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.troubleshooting.integration.models.TroubleshootingContext;
import io.kubernetes.client.openapi.models.V1Pod;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TroubleshootContextValidator {

    public static boolean validateContext(TroubleshootingContext context) {
        if (context == null || context.getResourceInfos() == null) {
            return false;
        }
        return true;
    }

    public static boolean isResourceInfoInValid(List<TroubleshootingContext.ResourceInfo> resourceData) {
        return resourceData == null || resourceData.isEmpty();
    }

    public static List<V1Pod> getPods(TroubleshootingContext context) {
        List<TroubleshootingContext.ResourceInfo> resourceData = context.getResourceInfos().get(ResourceKind.POD.getKind());

        // If PodsList is empty Issue might be with Parent Resource, do not exit from here
        if (resourceData == null || resourceData.isEmpty()) {
            return null;
        } else {
            return resourceData.stream().filter(each -> {
                return each.getResource() != null && each.getResource() instanceof V1Pod;
            }).map(each -> {
                return ((V1Pod) each.getResource());
            }).collect(Collectors.toUnmodifiableList());
        }
    }
}
