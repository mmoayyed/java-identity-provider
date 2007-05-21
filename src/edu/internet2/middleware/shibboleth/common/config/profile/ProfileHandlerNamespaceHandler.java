/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.profile;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for profile handlers.
 */
public class ProfileHandlerNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for profile handlers. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:profile-handler";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(ProfileHandlerGroupBeanDefinitionParser.ELEMENT_NAME,
                new ProfileHandlerGroupBeanDefinitionParser());

        registerBeanDefinitionParser(ShibbolethProfileHandlerManagerBeanDefinitionParser.SCHEMA_TYPE,
                new ShibbolethProfileHandlerManagerBeanDefinitionParser());

        registerBeanDefinitionParser(VelocityErrorHandlerBeanDefinitionParser.SCHEMA_TYPE,
                new VelocityErrorHandlerBeanDefinitionParser());

        registerBeanDefinitionParser(JSPErrorHandlerBeanDefinitionParser.SCHEMA_TYPE,
                new JSPErrorHandlerBeanDefinitionParser());
    }
}