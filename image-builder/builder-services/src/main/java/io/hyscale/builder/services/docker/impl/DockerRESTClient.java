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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.ws.rs.ProcessingException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.AuthConfig;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;
import com.github.dockerjava.api.model.ResponseItem.ErrorDetail;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.PushImageResultCallback;

import com.github.srujankujmar.builder.core.models.BuildContext;
import com.github.srujankujmar.builder.core.models.DockerImage;
import com.github.srujankujmar.builder.core.models.ImageBuilderActivity;
import com.github.srujankujmar.builder.services.config.ImageBuilderConfig;
import com.github.srujankujmar.builder.services.constants.DockerImageConstants;
import com.github.srujankujmar.builder.services.docker.HyscaleDockerClient;
import com.github.srujankujmar.builder.services.exception.ImageBuilderErrorCodes;
import com.github.srujankujmar.builder.services.spring.DockerClientCondition;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.io.HyscaleFilesUtil;
import com.github.srujankujmar.commons.logger.ActivityContext;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.models.ImageRegistry;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.commons.utils.ImageMetadataProvider;
import com.github.srujankujmar.servicespec.commons.model.service.Dockerfile;
import com.github.srujankujmar.servicespec.commons.model.service.Image;
import com.github.srujankujmar.servicespec.commons.util.ImageUtil;

/**
 * DockerClientImpl is docker client to docker daemon to perform all
 * image actions like pull image, tag image, push image etc.
 * It connects to the docker daemon through an environment
 * variable @see ImageBuilderConfig.getDockerHost() or through to
 * the default host based on the operating system.
 */

@Component
@Conditional(DockerClientCondition.class)
public class DockerRESTClient implements HyscaleDockerClient {

    private static final Logger logger = LoggerFactory.getLogger(DockerRESTClient.class);

    private static final String SHA256 = "sha256";

    @Autowired
    private ImageBuilderConfig imageBuilderConfig;

    @Autowired
    private ImageMetadataProvider imageMetadataProvider;

    private DefaultDockerClientConfig clientConfig;

    @PostConstruct
    public void init() {
        clientConfig
                = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(imageBuilderConfig.getDockerHost()).build();
        logger.debug("Using docker rest client for image building");
    }

    public DockerClient getDockerClient() {
        return DockerClientBuilder.getInstance(clientConfig).build();
    }

