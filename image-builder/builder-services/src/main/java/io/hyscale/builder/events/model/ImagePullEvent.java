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
package com.github.srujankujmar.builder.events.model;

import com.github.srujankujmar.commons.framework.events.model.ActivityEvent;
import com.github.srujankujmar.commons.framework.events.model.ActivityState;

public class ImagePullEvent extends ActivityEvent {

    private String pullImage;

    public ImagePullEvent(ActivityState state, String pullImage) {
        super(state);
        this.pullImage = pullImage;
    }

    public String getPullImage() {
        return pullImage;
    }

}
