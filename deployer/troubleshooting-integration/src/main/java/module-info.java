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