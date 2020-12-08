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
module commons {
	exports com.github.srujankujmar.commons.constants;
	exports com.github.srujankujmar.commons.models;
	exports com.github.srujankujmar.commons.utils;
	exports com.github.srujankujmar.commons.logger;
	exports com.github.srujankujmar.commons.commands;
	exports com.github.srujankujmar.commons.config;
	exports com.github.srujankujmar.commons.component;
	exports com.github.srujankujmar.commons.exception;
	exports com.github.srujankujmar.commons.framework.patch;
	exports com.github.srujankujmar.commons.commands.provider;
	exports com.github.srujankujmar.commons.io;
	exports com.github.srujankujmar.commons.validator;

	requires com.fasterxml.jackson.annotation;
	requires client.java.api;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.databind;
	requires slf4j.api;
	requires spring.context;
	requires spring.beans;
	requires org.apache.commons.lang3;
	requires com.fasterxml.jackson.dataformat.yaml;
	requires org.apache.commons.io;
	requires com.github.mustachejava;
	requires java.annotation;
	requires java.json;
	requires commons.exec;
}
