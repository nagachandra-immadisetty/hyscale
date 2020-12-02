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
package com.github.srujankujmar.controller.commands.undeploy;

import com.github.srujankujmar.controller.builder.K8sAuthConfigBuilder;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.model.WorkflowContext;
import com.github.srujankujmar.controller.util.CommandUtil;
import com.github.srujankujmar.controller.util.UndeployCommandUtil;
import com.github.srujankujmar.controller.validator.impl.ClusterValidator;

import java.util.concurrent.Callable;

import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.constants.ValidationConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.model.WorkflowContextBuilder;
import com.github.srujankujmar.controller.invoker.UndeployComponentInvoker;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 *  This class executes 'hyscale undeploy app' command
 *  It is a sub-command of the 'hyscale undeploy' command
 *  @see HyscaleUndeployCommand
 *  Every command/sub-command has to implement the {@link Callable} so that
 *  whenever the command is executed the {@link #call()}
 *  method will be invoked
 *
 * @option namespace  name of the namespace from which the
 * 					  app has to be undeployed
 * @option appName    name of the app to be undeployed
 *
 * Eg: hyscale undeploy app -a sample -n dev
 *
 * Undeploys the app from the given namespace in the
 * configured kubernetes cluster.
 * Undeploys all the resources except the pvcs from the cluster related 
 * to the app from the namespace.
 * @Note Note: Undeploy does not clear the namespace
 *
 */
@Command(name = "app", description = "Undeploys app from the kubernetes cluster")
@Component
public class HyscaleUndeployAppCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(HyscaleUndeployAppCommand.class);

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display the help information of the specified command")
    private boolean helpRequested = false;

    @Pattern(regexp = ValidationConstants.NAMESPACE_REGEX, message = ValidationConstants.INVALID_NAMESPACE_MSG)
    @Option(names = {"-n", "--namespace", "-ns"}, required = true, description = "Namespace of the deployed app")
    private String namespace;

    @Pattern(regexp = ValidationConstants.APP_NAME_REGEX, message = ValidationConstants.INVALID_APP_NAME_MSG)
    @Option(names = {"-a", "--app"}, required = true, description = "Application name")
    private String appName;

    @Autowired
    private ClusterValidator clusterValidator;

    @Autowired
    private UndeployComponentInvoker undeployComponentInvoker;

    @Autowired
    private K8sAuthConfigBuilder authConfigBuilder;

    @Override
    public Integer call() throws Exception {
        if (!CommandUtil.isInputValid(this)) {
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }

		WorkflowContext workflowContext = new WorkflowContextBuilder(appName).withNamespace(namespace).
				withAuthConfig(authConfigBuilder.getAuthConfig()).get();
        workflowContext.addAttribute(WorkflowConstants.CLEAN_UP_APP_DIR, true);

        if (!clusterValidator.validate(workflowContext)) {
            WorkflowLogger.logPersistedActivities();
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }

        WorkflowLogger.header(ControllerActivity.APP_NAME, appName);
        try {
            undeployComponentInvoker.execute(workflowContext);
        } catch (HyscaleException e) {
            logger.error("Error while undeploying app: {}, in namespace: {}", appName, namespace, e);
            throw e;
        } finally {
            UndeployCommandUtil.logUndeployInfo();
        }
        return ToolConstants.HYSCALE_SUCCESS_CODE;
    }

}
