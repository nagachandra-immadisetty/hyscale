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
    exports com.github.srujankujmar.controller.piccoli;
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
    requires gson;
    requires prettytime;

    requires commons;
    requires deployerModel;
    requires builderservices;
    requires builderCore;
    requires manifestGenerator;
    requires dockerfilegenservices;
    requires deployer.services;
    requires troubleshooting.integration;
    requires schema.validator;
    requires spring.boot;
    requires spring.beans;
    requires spring.boot.autoconfigure;
    requires java.annotation;
}