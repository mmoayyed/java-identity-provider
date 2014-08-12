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

package net.shibboleth.idp.saml.profile;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Constants to use for audit logging fields stored in an {@link net.shibboleth.idp.profile.context.AuditContext}.
 */
public final class SAMLAuditFields {

    /** Service Provider field. */
    @Nonnull @NotEmpty public static final String SERVICE_PROVIDER = "SP";

    /** Identity Provider field. */
    @Nonnull @NotEmpty public static final String IDENTITY_PROVIDER = "IDP";

    /** Protocol field. */
    @Nonnull @NotEmpty public static final String PROTOCOL = "p";

    /** Request binding field. */
    @Nonnull @NotEmpty public static final String REQUEST_BINDING = "b";

    /** Response binding field. */
    @Nonnull @NotEmpty public static final String RESPONSE_BINDING = "bb";
    
    /** Name identifier field. */
    @Nonnull @NotEmpty public static final String NAMEID = "n";

    /** Assertion ID field. */
    @Nonnull @NotEmpty public static final String ASSERTION_ID = "i";

    /** Assertion IssueInstant field. */
    @Nonnull @NotEmpty public static final String ASSERTION_ISSUE_INSTANT = "d";
    
    /** Request message ID field. */
    @Nonnull @NotEmpty public static final String REQUEST_ID = "I";
    
    /** InResponseTo field. */
    @Nonnull @NotEmpty public static final String IN_RESPONSE_TO = "II";

    /** Response message ID field. */
    @Nonnull @NotEmpty public static final String RESPONSE_ID = "III";
    
    /** Protocol message IssueInstant field. */
    @Nonnull @NotEmpty public static final String PROTOCOL_ISSUE_INSTANT = "D";

    /** Authentication timestamp field. */
    @Nonnull @NotEmpty public static final String AUTHN_INSTANT = "t";

    /** SessionIndex field. */
    @Nonnull @NotEmpty public static final String SESSION_INDEX = "x";

    /** Authentication method/context/decl field. */
    @Nonnull @NotEmpty public static final String AUTHN_CONTEXT = "ac";

    /** Logout result field. */
    @Nonnull @NotEmpty public static final String LOGOUT_RESULT = "L";

    /** Constructor. */
    private SAMLAuditFields() {

    }

}