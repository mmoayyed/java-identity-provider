/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.springframework.web.filter.DelegatingFilterProxy;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

/**
 * A {@link ServletContainerInitializer} implementation that registers a filter chain embedded in
 * our Spring configuration.
 *
 * <p>The filter registration is on by default but can be disabled by setting the context init-param
 * {@link RegisterFilterChainServletContextInitializer#INIT_PARAMETER_ACTIVATION} to false.</p>
 * 
 * <p>The chain is always mapped to all requests because it is expected that any further granularity is
 * configured via Spring.</p>
 */
public class RegisterFilterChainServletContextInitializer implements ServletContainerInitializer {

    /** System property name for the activation of this class. */
    @Nonnull @NotEmpty public static final String INIT_PARAMETER_ACTIVATION = "net.shibboleth.idp.registerFilterChain";

    /** The filter name for the embedded filter chain. */
    @Nonnull @NotEmpty public static final String FILTER_NAME = "ShibbolethFilterChain";

    /** The target bean name for the chain filter. */
    @Nonnull @NotEmpty public static final String TARGET_BEAN_NAME = "shibboleth.ChainingFilter";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(RegisterFilterChainServletContextInitializer.class);
    
    /** {@inheritDoc} */
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        
        final String flag = ctx.getInitParameter(INIT_PARAMETER_ACTIVATION);
        if ("false".equalsIgnoreCase(flag)) {
            log.info("Filter registration is disabled");
            return;
        }
        
        log.debug("Attempting to register filter '{}'", FILTER_NAME);
        final FilterRegistration.Dynamic headerFilter = ctx.addFilter(FILTER_NAME, DelegatingFilterProxy.class);
        headerFilter.addMappingForUrlPatterns(null, false, "/*");
        headerFilter.setInitParameter("targetBeanName", TARGET_BEAN_NAME);
        log.info("Registered the filter '{}'.", FILTER_NAME);
    }

}