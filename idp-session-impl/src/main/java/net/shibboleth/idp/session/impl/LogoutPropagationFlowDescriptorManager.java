/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.session.impl;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import net.shibboleth.idp.session.LogoutPropagationFlowDescriptor;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.spring.config.IdentifiedComponentManager;

/**
 * Manager of {@link LogoutPropagationFlowDescriptor} objects.
 * 
 * @since 4.1.0
 */
public class LogoutPropagationFlowDescriptorManager
        extends IdentifiedComponentManager<LogoutPropagationFlowDescriptor> {

    /**
     * Constructor.
     *
     * @param freeObjects  free-standing objects
     */
    @Autowired
    public LogoutPropagationFlowDescriptorManager(
            @Nullable @NonnullElements final List<LogoutPropagationFlowDescriptor> freeObjects) {
        super(freeObjects);
    }

}