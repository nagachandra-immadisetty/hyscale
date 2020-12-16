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
package com.github.srujankujmar.controller.exception;

import com.github.srujankujmar.controller.model.HyscaleInputSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.srujankujmar.commons.config.SetupConfig;
import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.logger.WorkflowLogger;
import com.github.srujankujmar.commons.utils.WindowsUtil;
import com.github.srujankujmar.controller.activity.ControllerActivity;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * Utility Class to handle exception
 *
 * @author tushart
 */
@Component
public class ExceptionHandler implements IExecutionExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    @Autowired
    private ExitCodeExceptionMapper exitCodeExceptionMapper;

    @Override
    public int handleExecutionException(Exception ex, CommandLine commandLine, ParseResult parseResult)
            throws Exception {
        logger.error("Caught Error: ", ex);

        if (!(ex instanceof HyscaleException)) {
            WorkflowLogger.footer();
            String logDir = SetupConfig.getMountPathOf(SetupConfig.getToolLogDir());
            logDir = WindowsUtil.updateToHostFileSeparator(logDir);
            WorkflowLogger.error(ControllerActivity.UNEXPECTED_ERROR, logDir);
            WorkflowLogger.footer();
        }

        if (ex instanceof HyscaleException) {
            WorkflowLogger.error(ControllerActivity.TROUBLESHOOT, ex.toString());
        }

        return exitCodeExceptionMapper.getExitCode(ex);

    }
}
