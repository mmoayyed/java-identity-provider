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

package net.shibboleth.idp.authn.duo.impl;

import com.duosecurity.duoweb.Base64;
import com.duosecurity.duoweb.DuoWeb;
import com.duosecurity.duoweb.DuoWebException;
import com.duosecurity.duoweb.Util;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;

import net.shibboleth.idp.authn.duo.DuoIntegration;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;

/**
 * Helpers for DuoWeb and Duo AuthAPI operations.
 * 
 * @since 3.3.0
 */
public final class DuoSupport {

    /** RFC 2822 formatter for date/time. */
    public static final DateTimeFormatter RFC_2822_DATE_FORMAT;

    /** Constructor. */
    private DuoSupport() {
    }
    
    /**
     * Created a signed request to Duo for a user.
     * 
     * @param duo integration parameters to use
     * @param username user to authenticate
     * @return the signed request string
     * 
     * @throws DuoWebException if an error occurs
     */
    @Nonnull @NotEmpty public static String generateSignedRequestToken(@Nonnull final DuoIntegration duo,
            @Nonnull @NotEmpty final String username)
            throws DuoWebException {
        final String signedRequestToken;
        
        if (username == null) {
            signedRequestToken = DuoWeb.ERR_USER;
        } else if (duo.getApplicationKey() == null) {
            signedRequestToken = DuoWeb.ERR_AKEY;
        } else {
            signedRequestToken = DuoWeb.signRequest(duo.getIntegrationKey(), duo.getSecretKey(),
                    duo.getApplicationKey(), username);
        }
        if (signedRequestToken.startsWith("ERR|")) {
            throw new DuoWebException(signedRequestToken);
        }
        return signedRequestToken;
    }

// Checkstyle: ThrowsCount OFF
    /**
     * Verify a signed response from Duo and extract the username.
     * 
     * @param duo integration parameters to use
     * @param signedResponseToken    response to validate
     * 
     * @return the username from the response
     * @throws DuoWebException if a Duo failure occurs
     * @throws InvalidKeyException if a key is invalid
     * @throws IOException if an I/O error occurs
     * @throws NoSuchAlgorithmException if the hashing algorithm is unavailable
     */
    @Nonnull @NotEmpty public static String validateSignedResponseToken(@Nonnull final DuoIntegration duo,
            @Nonnull @NotEmpty final String signedResponseToken)
        throws DuoWebException, InvalidKeyException, IOException, NoSuchAlgorithmException {
        try {
            if (duo.getApplicationKey() == null) {
                throw new DuoWebException(DuoWeb.ERR_AKEY);
            }
            final String username = DuoWeb.verifyResponse(duo.getIntegrationKey(), duo.getSecretKey(),
                    duo.getApplicationKey(), signedResponseToken);
            return username;
        } catch (final ArrayIndexOutOfBoundsException e) {
            // This guard is to prevent an unusual issue being encountered by at least one deployer.
            throw new DuoWebException(e.getMessage());
        }
    }
// Checkstyle: ThrowsCount ON
 
    /**
     * Sign a Duo AuthAPI request.
     * 
     * @param request the request to be signed
     * @param duo integration parameters to use
     * 
     * @throws InvalidKeyException bad skey value
     * @throws NoSuchAlgorithmException unknown encryption algorithm
     * @throws UnsupportedEncodingException failure from {@link java.net.URLEncoder}
     * 
     * @since 3.4.0
     */
    @NotEmpty public static void signRequest(@Nonnull final ClassicRequestBuilder request,
            @Nonnull final DuoIntegration duo)
            throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException {
        final String ikey = duo.getIntegrationKey();
        final String skey = duo.getSecretKey();
        final int sigVersion = 2;
        final String date = RFC_2822_DATE_FORMAT.format(ZonedDateTime.now());
        final String canon = canonRequest(request, date, sigVersion);
        final String sig = Util.hmacSign(skey, canon);

        final String auth = ikey + ":" + sig;
        final String header = "Basic " + Base64.encodeBytes(auth.getBytes());
        request.addHeader("Authorization", header);
        request.addHeader("Date", date);
    }

    /**
     * The signature requires that the request parameters being in a particular order as specified in the API.
     * 
     * @param request the request
     * @param date the date
     * @param sigVersion the signature version
     * 
     * @return the parameters to be signed in their canonical order
     * 
     * @throws UnsupportedEncodingException failure from {@link java.net.URLEncoder}
     */
    private static String canonRequest(@Nonnull final ClassicRequestBuilder request, @Nonnull final String date,
            final int sigVersion) throws UnsupportedEncodingException {
        final URI uri = request.getUri();
        String canon = "";
        if (sigVersion == 2) {
            canon += date + "\n";
        }
        canon += request.getMethod().toUpperCase() + "\n";
        canon += uri.getHost().toLowerCase() + "\n";
        canon += uri.getPath() + "\n";
        canon += createQueryString(request.getParameters());

        return canon;
    }

    /**
     * Builds a string representation of the query string with the parameter names is alphabetical order. The names and
     * values are URL encoded and then they are concatenated with '&amp;' in between.
     * 
     * @param params the name/value pairs to be joined
     * 
     * @return the canonical query string
     * 
     * @throws UnsupportedEncodingException failure from {@link java.net.URLEncoder}
     */
    private static String createQueryString(@Nonnull final List<NameValuePair> params)
            throws UnsupportedEncodingException {

        final ArrayList<String> args = new ArrayList<>();

        // sort by name
        Collections.sort(params, new Comparator<NameValuePair>() {
            public int compare(final NameValuePair nvp1, final NameValuePair nvp2) {
                return nvp1.getName().compareTo(nvp2.getName());
            }
        });

        // URL encode and join the name/values with '='
        final Escaper escaper = UrlEscapers.urlFormParameterEscaper();
        for (final NameValuePair nvp : params) {
            final String name = escaper.escape(nvp.getName()).replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
            final String value = escaper.escape(nvp.getValue()).replace("+", "%20").replace("*", "%2A")
                    .replace("%7E", "~");
            args.add(name + "=" + value);
        }

        // Concatenate everything togther with '&'
        return StringSupport.listToStringValue(args, "&");
    }
    
    static {
        RFC_2822_DATE_FORMAT = DateTimeFormatter.ofPattern("EEE', 'dd' 'MMM' 'yyyy' 'HH:mm:ss' 'Z");
    }
    
}