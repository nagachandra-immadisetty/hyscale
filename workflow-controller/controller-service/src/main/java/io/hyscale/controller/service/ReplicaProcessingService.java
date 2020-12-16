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
package com.github.srujankujmar.controller.service;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.TableFields;
import com.github.srujankujmar.commons.logger.TableFormatter;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.AuthConfig;
import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.controller.builder.K8sAuthConfigBuilder;
import com.github.srujankujmar.controller.util.StatusUtil;
import com.github.srujankujmar.deployer.services.deployer.Deployer;
import com.github.srujankujmar.deployer.services.model.ReplicaInfo;
import com.github.srujankujmar.deployer.services.processor.PodParentProvider;
import com.github.srujankujmar.deployer.services.provider.K8sClientProvider;
import io.kubernetes.client.openapi.ApiClient;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ReplicaProcessingService {

    @Autowired
    private Deployer deployer;

    @Autowired
    private K8sAuthConfigBuilder configBuilder;
    
    @Autowired
    private K8sClientProvider clientProvider;
    
    @Autowired
    private PodParentProvider podParentProvider;

    public List<ReplicaInfo> getReplicas(String appName, String serviceName, String namespace, boolean latest) throws HyscaleException {
        AuthConfig authConfig = configBuilder.getAuthConfig();
        if (latest) {
            return deployer.getLatestReplicas(authConfig, appName, serviceName, namespace);
        } else {
            return deployer.getReplicas(authConfig, appName, serviceName, namespace, true);
        }
    }
    
    public boolean hasService(AuthConfig authConfig, String appName, String serviceName, String namespace) throws HyscaleException {
        authConfig = authConfig == null ? configBuilder.getAuthConfig() : authConfig;
        ApiClient apiClient = clientProvider.get((K8sAuthorisation) authConfig);
        
        return podParentProvider.hasPodParent(apiClient, appName, serviceName, namespace);
    }

    public boolean doesReplicaExist(String replica, List<ReplicaInfo> replicaInfos) {
        if (replicaInfos == null || replicaInfos.isEmpty()) {
            return false;
        }

        if (StringUtils.isBlank(replica)) {
            return false;
        }

        return replicaInfos.stream().anyMatch(each -> {
            return replica.equals(each.getName());
        });
    }

    public Optional<Map<Integer, ReplicaInfo>> logReplicas(List<ReplicaInfo> replicaInfoList, boolean indexed) {
        Optional<Map<Integer, ReplicaInfo>> optionalMap = Optional.empty();
        if (replicaInfoList == null || replicaInfoList.isEmpty()) {
            return optionalMap;
        }
        TableFormatter.Builder builder = new TableFormatter.Builder();
        if (indexed) {
            builder.addField(TableFields.INDEX.getFieldName(), TableFields.INDEX.getLength());
        }
        builder.addField(TableFields.REPLICA_NAME.getFieldName(), TableFields.REPLICA_NAME.getLength())
                .addField(TableFields.STATUS.getFieldName())
                .addField(TableFields.AGE.getFieldName(), TableFields.AGE.getLength()).build();
        TableFormatter replicaTable = builder.build();
        if (indexed) {
            Map<Integer, ReplicaInfo> integerReplicaInfo = getIndexedReplicaInfo(replicaInfoList);
            integerReplicaInfo.entrySet().forEach(replicaInfoSet -> {
                replicaTable.addRow(ArrayUtils.insert(0,
                        StatusUtil.getReplicasData(replicaInfoSet.getValue()), replicaInfoSet.getKey().toString()));
            });
            optionalMap = Optional.of(integerReplicaInfo);
        } else {
            replicaInfoList.forEach(each -> {
                replicaTable.addRow(ArrayUtils.insert(0, StatusUtil.getReplicasData(each)));
            });
        }
        WorkflowLogger.logTable(replicaTable);
        WorkflowLogger.footer();
        return optionalMap;
    }

    private Map<Integer, ReplicaInfo> getIndexedReplicaInfo(List<ReplicaInfo> replicasInfo) {
        Map<Integer, ReplicaInfo> indexedReplicasInfo = new LinkedHashMap<Integer, ReplicaInfo>();
        Integer replicaIndex = 1;
        for (ReplicaInfo replicaInfo : replicasInfo) {
            indexedReplicasInfo.put(replicaIndex++, replicaInfo);
        }

        return indexedReplicasInfo;
    }


}
