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
package com.github.srujankujmar.troubleshooting.integration.conditions;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.deployer.services.model.PodCondition;
import com.github.srujankujmar.deployer.services.util.K8sPodUtil;
import com.github.srujankujmar.troubleshooting.integration.errors.TroubleshootErrorCodes;
import com.github.srujankujmar.troubleshooting.integration.models.*;
import io.kubernetes.client.openapi.models.V1Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class checks for any Unschedulable pod or any pod with {@link PodCondition#POD_SCHEDULED}
 * condition = true . In either of the cases the pod is not scheduled to any node. To know more
 * about pod condition, refer {@see https://kubernetes.io/docs/concepts/workloads/pods/pod-lifecycle/#pod-conditions}
 */

@Component
public class PodScheduleCondition extends ConditionNode<TroubleshootingContext> {

    private static final Logger logger = LoggerFactory.getLogger(PodScheduleCondition.class);

    @Override
    public boolean decide(TroubleshootingContext context) throws HyscaleException {
        List<TroubleshootingContext.ResourceInfo> resourceInfos = context.getResourceInfos().get(ResourceKind.POD.getKind());
        DiagnosisReport report = new DiagnosisReport();
        if (resourceInfos == null || resourceInfos.isEmpty()) {
            report.setReason(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.formatReason(context.getServiceInfo().getServiceName()));
            report.setRecommendedFix(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.getMessage());
            context.addReport(report);
            throw new HyscaleException(TroubleshootErrorCodes.SERVICE_IS_NOT_DEPLOYED, context.getServiceInfo().getServiceName());
        }

        List<V1Pod> podsList = resourceInfos.stream().filter(each -> {
            return each != null && each.getResource() != null && each.getResource() instanceof V1Pod;
        }).map(pod -> {
            return (V1Pod) pod.getResource();
        }).collect(Collectors.toList());

        if (podsList == null || podsList.isEmpty()) {
            report.setReason(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.formatReason(context.getServiceInfo().getServiceName()));
            report.setRecommendedFix(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.getMessage());
            context.addReport(report);
            throw new HyscaleException(TroubleshootErrorCodes.SERVICE_IS_NOT_DEPLOYED, context.getServiceInfo().getServiceName());
        }

        return podsList.stream().anyMatch(pod -> {
            if (pod instanceof V1Pod) {
                V1Pod v1Pod = (V1Pod) pod;
                return !K8sPodUtil.checkForPodCondition((V1Pod) pod, PodCondition.POD_SCHEDULED) ||
                        K8sPodUtil.checkForPodCondition(v1Pod, PodCondition.UNSCHEDULABLE);
            }
            return false;
        });
    }


    @Override
    public Node<TroubleshootingContext> onSuccess() {
        return null;
    }

    @Override
    public Node<TroubleshootingContext> onFailure() {
        return null;
    }


    @Override
    public String describe() {
        return "Are all pods scheduled ?";
    }

}
