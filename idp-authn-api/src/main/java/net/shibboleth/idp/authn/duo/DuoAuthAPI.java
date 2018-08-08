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

package net.shibboleth.idp.authn.duo;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Constants defined in the Duo AuthAPI.
 * 
 * @since 3.4.0
 */
public final class DuoAuthAPI {

    /** Duo AuthAPI parameter name. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR = "factor";

    /** Duo AuthAPI parameter name. */
    @Nonnull @NotEmpty public static final String DUO_DEVICE = "device";

    /** Duo AuthAPI parameter name. */
    @Nonnull @NotEmpty public static final String DUO_PASSCODE = "passcode";

    /** Duo AuthAPI factor "auto" value. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR_AUTO = "auto";

    /** Duo AuthAPI factor "push" value. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR_PUSH = "push";

    /** Duo AuthAPI factor "passcode" value. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR_PASSCODE = "passcode";

    /** Duo AuthAPI factor "sms" value. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR_SMS = "sms";

    /** Duo AuthAPI factor "enum" value. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR_PHONE = "phone";

    /** Duo AuthAPI device "auto" value. */
    @Nonnull @NotEmpty public static final String DUO_DEVICE_AUTO = "auto";

    /** Duo AuthAPI preauth "allow" result value. */
    @Nonnull @NotEmpty public static final String DUO_PREAUTH_RESULT_ALLOW = "allow";

    /** Duo AuthAPI preauth "auth" result value. */
    @Nonnull @NotEmpty public static final String DUO_PREAUTH_RESULT_AUTH = "auth";

    /** Duo AuthAPI preauth "deny" result value. */
    @Nonnull @NotEmpty public static final String DUO_PREAUTH_RESULT_DENY = "deny";

    /** Duo AuthAPI preauth "enroll" result value. */
    @Nonnull @NotEmpty public static final String DUO_PREAUTH_RESULT_ENROLL = "enroll";

    /** Duo AuthAPI auth "allow" result value. */
    @Nonnull @NotEmpty public static final String DUO_AUTH_RESULT_ALLOW = "allow";

    /** Duo AuthAPI auth "deny" result value. */
    @Nonnull @NotEmpty public static final String DUO_AUTH_RESULT_DENY = "deny";

    /** Duo AuthAPI auth "bypass" result value. */
    @Nonnull @NotEmpty public static final String DUO_AUTH_STATUS_BYPASS = "bypass";

    /** Duo flow default header name for factor. */
    @Nonnull @NotEmpty public static final String DUO_FACTOR_HEADER_NAME = "X-Shibboleth-Duo-Factor";
    
    /** Duo flow default header name for device ID. */
    @Nonnull @NotEmpty public static final String DUO_DEVICE_HEADER_NAME = "X-Shibboleth-Duo-Device";

    /** Duo flow default header name for passcode. */
    @Nonnull @NotEmpty public static final String DUO_PASSCODE_HEADER_NAME = "X-Shibboleth-Duo-Passcode";

    /** Constructor. */
    private DuoAuthAPI() {
    }

}