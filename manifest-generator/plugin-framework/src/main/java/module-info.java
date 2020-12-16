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
module pluginframework {
	exports com.github.srujankujmar.plugin.framework.models;
	exports com.github.srujankujmar.plugin.framework.util;
	exports com.github.srujankujmar.plugin.framework.annotation;
	exports com.github.srujankujmar.plugin.framework.handler;

	requires service_spec_commons;
	requires commons;
	requires com.google.gson;
	requires java.validation;
	requires com.fasterxml.jackson.databind;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires org.apache.commons.lang3;
}
