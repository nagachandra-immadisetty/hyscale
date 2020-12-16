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
package com.github.srujankujmar.generator.services.plugins;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.github.srujankujmar.commons.exception.HyscaleException;
import com.github.srujankujmar.commons.models.ManifestContext;
import com.github.srujankujmar.generator.services.model.ManifestResource;
import com.github.srujankujmar.generator.services.utils.ManifestContextTestUtil;
import com.github.srujankujmar.generator.services.utils.ServiceSpecTestUtil;
import com.github.srujankujmar.plugin.framework.models.ManifestSnippet;
import com.github.srujankujmar.servicespec.commons.fields.HyscaleSpecFields;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

@SpringBootTest
class StartCommandHandlerTest {

    @Autowired
    private StartCommandHandler startCommandHandler;

    private List<String> PATHS = Arrays.asList("spec.template.spec.containers[0].command",
            "spec.template.spec.containers[0].args");

    private static Stream<Arguments> input() throws HyscaleException {
        return Stream.of(
                Arguments.of(ServiceSpecTestUtil.getServiceSpec("/input/myservice-min.hspec"),
                        ManifestContextTestUtil.getManifestContext(ManifestResource.STATEFUL_SET)),
                Arguments.of(ServiceSpecTestUtil.getServiceSpec("/input/myservice.hspec"),
                        ManifestContextTestUtil.getManifestContext(ManifestResource.STATEFUL_SET)));
    }

    @ParameterizedTest
    @MethodSource("input")
    void testStartCommandHandler(ServiceSpec serviceSpec, ManifestContext context)
            throws HyscaleException, IOException {
        List<ManifestSnippet> manifestList = startCommandHandler.handle(serviceSpec, context);

        String startCommand = serviceSpec.get(HyscaleSpecFields.startCommand, String.class);

        if (StringUtils.isBlank(startCommand)) {
            assertTrue(CollectionUtils.isEmpty(manifestList));
            return;
        }

        List<String> manifestPaths = manifestList.stream().map(manifest -> manifest.getPath())
                .collect(Collectors.toList());

        assertTrue(CollectionUtils.isEqualCollection(PATHS, manifestPaths));

    }
}
