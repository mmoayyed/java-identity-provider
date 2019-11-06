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

/** Command line processing for MetadataQuery flow. */
public class MetadataQueryArguments extends AbstractCommandLineArguments {

    /** EntityID. */
    @Parameter(names = {"-e", "--entityID"}, required = true, description = "EntityID to find metadata for")
    @Nullable private String entityID;

    /** Role protocol.  */
    @Parameter(names = {"--protocol"}, description = "Optional protocol to find metadata role for")
    @Nullable private String protocol;

    /** SAML 1.1 protocol. */
    @Parameter(names = {"--saml1"}, description = "Query for SAML 1.1 role")
    private boolean saml1;

    /** SAML 2.0 protocol. */
    @Parameter(names = {"--saml2"}, description = "Query for SAML 2.0 role")
    private boolean saml2;

    /** CAS protocol. */
    @Parameter(names = {"--cas"}, description = "Query for CAS role")
    private boolean cas;

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override
    public void validate() {
        try {
            if (saml1) {
                if (saml2 || cas || protocol != null) {
                    throw new IllegalArgumentException();
                }
            } else if (saml2) {
                if (cas || protocol != null) {
                    throw new IllegalArgumentException();
                }
            } else if (cas) {
                if (protocol != null) {
                    throw new IllegalArgumentException();
                }
            }
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("The saml1, saml2, cas, and protocol options are mutually exclusive");
        }
    }
// Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    @Override
    protected StringBuilder doBuildURL(@Nonnull final StringBuilder builder) {
        
        if (getPath() == null) {
            builder.append("/profile/admin/mdquery");
        }
        
        if (builder.toString().contains("?")) {
            builder.append('&');
        } else {
            builder.append('?');
        }
        
        try {
            builder.append("entityID=").append(URLEncoder.encode(entityID, "UTF-8"));
            if (saml1) {
                builder.append("&saml1");
            } else if (saml2) {
                builder.append("&saml2");
            } else if (cas) {
                builder.append("&cas");
            } else if (protocol != null) {
                builder.append("&protocol=").append(URLEncoder.encode(protocol, "UTF-8"));
            }
        } catch (final UnsupportedEncodingException e) {
            // UTF-8 is a required encoding. 
        }
        
        return builder;
    }
        
}