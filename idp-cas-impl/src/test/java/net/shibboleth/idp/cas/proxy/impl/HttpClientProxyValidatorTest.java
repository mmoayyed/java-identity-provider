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

package net.shibboleth.idp.cas.proxy.impl;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateException;

import javax.security.auth.login.FailedLoginException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import net.shibboleth.idp.cas.flow.impl.AbstractFlowActionTest;
import net.shibboleth.idp.cas.protocol.ProtocolContext;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;

/**
 * Unit test for {@link HttpClientProxyValidator} class.
 *
 * @author Marvin S. Addison
 */
public class HttpClientProxyValidatorTest extends AbstractFlowActionTest {

    @Autowired
    private HttpClientProxyValidator validator;

    @SuppressWarnings("unused")
    @Autowired
    private ApplicationContext context;

    @DataProvider(name = "data")
    public Object[][] buildTestData() {
        return new Object[][] {
                // Trusted cert from static trust source and acceptable response code
                new Object[] { "https://localhost:8443", "src/test/resources/credentials/localhost.p12", 200, null },

                // Trusted cert from metadata source and acceptable response code
                new Object[] {
                        "https://nobody-1.middleware.vt.edu/",
                        "src/test/resources/credentials/nobody-1.p12",
                        200,
                        null,
                },

                // Trusted cert from static trust source and unacceptable response code
                new Object[] {
                        "https://localhost:8443",
                        "src/test/resources/credentials/localhost.p12",
                        404,
                        new FailedLoginException()
                },

                // Untrusted self-signed cert
                new Object[] {
                        "https://localhost:8443",
                        "src/test/resources/credentials/nobody-2.p12",
                        200,
                        new CertificateException(),
                },

                // Untrusted cert signed by commercial CA that appears in default system truststore
                new Object[] {
                        "https://localhost:8443",
                        "src/test/resources/credentials/idp-1371.p12",
                        200,
                        new CertificateException(),
                },
        };
    }

    @Test(dataProvider = "data")
    public void testAuthenticate(
            final String serviceURL, final String keyStorePath, final int status, final Exception expected)
            throws Exception {
        Server server = null;
        try {
            server = startServer(keyStorePath, new ConfigurableStatusHandler(status));
            validator.validate(
                    buildProfileRequestContext(serviceURL),
                    new URI("https://localhost:8443/?pgtId=A&pgtIOU=B"));
            if (expected != null) {
                fail("Proxy authentication should have failed with " + expected);
            }
        } catch (Exception e) {
            if (expected == null) {
                throw e;
            }
            assertTrue(expected.getClass().isAssignableFrom(e.getClass()));
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private Server startServer(final String keyStorePath, final Handler handler) {
        final Server server = new Server();

        final SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStoreType("PKCS12");
        sslContextFactory.setKeyStorePath(keyStorePath);
        sslContextFactory.setKeyStorePassword("changeit");
        final ServerConnector connector = new ServerConnector(server, sslContextFactory);
        connector.setHost("127.0.0.1");
        connector.setPort(8443);
        server.setConnectors(new Connector[] { connector });
        server.setHandler(handler);
        try {
            server.start();
        } catch (Exception e) {
            try {
                server.stop();
            } catch (Exception e2) {}
            throw new RuntimeException("Jetty startup failed", e);
        }
        final Thread serverRunner = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server.join();
                } catch (InterruptedException e) {}
            }
        });
        serverRunner.start();
        return server;
    }

    private static class ConfigurableStatusHandler extends AbstractHandler {

        final int status;

        public ConfigurableStatusHandler(final int s) {
            status = s;
        }

        @Override
        public void handle(
                final String target,
                final Request request,
                final HttpServletRequest servletRequest,
                final HttpServletResponse servletResponse) throws IOException, ServletException {

            servletResponse.setContentType("text/plain;charset=utf-8");
            servletResponse.setStatus(status);
            request.setHandled(true);
            servletResponse.getWriter().println("OK");
        }
    }

    private ProfileRequestContext buildProfileRequestContext(final String serviceUrl) {
        final ProfileRequestContext prc = new ProfileRequestContext();
        final ProtocolContext<?,?> protoCtx = new ProtocolContext<>();
        prc.addSubcontext(protoCtx);
        protoCtx.addSubcontext(new ServiceContext(new Service(serviceUrl, "unknown", true, false)));
        return prc;
    }
}
