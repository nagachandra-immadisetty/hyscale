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
package io.hyscale.controller.commands.generate;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.PreDestroy;
import javax.validation.constraints.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.hyscale.commons.config.SetupConfig;
import io.hyscale.commons.constants.ToolConstants;
import io.hyscale.commons.constants.ValidationConstants;
import io.hyscale.commons.exception.HyscaleException;
import io.hyscale.commons.logger.WorkflowLogger;
import io.hyscale.controller.activity.ControllerActivity;
import io.hyscale.controller.commands.input.ProfileInput;
import io.hyscale.controller.constants.WorkflowConstants;
import io.hyscale.controller.invoker.ManifestGeneratorComponentInvoker;
import io.hyscale.controller.model.HyscaleCommandSpec;
import io.hyscale.controller.model.EffectiveServiceSpec;
import io.hyscale.controller.model.WorkflowContext;
import io.hyscale.controller.processor.ServiceSpecProcessor;
import io.hyscale.controller.util.CommandUtil;
import io.hyscale.controller.util.ManifestAndDeployHelper;
import io.hyscale.controller.validator.impl.InputSpecPostValidator;
import io.hyscale.controller.validator.impl.InputSpecValidator;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

/**
 * This class executes 'hyscale generate service manifests' command
 * It is a sub-command of the 'hyscale generate service' command
 * @see HyscaleGenerateServiceCommand
 * Every command/sub-command has to implement the Runnable so that
 * whenever the command is executed the {@link #run()}
 * method will be invoked
 *
 * @option appName  name of the app
 * @option serviceSpecs  list of service specs
 * @option profiles  list of profiles for services
 *
 * Eg: hyscale generate service manifests -f svc.hspec -f svcb.hspec -p dev-svc.hprof -a sample
 *
 * Generates the manifests from the given hspec and writes the manifests
 * to <USER.HOME/hyscale/apps/[<appName]/[serviceName]/generated-files/manifests/
 *
 *
 */
@CommandLine.Command(name = "manifests", aliases = {"manifest"},
        description = {"Generates manifests from the given service specs"})
@Component
public class HyscaleGenerateServiceManifestsCommand implements Callable<Integer> {
    
    private static final Logger logger = LoggerFactory.getLogger(HyscaleGenerateServiceManifestsCommand.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Display help message about the specified command")
    private boolean helpRequested = false;

    @Pattern(regexp = ValidationConstants.APP_NAME_REGEX, message = ValidationConstants.INVALID_APP_NAME_MSG)
    @CommandLine.Option(names = {"-a", "--app"}, required = true, description = "Application name")
    private String appName;

    @CommandLine.Option(names = {"-f", "--files"}, required = true, description = "Service specs files.", split = ",")
    private List<File> serviceSpecs;

    @ArgGroup(exclusive = true)
    private ProfileInput profileInput;
    
    @Autowired
    private InputSpecValidator inputSpecValidator;
    
    @Autowired
    private InputSpecPostValidator inputSpecPostValidator;
    
    @Autowired
    private ServiceSpecProcessor serviceSpecProcessor;
    
    @Autowired
    private ManifestGeneratorComponentInvoker manifestGeneratorComponentInvoker;
    
    @Autowired
    private ManifestAndDeployHelper manifestAndDeployHelper;
    
    @Override
    public Integer call() throws Exception {
        if (!CommandUtil.isInputValid(this)) {
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }
        
        HyscaleCommandSpec commandSpec = new HyscaleCommandSpec();
        commandSpec.setAppName(appName);
        commandSpec.setServiceSpecFiles(serviceSpecs);
        if (profileInput != null) {
            commandSpec.setProfileFiles(profileInput.getProfiles());
            commandSpec.setProfileName(profileInput.getProfileName());
        }
        
        if (!inputSpecValidator.validate(commandSpec)) {
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }
        List<File> profiles = commandSpec.getProfileFiles();
        
        List<EffectiveServiceSpec> effectiveServiceSpecList = serviceSpecProcessor.getEffectiveServiceSpec(serviceSpecs,
                profiles);
        
        List<WorkflowContext> contextList = manifestAndDeployHelper.getContextList(effectiveServiceSpecList, appName, null);
        
        manifestAndDeployHelper.getManifestPostValidators().forEach( each -> inputSpecPostValidator.addValidator(each));
        if (!inputSpecPostValidator.validate(contextList)) {
            WorkflowLogger.logPersistedActivities();
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }
        
        boolean isFailed = false;
        for (EffectiveServiceSpec effectiveServiceSpec : effectiveServiceSpecList) {
            WorkflowContext workflowContext = new WorkflowContext();
            String serviceName = effectiveServiceSpec.getServiceMetadata().getServiceName();
            WorkflowLogger.header(ControllerActivity.SERVICE_NAME, serviceName);
            workflowContext.setServiceSpec(effectiveServiceSpec.getServiceSpec());
            workflowContext.setServiceName(serviceName);
            SetupConfig.clearAbsolutePath();
            SetupConfig.setAbsolutePath(effectiveServiceSpec.getServiceSpecFile().getAbsoluteFile().getParent());

            workflowContext.setAppName(appName.trim());
            workflowContext.setEnvName(CommandUtil.getEnvName(effectiveServiceSpec.getServiceMetadata().getEnvName()));
            try {
                manifestGeneratorComponentInvoker.execute(workflowContext);
            } catch (HyscaleException e) {
                logger.error("Error while generating manifest for app: {}, service: {}", appName, serviceName, e);
                isFailed = true;
            }
            if (workflowContext.isFailed()) {
                isFailed = true;
            }
            WorkflowLogger.footer();
            WorkflowLogger.logPersistedActivities();
            CommandUtil.logMetaInfo(
                    SetupConfig.getMountPathOf((String) workflowContext.getAttribute(WorkflowConstants.MANIFESTS_PATH)),
                    ControllerActivity.MANIFESTS_GENERATION_PATH);
        }
        return isFailed ? ToolConstants.HYSCALE_ERROR_CODE : 0;
    }
    
    @PreDestroy
    public void clear() {
        SetupConfig.clearAbsolutePath();
    }
}