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
import org.springframework.web.context.ContextLoaderListener;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import net.shibboleth.idp.spring.IdPPropertiesApplicationContextInitializer;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.spring.context.DelimiterAwareApplicationContext;

/**
 * A {@link ServletContainerInitializer} implementation that sets core parameters used to
 * install Spring support into the context.
 */
public class SpringConfigServletContextInitializer implements ServletContainerInitializer {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SpringConfigServletContextInitializer.class);

    /** System property name for the activation of this class. */
    @Nonnull @NotEmpty public static final String INIT_PARAMETER_ACTIVATION = "net.shibboleth.idp.registerSpringConfig";

    /** {@inheritDoc} */
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {

        final String flag = ctx.getInitParameter(INIT_PARAMETER_ACTIVATION);
        if (!"true".equalsIgnoreCase(flag)) {
            log.info("Spring config registration is disabled");
            return;
        }

        log.info("Setting init parameters and installing Spring listener");
        ctx.setInitParameter("contextClass", DelimiterAwareApplicationContext.class.getName());
        ctx.setInitParameter("contextInitializerClasses", IdPPropertiesApplicationContextInitializer.class.getName());
        ctx.setInitParameter("contextConfigLocation",
                "classpath*:/META-INF/net.shibboleth.idp/preconfig.xml,classpath:/net/shibboleth/idp/conf/global-system.xml,classpath*:/META-INF/net.shibboleth.idp/postconfig.xml");
        
        ctx.addListener(ContextLoaderListener.class.getName());
    }

}