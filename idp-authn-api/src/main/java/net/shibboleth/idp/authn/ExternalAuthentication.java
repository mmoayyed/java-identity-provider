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

package net.shibboleth.idp.authn;

import java.io.IOException;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.context.ExternalContextHolder;
import org.springframework.webflow.context.servlet.ServletExternalContext;
import org.springframework.webflow.execution.FlowExecution;
import org.springframework.webflow.execution.repository.FlowExecutionRepository;
import org.springframework.webflow.execution.repository.FlowExecutionRepositoryException;
import org.springframework.webflow.executor.FlowExecutorImpl;

import com.google.common.base.Strings;
import com.google.common.net.UrlEscapers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.context.ExternalAuthenticationContext;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.logic.Constraint;

/** Public interface supporting external authentication outside the webflow engine. */
public abstract class ExternalAuthentication {

    /** Parameter supplied to locate the SWF object needed in the servlet context. */
    @Nonnull @NotEmpty public static final String SWF_KEY = "net.shibboleth.idp.flowExecutor";

    /** Parameter supplied to identify the per-conversation parameter. */
    @Nonnull @NotEmpty public static final String CONVERSATION_KEY = "conversation";
    
    /** Request attribute to which user's principal should be bound. */
    @Nonnull @NotEmpty public static final String PRINCIPAL_KEY = "principal";

    /** Request attribute to which user's principal name should be bound. */
    @Nonnull @NotEmpty public static final String PRINCIPAL_NAME_KEY = "principal_name";

    /** Request attribute to which user's subject should be bound. */
    @Nonnull @NotEmpty public static final String SUBJECT_KEY = "subject";

    /** Request attribute to which an authentication timestamp may be bound. */
    @Nonnull @NotEmpty public static final String AUTHENTICATION_INSTANT_KEY = "authnInstant";
    
    /** 
     * Request attribute to which a collection of authenticating authorities may be bound.
     * 
     * @since 3.4.0
     */
    @Nonnull @NotEmpty public static final String AUTHENTICATING_AUTHORITIES_KEY = "authnAuthorities";

    /** 
     * Request attribute to which a collection of {@link IdPAttribute} objects may be bound.
     * 
     * @since 4.0.0
     */
    @Nonnull @NotEmpty public static final String ATTRIBUTES_KEY = "attributes";

    /** Request attribute to which an error message may be bound. */
    @Nonnull @NotEmpty public static final String AUTHENTICATION_ERROR_KEY = "authnError";

    /** Request attribute to which an exception may be bound. */
    @Nonnull @NotEmpty public static final String AUTHENTICATION_EXCEPTION_KEY = "authnException";

    /** Request attribute to which a signal not to cache the result may be bound. */
    @Nonnull @NotEmpty public static final String DONOTCACHE_KEY = "doNotCache";

    /**
     * Request attribute to which a signal to revoke consent for attribute release may be bound.
     * 
     * @since 3.2.0
     */
    @Nonnull @NotEmpty public static final String REVOKECONSENT_KEY = "revokeConsent";

    /**
     * Request attribute to which a signal to set
     * {@link net.shibboleth.idp.authn.AuthenticationResult#setPreviousResult(boolean)} may be bound.
     * 
     * @since 3.3.0
     */
    @Nonnull @NotEmpty public static final String PREVIOUSRESULT_KEY = "previousResult";
    
    /** Request attribute that indicates whether the authentication request requires forced authentication. */
    @Nonnull @NotEmpty public static final String FORCE_AUTHN_PARAM = "forceAuthn";

    /** Request attribute that indicates whether the authentication requires passive authentication. */
    @Nonnull @NotEmpty public static final String PASSIVE_AUTHN_PARAM = "isPassive";

    /** Request attribute that provides the entity ID of the relying party that is requesting authentication. */
    @Nonnull @NotEmpty public static final String RELYING_PARTY_PARAM = "relyingParty";

    /**
     * Request attribute that indicates whether we're being called as an extension of another login flow.
     * 
     * @since 3.2.0
     */
    @Nonnull @NotEmpty public static final String EXTENDED_FLOW_PARAM = "extended";

    /**
     * Computes the appropriate location to pass control to to invoke an external authentication mechanism.
     * 
     *  <p>The input location should be suitable for use in a Spring "externalRedirect" expression, and may
     *  contain a query string. The result will include any additional parameters needed to invoke the
     *  mechanism.</p>
     * 
     * @param baseLocation the base location to build off of
     * @param conversationValue the value to include as a conversation ID
     * 
     * @return the computed location
     * 
     * @since 3.2.0
     */
    @Nonnull @NotEmpty public static String getExternalRedirect(@Nonnull @NotEmpty final String baseLocation,
            @Nonnull @NotEmpty final String conversationValue) {
        Constraint.isNotEmpty(baseLocation, "Base location cannot be null or empty");
        
        final StringBuilder url = new StringBuilder(baseLocation);
        
        // Add a parameter separator for the conversation ID.
        url.append(baseLocation.indexOf('?') == -1 ? '?' : '&');
        url.append(CONVERSATION_KEY).append('=').append(
                UrlEscapers.urlFormParameterEscaper().escape(conversationValue));
        final String result = url.toString();
        assert result != null;
        return result;
    }
    
