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
package com.github.srujankujmar.controller.commands.deploy;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import com.github.srujankujmar.commons.component.ComponentInvoker;
import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.constants.ValidationConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.validator.Validator;
import com.github.srujankujmar.controller.builder.K8sAuthConfigBuilder;
import com.github.srujankujmar.controller.constants.WorkflowConstants;
import com.github.srujankujmar.controller.invoker.DockerfileGeneratorComponentInvoker;
import com.github.srujankujmar.controller.model.*;
import com.github.srujankujmar.controller.profile.ServiceSpecProcessor;
import com.github.srujankujmar.controller.util.CommandUtil;
import com.github.srujankujmar.controller.util.ServiceSpecUtil;
import com.github.srujankujmar.controller.validator.impl.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.Manifest;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import com.github.srujankujmar.controller.model.WorkflowContextBuilder;
import com.github.srujankujmar.controller.commands.input.ProfileArg;
import com.github.srujankujmar.controller.invoker.DeployComponentInvoker;
import com.github.srujankujmar.controller.invoker.ImageBuildComponentInvoker;
import com.github.srujankujmar.controller.invoker.ManifestGeneratorComponentInvoker;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.constraints.Pattern;

/**
 * This class executes 'hyscale deploy service' command
 * It is a sub-command of the 'hyscale deploy' command
 *
 * @option namespace  name of the namespace in which the service to be deployed
 * @option appName   name of the app to logically group your services
 * @option serviceSpecs   list of service specs that are to be deployed
 * @option profiles list of profiles for services
 * @option profile profile name to look for. Profile file should be present for all services in service spec
 * (profiles and profile are mutually exclusive)
 * @option verbose  prints the verbose output of the deployment
 * <p>
 * Eg 1: hyscale deploy service -f svca.hspec -f svcb.hspec -p dev-svca.hprof -n dev -a sample
 * Eg 2: hyscale deploy service -f svca.hspec -f svcb.hspec -P dev -n dev -a sample
 * <p>
 * Responsible for deploying a service with the given 'hspec' to
 * the configured kubernetes cluster.
 * performs a validation of input before starting deployment.
 * Performs functions ranging from image building to manifest generation to deployment.
 * Creates a WorkflowContext to communicate across all deployment stages.
 * @see HyscaleDeployCommand
 * Every command/sub-command has to implement the {@link Callable} so that
 * whenever the command is executed the {@link #call()}
 * method will be invoked
 */
@CommandLine.Command(name = "service", aliases = {"services"},
        description = "Deploys the service to kubernetes cluster", exitCodeOnInvalidInput = 223, exitCodeOnExecutionException = 123)
@Component
public class HyscaleDeployServiceCommand implements Callable<Integer> {

    private static final Logger logger = LoggerFactory.getLogger(HyscaleDeployServiceCommand.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "Displays the help information of the specified command")
    private boolean helpRequested = false;

    @Pattern(regexp = ValidationConstants.NAMESPACE_REGEX, message = ValidationConstants.INVALID_NAMESPACE_MSG)
    @CommandLine.Option(names = {"-n", "--namespace", "-ns"}, required = true, description = "Namespace of the service ")
    private String namespace;

    @Pattern(regexp = ValidationConstants.APP_NAME_REGEX, message = ValidationConstants.INVALID_APP_NAME_MSG)
    @CommandLine.Option(names = {"-a", "--app"}, required = true, description = "Application name")
    private String appName;

    @CommandLine.Option(names = {"-v", "--verbose", "-verbose"}, required = false, description = "Verbose output")
    private boolean verbose = false;

    @CommandLine.Option(names = {"-f", "--files"}, required = true, description = "Service specs files.", split = ",")
    private List<File> serviceSpecsFiles;

    @ArgGroup(exclusive = true, heading = "Profile options", order = 10)
    private ProfileArg profileArg;

    @Autowired
    private InputSpecPostValidator inputSpecPostValidator;

    @Autowired
    private ImageBuildComponentInvoker imageBuildComponentInvoker;

    @Autowired
    private ManifestGeneratorComponentInvoker manifestGeneratorComponentInvoker;

    @Autowired
    private DockerfileGeneratorComponentInvoker dockerfileGeneratorComponentInvoker;

    @Autowired
    private DeployComponentInvoker deployComponentInvoker;

    @Autowired
    private ServiceSpecInputValidator serviceSpecInputValidator;

    @Autowired
    private ServiceSpecProcessor serviceSpecProcessor;

    @Autowired
    private K8sAuthConfigBuilder authConfigBuilder;

    @Autowired
    private DockerDaemonValidator dockerValidator;

    @Autowired
    private RegistryValidator registryValidator;

    @Autowired
    private ManifestValidator manifestValidator;

    @Autowired
    private ClusterValidator clusterValidator;

    @Autowired
    private VolumeValidator volumeValidator;

    private List<Validator<WorkflowContext>> postValidators;

    @PostConstruct
    public void init() {
        this.postValidators = new LinkedList<>();
        this.postValidators.add(dockerValidator);
        this.postValidators.add(registryValidator);
        this.postValidators.add(manifestValidator);
        this.postValidators.add(clusterValidator);
        this.postValidators.add(volumeValidator);
    }

