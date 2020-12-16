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
package com.github.srujankujmar.builder.services.docker.impl;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.srujankujmar.commons.commands.provider.ImageCommandProvider;
import com.github.srujankujmar.builder.services.util.DockerImageUtil;
import com.github.srujankujmar.builder.services.util.ImageLogUtil;
import com.github.srujankujmar.builder.services.spring.DockerBinaryCondition;
import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.models.CommandResult;
import com.github.srujankujmar.commons.utils.ImageMetadataProvider;
import com.github.srujankujmar.commons.utils.ObjectMapperFactory;
import com.github.srujankujmar.servicespec.commons.model.service.Image;
import com.github.srujankujmar.servicespec.commons.util.ImageUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.builder.services.config.ImageBuilderConfig;
import com.github.srujankujmar.builder.services.docker.HyscaleDockerClient;
import com.github.srujankujmar.builder.services.exception.ImageBuilderErrorCodes;
import com.github.srujankujmar.builder.core.models.BuildContext;
import com.github.srujankujmar.builder.core.models.DockerImage;
import com.github.srujankujmar.builder.core.models.ImageBuilderActivity;
import com.github.srujankujmar.commons.commands.CommandExecutor;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.servicespec.commons.model.service.Dockerfile;
import com.github.srujankujmar.commons.models.Status;

import javax.annotation.PostConstruct;

@Component
@Conditional(DockerBinaryCondition.class)
public class DockerBinaryClient implements HyscaleDockerClient {

    private static final Logger logger = LoggerFactory.getLogger(DockerBinaryClient.class);

    @Autowired
    private ImageCommandProvider imageCommandProvider;

    @Autowired
    private ImageMetadataProvider imageMetadataProvider;

    @Autowired
    private DockerImageUtil dockerImageUtil;

    @Autowired
    private ImageLogUtil imageLogUtil;

    @Autowired
    private ImageBuilderConfig imageBuilderConfig;

    @PostConstruct
    public void init() {
        logger.debug("Using Docker Binary for Image Building");
    }

    @Override
    public boolean isDockerRunning() {
        String command = imageCommandProvider.dockerImages();
        logger.debug("Docker Daemon running check command: {}", command);
        return CommandExecutor.execute(command);
    }

    @Override
    public boolean checkForDocker() {
        String command = imageCommandProvider.dockerVersion();
        logger.debug("Docker Installed check command: {}", command);
        return CommandExecutor.execute(command);
    }

    @Override
    public void deleteImages(List<String> imageIds, boolean force) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        boolean isSuccess = CommandExecutor
                .execute(imageCommandProvider.removeDockerImages(imageIds.stream().collect(Collectors.toSet()), force));

