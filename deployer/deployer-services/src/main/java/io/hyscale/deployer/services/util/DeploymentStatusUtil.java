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
package com.github.srujankujmar.deployer.services.util;

import java.util.List;

import com.github.srujankujmar.deployer.services.model.PodCondition;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.utils.HyscaleStringUtil;
import com.github.srujankujmar.deployer.core.model.DeploymentStatus;
import io.kubernetes.client.openapi.models.V1Pod;

/**
 * 
 *	Utility for Service Deployment status
 */
public class DeploymentStatusUtil {

	/**
	 *Status for service not deployed on cluster
	 * @param serviceName
	 * @return DeploymentStatus
	 */
    public static DeploymentStatus getNotDeployedStatus(String serviceName) {
        if (StringUtils.isBlank(serviceName)) {
            return null;
        }
        DeploymentStatus status = new DeploymentStatus();
        status.setServiceName(serviceName);
        status.setServiceStatus(DeploymentStatus.ServiceStatus.NOT_DEPLOYED);
        status.setAge(null);
        return status;
    }
    
    /**
     * Message from pods not in ready state
     * @param v1PodList
     * @return null if pods are in ready condition, else pods message
     */
    public static String getMessage(List<V1Pod> v1PodList) {
	if (v1PodList == null || v1PodList.isEmpty()) {
	    return null;
	}
        boolean ready = true;
        StringBuilder message = new StringBuilder();
        for (V1Pod v1Pod : v1PodList) {

            ready = ready && K8sPodUtil.checkForPodCondition(v1Pod, PodCondition.READY);
            if (!ready) {
                message.append(K8sPodUtil.getAggregatedStatusOfContainersForPod(v1Pod));
                String failureMessage = K8sPodUtil.getPodMessage(v1Pod);
                if(StringUtils.isBlank(failureMessage)){
                    failureMessage = K8sPodUtil.getFailureMessage(v1Pod);
                }
                if (StringUtils.isNotBlank(failureMessage)) {
                    message.append("::");
                    message.append(failureMessage);
                    message.append(ToolConstants.COMMA);
                }
            }
        }
        if (!ready) {
            return HyscaleStringUtil.removeSuffixStr(message, ToolConstants.COMMA);
        }
        return null;
    }

    public static DeploymentStatus.ServiceStatus getStatus(List<V1Pod> v1PodList) {
        if (v1PodList == null || v1PodList.isEmpty()) {
            return DeploymentStatus.ServiceStatus.NOT_DEPLOYED;
        }
        boolean ready = true;
        for (V1Pod v1Pod : v1PodList) {
            ready = K8sPodUtil.checkForPodCondition(v1Pod, PodCondition.READY) && ready;
        }
        if (ready) {
            return DeploymentStatus.ServiceStatus.RUNNING;
        } else {
            return DeploymentStatus.ServiceStatus.NOT_RUNNING;
        }
    }

    /**
     * DateTime from 1st pod
     * Give estimation of how long ago current status was updated
     * @param v1PodList
     * @return Datetime
     */
    public static DateTime getAge(List<V1Pod> v1PodList) {
        if (v1PodList == null || v1PodList.isEmpty()) {
            return null;
        }
        DateTime dateTime = null;
        V1Pod v1Pod = v1PodList.get(0);
        dateTime = v1Pod.getStatus().getStartTime();
        return dateTime != null ? dateTime : v1Pod.getMetadata().getCreationTimestamp();
    }

}
