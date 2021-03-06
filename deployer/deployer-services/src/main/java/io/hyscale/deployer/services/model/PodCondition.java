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
package com.github.srujankujmar.deployer.services.model;

/**
 * K8s Pod conditions
 *
 */
public enum PodCondition {

    READY("Ready"), POD_SCHEDULED("PodScheduled"), INITIALIZED("Initialized"), CONTAINERS_READY("ContainersReady"),
    UNSCHEDULABLE("Unschedulable");

    private String condition;

    PodCondition(String podCondition) {
        this.condition = podCondition;
    }

    public String getPodCondition() {
        return condition;
    }

    public static PodCondition fromString(String podCondition) {
        if (podCondition == null) {
            return null;
        }
        for (PodCondition condition : PodCondition.values()) {
            if (condition.getPodCondition().equalsIgnoreCase(podCondition)) {
                return condition;
            }
        }
        return null;
    }
}