    @Override
    public boolean isDockerRunning() {
        DockerClient dockerClient = getDockerClient();
        ListImagesCmd listImagesCmd = dockerClient.listImagesCmd();
        try {
            listImagesCmd.exec();
        } catch (ProcessingException | DockerException e) {
            logger.error("Error while validating isDockerRunning", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean checkForDocker() {
        DockerClient dockerClient = getDockerClient();
        VersionCmd versionCmd = dockerClient.versionCmd();
        try {
            versionCmd.exec();
        } catch (ProcessingException | DockerException e) {
            logger.error("Error while checking for docker", e);
            return false;
        }
        return true;
    }

    @Override
    public void deleteImages(List<String> imageIds, boolean force) {
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }
        DockerClient dockerClient = getDockerClient();
        imageIds.stream().forEach(imageId -> {
            RemoveImageCmd removeCmd = dockerClient.removeImageCmd(imageId).withForce(force);
            try {
                removeCmd.exec();
            } catch (DockerException e) {
                logger.error("Error while deleting image: {}, ignoring", imageId, e);
            }
        });
    }

    @Override
    public void deleteImage(String imageId, boolean force) {
        if (StringUtils.isBlank(imageId)) {
            return;
        }
        deleteImages(Arrays.asList(imageId), force);
    }

    @Override
    public DockerImage build(Dockerfile dockerfile, String tag, BuildContext buildContext) throws HyscaleException {
        ActivityContext buildActivity = new ActivityContext(ImageBuilderActivity.IMAGE_BUILD);
        WorkflowLogger.startActivity(buildActivity);
        // validate dockerfile
        try {
            validate(dockerfile);
        } catch (HyscaleException e) {
            handleOutput(false, buildActivity, Status.FAILED);
            logger.error("Failed to validate dockerfile before build", e);
            throw e;
        }
        String appName = buildContext.getAppName();
        String serviceName = buildContext.getServiceName();

        String buildImageName = imageMetadataProvider.getBuildImageNameWithTag(appName, serviceName, tag);
        BuildImageCmd buildImageCmd = getBuildCommand(dockerfile, buildImageName);

        String logFilePath = imageBuilderConfig.getDockerBuildlog(appName, serviceName);
        buildContext.setBuildLogs(logFilePath);
        if (buildContext.isVerbose()) {
            WorkflowLogger.header(ImageBuilderActivity.BUILD_LOGS);
        }
        BuildImageResultCallback callback = new BuildImageResultCallback() {
            @Override
            public void onNext(BuildResponseItem item) {
                ErrorDetail errorDetail = item.getErrorDetail();
                String message = item.isErrorIndicated() && errorDetail != null ? errorDetail.getMessage() : item.getStream();
                handleOutput(message, logFilePath, buildActivity, buildContext.isVerbose());
                if (item.isErrorIndicated()) {
                    logger.error("Error while building image: {}", errorDetail);
                    onError(new HyscaleException(ImageBuilderErrorCodes.FAILED_TO_BUILD_IMAGE));
                }
                super.onNext(item);
            }
        };
        try {
            buildImageCmd.exec(callback).awaitCompletion();
        } catch (RuntimeException e) {
            handleOutput(buildContext.isVerbose(), buildActivity, Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_BUILD_IMAGE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleOutput(buildContext.isVerbose(), buildActivity, Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_BUILD_IMAGE);
        }

        handleOutput(buildContext.isVerbose(), buildActivity, Status.DONE);
        DockerImage dockerImage = new DockerImage();
        dockerImage.setName(imageMetadataProvider.getBuildImageName(appName, serviceName));
        dockerImage.setTag(tag);
        return dockerImage;
    }

    private void validate(Dockerfile dockerfileModel) throws HyscaleException {
        if (dockerfileModel == null) {
            throw new HyscaleException(ImageBuilderErrorCodes.DOCKERFILE_REQUIRED);
        }
        File dockerfile = getDockerFile(dockerfileModel.getDockerfilePath());
        if (!dockerfile.exists() || dockerfile.isDirectory()) {
            throw new HyscaleException(ImageBuilderErrorCodes.DOCKERFILE_NOT_FOUND, dockerfile.getAbsolutePath());
        }
    }

    private BuildImageCmd getBuildCommand(Dockerfile dockerfile, String tag) {
        Set<String> tags = new HashSet<>();
        tags.add(tag);
        Map<String, String> labels = imageMetadataProvider.getImageOwnerLabel();

        DockerClient dockerClient = getDockerClient();
        BuildImageCmd buildImageCmd = dockerClient.buildImageCmd()
                .withDockerfile(getDockerFile(dockerfile.getDockerfilePath()))
                .withPull(true)
                .withNoCache(true)
                .withLabels(labels)
                .withTags(tags)
                .withTarget(dockerfile.getTarget());
        if (dockerfile.getPath() != null) {
            buildImageCmd.withBaseDirectory(new File(dockerfile.getPath()));
        }
        if (dockerfile.getArgs() != null && !dockerfile.getArgs().isEmpty()) {
            dockerfile.getArgs().entrySet().stream().forEach(each -> buildImageCmd.withBuildArg(each.getKey(), each.getValue()));
        }
        return buildImageCmd;
    }

    private File getDockerFile(String dockerFilePath) {
        return new File(dockerFilePath + ToolConstants.LINUX_FILE_SEPARATOR + DockerImageConstants.DOCKERFILE_NAME);
    }
    
    @Override
    public void pull(String image, BuildContext context) throws HyscaleException {
        ActivityContext pullActivity = new ActivityContext(ImageBuilderActivity.IMAGE_PULL);
        WorkflowLogger.startActivity(pullActivity);
        if (StringUtils.isBlank(image)) {
            handleOutput(false, pullActivity, Status.SKIPPING);
            return;
        }
        DockerClient dockerClient = getDockerClient();
        AuthConfig authConfig = getAuthConfig(context.getPullRegistry());
        PullImageCmd pullImageCmd = dockerClient.pullImageCmd(image);
        if (authConfig != null) {
            pullImageCmd.withAuthConfig(authConfig);
        }
        try {
            pullImageCmd.exec(new PullImageResultCallback() {
                @Override
                public void onNext(PullResponseItem item) {
                    super.onNext(item);
                    WorkflowLogger.continueActivity(pullActivity);
                }

            }).awaitCompletion();
        } catch (RuntimeException e) {
            logger.error("Error while pulling the image {}", image, e);
            handleOutput(false, pullActivity, Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_PULL_IMAGE);
        } catch (InterruptedException e) {
            logger.error("Error while pulling the image {}", image, e);
            Thread.currentThread().interrupt();
            handleOutput(false, pullActivity, Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_PULL_IMAGE);
        }
        handleOutput(false, pullActivity, Status.DONE);
    }

    @Override
    public void tag(String source, Image dest) throws HyscaleException {
        WorkflowLogger.startActivity(ImageBuilderActivity.IMAGE_TAG);
        if (StringUtils.isBlank(source)) {
            WorkflowLogger.endActivity(Status.SKIPPING);
            return;
        }
        DockerClient dockerClient = getDockerClient();
        try {
            String tag = StringUtils.isNotBlank(dest.getTag()) ? dest.getTag() : ImageUtil.DEFAULT_TAG;
            TagImageCmd tagImageCmd = dockerClient.tagImageCmd(source, ImageUtil.getImageWithoutTag(dest), tag);
            tagImageCmd.exec();
        } catch (DockerException e) {
            logger.error("Error while tagging image",e);
            WorkflowLogger.endActivity(Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_TAG_IMAGE);
        }
        WorkflowLogger.endActivity(Status.DONE);
    }


    @Override
    public void push(Image image, BuildContext buildContext) throws HyscaleException {

        ActivityContext pushActivity = new ActivityContext(ImageBuilderActivity.IMAGE_PUSH);
        WorkflowLogger.startActivity(pushActivity);
        
        AuthConfig authConfig = getAuthConfig(buildContext.getPushRegistry());

        DockerClient dockerClient = getDockerClient();

        // Push image
        String logFilePath = imageBuilderConfig.getDockerPushLogDir(buildContext.getAppName(), buildContext.getServiceName());
        buildContext.setPushLogs(logFilePath);
        String tag = StringUtils.isNotBlank(image.getTag()) ? image.getTag() : ImageUtil.DEFAULT_TAG;
        PushImageCmd pushImageCmd = dockerClient
                .pushImageCmd(ImageUtil.getImageWithoutTag(image)).withTag(tag).withAuthConfig(authConfig);
        
        if (buildContext.isVerbose()) {
            WorkflowLogger.header(ImageBuilderActivity.IMAGE_PUSH_LOG);
        }
        PushImageResultCallback callback = new PushImageResultCallback() {
            @Override
            public void onNext(PushResponseItem item) {
                ErrorDetail errorDetail = item.getErrorDetail();
                String message = item.isErrorIndicated() && errorDetail != null ? errorDetail.getMessage() : item.getStatus();
                handleOutput(message, logFilePath, pushActivity, buildContext.isVerbose());
                if (item.isErrorIndicated()) {
                    logger.error("Error while pushing image: {}", errorDetail);
                    onError(new HyscaleException(ImageBuilderErrorCodes.FAILED_TO_PUSH_IMAGE));
                }
                if (StringUtils.isNotBlank(message) && message.contains(SHA256)) {
                    buildContext.setImageShaSum(getImageDigest(message));
                }
                super.onNext(item);
            }
        };

        try {
            pushImageCmd.exec(callback).awaitCompletion();
        } catch (RuntimeException e) {
            handleOutput(buildContext.isVerbose(), pushActivity, Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_PUSH_IMAGE);
        } catch (InterruptedException  e) {
            Thread.currentThread().interrupt();
            handleOutput(buildContext.isVerbose(), pushActivity, Status.FAILED);
            throw new HyscaleException(e, ImageBuilderErrorCodes.FAILED_TO_PUSH_IMAGE);
        }
        handleOutput(buildContext.isVerbose(), pushActivity, Status.DONE);
    }
    
    private void handleOutput(String output, String filePath, ActivityContext context, boolean isVerbose) {
        if (output == null) {
            return;
        }
        if (isVerbose) {
            WorkflowLogger.log(output);
        } else {
            WorkflowLogger.continueActivity(context);
        }
        try {
            HyscaleFilesUtil.updateFile(filePath, output.concat(ToolConstants.NEW_LINE));
        } catch (HyscaleException e) {
            logger.error("Error while writing output to log file: {}", filePath, e);
        }
    }
    
    private void handleOutput(boolean isVerbose, ActivityContext activityContext, Status status) {
        if (isVerbose) {
            WorkflowLogger.endActivity(status);
            WorkflowLogger.footer();
        } else {
            WorkflowLogger.endActivity(activityContext, status);
        }
    }

    private AuthConfig getAuthConfig(ImageRegistry imageRegistry) {
        if (imageRegistry == null) {
            return null;
        }
        AuthConfig authConfig = new AuthConfig();
        authConfig.withRegistryAddress(imageRegistry.getUrl());
        String decodedAuth = new String(Base64.getDecoder().decode(imageRegistry.getToken()));
        int delimiter = decodedAuth.indexOf(':');
        if (delimiter > 0) {
            authConfig.withUsername(decodedAuth.substring(0,delimiter));
            authConfig.withPassword(decodedAuth.substring(delimiter+1));
        }
        return authConfig;
    }

    private String getImageDigest(String status) {
        Optional<String> digest = Arrays.asList(status.split(" ")).stream().filter(each -> each.contains(SHA256))
                .findFirst();
        if (digest.isPresent()) {
            return digest.get().trim();
        }
        return null;
    }

    @Override
    public List<String> getImageIds(String imageName, Map<String, String> labels) throws HyscaleException {
        ListImagesCmd listImageCmd = getDockerClient().listImagesCmd();
        if (StringUtils.isNotBlank(imageName)) {
            listImageCmd.withImageNameFilter(imageName);
        }
        if (labels != null) {
            listImageCmd.withLabelFilter(labels);
        }

        List<com.github.dockerjava.api.model.Image> imageList = listImageCmd.exec();
        if (imageList == null || imageList.isEmpty()) {
            logger.debug("No images found to clean from the host machine");
            return Collections.emptyList();
        }
        return imageList.stream().map(com.github.dockerjava.api.model.Image::getId).collect(Collectors.toCollection(LinkedList::new));
    }

}
