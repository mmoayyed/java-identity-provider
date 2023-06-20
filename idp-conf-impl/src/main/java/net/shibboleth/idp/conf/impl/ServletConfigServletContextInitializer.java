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
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
//import net.shibboleth.idp.authn.impl.RemoteUserAuthServlet;
//import net.shibboleth.idp.authn.impl.X509AuthServlet;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.spring.context.DelimiterAwareApplicationContext;
import net.shibboleth.shared.spring.servlet.impl.DelegatingServletProxy;

/**
 * A {@link ServletContainerInitializer} implementation that registers the servlets used by the IdP.
 *
 * <p>Registration of each servlet must be enabled by adding specific servlet init-params and is
 * therefore off by default for backward compatibility with old web.xml files.</p>
 */
public class ServletConfigServletContextInitializer implements ServletContainerInitializer {

    /** Context parameter name for the installation of the IdP servlet. */
    @Nonnull @NotEmpty public static final String INIT_PARAMETER_IDP_ACTIVATION =
            "net.shibboleth.idp.registerIdPServlet";

    /** Context parameter name for the installation of the IdP servlet. */
    @Nonnull @NotEmpty public static final String INIT_PARAMETER_REMOTEUSER_ACTIVATION =
            "net.shibboleth.idp.registerRemoteUserServlet";

    /** Context parameter name for the installation of the IdP servlet. */
    @Nonnull @NotEmpty public static final String INIT_PARAMETER_X509_ACTIVATION =
            "net.shibboleth.idp.registerX509Servlet";
    
    /** Context parameter name for the installation of the IdP servlet. */
    @Nonnull @NotEmpty public static final String INIT_PARAMETER_METADATA_ACTIVATION =
            "net.shibboleth.idp.registerMetadataServlet";
    

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ServletConfigServletContextInitializer.class);
    
    /** {@inheritDoc} */
    @Override
    public void onStartup(final Set<Class<?>> c, final ServletContext ctx) throws ServletException {
        
        final String idpFlag = ctx.getInitParameter(INIT_PARAMETER_IDP_ACTIVATION);
        final String remoteUserFlag = ctx.getInitParameter(INIT_PARAMETER_REMOTEUSER_ACTIVATION);
        final String x509Flag = ctx.getInitParameter(INIT_PARAMETER_X509_ACTIVATION);
        final String metadataFlag = ctx.getInitParameter(INIT_PARAMETER_METADATA_ACTIVATION);
        
        if ("true".equalsIgnoreCase(idpFlag)) {
            log.info("Registering primary IdP servlet");
            final ServletRegistration.Dynamic registration = ctx.addServlet("idp", DispatcherServlet.class);
            registration.addMapping("/status", "/profile/*");
            registration.setInitParameter("contextClass", DelimiterAwareApplicationContext.class.getName());
            registration.setInitParameter("contextConfigLocation",
                    "classpath*:/META-INF/net/shibboleth/idp/mvc/preconfig.xml,classpath:/net/shibboleth/idp/conf/mvc-beans.xml,classpath:/net/shibboleth/idp/conf/webflow-config.xml,classpath*:/META-INF/net/shibboleth/idp/mvc/postconfig.xml");
        }

        if ("true".equalsIgnoreCase(remoteUserFlag)) {
            log.info("Registering RemoteUser authentication servlet");
            final ServletRegistration.Dynamic registration = ctx.addServlet("RemoteUserAuthHandler",
                    new DelegatingServletProxy("shibboleth.RemoteUserAuthServlet"));
            registration.addMapping("/Authn/RemoteUser");
        }
        
        if ("true".equalsIgnoreCase(x509Flag)) {
            log.info("Registering X.509 authentication servlet");
            final ServletRegistration.Dynamic registration = ctx.addServlet("X509AuthHandler",
                    new DelegatingServletProxy("shibboleth.X509AuthServlet"));
            registration.addMapping("/Authn/X509");
        }
        
        if ("true".equals(metadataFlag)) {
            log.info("Registering metadata endpoint servlet");
            final ServletRegistration.Dynamic registration =
                    ctx.addJspFile("MetadataAccessHandler", "/WEB-INF/jsp/metadata.jsp");
            registration.addMapping("/shibboleth");
        }
    }

}