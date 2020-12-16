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
package com.github.srujankujmar.commons.exception;

public enum CommonErrorCode implements HyscaleError {
    FAILED_TO_GET_VALID_INPUT("Could not get valid input"),
    INVALID_INPUT_PROVIDED("Input \"{}\" is invalid"),
    FAILED_TO_EXECUTE_COMMAND("Failed to execute command {}"),
    FAILED_TO_READ_FILE("Failed to read file {}"),
    FAILED_TO_WRITE_STDIN("Failed to write standard input to the process"),
    FAILED_TO_COPY_FILE("Failed to copy the file {}"),
    DIRECTORY_REQUIRED_TO_COPY_FILE("Directory required to copy file {}"),
    FILE_NOT_FOUND("File {} not found"),
    LOGFILE_NOT_FOUND("Failed to log due to invalid file path."),
    FAILED_TO_RESOLVE_TEMPLATE("Failed to resolve {} template"),
    FAILED_TO_WRITE_FILE("Failed to write to file {}"),
    FAILED_TO_WRITE_FILE_DATA("Failed to write data into file {}"),
    SERVICE_SPEC_REQUIRED("Service spec required"),
    EMPTY_FILE_PATH("Empty file found"),
    FAILED_TO_CLEAN_DIRECTORY("Failed to clean directory {}"),
    FAILED_TO_DELETE_DIRECTORY("Failed to delete the directory {}"),
    FOUND_DIRECTORY_INSTEAD_OF_FILE("Found directory {} instead of file"),
    TEMPLATE_CONTEXT_NOT_FOUND("Template Context not found for template {}"),
    INPUTSTREAM_NOT_FOUND("Cannot find inputstream and so cannot write to logfile"),
    FAILED_TO_READ_LOGFILE("Failed to read logs at log file {}"),
    OUTPUTSTREAM_NOT_FOUND("Cannot find output stream and so cannot write to stream"),
    STRATEGIC_MERGE_KEY_NOT_FOUND("Merge key not found while merging {}"),
    YAML_TO_JSON_CONVERSION_FAILURE("Error occured while converting given yaml to json. {}"),
    EMPTY_REFERENCE_SCHEMA_FOUND("Cannot be process empty schema"),
    SCHEMA_PROCESSING_ERROR("Unable process schema path,missing node"),
    EMPTY_FILE_FOUND("Empty file:{} cannot be processed."),
    INVALID_FILE_INPUT("Given input {} is not a file. Expecting file input."),
    UNABLE_READ_SCHEMA("Cannot process empty schema {}"),
    ERROR_OCCURED_WHILE_SCHEMA_VALIDATION("Schema validation failed due to \"{}\""),
    INVALID_JSON_FORMAT("Json format is invalid"),
    FAILED_TO_CONNECT_TO_CLUSTER("Failed to connect to cluster",HyscaleErrorGroup.UPFRONT_VALIDATION);

    private String message;
    private int code;

    CommonErrorCode(String message) {
        this.message = message;
    }

    CommonErrorCode(String message,HyscaleErrorGroup errorGroup){
        this.message=message;
        this.code=errorGroup.getGroupCode();
    }
    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public int getCode() {
        return this.code;
    }

}
