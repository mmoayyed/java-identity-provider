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

import java.io.IOException;

import javax.annotation.Nonnull;

import org.slf4j.MDC;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import net.shibboleth.idp.Version;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.servlet.AbstractConditionalFilter;
import net.shibboleth.shared.servlet.HttpServletSupport;
import net.shibboleth.shared.spring.servlet.ChainableFilter;


/**
 * Servlet filter that sets some interesting MDC attributes as the request comes in and clears the MDC as the response
 * is returned.
 */
public class SLF4JMDCServletFilter extends AbstractConditionalFilter implements ChainableFilter {
    
    /** MDC attribute name for host name of the server to which the current request was sent. */
    @Nonnull @NotEmpty public static final String SERVER_ADDRESS_MDC_ATTRIBUTE = "idp.server_hostname";

    /** MDC attribute name for port number to which the current request was sent. */
    @Nonnull @NotEmpty public static final String SERVER_PORT_MDC_ATTRIBUTE = "idp.server_port";
    
    /** MDC attribute name for client address. */
    @Nonnull @NotEmpty public static final String CLIENT_ADDRESS_MDC_ATTRIBUTE = "idp.remote_addr";

    /** MDC attribute name for container session ID. */
    @Nonnull @NotEmpty public static final String JSESSIONID_MDC_ATTRIBUTE = "idp.jsessionid";

    /** Whether to create a session if it doesn't already exist. */
    private boolean createSession;

    /** Constructor. */
    public SLF4JMDCServletFilter() {
        createSession = true;
    }
    
    /**
     * Sets whether to create a session if one does not exist.
     * 
     * @param flag flag to set
     */
    public void setCreateSession(final boolean flag) {
        createSession = flag;
    }

    /** {@inheritDoc} */
    @Override
    protected void runFilter(final @Nonnull ServletRequest request, final @Nonnull ServletResponse response,
            final @Nonnull FilterChain chain) throws IOException, ServletException {
        try {
            MDC.put(Version.MDC_ATTRIBUTE, Version.getVersion());
            MDC.put(CLIENT_ADDRESS_MDC_ATTRIBUTE, HttpServletSupport.getRemoteAddr(request));
            MDC.put(SERVER_ADDRESS_MDC_ATTRIBUTE, request.getServerName());
            MDC.put(SERVER_PORT_MDC_ATTRIBUTE, Integer.toString(request.getServerPort()));
            if (request instanceof HttpServletRequest) {
                final HttpSession session = ((HttpServletRequest) request).getSession(createSession);
                if (session != null) {
                    MDC.put(JSESSIONID_MDC_ATTRIBUTE, session.getId());
                }
            }
            
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

}