    /**
     * Initialize a request for external authentication by seeking out the information stored in
     * the servlet session and exposing it as request attributes.
     * 
     * @param request servlet request
     * 
     * @return a handle to subsequent use of
     *      {@link #finishExternalAuthentication(java.lang.String, HttpServletRequest, HttpServletResponse)}
     * 
     * @throws ExternalAuthenticationException if an error occurs
     */
    @Nonnull @NotEmpty public static String startExternalAuthentication(@Nonnull final HttpServletRequest request)
            throws ExternalAuthenticationException {
        final String key = request.getParameter(CONVERSATION_KEY);
        if (Strings.isNullOrEmpty(key)) {
            throw new ExternalAuthenticationException("No conversation key found in request");
        }
        assert key != null;
        
        final ProfileRequestContext profileRequestContext = getProfileRequestContext(key, request);
        final ExternalAuthenticationContext extContext = getExternalAuthenticationContext(profileRequestContext);
        extContext.getExternalAuthentication().doStart(request, profileRequestContext, extContext);
        
        return key;
    }
    
    /**
     * Complete a request for external authentication by seeking out the information stored in
     * request attributes and transferring to the session's conversation state, and then transfer
     * control back to the authentication web flow.
     * 
     * @param key   the value returned by {@link #startExternalAuthentication(HttpServletRequest)}
     * @param request servlet request
     * @param response servlet response
     * 
     * @throws ExternalAuthenticationException if an error occurs
     * @throws IOException if the redirect cannot be issued
     */
    public static void finishExternalAuthentication(@Nonnull @NotEmpty final String key,
            @Nonnull final HttpServletRequest request, @Nonnull final HttpServletResponse response)
            throws ExternalAuthenticationException, IOException {
        
        final ProfileRequestContext profileRequestContext = getProfileRequestContext(key, request);
        final ExternalAuthenticationContext extContext = getExternalAuthenticationContext(profileRequestContext);
        extContext.getExternalAuthentication().doFinish(request, response, profileRequestContext, extContext);
    }

    /**
     * Get the {@link ProfileRequestContext} associated with a request.
     * 
     * @param key   the value returned by {@link #startExternalAuthentication(HttpServletRequest)}
     * @param request servlet request
     * 
     * @return the profile request context
     * @throws ExternalAuthenticationException if an error occurs
     */
    @Nonnull public static ProfileRequestContext getProfileRequestContext(@Nonnull @NotEmpty final String key,
            @Nonnull final HttpServletRequest request) throws ExternalAuthenticationException {
        
        final Object obj = request.getServletContext().getAttribute(SWF_KEY);
        if (!(obj instanceof FlowExecutorImpl)) {
            // This is a testing hook for injecting the PRC directly.
            if (obj instanceof ProfileRequestContext) {
                return (ProfileRequestContext) obj;
            }
            throw new ExternalAuthenticationException("No FlowExecutor available in servlet context");
        }

        try {
            final FlowExecutionRepository repo = ((FlowExecutorImpl) obj).getExecutionRepository();
            ExternalContextHolder.setExternalContext(
                    new ServletExternalContext(request.getServletContext(), request, null));
            
            final FlowExecution execution = repo.getFlowExecution(repo.parseFlowExecutionKey(key));
            final Object prc = execution.getConversationScope().get(ProfileRequestContext.BINDING_KEY);
            if (!(prc instanceof ProfileRequestContext)) {
                throw new ExternalAuthenticationException(
                        "ProfileRequestContext not available in webflow conversation scope");
            }
            
            return (ProfileRequestContext) prc;
        } catch (final FlowExecutionRepositoryException e) {
            throw new ExternalAuthenticationException("Error retrieving flow conversation", e);
        } finally {
            ExternalContextHolder.setExternalContext(null);
        }
    }
    
    /**
     * Utility method to access the {@link ExternalAuthenticationContext}.
     * 
     * @param profileRequestContext profile request context
     * 
     * @return the {@link ExternalAuthenticationContext} to operate on
     * 
     * @throws ExternalAuthenticationException if the context is missing
     */
    @Nonnull private static ExternalAuthenticationContext getExternalAuthenticationContext(
            @Nonnull final ProfileRequestContext profileRequestContext) throws ExternalAuthenticationException {
        
        final AuthenticationContext authContext = profileRequestContext.getSubcontext(AuthenticationContext.class);
        if (authContext == null) {
            throw new ExternalAuthenticationException("No AuthenticationContext found");
        }
        
        
        final ExternalAuthenticationContext extContext = authContext.getSubcontext(ExternalAuthenticationContext.class);
        if (extContext == null) {
            throw new ExternalAuthenticationException("No ExternalInterceptorContext found");
        }
        
        return extContext;
    }
    
    /**
     * Initialize a request for external authentication by seeking out the information stored in
     * the servlet session and exposing it as request attributes.
     * 
     * @param request servlet request
     * @param profileRequestContext current profile request context
     * @param externalAuthenticationContext external authentication context
     * 
     * @throws ExternalAuthenticationException if an error occurs
     */
    protected void doStart(@Nonnull final HttpServletRequest request,
            @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalAuthenticationContext externalAuthenticationContext)
                    throws ExternalAuthenticationException {
        
        request.setAttribute(ProfileRequestContext.BINDING_KEY, profileRequestContext);
    }

    /**
     * Complete a request for external authentication by seeking out the information stored in
     * request attributes and transferring to the session's conversation state, and then transfer
     * control back to the authentication web flow.
     * 
     * @param request servlet request
     * @param response servlet response
     * @param profileRequestContext current profile request context
     * @param externalAuthenticationContext external authentication context
     * 
     * @throws ExternalAuthenticationException if an error occurs
     * @throws IOException if the redirect cannot be issued
     */
    protected abstract void doFinish(@Nonnull final HttpServletRequest request,
            @Nonnull final HttpServletResponse response, @Nonnull final ProfileRequestContext profileRequestContext,
            @Nonnull final ExternalAuthenticationContext externalAuthenticationContext)
            throws ExternalAuthenticationException, IOException;
    
}
