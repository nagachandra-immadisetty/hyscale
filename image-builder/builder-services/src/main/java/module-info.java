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
module builderservices {
	exports com.github.srujankujmar.builder.services.util;
	exports com.github.srujankujmar.builder.services.constants;
	exports com.github.srujankujmar.builder.services.config;
	exports com.github.srujankujmar.builder.services.exception;
	exports com.github.srujankujmar.builder.services.impl;

	requires transitive commons;
	requires transitive service_spec_commons;
	requires transitive builderCore;
	requires org.apache.commons.lang3;

	requires spring.beans;
	requires spring.context;
	requires java.annotation;
	requires spring.boot;
	requires spring.core;
	requires slf4j.api;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.core;
}
