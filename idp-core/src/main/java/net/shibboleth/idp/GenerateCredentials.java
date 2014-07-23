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

package net.shibboleth.idp;

import java.util.Collections;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.io.Resource;

/**
 * Generate credentials for the IdP using Spring.
 */
public final class GenerateCredentials {

    /** Spring resource. */
    @Nonnull public static final String RESOURCE = "conf/generate-credentials.xml";

    /** Constructor. */
    private GenerateCredentials() {
    }

    /**
     * Generate credentials for the IdP.
     * 
     * @param args program arguments
     */
    public static void main(String[] args) {

        final PreferFileSystemResourceLoader resourceLoader = new PreferFileSystemResourceLoader();

        final Resource resource = resourceLoader.getResource(RESOURCE);

        final ApplicationContextInitializer initializer = new IdPPropertiesApplicationContextInitializer();

        SpringSupport.newContext(GenerateCredentials.class.getName(), Collections.singletonList(resource),
                Collections.<BeanPostProcessor> emptyList(), Collections.singletonList(initializer), null);
    }
}
