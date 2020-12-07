module deployer.services {
    exports com.github.srujankujmar.deployer.services.deployer;
    exports com.github.srujankujmar.deployer.services.broker;
    exports com.github.srujankujmar.deployer.services.builder;
    exports com.github.srujankujmar.deployer.services.config;
    exports com.github.srujankujmar.deployer.services.constants;
    exports com.github.srujankujmar.deployer.services.exception;
    exports com.github.srujankujmar.deployer.services.factory;
    exports com.github.srujankujmar.deployer.services.handler;
    exports com.github.srujankujmar.deployer.services.listener;
    exports com.github.srujankujmar.deployer.services.manager;
    exports com.github.srujankujmar.deployer.services.model;
    exports com.github.srujankujmar.deployer.services.predicates;
    exports com.github.srujankujmar.deployer.services.processor;
    exports com.github.srujankujmar.deployer.services.progress;
    exports com.github.srujankujmar.deployer.services.provider;
    exports com.github.srujankujmar.deployer.services.util;

    requires deployerModel;
    requires commons;
    requires slf4j.api;
    requires gson;
    requires org.apache.commons.lang3;
    requires com.google.common;
    requires com.fasterxml.jackson.databind;
    requires zjsonpatch;
}