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
package com.github.srujankujmar.controller.invoker;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.component.ComponentInvoker;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.DeploymentContext;
import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.controller.builder.DeploymentContextBuilder;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.controller.util.TroubleshootUtil;
import com.github.srujankujmar.deployer.core.model.DeploymentStatus;
import com.github.srujankujmar.deployer.services.deployer.Deployer;
import com.github.srujankujmar.troubleshooting.integration.actions.ServiceNotDeployedAction;
import com.github.srujankujmar.troubleshooting.integration.models.DiagnosisReport;
import com.github.srujankujmar.troubleshooting.integration.models.ServiceInfo;
import com.github.srujankujmar.troubleshooting.integration.models.TroubleshootingContext;
import com.github.srujankujmar.troubleshooting.integration.service.TroubleshootService;

/**
 * ServiceStatus component acts as a bridge between workflow controller and deployer for status operation
 * provides link between {@link WorkflowContext} and {@link DeploymentContext}
 * Responsible for calling troubleshooting in case service is in Not running state
 * @author tushar
 *
 */
@Component
public class StatusComponentInvoker extends ComponentInvoker<WorkflowContext> {
    
    private static final Logger logger = LoggerFactory.getLogger(StatusComponentInvoker.class);

    @Autowired
    private Deployer deployer;
    
    @Autowired
    private DeploymentContextBuilder deploymentContextBuilder;
    
    @Autowired
    private TroubleshootService troubleshootService;
    
    @Autowired
    private ServiceNotDeployedAction serviceNotDeployedAction;
    
    @Override
    protected void doExecute(WorkflowContext context) throws HyscaleException {
        String serviceName = context.getServiceName();
        
        DeploymentContext deploymentContext = deploymentContextBuilder.build(context);
        deploymentContext.setWaitForReadiness(false);
        
        if (StringUtils.isNotBlank(serviceName)) {
            // Service status command
            DeploymentStatus serviceStatus = deployer.getServiceDeploymentStatus(deploymentContext);
            if (serviceStatus != null) {
                serviceStatus.setMessage(getServiceMessage(serviceStatus, deploymentContext));
            }
            context.addAttribute(WorkflowConstants.DEPLOYMENT_STATUS, serviceStatus);
            return;
        }
        // App status command
        List<DeploymentStatus> deploymentStatusList = deployer.getDeploymentStatus(deploymentContext);
        if (deploymentStatusList != null) {
            for (DeploymentStatus serviceStatus : deploymentStatusList) {
                if (serviceStatus != null) {
                    serviceStatus.setMessage(getServiceMessage(serviceStatus, deploymentContext));
                }
            }
            context.addAttribute(WorkflowConstants.DEPLOYMENT_STATUS_LIST, deploymentStatusList);
        }
        
    }
    
    private String getServiceMessage(DeploymentStatus serviceStatus, DeploymentContext context) {
        if (serviceStatus == null) {
            return null;
        }
        /*
         * Fetch service name from service status as
         * context can have the previous service name in case of app deploy
         */
        context.setServiceName(serviceStatus.getServiceName());
        List<DiagnosisReport> diagnosisReports = null;
        if (!DeploymentStatus.ServiceStatus.SCALING_DOWN.equals(serviceStatus.getServiceStatus())) {
            if (DeploymentStatus.ServiceStatus.NOT_DEPLOYED.equals(serviceStatus.getServiceStatus())) {
                TroubleshootingContext toubleshootingContext = new TroubleshootingContext();
                serviceNotDeployedAction.process(toubleshootingContext);
                diagnosisReports = toubleshootingContext.getDiagnosisReports();
            } else if (!DeploymentStatus.ServiceStatus.RUNNING.equals(serviceStatus.getServiceStatus())) {
                diagnosisReports = troubleshoot(context);
            }
        }
        return TroubleshootUtil.getTroubleshootMessage(diagnosisReports);
    }
    
    private List<DiagnosisReport> troubleshoot(DeploymentContext deploymentContext) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setAppName(deploymentContext.getAppName());
        serviceInfo.setServiceName(deploymentContext.getServiceName());
        try {
            return troubleshootService.troubleshoot(serviceInfo, (K8sAuthorisation) deploymentContext.getAuthConfig(), 
                    deploymentContext.getNamespace());
        } catch (HyscaleException e) {
            logger.error("Error while executing troubleshooot serice {}", e);
        }
        return null;
    }
    
    @Override
    protected void onError(WorkflowContext context, HyscaleException th) throws HyscaleException {
        if (th != null) {
            logger.error("Error while getting status", th.getMessage());
            throw th;
        }
    }

}
