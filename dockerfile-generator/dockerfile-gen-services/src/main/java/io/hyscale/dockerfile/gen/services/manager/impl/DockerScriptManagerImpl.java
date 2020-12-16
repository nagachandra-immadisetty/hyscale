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
package com.github.srujankujmar.dockerfile.gen.services.manager.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.dockerfile.gen.services.manager.DockerfileEntityManager;
import com.github.srujankujmar.dockerfile.gen.services.constants.DockerfileGenConstants;
import com.github.srujankujmar.dockerfile.gen.services.exception.DockerfileErrorCodes;
import com.github.srujankujmar.dockerfile.gen.services.templates.CommandsTemplateProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.exception.CommonErrorCode;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.servicespec.commons.model.service.BuildSpec;
import com.github.srujankujmar.commons.models.ConfigTemplate;
import com.github.srujankujmar.commons.utils.MustacheTemplateResolver;
import com.github.srujankujmar.dockerfile.gen.services.model.DockerfileGenContext;
import com.github.srujankujmar.dockerfile.gen.services.model.CommandType;
import com.github.srujankujmar.commons.models.FileSpec;
import com.github.srujankujmar.commons.models.SupportingFile;
import com.github.srujankujmar.dockerfile.gen.core.models.DockerfileActivity;
import com.github.srujankujmar.dockerfile.gen.services.config.DockerfileGenConfig;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

@Component
public class DockerScriptManagerImpl implements DockerfileEntityManager {

	@Autowired
	private CommandsTemplateProvider templateProvider;

	@Autowired
	private MustacheTemplateResolver templateResolver;

	/**
	 * If Script provided copy it to the directory Config Script, Run Script, Init
	 * Script In case of commands write it to script file
	 */
	@Override
	public List<SupportingFile> getSupportingFiles(ServiceSpec serviceSpec, DockerfileGenContext context)
			throws HyscaleException {
		if (serviceSpec == null) {
			throw new HyscaleException(CommonErrorCode.SERVICE_SPEC_REQUIRED);
		}
		List<SupportingFile> supportingFiles = new ArrayList<>();

		BuildSpec buildSpec = serviceSpec
				.get(HyscaleSpecFields.getPath(HyscaleSpecFields.image, HyscaleSpecFields.buildSpec), BuildSpec.class);

		boolean configCmdAvailable = scriptAvailable(buildSpec.getConfigCommands(), buildSpec.getConfigCommandsScript());
		SupportingFile configFile = null;
		if (configCmdAvailable) {
			configFile = getCommandSupportFile(buildSpec.getConfigCommands(), buildSpec.getConfigCommandsScript(),
					CommandType.CONFIGURE);
			if (configFile != null) {
				supportingFiles.add(configFile);
			}
		}
		boolean runCmdAvailable = scriptAvailable(buildSpec.getRunCommands(), buildSpec.getRunCommandsScript());
		SupportingFile runCmdFile = null;
		if (runCmdAvailable) {
			runCmdFile = getCommandSupportFile(buildSpec.getRunCommands(), buildSpec.getRunCommandsScript(), CommandType.RUN);
			if (runCmdFile != null) {
				supportingFiles.add(runCmdFile);
			}
		}

		return supportingFiles;
	}

    private SupportingFile getCommandSupportFile(String commands, String script, CommandType commandType)
            throws HyscaleException {

        FileSpec fileSpec = new FileSpec();

        SupportingFile supportingFile = new SupportingFile();

        if (StringUtils.isNotBlank(script) && StringUtils.isNotBlank(commands)) {
            // Command and script both found ignoring script file
            WorkflowLogger.persist(DockerfileActivity.COMMANDS_AND_SCRIPT_FOUND, commandType.toString());
        }
        if (StringUtils.isNotBlank(commands)) {
            fileSpec.setContent(getScript(commandType, commands));
            fileSpec.setName(getFileName(commandType));
        } else if (StringUtils.isNotBlank(script)) {
            File scriptFile = new File(SetupConfig.getAbsolutePath(script));
            if (!scriptFile.exists() || !scriptFile.isFile()) {
                throw new HyscaleException(DockerfileErrorCodes.SCRIPT_FILE_NOT_FOUND, script);
            }
            supportingFile.setFile(scriptFile);
            return supportingFile;
        }
        if (StringUtils.isBlank(fileSpec.getContent())) {
            return null;
        }
        supportingFile.setFileSpec(fileSpec);

        return supportingFile;
    }

	private String getFileName(CommandType commandType) {
	    switch (commandType) {
        case CONFIGURE:
            return DockerfileGenConfig.CONFIGURE_SCRIPT;
        case RUN:
            return DockerfileGenConfig.RUN_SCRIPT;
        default:
            break;
        }
	    return null;
    }

    public String getScript(CommandType commandType, String commands) throws HyscaleException {
		if (StringUtils.isBlank(commands)) {
			return null;
		}
		ConfigTemplate configTemplate = templateProvider.getTemplateFor(commandType);
		Map<String, Object> configureCmdContext = new HashMap<>();
		configureCmdContext.put(commandType.getTemplateField(), commands);
		
		return templateResolver.resolveTemplate(configTemplate.getTemplatePath(), configureCmdContext);
	}

	public String getScriptFile(String scriptFile, CommandType commandType) {
		if (StringUtils.isNotBlank(scriptFile)) {
			return new File(scriptFile).getName();
		}
		switch (commandType) {
		case CONFIGURE:
			return DockerfileGenConfig.CONFIGURE_SCRIPT;
		case RUN:
			return DockerfileGenConfig.RUN_SCRIPT;
		default:
			break;

		}
		return null;
	}

	public boolean scriptAvailable(String commands, String script) {
	    return (StringUtils.isNotBlank(script) || StringUtils.isNotBlank(commands));
	}
	
	/**
	 * Update script command to remove windows carriage return in scripts
	 * sed -i 's/\r$//' #scriptfile
	 * @param scriptFile
	 * @return script update command
	 */
	public String getScriptUpdateCommand(String scriptFile) {
	    if (StringUtils.isBlank(scriptFile)) {
	        return scriptFile;
	    }
	    
	    StringBuilder scriptUpdateCmd = new StringBuilder();
	    scriptUpdateCmd.append(DockerfileGenConstants.WINDOWS_NEW_LINE_CHANGE_COMMAND)
	        .append(ToolConstants.SPACE).append(scriptFile);
	    
	    return scriptUpdateCmd.toString();
	}

}
