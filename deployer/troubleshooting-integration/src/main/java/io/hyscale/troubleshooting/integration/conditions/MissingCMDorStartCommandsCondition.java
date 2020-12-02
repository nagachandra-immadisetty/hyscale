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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.github.srujankujmar.commons.commands.CommandExecutor;
import com.github.srujankujmar.commons.commands.provider.ImageCommandProvider;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.CommandResult;
import com.github.srujankujmar.commons.utils.ObjectMapperFactory;
import com.github.srujankujmar.deployer.core.model.ResourceKind;
import com.github.srujankujmar.troubleshooting.integration.errors.TroubleshootErrorCodes;
import com.github.srujankujmar.troubleshooting.integration.models.*;
import com.github.srujankujmar.troubleshooting.integration.actions.DockerfileCMDMissingAction;
import io.kubernetes.client.openapi.models.V1Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//TODO JAVADOC
@Component
public class MissingCMDorStartCommandsCondition extends ConditionNode<TroubleshootingContext> {

    private static final Logger logger = LoggerFactory.getLogger(MissingCMDorStartCommandsCondition.class);
    private static final String DOCKER_INSTALLATION_NOTFOUND_MESSAGE = "Docker is not installed";
    private static final String IMAGE_NOT_FOUND_LOCALLY = "Image %s is not found locally to inspect";

    private Predicate<TroubleshootingContext> containerStartCommandCheck;

    @Autowired
    private ImageCommandProvider commandProvider;

    @Autowired
    private DockerfileCMDMissingAction dockerfileCMDMissingAction;

    @Autowired
    private MultipleContainerRestartsCondition multipleContainerRestartsCondition;


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

        if (podsList == null && podsList.isEmpty()) {
            report.setReason(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.formatReason(context.getServiceInfo().getServiceName()));
            report.setRecommendedFix(AbstractedErrorMessage.SERVICE_NOT_DEPLOYED.getMessage());
            context.addReport(report);
            throw new HyscaleException(TroubleshootErrorCodes.SERVICE_IS_NOT_DEPLOYED, context.getServiceInfo().getServiceName());
        }

        V1Pod pod = (V1Pod) podsList.get(0);

        if (checkForStartCommands(pod)) {
            return false;
        }

        String image = getImageFromPods(pod);
        if (image == null) {
            report.setReason(AbstractedErrorMessage.CANNOT_INFER_ERROR.getReason());
            report.setRecommendedFix(AbstractedErrorMessage.CANNOT_INFER_ERROR.getMessage());
            context.addReport(report);
            throw new HyscaleException(TroubleshootErrorCodes.SERVICE_IS_NOT_DEPLOYED, context.getServiceInfo().getServiceName());
        }

        String dockerInstallCommand = commandProvider.dockerVersion();
        if (!CommandExecutor.execute(dockerInstallCommand)) {
            report.setRecommendedFix(DOCKER_INSTALLATION_NOTFOUND_MESSAGE);
            context.addReport(report);
            return false;
        }


        CommandResult result = CommandExecutor.executeAndGetResults(commandProvider.dockerInspect(image));
        if (result == null || StringUtils.isEmpty(result.getCommandOutput()) || result.getExitCode() != 0) {
            report.setRecommendedFix(String.format(IMAGE_NOT_FOUND_LOCALLY, image));
            context.addReport(report);
            return false;
        }

        // return true when CMD is Missing in Dockerfile
        return checkForDockerfileCMD(result.getCommandOutput());
    }

    private boolean checkForDockerfileCMD(String commandOutput) {
        if (StringUtils.isEmpty(commandOutput)) {
            return true;
        }
        ObjectMapper mapper = ObjectMapperFactory.jsonMapper();
        try {
            JsonNode node = mapper.readTree(commandOutput);
            JsonNode cmdNode = null;
            if (node.isArray()) {
                cmdNode = node.get(0).get("Config").get("Cmd");
            } else {
                cmdNode = node.get("Config").get("Cmd");
            }
            if (cmdNode == null) {
                return true;
            }

                    /* If CMD is an array and is not empty, then it means CMD is
                        present in the dockerfile.
                     */
            if (cmdNode.isArray()) {
                ArrayNode arrayNode = (ArrayNode) cmdNode;
                return arrayNode.isEmpty();
            } else {
                // CMD is not present in Dockerfile
                return true;
            }
        } catch (IOException e) {
            logger.error("Error while processing image inspect results ", e);
            //TODO Stop the troubleshooting and inform user to do check this condition
            return false;
        }
    }

    @Override
    public Node<TroubleshootingContext> onSuccess() {
        return dockerfileCMDMissingAction;
    }

    @Override
    public Node<TroubleshootingContext> onFailure() {
        return multipleContainerRestartsCondition;
    }

    @Override
    public String describe() {
        return "Is either of Dockerfile CMD or Args in kubernetes yaml defined ?";
    }

    private boolean checkForStartCommands(V1Pod pod) {
        if (pod == null) {
            return false;
        }
        return !StringUtils.isEmpty(pod.getSpec().getContainers().get(0).getArgs()) || !StringUtils.isEmpty(pod.getSpec().getContainers().get(0).getCommand());
    }

    private String getImageFromPods(V1Pod pod) {
        if (pod == null) {
            return null;
        }
        return pod.getSpec().getContainers().get(0).getImage();
    }
}
