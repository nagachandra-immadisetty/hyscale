module schema.validator {
    exports com.github.srujankujmar.schema.validator;

    requires commons;
    requires spring.boot;
    requires spring.beans;
    requires slf4j.api;
    requires json.schema.core;
    requires org.apache.commons.io;
    requires json.schema.validator;
    requires com.fasterxml.jackson.databind;
    requires jackson.coreutils;
    requires spring.context;
    requires com.github.mustachejava;
}