        logger.debug("Image clean up {}", isSuccess ? Status.DONE.getMessage() : Status.FAILED.getMessage());
    }

    @Override
    public void deleteImage(String imageId, boolean force) {
        if (StringUtils.isBlank(imageId)) {
            return;
        }
        deleteImages(Arrays.asList(imageId), force);
    }

    @Override
    public DockerImage build(Dockerfile dockerfile, String tag, BuildContext context) throws HyscaleException {
        WorkflowLogger.startActivity(ImageBuilderActivity.IMAGE_BUILD);
        String appName = context.getAppName();
        String serviceName = context.getServiceName();
        boolean verbose = context.isVerbose();

        String dockerBuildCommand = imageCommandProvider.dockerBuildCommand(appName, serviceName, tag, dockerfile.getDockerfilePath(),
                dockerfile.getTarget(),
                dockerfile.getArgs());

        logger.debug("Docker build command {}", dockerBuildCommand);

        String logFilePath = imageBuilderConfig.getDockerBuildlog(appName, serviceName);
        File logFile = new File(logFilePath);
        context.setBuildLogs(logFilePath);

        // TODO keep continuation activity for user
        boolean status = CommandExecutor.executeInDir(dockerBuildCommand, logFile,
                dockerfile.getPath() != null ? SetupConfig.getAbsolutePath(dockerfile.getPath()) : null);
        if (!status) {
            WorkflowLogger.endActivity(Status.FAILED);
            logger.error("Failed to build docker image");
        } else {
            WorkflowLogger.endActivity(Status.DONE);
        }

        if (verbose) {
            imageLogUtil.readBuildLogs(appName, serviceName);
        }

        if (!status) {
            throw new HyscaleException(ImageBuilderErrorCodes.FAILED_TO_BUILD_IMAGE);
        }

        DockerImage dockerImage = new DockerImage();
        dockerImage.setName(imageMetadataProvider.getBuildImageName(appName, serviceName));
        dockerImage.setTag(tag);

        return dockerImage;
    }

    /**
     * Check docker exists, If stack image as service image pull, tag
     * Push image if required else return
     *
     * @throws HyscaleException
     */
    @Override
    public void push(Image image, BuildContext buildContext) throws HyscaleException {
        WorkflowLogger.startActivity(ImageBuilderActivity.IMAGE_PUSH);
        String appName = buildContext.getAppName();
        String serviceName = buildContext.getServiceName();
        boolean verbose = buildContext.isVerbose();
        String imageFullPath = ImageUtil.getImage(image);
        String pushImageCommand = imageCommandProvider.dockerPush(imageFullPath);
        String logFilePath = imageBuilderConfig.getDockerPushLogDir(appName, serviceName);
        File logFile = new File(logFilePath);
        buildContext.setPushLogs(logFilePath);
        // TODO keep continuation activity for user , launch a new thread & waitFor
        boolean status = CommandExecutor.execute(pushImageCommand, logFile);
        if (!status) {
            WorkflowLogger.endActivity(Status.FAILED);
            logger.error("Failed to push docker image");
        } else {
            String inspectCommand = imageCommandProvider.dockerInspect(imageFullPath);
            CommandResult result = CommandExecutor.executeAndGetResults(inspectCommand);
            buildContext.setImageShaSum(getImageDigest(result));
            WorkflowLogger.endActivity(Status.DONE);
        }

        if (verbose) {
            imageLogUtil.readPushLogs(appName, serviceName);
        }

        if (!status) {
            throw new HyscaleException(ImageBuilderErrorCodes.FAILED_TO_PUSH_IMAGE);
        }
    }

    @Override
    public void pull(String image, BuildContext context) throws HyscaleException {
        WorkflowLogger.startActivity(ImageBuilderActivity.IMAGE_PULL);

        if (StringUtils.isBlank(image)) {
            WorkflowLogger.endActivity(Status.SKIPPING);
            return;
        }
        try {
            dockerImageUtil.pullImage(image);
        } catch (HyscaleException e) {
            WorkflowLogger.endActivity(Status.FAILED);
            logger.error(e.toString());
            throw e;
        }
        WorkflowLogger.endActivity(Status.DONE);
    }

    @Override
    public void tag(String source, Image dest) throws HyscaleException {
        WorkflowLogger.startActivity(ImageBuilderActivity.IMAGE_TAG);

        if (StringUtils.isBlank(source)) {
            WorkflowLogger.endActivity(Status.SKIPPING);
            return;
        }
        try {
            dockerImageUtil.tagImage(source, ImageUtil.getImage(dest));
        } catch (HyscaleException e) {
            WorkflowLogger.endActivity(Status.FAILED);
            logger.error(e.toString());
            throw e;
        }
        WorkflowLogger.endActivity(Status.DONE);
    }

    /**
     * Gets latest digest from inspect image command result.
     *
     * @param result CommandResult obtained after executing docker inspect command.
     *               1.result is null  - null
     *               2.result not null - null if no digests
     *               - last digest if digests exist
     * @return digest latest digest from image command result.
     */
    private String getImageDigest(CommandResult result) {
        if (result == null || result.getExitCode() > 0 || StringUtils.isBlank(result.getCommandOutput())) {
            return null;
        }
        ObjectMapper mapper = ObjectMapperFactory.jsonMapper();
        try {
            JsonNode node = mapper.readTree(result.getCommandOutput());
            JsonNode digestNode = null;
            if (node.isArray()) {
                digestNode = node.get(0).get("RepoDigests");
            } else {
                digestNode = node.get("RepoDigests");
            }
            if (digestNode == null) {
                return null;
            }
            List<String> digestList = mapper.convertValue(digestNode, new TypeReference<List<String>>() {
            });
            String latestRepoDigest = digestList.get(digestList.size() - 1);
            if (StringUtils.isNotBlank(latestRepoDigest) && latestRepoDigest.contains(ToolConstants.AT_SIGN)) {
                return latestRepoDigest.split(ToolConstants.AT_SIGN)[1];
            }
        } catch (IOException e) {
            logger.debug("Error while processing image inspect results ", e);
        }
        return null;
    }

    @Override
    public List<String> getImageIds(String imageName, Map<String, String> labels) throws HyscaleException {
        String imageIdsAsString = CommandExecutor
                .executeAndGetResults(imageCommandProvider.dockerImageIds(imageName, labels))
                .getCommandOutput();
        String[] imageIds = StringUtils.isNotBlank(imageIdsAsString) ? imageIdsAsString.split("\\s+") : null;
        if (imageIds == null || imageIds.length == 0) {
            logger.debug("No images found to clean from the host machine");
            return null;
        }
        // Need to preserve the order of output, hence a LinkedHashset
        return new LinkedList<>(Arrays.asList(imageIds));
    }
}
