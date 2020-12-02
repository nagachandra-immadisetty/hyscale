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
import com.github.srujankujmar.plugin.framework.handler.ManifestHandler;

module manifestGenerator {
	exports com.github.srujankujmar.generator.services.config;
	exports com.github.srujankujmar.generator.services.constants;
	exports com.github.srujankujmar.generator.services.exception;
	exports com.github.srujankujmar.generator.services.generator;
	exports com.github.srujankujmar.generator.services.json;
	exports com.github.srujankujmar.generator.services.listener;
	exports com.github.srujankujmar.generator.services.model;
	exports com.github.srujankujmar.generator.services.predicates;
	exports com.github.srujankujmar.generator.services.plugins;
	exports com.github.srujankujmar.generator.services.processor;
	exports com.github.srujankujmar.generator.services.provider;
	exports com.github.srujankujmar.generator.services.utils;

	uses ManifestHandler;

	requires slf4j.api;
	requires com.fasterxml.jackson.databind;
	requires org.apache.commons.codec;
	requires com.fasterxml.jackson.core;
	requires org.apache.commons.lang3;
	requires com.google.common;
	requires transitive commons;
	requires transitive pluginframework;
	requires transitive service_spec_commons;
	requires com.fasterxml.jackson.dataformat.yaml;
}