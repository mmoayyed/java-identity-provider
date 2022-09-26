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

package net.shibboleth.idp.authn.impl;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.security.x509.X509Support;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Servlet filter to translate Apache mod_ssl certificate variables into Java servlet attributes.
 */
public class X509ProxyFilter implements Filter {

    /** Init parameter controlling what headers to check for the leaf certificate. */
    @Nonnull @NotEmpty public static final String LEAF_HEADER_PARAM = "leafHeader";

    /** Init parameter controlling what headers to check for the chain certificates. */
    @Nonnull @NotEmpty public static final String CHAIN_HEADERS_PARAM = "chainHeaders";

    /** Apache null indicator. */
    @Nonnull @NotEmpty private static final String APACHE_NULL = "(null)";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(X509ProxyFilter.class);

    /** Name of header containing end-entity certificate. */
    @Nullable @NotEmpty private String leafHeader;

    /** Name of headers containing chain certificates. */
    @Nonnull @NonnullElements private Collection<String> chainHeaders;

    /** Constructor. */
    public X509ProxyFilter() {
        chainHeaders = Collections.emptyList();
    }

    /** {@inheritDoc} */
    public void init(final FilterConfig config) throws ServletException {
        leafHeader = config.getInitParameter(LEAF_HEADER_PARAM);
        if (leafHeader == null) {
            throw new ServletException("Required init-parameter " + LEAF_HEADER_PARAM + " missing");
        }
        log.info("X509ProxyFilter will check for the end-entity certificate in: {}", leafHeader);
    
        final String param = config.getInitParameter(CHAIN_HEADERS_PARAM);
        if (param != null) {
            final String[] headers = param.split(" ");
            if (headers != null) {
                chainHeaders = StringSupport.normalizeStringCollection(Arrays.asList(headers));
            }
        }
        log.info("X509ProxyFilter will check for chain certificates in: {}", chainHeaders);
    }

// Checkstyle: CyclomaticComplexity OFF 
    /** {@inheritDoc} */
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) request;
                
        try {
            X509Certificate[] certs =
                    (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
            if (null == certs || 0 == certs.length) {
                certs = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
            }
            
            if (null == certs || 0 == certs.length) {
                final List<X509Certificate> proxyCerts = new ArrayList<>();

                if (leafHeader != null) {
                    final String pem = httpRequest.getHeader(leafHeader);
                    if (pem != null && !pem.isEmpty() && !APACHE_NULL.equals(pem)) {
                        proxyCerts.add(X509Support.decodeCertificate(pem.getBytes()));
                    } else {
                        log.warn("No end-entity certificate found");
                        return;
                    }
                } else {
                    log.warn("No end-entity certificate found");
                    return;
                }
                
                for (final String s : chainHeaders) {
                    final String pem = httpRequest.getHeader(s);
                    if (pem != null && !pem.isEmpty() && !APACHE_NULL.equals(pem)) {
                        proxyCerts.add(X509Support.decodeCertificate(pem.getBytes()));
                    }
                }
                
                if (!proxyCerts.isEmpty()) {
                    // TODO: I guess we'd check the class name(s) we're using here to
                    // know which attribute to populate?
                    request.setAttribute("jakarta.servlet.request.X509Certificate",
                            proxyCerts.toArray(new X509Certificate[proxyCerts.size()]));
                }
            }
        } catch (final Exception e) {
            log.error(e.getMessage());
        } finally {
            chain.doFilter(request, response);
        }
    }
// Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    public void destroy() {
        
    }
    
}
