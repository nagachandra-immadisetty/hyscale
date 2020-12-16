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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.component.ComponentInvoker;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.DeploymentContext;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.builder.DeploymentContextBuilder;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import com.github.srujankujmar.controller.hooks.AppDirCleanUpHook;
import com.github.srujankujmar.controller.hooks.ServiceDirCleanUpHook;
import com.github.srujankujmar.controller.hooks.StaleVolumeDetailsHook;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.deployer.services.deployer.Deployer;

/**
 *	Undeploy component acts as a bridge between workflow controller and deployer for undeploy operation
 *	provides link between {@link WorkflowContext} and {@link DeploymentContext}
 */
@Component
public class UndeployComponentInvoker extends ComponentInvoker<WorkflowContext> {

    private static final Logger logger = LoggerFactory.getLogger(UndeployComponentInvoker.class);

    @Autowired
    private Deployer deployer;
    
    @Autowired
    private DeploymentContextBuilder deploymentContextBuilder;

    @Autowired
    private ServiceDirCleanUpHook serviceDirCleanUpHook;

    @Autowired
    private AppDirCleanUpHook appDirCleanUpHook;
    
    @Autowired
    private StaleVolumeDetailsHook staleVolumeDetailsHook;

    @PostConstruct
    public void init() {
        addHook(serviceDirCleanUpHook);
        addHook(appDirCleanUpHook);
        addHook(staleVolumeDetailsHook);
    }
    
    @Override
    protected void doExecute(WorkflowContext context) throws HyscaleException {
        if (context == null) {
            return;
        }
        String namespace = context.getNamespace();
        String appName = context.getAppName();
        String serviceName = context.getServiceName();
        
        WorkflowLogger.header(ControllerActivity.STARTING_UNDEPLOYMENT);
        DeploymentContext deploymentContext = deploymentContextBuilder.build(context);

        try {
            deployer.unDeploy(deploymentContext);
        } catch (HyscaleException ex) {
            WorkflowLogger.footer();
            WorkflowLogger.error(ControllerActivity.UNDEPLOYMENT_FAILED, ex.getMessage());
            logger.error("Error while undeploying app: {}, service: {}, in namespace: {}", appName, serviceName, namespace, ex);
            throw ex;
        } finally {
            WorkflowLogger.footer();
        }
    }

    @Override
    protected void onError(WorkflowContext context, HyscaleException he) throws HyscaleException {
        WorkflowLogger.header(ControllerActivity.ERROR);
        WorkflowLogger.error(ControllerActivity.CAUSE, he != null ?
                he.getMessage() : ControllerErrorCodes.UNDEPLOYMENT_FAILED.getMessage());
        context.setFailed(true);
        if (he != null) {
            throw he;
        }
    }

}
