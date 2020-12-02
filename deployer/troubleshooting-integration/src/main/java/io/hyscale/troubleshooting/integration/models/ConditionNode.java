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
package com.github.srujankujmar.troubleshooting.integration.models;

import com.github.srujankujmar.commons.exception.HyscaleException;

public abstract class ConditionNode<C extends NodeContext> implements Node<C> {

    public abstract boolean decide(C context) throws HyscaleException;

    public abstract Node<C> onSuccess();

    public abstract Node<C> onFailure();

    @Override
    public final Node<C> next(C context) throws HyscaleException {
        return decide(context) ? onSuccess() : onFailure();
    }
}
