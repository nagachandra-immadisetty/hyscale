module schema.validator {
    exports com.github.srujankujmar.schema.validator;

    requires commons;
    requires spring.beans;
    requires slf4j.api;
    requires org.apache.commons.io;
    requires com.fasterxml.jackson.databind;
    requires jackson.coreutils;
    requires spring.context;
    requires json.schema.core;
    requires spring.boot;
    requires json.schema.validator;
}