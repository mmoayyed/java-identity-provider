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

package net.shibboleth.idp.authn.impl;

import java.util.Enumeration;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;

import net.shibboleth.idp.authn.AbstractExtractionAction;
import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.UsernamePasswordContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.codec.Base64Support;
import net.shibboleth.shared.codec.DecodingException;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

import jakarta.servlet.http.HttpServletRequest;

/**
/**
 * An action that extracts a username and password from the HTTP {@link HttpHeaders#AUTHORIZATION} header,
 * creates a {@link UsernamePasswordContext}, and attaches it to the {@link AuthenticationContext}.
 * 
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 * @event {@link AuthnEventIds#NO_CREDENTIALS}
 * @event {@link AuthnEventIds#INVALID_CREDENTIALS}
 * @pre <pre>ProfileRequestContext.getSubcontext(AuthenticationContext.class, false) != null</pre>
 * @post If getHttpServletRequest() != null, the content of the {@link HttpHeaders#AUTHORIZATION}
 * header is parsed and any correctly-encoded information will be attached via a {@link UsernamePasswordContext}.
 */
public class ExtractUsernamePasswordFromBasicAuth extends AbstractExtractionAction {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ExtractUsernamePasswordFromBasicAuth.class);
    
    /** {@inheritDoc} */
    // CheckStyle: ReturnCount OFF
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final AuthenticationContext authenticationContext) {

        final UsernamePasswordContext upCtx =
                authenticationContext.ensureSubcontext(UsernamePasswordContext.class);
        upCtx.setUsername(null);
        upCtx.setPassword(null);
        
        final HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            log.debug("{} Profile action does not contain an HttpServletRequest", getLogPrefix());
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        final String encodedCredentials = extractCredentials(request);
        if (encodedCredentials == null) {
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.NO_CREDENTIALS);
            return;
        }
        
        final Pair<String, String> decodedCredentials = decodeCredentials(encodedCredentials);
        if (decodedCredentials == null) {
            ActionSupport.buildEvent(profileRequestContext, AuthnEventIds.INVALID_CREDENTIALS);
            return;
        }
        
        upCtx.setUsername(applyTransforms(decodedCredentials.getFirst())).setPassword(decodedCredentials.getSecond());
    }
    // CheckStyle: ReturnCount ON

    /**
     * Gets the encoded credentials passed in via the {@link HttpHeaders#AUTHORIZATION} header. This method checks to
     * ensure that the authentication scheme is {@link HttpServletRequest#BASIC_AUTH} and then strips off
     * and returns the follow on Base64-encoded credentials.
     * 
     * @param httpRequest current HTTP request
     * 
     * @return the Base64 encoded credentials, or null
     */
    @Nullable protected String extractCredentials(@Nonnull final HttpServletRequest httpRequest) {

        final Enumeration<String> header = httpRequest.getHeaders(HttpHeaders.AUTHORIZATION);
        while (header.hasMoreElements()) {
            final String[] splitValue = header.nextElement().split(" ");
            if (splitValue.length == 2) {
                final String authnScheme = StringSupport.trimOrNull(splitValue[0]);
                if (HttpServletRequest.BASIC_AUTH.equalsIgnoreCase(authnScheme)) {
                    return StringSupport.trimOrNull(splitValue[1]);
                }
            }
        }
        
        log.debug("{} No appropriate Authorization header found", getLogPrefix());
        return null;
    }

    /**
     * Decodes the credential string provided in the HTTP header,
     * splits it in to a username and password, and returns them.
     * 
     * @param encodedCredentials the Base64 encoded credentials
     * 
     * @return a pair containing the username and password, respectively, or null
     */
    @Nullable protected Pair<String,String> decodeCredentials(@Nonnull @NotEmpty final String encodedCredentials) {
        String decodedUserPass = null;
        try {
            decodedUserPass = new String(Base64Support.decode(encodedCredentials), Charsets.UTF_8);
        } catch (final DecodingException e) {
            log.warn("{} Credentials could not be base64 decoded: {}",getLogPrefix(),e.getMessage());
            //nothing else, decodedUserPass remains null.
        }

        if (decodedUserPass != null && decodedUserPass.contains(":")) {
            final String username = decodedUserPass.substring(0, decodedUserPass.indexOf(':'));
            if (username != null && decodedUserPass.length() > username.length() + 1) {
                final String password = decodedUserPass.substring(decodedUserPass.indexOf(':') + 1);
                if (password != null) {
                    return new Pair<>(username, password);
                }
            }
        }

        log.debug("{} Request did not contain a well-formed Basic authorization header value", getLogPrefix());
        return null;
    }
}