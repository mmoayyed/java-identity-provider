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

package net.shibboleth.idp.profile.spring.factory;

import javax.annotation.Nonnull;

import org.springframework.core.io.ResourceLoader;

/**
 * Derivation of SWF-supplied resource factory for flow definitions.
 * 
 * <p>This implementation overrides the behavior of the built-in factory with regard
 * to handling absolute paths while still supporting relative paths.
 */
public class FlowDefinitionResourceFactory extends org.springframework.webflow.config.FlowDefinitionResourceFactory {

    /**
     * Constructor.
     *
     * @param resourceLoader resource loader to use
     */
    public FlowDefinitionResourceFactory(@Nonnull final ResourceLoader resourceLoader) {
        super(resourceLoader);
    }

}