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
module troubleshooting.integration {
    exports com.github.srujankujmar.troubleshooting.integration.actions;
    exports com.github.srujankujmar.troubleshooting.integration.builder;
    exports com.github.srujankujmar.troubleshooting.integration.conditions;
    exports com.github.srujankujmar.troubleshooting.integration.constants;
    exports com.github.srujankujmar.troubleshooting.integration.errors;
    exports com.github.srujankujmar.troubleshooting.integration.models;
    exports com.github.srujankujmar.troubleshooting.integration.service;
    exports com.github.srujankujmar.troubleshooting.integration.spring;
    exports com.github.srujankujmar.troubleshooting.integration.util;

    requires spring.context;
    requires spring.beans;
    requires deployer.services;
    requires deployerModel;
    requires client.java.api;
    requires commons;
    requires slf4j.api;
    requires spring.core;
    requires java.annotation;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.lang3;
}