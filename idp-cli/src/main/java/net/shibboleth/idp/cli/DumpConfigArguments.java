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

package net.shibboleth.idp.cli;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.beust.jcommander.Parameter;

/** Command line processing for DumpConfig flow. */
public class DumpConfigArguments extends AbstractCommandLineArguments {

    /** Attribute requester identity. */
    @Parameter(names = {"-r", "--requester"}, required = true, description = "Relying party identity")
    @Nullable private String requester;

    /** Index into metadata.  */
    @Parameter(names = {"-P", "--profile"}, description = "Profile identifier")
    @Nullable private String profile;

    /** Exact protocol for metadata lookup.  */
    @Parameter(names = {"--protocol"}, description = "Protocol support enumeration for metadata lookup")
    @Nullable private String protocol;

    /** SAML 1.1 protocol. */
    @Parameter(names = {"--saml1"}, description = "Use SAML 1.1 protocol for metadata lookup")
    private boolean saml1;

    /** SAML 2.0 protocol. */
    @Parameter(names = {"--saml2"}, description = "Use SAML 2.0 protocol for metadata lookup")
    private boolean saml2;

    /** CAS protocol. */
    @Parameter(names = {"--cas"}, description = "Use CAS protocol for metadata lookup")
    private boolean cas;

    /** OIDC protocol. */
    @Parameter(names = {"--oidc"}, description = "Use OIDC protocol for metadata lookup")
    private boolean oidc;

    /** {@inheritDoc} */
// Checkstyle: CyclomaticComplexity OFF
    @Override
    public void validate() {
        final String msg =  "The saml1, saml2, cas, oidc, and protocol options are mutually exclusive";
        if (saml1) {
            if (saml2 || cas || oidc || protocol != null) {
                throw new IllegalArgumentException(msg);
            }
        } else if (saml2) {
            if (saml1 || cas || oidc || protocol != null) {
                throw new IllegalArgumentException(msg);
            }
        } else if (cas) {
            if (saml1 || saml2 || oidc || protocol != null) {
                throw new IllegalArgumentException(msg);
            }
        } else if (oidc) {
            if (saml1 || saml2 || cas || protocol != null) {
                throw new IllegalArgumentException(msg);
            }
        } else if (protocol != null) {
            if (saml1 || saml2 || cas || oidc) {
                throw new IllegalArgumentException(msg);
            }
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** {@inheritDoc} */
    @Override
    protected @Nonnull StringBuilder doBuildURL(@Nonnull final StringBuilder builder) {
        
        if (getPath() == null) {
            builder.append("/profile/admin/dumpconfig");
        }
        
        if (builder.toString().contains("?")) {
            builder.append('&');
        } else {
            builder.append('?');
        }
        
        try {
            builder.append("requester=").append(URLEncoder.encode(requester, "UTF-8"));
            if (saml1) {
                builder.append("&saml1");
            } else if (saml2) {
                builder.append("&saml2");
            } else if (cas) {
                builder.append("&cas");
            } else if (oidc) {
                builder.append("&oidc");
            } else if (protocol != null) {
                builder.append("&protocol=").append(URLEncoder.encode(protocol, "UTF-8"));
            }
        } catch (final UnsupportedEncodingException e) {
            // UTF-8 is a required encoding. 
        }
        
        return builder;
    }
        
}