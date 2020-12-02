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

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class NormalizationUtilTests {
    public static Stream<Arguments> input() {
        return Stream.of(Arguments.of("normaLize", "normalize"),
                Arguments.of("normalize@1", "normalize1"),
                Arguments.of(null, null),
                Arguments.arguments(" ", ""),
                Arguments.arguments("normalize@ 1", "normalize-1"),
                Arguments.arguments("normalize@1 ", "normalize1"),
                Arguments.arguments("normalize@1$", "normalize1"),
                Arguments.arguments("norm@3alize ", "norm3alize"),
                Arguments.arguments("norm@ alize ", "norm-alize"));
    }

    @ParameterizedTest
    @MethodSource(value = "input")
    public void testNormalizeWithLength(String input, String expected) {
        String actualString = NormalizationUtil.normalize(input, 7);
        assertEquals(StringUtils.isEmpty(expected) ? expected : expected.substring(0, 7), actualString);
    }

    @ParameterizedTest
    @MethodSource(value = "input")
    public void testNormalize(String input, String expected) {
        String actualString = NormalizationUtil.normalize(input);
        assertEquals(expected, actualString);
    }
}
