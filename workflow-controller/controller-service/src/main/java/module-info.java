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
module controller.service {
    exports com.github.srujankujmar.controller.activity;
    exports com.github.srujankujmar.controller.builder;
    exports com.github.srujankujmar.controller.commands;
    exports com.github.srujankujmar.controller.config;
    exports com.github.srujankujmar.controller.constants;
    exports com.github.srujankujmar.controller.directive;
    exports com.github.srujankujmar.controller.exception;
    exports com.github.srujankujmar.controller.hooks;
    exports com.github.srujankujmar.controller.initializer;
    exports com.github.srujankujmar.controller.invoker;
    exports com.github.srujankujmar.controller.manager;
    exports com.github.srujankujmar.controller.model;
    exports com.github.srujankujmar.controller.profile;
    exports com.github.srujankujmar.controller.provider;
    exports com.github.srujankujmar.controller.service;
    exports com.github.srujankujmar.controller.util;
    exports com.github.srujankujmar.controller.validator;

    requires org.apache.commons.lang3;
    requires spring.context;
    requires slf4j.api;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires info.picocli;
    requires json.schema.core;
    requires com.google.gson;
    requires prettytime;
    requires client.java.api;
    requires client.java;
    requires org.joda.time;

    requires commons;
    requires builderCore;
    requires manifestGenerator;
    requires dockerfilegenservices;
    requires troubleshooting.integration;
    requires schema.validator;
    requires spring.boot;
    requires spring.beans;
    requires spring.boot.autoconfigure;
    requires java.annotation;
    requires spring.core;
    requires deployerModel;
    requires builderservices;
    requires deployer.services;
}