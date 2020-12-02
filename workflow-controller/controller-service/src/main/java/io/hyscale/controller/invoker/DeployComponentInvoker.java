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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;

import com.github.srujankujmar.commons.models.K8sAuthorisation;
import com.github.srujankujmar.troubleshooting.integration.models.DiagnosisReport;
import com.github.srujankujmar.troubleshooting.integration.models.ServiceInfo;
import com.github.srujankujmar.troubleshooting.integration.service.TroubleshootService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import com.github.srujankujmar.commons.component.ComponentInvoker;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.io.LogProcessor;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.DeploymentContext;
import com.github.srujankujmar.commons.models.Manifest;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.builder.DeploymentContextBuilder;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import com.github.srujankujmar.controller.hooks.K8SResourcesCleanUpHook;
import com.github.srujankujmar.controller.hooks.VolumeCleanUpHook;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.controller.util.TroubleshootUtil;
import com.github.srujankujmar.deployer.services.config.DeployerConfig;
import com.github.srujankujmar.deployer.services.deployer.Deployer;
import com.github.srujankujmar.deployer.services.exception.DeployerErrorCodes;
import com.github.srujankujmar.deployer.services.model.ServiceAddress;
import com.github.srujankujmar.servicespec.commons.exception.ServiceSpecErrorCodes;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.Port;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

/**
 * Deployer component acts as a bridge between workflow controller and deployer for deploy operation
 * provides link between {@link WorkflowContext} and {@link DeploymentContext}
 */
@Component
public class DeployComponentInvoker extends ComponentInvoker<WorkflowContext> {

    private static final Logger logger = LoggerFactory.getLogger(DeployComponentInvoker.class);

    @Autowired
    private Deployer deployer;

    @Autowired
    private DeploymentContextBuilder deploymentContextBuilder;

    @Autowired
    private DeployerConfig deployerConfig;

    @Autowired
    private LogProcessor logProcessor;

    @Autowired
    private K8SResourcesCleanUpHook k8sResourcesCleanUpHook;

    @Autowired
    private VolumeCleanUpHook volumeCleanUpHook;

    @Autowired
    private TroubleshootService troubleshootService;

    @PostConstruct
    public void init() {
        super.addHook(k8sResourcesCleanUpHook);
        super.addHook(volumeCleanUpHook);
    }

    /**
     * Deploys the service to the kubernetes cluster
     * <p>
     * 1. Deploying the service to the cluster
     * 2. Wait for deployment completion
     * 3. Get service address from the cluster if externalized
     */
    @Override
    protected void doExecute(WorkflowContext context) throws HyscaleException {
        if (context == null || context.isFailed()) {
            return;
        }
        DeploymentContext deploymentContext = deploymentContextBuilder.build(context);

        ServiceSpec serviceSpec = context.getServiceSpec();
        if (serviceSpec == null) {
            context.setFailed(true);
            logger.error("Service Spec is required for deployment");
            throw new HyscaleException(ServiceSpecErrorCodes.SERVICE_SPEC_REQUIRED);
        }

        List<Manifest> mainfestList = deploymentContext.getManifests();
        if (mainfestList == null || mainfestList.isEmpty()) {
            context.setFailed(true);
            logger.error("Manifest is required for deployment");
            throw new HyscaleException(ControllerErrorCodes.MANIFEST_REQUIRED);
        }

        String serviceName = deploymentContext.getServiceName();

        /*
         * Deploys and waits for the deployment completion
         */

        try {
            deployer.deploy(deploymentContext);
            deployer.waitForDeployment(deploymentContext);

        } catch (HyscaleException e) {
            logger.error("Deployment failed with error: {}, running troubleshoot", e.toString());
            String troubleshootMessage = TroubleshootUtil.getTroubleshootMessage(troubleshoot(deploymentContext));
            context.addAttribute(WorkflowConstants.TROUBLESHOOT_MESSAGE, troubleshootMessage);
            throw e;
        } finally {
            writeDeployLogs(context, deploymentContext);
        }
        context.addAttribute(WorkflowConstants.OUTPUT, true);

        Boolean external = serviceSpec.get(HyscaleSpecFields.external, Boolean.class);
        external = external == null ? false : external;
        logger.debug("Checking whether service {} is external {}", serviceName, external);
        if (external) {
            TypeReference<List<Port>> typeReference = new TypeReference<List<Port>>() {
            };
            List<Port> servicePorts = serviceSpec.get(HyscaleSpecFields.ports, typeReference);
            if (servicePorts != null) {
                try {
                    ServiceAddress serviceAddress = deployer.getServiceAddress(deploymentContext);
                    if (serviceAddress != null) {
                        context.addAttribute(WorkflowConstants.SERVICE_IP, serviceAddress.toString());
                    }
                } catch (HyscaleException e) {
                    logger.error("Error while getting service IP address {}, running troubleshoot", e.getMessage());
                    String troubleshootMessage = TroubleshootUtil.getTroubleshootMessage(troubleshoot(deploymentContext));
                    context.addAttribute(WorkflowConstants.TROUBLESHOOT_MESSAGE, troubleshootMessage);
                    throw e;
                }
            }
        }
    }

    private List<DiagnosisReport> troubleshoot(DeploymentContext deploymentContext) {
        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setAppName(deploymentContext.getAppName());
        serviceInfo.setServiceName(deploymentContext.getServiceName());
        try {
            return troubleshootService.troubleshoot(serviceInfo, (K8sAuthorisation) deploymentContext.getAuthConfig(), deploymentContext.getNamespace());
        } catch (HyscaleException e) {
            logger.error("Error while executing troubleshooot serice {}", e);
        }
        return null;
    }

    /**
     * Write deployment logs to file for later access
     *
     * @param context
     * @param deploymentContext
     */
    private void writeDeployLogs(WorkflowContext context, DeploymentContext deploymentContext) {
        String serviceName = deploymentContext.getServiceName();
        try (InputStream is = deployer.logs(deploymentContext.getAuthConfig(), serviceName,
                deploymentContext.getNamespace(), null, serviceName, deploymentContext.getReadLines(), deploymentContext.isTailLogs())) {
            String deploylogFile = deployerConfig.getDeployLogDir(deploymentContext.getAppName(),
                    serviceName);
            logProcessor.writeLogFile(is, deploylogFile);
            context.addAttribute(WorkflowConstants.DEPLOY_LOGS,
                    deployerConfig.getDeployLogDir(deploymentContext.getAppName(), deploymentContext.getServiceName()));
        } catch (IOException e) {
            logger.error("Failed to get deploy logs {}", deploymentContext.getServiceName(), e);
        } catch (HyscaleException ex) {
            logger.error("Failed to get deploy logs {}", deploymentContext.getServiceName(), ex);
        }
    }

    @Override
    protected void onError(WorkflowContext context, HyscaleException he) throws HyscaleException {
        WorkflowLogger.header(ControllerActivity.ERROR);
        Object troubleshootMsgObj = context.getAttribute(WorkflowConstants.TROUBLESHOOT_MESSAGE);
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append(he != null ? he.getMessage() : DeployerErrorCodes.FAILED_TO_APPLY_MANIFEST.getErrorMessage());
        if (troubleshootMsgObj != null) {
            String troubleshootMessage = (String) troubleshootMsgObj;
            logger.error("Troubleshoot message: {}", troubleshootMessage);
            errorMessage.append(ToolConstants.NEW_LINE).append(troubleshootMessage);
        }
        WorkflowLogger.error(ControllerActivity.TROUBLESHOOT, errorMessage.toString());
        context.setFailed(true);
        if (he != null) {
            throw he;
        }
    }
}
