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

package net.shibboleth.idp.conf.impl;

import java.util.Set;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.DelegatingFilterProxy;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A {@link ServletContainerInitializer} implementation that registers a filter chain embedded in
 * our Spring configuration.
 *
 * The filter registration can be disabled by setting the system property
 * {@link RegisterFilterChainServletContextInitializer#SYSTEM_PROPERTY_ACTIVATION} to <pre>disabled</pre>.
 * 
 * The chain is always mapped to all requests because it is expected that any further granularity is
 * configured via Spring.
 */
public class RegisterFilterChainServletContextInitializer implements ServletContainerInitializer {

    /** System property name for the activation of this class. */
    @Nonnull @NotEmpty public static final String SYSTEM_PROPERTY_ACTIVATION =
            RegisterFilterChainServletContextInitializer.class.getCanonicalName();

    /** System property name for the activation of this class. */
    @Nonnull @NotEmpty public static final String SYSTEM_PROPERTY_SERVLET =
            RegisterFilterChainServletContextInitializer.class.getCanonicalName() + ".servlet";

    /** Name of servlet that MUST be registered for us to run. */
    @Nonnull @NotEmpty public static final String DEFAULT_SERVLET_TO_CHECK = "idp";

    /** The filter name for the embedded filter chain. */
    @Nonnull @NotEmpty public static final String FILTER_NAME = "ShibbolethFilterChain";

    /** The target bean name for the chain filter. */
    @Nonnull @NotEmpty public static final String TARGET_BEAN_NAME = "shibboleth.ChainingFilter";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RegisterFilterChainServletContextInitializer.class);
    
    /** {@inheritDoc} */
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        
        final String flag = System.getProperty(SYSTEM_PROPERTY_ACTIVATION);
        log.debug("The value of the flag {}: {}", SYSTEM_PROPERTY_ACTIVATION, flag);
        if ("disabled".equalsIgnoreCase(flag)) {
            log.info("Filter registration is disabled according to the system properties");
            return;
        } else if (ctx.getServletRegistration(StringSupport.trimOrNull(
                System.getProperty(SYSTEM_PROPERTY_SERVLET, DEFAULT_SERVLET_TO_CHECK))) == null) {
            log.debug("Ignoring invocation outside IdP context");
            return;
        }
        
        log.debug("Attempting to register filter '{}'", FILTER_NAME);
        final FilterRegistration.Dynamic headerFilter = ctx.addFilter(FILTER_NAME, DelegatingFilterProxy.class);
        headerFilter.addMappingForUrlPatterns(null, false, "/*");
        headerFilter.setInitParameter("targetBeanName", TARGET_BEAN_NAME);
        log.info("Registered the filter '{}'.", FILTER_NAME);
    }

}