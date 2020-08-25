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

package net.shibboleth.idp.authn.impl;

import java.util.List;

import javax.annotation.Nullable;

import org.springframework.beans.factory.annotation.Autowired;

import net.shibboleth.ext.spring.util.IdentifiedComponentManager;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

/**
 * Manager of {@link AuthenticationFlowDescriptor} objects.
 * 
 * @since 4.1.0
 */
public class AuthenticationFlowDescriptorManager extends IdentifiedComponentManager<AuthenticationFlowDescriptor> {

    /**
     * Constructor.
     *
     * @param freeObjects  free-standing objects
     */
    @Autowired
    public AuthenticationFlowDescriptorManager(
            @Nullable @NonnullElements final List<AuthenticationFlowDescriptor> freeObjects) {
        super(freeObjects);
    }

}