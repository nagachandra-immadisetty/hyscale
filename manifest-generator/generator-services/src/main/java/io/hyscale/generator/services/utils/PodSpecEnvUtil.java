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
package com.github.srujankujmar.generator.services.utils;

import com.github.srujankujmar.commons.models.DecoratedArrayList;
import com.github.srujankujmar.servicespec.commons.model.PropType;
import com.github.srujankujmar.servicespec.commons.model.service.Props;
import com.github.srujankujmar.servicespec.commons.model.service.Secrets;
import com.github.srujankujmar.servicespec.commons.model.service.*;
import io.kubernetes.client.openapi.models.V1ConfigMapKeySelector;
import io.kubernetes.client.openapi.models.V1EnvVar;
import io.kubernetes.client.openapi.models.V1EnvVarSource;
import io.kubernetes.client.openapi.models.V1SecretKeySelector;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Component
public class PodSpecEnvUtil {
    
    private PodSpecEnvUtil() {}

    public static List<V1EnvVar> getPropEnv(Props props, String configMapName) {
        List<V1EnvVar> envVarList = new DecoratedArrayList<>();
        if (props == null || props.getProps().isEmpty()) {
            return envVarList;
        }
        props.getProps().entrySet().stream().filter(each -> {
            return each != null && !PropType.FILE.getPatternMatcher().matcher(each.getValue()).matches();
        }).forEach(each -> {
            V1EnvVar envVar = new V1EnvVar();
            envVar.setName(each.getKey());

            V1EnvVarSource envVarSource = new V1EnvVarSource();
            V1ConfigMapKeySelector configMapKeySelector = new V1ConfigMapKeySelector();
            configMapKeySelector.setName(configMapName);
            configMapKeySelector.setKey(each.getKey());
            envVarSource.setConfigMapKeyRef(configMapKeySelector);
            envVar.setValueFrom(envVarSource);
            envVarList.add(envVar);
        });
        return envVarList;
    }

    public static List<V1EnvVar> getSecretEnv(Secrets secrets, String secretName) {
        if (secrets == null ) {
            return Collections.emptyList();
        }
        Set<String> secretKeys = getSecretKeys(secrets);
        if (secretKeys == null) {
            return Collections.emptyList();
        }
        List<V1EnvVar> envVarList = new DecoratedArrayList<>();
        secretKeys.stream().forEach(each -> {
            V1EnvVar envVar = new V1EnvVar();
            envVar.setName(each);

            V1EnvVarSource envVarSource = new V1EnvVarSource();
            V1SecretKeySelector secretKeySelector = new V1SecretKeySelector();
            secretKeySelector.setName(secretName);
            secretKeySelector.setKey(each);
            envVarSource.setSecretKeyRef(secretKeySelector);
            envVar.setValueFrom(envVarSource);
            envVarList.add(envVar);
        });
        return envVarList;
    }

    private static Set<String> getSecretKeys(Secrets secrets) {
        if (secrets == null) {
            return Collections.emptySet();
        }
        if(secrets.getType()  == SecretType.MAP){
            MapBasedSecrets mapBasedSecrets = (MapBasedSecrets) secrets;
            return mapBasedSecrets.keySet();
        }
        if(secrets.getType() == SecretType.SET){
            return (SetBasedSecrets) secrets;
        }
        return Collections.emptySet();
    }
}
