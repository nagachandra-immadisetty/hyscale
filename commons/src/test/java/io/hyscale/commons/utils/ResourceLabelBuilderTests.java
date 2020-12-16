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
package com.github.srujankujmar.commons.utils;

import com.github.srujankujmar.commons.models.ResourceLabelKey;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

public class ResourceLabelBuilderTests {
    private static final String APP_NAME = "myApp";
    private static final String SVC_NAME = "mySvc";
    private static final String ENV_NAME = "myEnv";
    private static Date date;
    private static Long longDate;

    @BeforeAll
    public static void init() {
        date = Calendar.getInstance().getTime();
        longDate = date.getTime();
    }

    @Test
    public void testBuildWithAppSvcEnv() {
        Map<ResourceLabelKey, String> label = ResourceLabelBuilder.build(APP_NAME, ENV_NAME, SVC_NAME);
        assertEquals(ENV_NAME, label.get(ResourceLabelKey.ENV_NAME));
        assertEquals(APP_NAME, label.get(ResourceLabelKey.APP_NAME));
        assertEquals(SVC_NAME, label.get(ResourceLabelKey.SERVICE_NAME));
    }

    @Test
    public void testBuildWithAppEnv() {
        Map<ResourceLabelKey, String> label = ResourceLabelBuilder.build(APP_NAME, ENV_NAME);
        assertEquals(ENV_NAME, label.get(ResourceLabelKey.ENV_NAME));
        assertEquals(APP_NAME, label.get(ResourceLabelKey.APP_NAME));
    }

    @Test
    public void testBuildWithApp() {
        Map<ResourceLabelKey, String> label = ResourceLabelBuilder.build(APP_NAME);
        assertEquals(APP_NAME, label.get(ResourceLabelKey.APP_NAME));
    }

    @Test
    public void testBuild() {
        Map<ResourceLabelKey, String> label = ResourceLabelBuilder.build(APP_NAME, ENV_NAME, SVC_NAME, "1", longDate);
        assertEquals(ENV_NAME, label.get(ResourceLabelKey.ENV_NAME));
        assertEquals(APP_NAME, label.get(ResourceLabelKey.APP_NAME));
        assertEquals(SVC_NAME, label.get(ResourceLabelKey.SERVICE_NAME));
        assertEquals("1-" + longDate.toString(), label.get(ResourceLabelKey.RELEASE_VERSION));

    }

    @Test
    public void testBuildServiceLabel() {
        Map<ResourceLabelKey, String> label = ResourceLabelBuilder.buildServiceLabel(APP_NAME, SVC_NAME);
        assertEquals(APP_NAME, label.get(ResourceLabelKey.APP_NAME));
        assertEquals(SVC_NAME, label.get(ResourceLabelKey.SERVICE_NAME));
    }

    public static Stream<Arguments> input() {
        return Stream.of(Arguments.of("normaLize", "normaLize"),
                Arguments.of("normalize@1", "normalize1"),
                Arguments.of(null, null),
                Arguments.arguments(" ", ""),
                Arguments.arguments("normalize@ 1", "normalize-1"),
                Arguments.arguments("normalize@1 ", "normalize1"),
                Arguments.arguments("normalize@1$", "normalize1"),
                Arguments.arguments("norm@3alize ", "norm3alize"),
                Arguments.arguments(" %norm@ ", "norm"));
    }

    @ParameterizedTest
    @MethodSource(value = "input")
    public void testNormalize(String input, String expected) {
        assertEquals(expected, ResourceLabelBuilder.normalize(input));
    }
}
