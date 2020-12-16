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
module deployer.services {
    exports com.github.srujankujmar.deployer.services.deployer;
    exports com.github.srujankujmar.deployer.services.broker;
    exports com.github.srujankujmar.deployer.services.builder;
    exports com.github.srujankujmar.deployer.services.config;
    exports com.github.srujankujmar.deployer.services.constants;
    exports com.github.srujankujmar.deployer.services.exception;
    exports com.github.srujankujmar.deployer.services.factory;
    exports com.github.srujankujmar.deployer.services.handler;
    exports com.github.srujankujmar.deployer.services.handler.impl;
    exports com.github.srujankujmar.deployer.services.listener;
    exports com.github.srujankujmar.deployer.services.manager;
    exports com.github.srujankujmar.deployer.services.model;
    exports com.github.srujankujmar.deployer.services.predicates;
    exports com.github.srujankujmar.deployer.services.processor;
    exports com.github.srujankujmar.deployer.services.progress;
    exports com.github.srujankujmar.deployer.services.provider;
    exports com.github.srujankujmar.deployer.services.util;
    exports com.github.srujankujmar.deployer.services.client;

    requires deployerModel;
    requires commons;
    requires slf4j.api;
    requires com.google.gson;
    requires com.google.common;
    requires com.fasterxml.jackson.databind;
    requires zjsonpatch;
    requires okhttp3;
    requires client.java;
    requires client.java.api;
    requires org.apache.commons.lang3;
    requires org.bouncycastle.provider;
    requires joda.time;
    requires spring.beans;
    requires spring.core;
}