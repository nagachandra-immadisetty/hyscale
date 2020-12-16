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

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
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
import com.github.srujankujmar.servicespec.commons.model.service.Replicas;
import com.github.srujankujmar.servicespec.commons.model.service.ServiceSpec;

@SpringBootTest
class ReplicasHandlerTest {

    @Autowired
    private ReplicasHandler replicasHandler;

    private static Stream<Arguments> input() throws HyscaleException {
        return Stream.of(Arguments.of(ServiceSpecTestUtil.getServiceSpec("/input/myservice-min.hspec")),
                Arguments.of(ServiceSpecTestUtil.getServiceSpec("/plugins/replica/replicas.hspec")));
    }

    @ParameterizedTest
    @MethodSource("input")
    void testReplicasHandler(ServiceSpec serviceSpec) throws HyscaleException {
        ManifestContext context = ManifestContextTestUtil.getManifestContext(ManifestResource.STATEFUL_SET);
        List<ManifestSnippet> manifestList = replicasHandler.handle(serviceSpec, context);

        Replicas replicas = serviceSpec.get(HyscaleSpecFields.replicas, Replicas.class);
        if (replicas == null) {
            assertTrue(CollectionUtils.isEmpty(manifestList));
            return;
        }
        assertTrue(manifestList.stream().allMatch(manifest -> manifest.getPath().equals("spec.replicas")
                && manifest.getSnippet().equals(String.valueOf(replicas.getMin()))));
    }
}
