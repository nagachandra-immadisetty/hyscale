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
package com.github.srujankujmar.controller.initializer;

import com.github.srujankujmar.controller.exception.ParameterExceptionHandler;
import com.github.srujankujmar.controller.piccoli.ProfileArgsManipulator;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.github.srujankujmar.builder.core.models.ImageBuilder;
import com.github.srujankujmar.builder.services.config.ImageBuilderConfig;
import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.constants.ToolConstants;
import com.github.srujankujmar.controller.commands.HyscaleCommand;
import com.github.srujankujmar.controller.exception.ControllerErrorCodes;
import com.github.srujankujmar.controller.exception.ExceptionHandler;
import com.github.srujankujmar.controller.util.ResourceCleanUpUtil;
import com.github.srujankujmar.controller.util.ShutdownHook;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.IHelpSectionRenderer;
import picocli.CommandLine.ParameterException;

/**
 * Starting point for the hyscale tool
 * <p>
 * This class is responsible for initializing the spring application context
 * and execute the given commands. It works on top of picoli
 * @see <a href="https://picocli.info/">https://picocli.info/</a>
 *
 * </p>
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.github.srujankujmar")
public class HyscaleInitializer implements CommandLineRunner {

    private final Logger logger = LoggerFactory.getLogger(HyscaleInitializer.class);

    @Autowired
    private IFactory factory;
    
    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private ParameterExceptionHandler parameterExceptionHandler;
    
    @Autowired
    private HyscaleCommand hyscaleCommand;
    
    private static final boolean IS_LAZY_INITIALIZATION = true;

    static {
        System.setProperty(ImageBuilderConfig.IMAGE_BUILDER_PROP, ImageBuilder.LOCAL.name());
        System.setProperty(ToolConstants.HYSCALECTL_LOGS_DIR_PROPERTY, SetupConfig.getToolLogDir());
        System.setProperty(ToolConstants.NASHORNS_ARGS, ToolConstants.NASHORNS_DEPRECATION_WARNING_FLAG);
        System.setProperty(ToolConstants.JDK_TLS_CLIENT_PROTOCOLS_ARGS, ToolConstants.JDK_TLS_CLIENT_VERSION);
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(HyscaleInitializer.class);
        app.setLazyInitialization(IS_LAZY_INITIALIZATION);
        app.run(args);
    }

    @Override
    public void run(String... args) {
        int exitCode = 1;
        try {
            Runtime.getRuntime().addShutdownHook(new ShutdownHook());
            CommandLine commandLine = new CommandLine(hyscaleCommand, factory);
            commandLine.setExecutionExceptionHandler(exceptionHandler);
            commandLine.setParameterExceptionHandler(parameterExceptionHandler);
            Map<String, IHelpSectionRenderer> updatedHelp = ProfileArgsManipulator.updateHelp(commandLine);
            commandLine.setHelpSectionMap(updatedHelp);
            args = ProfileArgsManipulator.updateArgs(args);
            exitCode = commandLine.execute(args);
        } catch (ParameterException e) {
            logger.error("Error while processing command, error {}", ControllerErrorCodes.INVALID_COMMAND.getErrorMessage(), e);
        } catch (Throwable e) {
            logger.error("Unexpected error in processing command, error {}", ControllerErrorCodes.UNEXPECTED_ERROR.getErrorMessage(), e);
        } finally {
            logger.debug("HyscaleInitializer::exit code: {}", exitCode);
            ResourceCleanUpUtil.performCleanUp();
        }
        System.exit(exitCode);
    }
    
}