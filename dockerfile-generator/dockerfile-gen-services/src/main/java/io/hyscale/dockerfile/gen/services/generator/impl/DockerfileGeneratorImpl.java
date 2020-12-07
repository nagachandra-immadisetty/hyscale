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
package com.github.srujankujmar.dockerfile.gen.services.generator.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.srujankujmar.commons.exception.CommonErrorCode;
import com.github.srujankujmar.dockerfile.gen.services.generator.DockerfileContentGenerator;
import com.github.srujankujmar.dockerfile.gen.services.generator.DockerfileGenerator;
import com.github.srujankujmar.dockerfile.gen.services.predicates.DockerfileGenPredicates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.servicespec.commons.model.service.Artifact;
import com.github.srujankujmar.servicespec.commons.model.service.BuildSpec;
import com.github.srujankujmar.servicespec.commons.model.service.Dockerfile;
import com.github.srujankujmar.commons.models.DockerfileEntity;
import com.github.srujankujmar.commons.models.Status;
import com.github.srujankujmar.dockerfile.gen.services.model.DockerfileGenContext;
import com.github.srujankujmar.dockerfile.gen.core.models.DockerfileActivity;
import com.github.srujankujmar.dockerfile.gen.core.models.DockerfileContent;
import com.github.srujankujmar.dockerfile.gen.services.exception.DockerfileErrorCodes;
import com.github.srujankujmar.commons.models.SupportingFile;
import com.github.srujankujmar.dockerfile.gen.services.config.DockerfileGenConfig;
import com.github.srujankujmar.dockerfile.gen.services.persist.DockerfilePersistenceService;
import com.github.srujankujmar.dockerfile.gen.services.manager.impl.ArtifactManagerImpl;
import com.github.srujankujmar.dockerfile.gen.services.manager.impl.DockerScriptManagerImpl;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

/**
 * Implementation to @see {@link DockerfileGenerator}
 * <p>
 * Responsible for
 * <ol>
 * <li>Copying supporting files to the path relative to dockerfile </li>
 * <li>Handles the configuration commands and runCommands to dockerfile</li>
 * <li> Generates Dockerfile content</li>
 * <li> Persists the dockerfile at
 * USER.HOME/hyscale/apps/[appName]/[serviceName/generated-files/dockerfiles</li>
 * </ol>
 */

@Component
public class DockerfileGeneratorImpl implements DockerfileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(DockerfileGeneratorImpl.class);

    @Autowired
    private DockerfileGenConfig dockerfileGenConfig;

    @Autowired
    private DockerfileContentGenerator dockerfileContentGenerator;

    @Autowired
    private DockerScriptManagerImpl dockerScriptHelper;

    @Autowired
    private ArtifactManagerImpl artifactHelper;

    @Autowired
    private DockerfilePersistenceService dockerfilePersistenceService;

    /**
     * Validates service spec if dockerfile generation has to be skipped
     * Copies the artifacts to relative path of dockerfile
     * Copies the scripts to relative path of dockerfile
     * Generates the dockerfile content
     * Writes  dockerfile to a file
     */
    @Override
    public DockerfileEntity generateDockerfile(ServiceSpec serviceSpec, DockerfileGenContext context)
            throws HyscaleException {

        try {
            validate(serviceSpec, context);
            String serviceName = serviceSpec.get(HyscaleSpecFields.name, String.class);
            context.setServiceName(serviceName.trim());
            //validates service spec if dockerfile generation has to be skipped
            if (!skipDockerfileGen(serviceSpec, context)) {
                return null;
            }
        } catch (HyscaleException e) {
            WorkflowLogger.startActivity(DockerfileActivity.DOCKERFILE_GENERATION);
            WorkflowLogger.endActivity(Status.FAILED);
            throw e;
        }

        boolean isSuccess = false;
        DockerfileEntity dockerFileEntity = new DockerfileEntity();
        try {
            BuildSpec buildSpec = serviceSpec.get(
                    HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.buildSpec), BuildSpec.class);
            // Get updated artifacts
            List<Artifact> originalArtifacts = buildSpec.getArtifacts();

            if (!context.isSkipCopy()) {
                context.setEffectiveArtifacts(artifactHelper.getUpdatedArtifacts(originalArtifacts));
            } else {
                context.setEffectiveArtifacts(originalArtifacts);
            }

            // Use updated artifacts from context
            DockerfileContent dockerfileContent = dockerfileContentGenerator.generate(serviceSpec, context);
            String appName = context.getAppName();
            String serviceName = serviceSpec.get(HyscaleSpecFields.name, String.class);

            File dockerfile = new File(dockerfileGenConfig.getDockerFileDir(appName, serviceName));
            dockerFileEntity.setDockerfile(dockerfile);

            // Get Supporting Files
            List<SupportingFile> supportingFiles = new ArrayList<SupportingFile>();
            supportingFiles.addAll(dockerScriptHelper.getSupportingFiles(serviceSpec, context));
            supportingFiles.addAll(artifactHelper.getSupportingFiles(serviceSpec, context));

            dockerFileEntity.setSupportingFileList(supportingFiles);

            isSuccess = dockerfilePersistenceService.persistDockerfiles(dockerfileContent, supportingFiles, context);
        } catch (HyscaleException ex) {
            logger.error("Error while generating dockerfile, error {}", ex.toString());
            throw ex;
        }
        if (!isSuccess) {
            throw new HyscaleException(DockerfileErrorCodes.FAILED_TO_GENERATE_DOCKERFILE);
        }
        logger.debug("Dockerfile entity {}", dockerFileEntity);
        return dockerFileEntity;
    }

    private void validate(ServiceSpec serviceSpec, DockerfileGenContext context) throws HyscaleException {
        if (serviceSpec == null) {
            throw new HyscaleException(CommonErrorCode.SERVICE_SPEC_REQUIRED);
        }
        if (context == null) {
            throw new HyscaleException(DockerfileErrorCodes.FAILED_TO_PROCESS_DOCKERFILE_GENERATION);
        }
    }

    /**
     * Skip dockerfile generation when 1. User has provided dockerfile in the
     * service spec 2. Both dockerfile & buildSpec is not provided 
     * 3. BuildSpec exists & if they don't have artifacts , configureCommands, runCommands
     * ,configScript and runScript
     * <p>
     * Also, validates whether dockerfile & buildSpec both are provided. According
     * to service spec dockerfile & buildSpec are mutually exclusive
     *
     * @param serviceSpec
     * @return true if docker file generation required
     * @throws HyscaleException
     */
    private boolean skipDockerfileGen(ServiceSpec serviceSpec, DockerfileGenContext context) throws HyscaleException {
        Dockerfile userDockerfile = serviceSpec.get(
                HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.dockerfile), Dockerfile.class);
        BuildSpec buildSpec = serviceSpec
                .get(HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.buildSpec), BuildSpec.class);

        if (userDockerfile != null && buildSpec != null) {
            throw new HyscaleException(DockerfileErrorCodes.DOCKERFILE_OR_BUILDSPEC_REQUIRED);
        }

        if (DockerfileGenPredicates.skipDockerfileGen().test(serviceSpec)) {
            WorkflowLogger.startActivity(DockerfileActivity.DOCKERFILE_GENERATION);
            WorkflowLogger.endActivity(Status.SKIPPING);
            if (DockerfileGenPredicates.stackAsServiceImage().test(buildSpec)) {
                context.setStackAsServiceImage(true);
            }
            return false;
        }

        return true;
    }

}
