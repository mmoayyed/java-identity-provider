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

package net.shibboleth.idp.test.flows.interceptor;

import java.io.IOException;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.support.ResourcePropertySource;

/**
 * Adds a properties file to the application context environment.
 */
public class InterceptAppCtxInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /** Location of test properties file. */
    @Nonnull @NotEmpty public final static String INTERCEPT_TEST_PROPERTIES =
            "classpath:/intercept/intercept.properties";

    /** {@inheritDoc} */
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            applicationContext.getEnvironment().getPropertySources()
                    .addLast(new ResourcePropertySource(INTERCEPT_TEST_PROPERTIES));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