    @Override
    public Integer call() throws Exception {
        if (!CommandUtil.isInputValid(this)) {
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }

        // Validate Service specs with schema
        if (!serviceSpecInputValidator.validate(serviceSpecsFiles)) {
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }

        Map<String, File> serviceVsSpecFile = new HashMap<String, File>();
        for (File serviceSpec : serviceSpecsFiles) {
            serviceVsSpecFile.put(ServiceSpecUtil.getServiceName(serviceSpec), serviceSpec);
        }

        // Process servicespecs to form EffectiveServiceSpec from ProfileArg & ServiceSpecFiles
        List<EffectiveServiceSpec> effectiveServiceSpecs = serviceSpecProcessor.process(profileArg, serviceSpecsFiles);

        // Construct WorkflowContext
        List<WorkflowContext> contextList = new ArrayList<>();
        for (EffectiveServiceSpec each : effectiveServiceSpecs) {
            if (each != null && each.getServiceSpec() != null && each.getServiceMetadata() != null) {
                WorkflowContextBuilder builder = new WorkflowContextBuilder(appName);
                try {
                    builder.withProfile(each.getServiceMetadata().getEnvName());
                    builder.withAuthConfig(authConfigBuilder.getAuthConfig());
                    builder.withService(each.getServiceSpec());
                    builder.withNamespace(namespace);
                    contextList.add(builder.get());
                } catch (HyscaleException e) {
                    logger.error("Error while preparing workflow context ", e);
                    throw e;
                }
            }
        }

        postValidators.forEach(each -> inputSpecPostValidator.addValidator(each));

        if (!inputSpecPostValidator.validate(contextList)) {
            WorkflowLogger.logPersistedActivities();
            return ToolConstants.INVALID_INPUT_ERROR_CODE;
        }

        boolean isCommandFailed = false;
        for (WorkflowContext workflowContext : contextList) {
            String serviceName = workflowContext.getServiceName();
            WorkflowLogger.header(ControllerActivity.SERVICE_NAME, serviceName);

            workflowContext.addAttribute(WorkflowConstants.DEPLOY_START_TIME, System.currentTimeMillis());
            SetupConfig.clearAbsolutePath();
            SetupConfig.setAbsolutePath(serviceVsSpecFile.get(serviceName).getAbsoluteFile().getParent());
            workflowContext.addAttribute(WorkflowConstants.VERBOSE, verbose);

            // clean up service dir before dockerfileGen
            workflowContext.addAttribute(WorkflowConstants.CLEAN_UP_SERVICE_DIR, true);

            executeInvoker(dockerfileGeneratorComponentInvoker, workflowContext);

            executeInvoker(imageBuildComponentInvoker, workflowContext);

            executeInvoker(manifestGeneratorComponentInvoker, workflowContext);

            if (!workflowContext.isFailed()) {
                List<Manifest> manifestList = (List<Manifest>) workflowContext.getAttribute(WorkflowConstants.OUTPUT);
                workflowContext.addAttribute(WorkflowConstants.GENERATED_MANIFESTS, manifestList);
                WorkflowLogger.header(ControllerActivity.STARTING_DEPLOYMENT);
                executeInvoker(deployComponentInvoker, workflowContext);
            }
            logWorkflowInfo(workflowContext);
            isCommandFailed = isCommandFailed ? isCommandFailed : workflowContext.isFailed();
        }

        return isCommandFailed ? ToolConstants.HYSCALE_ERROR_CODE : 0;
    }

    private boolean executeInvoker(ComponentInvoker<WorkflowContext> invoker, WorkflowContext context) {
        if (context.isFailed()) {
            return false;
        }
        try {
            invoker.execute(context);
        } catch (HyscaleException e) {
            logger.error("Error while executing component invoker: {}, for app: {}, service: {}",
                    invoker.getClass(), appName, context.getServiceName(), e);
            context.setFailed(true);
        }
        return context.isFailed() ? false : true;
    }

    private void logWorkflowInfo(WorkflowContext workflowContext) {
        WorkflowLogger.header(ControllerActivity.INFORMATION);

        WorkflowLogger.logPersistedActivities();

        long startTime = (long) workflowContext.getAttribute(WorkflowConstants.DEPLOY_START_TIME);
        CommandUtil.logMetaInfo(String.valueOf((System.currentTimeMillis() - startTime) / 1000) + "s", ControllerActivity.TOTAL_TIME);
        CommandUtil.logMetaInfo(SetupConfig.getMountPathOf((String) workflowContext.getAttribute(WorkflowConstants.DOCKERFILE_INPUT)),
                ControllerActivity.DOCKERFILE_PATH);
        CommandUtil.logMetaInfo(SetupConfig.getMountPathOf((String) workflowContext.getAttribute(WorkflowConstants.BUILD_LOGS)),
                ControllerActivity.BUILD_LOGS);
        CommandUtil.logMetaInfo(SetupConfig.getMountPathOf((String) workflowContext.getAttribute(WorkflowConstants.PUSH_LOGS)),
                ControllerActivity.PUSH_LOGS);
        CommandUtil.logMetaInfo(SetupConfig.getMountPathOf((String) workflowContext.getAttribute(WorkflowConstants.MANIFESTS_PATH)),
                ControllerActivity.MANIFESTS_GENERATION_PATH);
        CommandUtil.logMetaInfo(SetupConfig.getMountPathOf((String) workflowContext.getAttribute(WorkflowConstants.DEPLOY_LOGS)),
                ControllerActivity.DEPLOY_LOGS_AT);
        WorkflowLogger.footer();
        CommandUtil.logMetaInfo((String) workflowContext.getAttribute(WorkflowConstants.SERVICE_IP),
                ControllerActivity.SERVICE_URL);
    }

    @PreDestroy
    public void clear() {
        SetupConfig.clearAbsolutePath();
    }
}
