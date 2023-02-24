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

package net.shibboleth.idp.ui.csrf;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.util.function.BiPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.ui.csrf.impl.SimpleCSRFToken;
import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;
import net.shibboleth.shared.security.RandomIdentifierParameterSpec;

/**
 * A thread-safe helper class for dealing with cross-site request forgery tokens. 
 */
@ThreadSafe
public final class CSRFTokenManager extends AbstractInitializableComponent {
        
    /** The name of the HTTP parameter that contains the anti-csrf token.*/
   @Nonnull private String csrfParameterName;
   
   /** The strategy used to generate a CSRF token value. */
   @NonnullAfterInit private IdentifierGenerationStrategy tokenGenerationStrategy;
   
   /** Predicate to validate the CSRF token.*/
   @Nonnull private BiPredicate<CSRFToken,String> csrfTokenValidationPredicate;
    
    /** Constructor. */
    public CSRFTokenManager() {
        csrfParameterName = "csrf_token";
        csrfTokenValidationPredicate = new DefaultCSRFTokenValidationPredicate();
    }
    
    /**
     * Set the CSRF token generation strategy.
     * 
     * @param tokenStrategy CSRF token generation strategy
     */
    public void setTokenGenerationStrategy(@Nonnull final IdentifierGenerationStrategy tokenStrategy) {
        checkSetterPreconditions();
        tokenGenerationStrategy = Constraint.isNotNull(tokenStrategy, "tokenGenerationStrategy cannot be null");
    }
    
    /**
     * Set the CSRF token validation predicate.
     * 
     * @param tokenValidationPredicate the CSRF token validation predicate.
     */
    public void setCsrfTokenValidationPredicate(@Nonnull final BiPredicate<CSRFToken,String> tokenValidationPredicate) {
        checkSetterPreconditions();
        csrfTokenValidationPredicate = Constraint.isNotNull(tokenValidationPredicate, 
                "CSRF token validation predicate can not be null");
    }
    
    /**
     * Set the CSRF HTTP parameter name.
     * 
     * @param parameterName CSRF parameter name
     */
    public void setCsrfParameterName(@Nonnull @NotEmpty final String parameterName) {
        checkSetterPreconditions();
        csrfParameterName = Constraint.isNotEmpty(parameterName, "CsrfParameterName cannot be null or empty");
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (tokenGenerationStrategy == null) {
            try {
                tokenGenerationStrategy = IdentifierGenerationStrategy.getInstance(ProviderType.SECURE,
                        new RandomIdentifierParameterSpec(null, 20, null));
            } catch (final InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
                throw new ComponentInitializationException(e);
            }
        }
    }

    /**
     * Generate a {@link CSRFToken} using the token generation strategy derived token value. Set
     * the HTTP parameter name from the <code>csrfParameterName</code> field.
     * 
     * @return a CSRF token
     */
    @Nonnull public CSRFToken generateCSRFToken() {
        checkComponentActive();
        return new SimpleCSRFToken(tokenGenerationStrategy.generateIdentifier(),csrfParameterName);        
        
    }
    
    /**
     * Check the CSRF token matches the CSRF token in the request using the <code>csrfTokenValidationPredicate</code>. 
     * 
     * @param csrfToken the server side CSRF token.
     * @param requestCsrfToken the CSRF token from the request.
     * @return true iff the CSRF token value matches the request CSRF token. False if they do not match.
     */
    public boolean isValidCSRFToken(@Nullable final CSRFToken csrfToken, 
            @Nullable final String requestCsrfToken) {
        checkComponentActive();
        return csrfTokenValidationPredicate.test(csrfToken, requestCsrfToken);       
    }
    
    /**
     * A simple default CSRF token validation predicate. Tests the CSRF token matches the request token
     * by exact string (sequence of characters) match. Tokens will not match, returns false, if either 
     * of the tokens are {@literal null}.
     */
    private static class DefaultCSRFTokenValidationPredicate implements BiPredicate<CSRFToken,String>{

        /** {@inheritDoc} */
        public boolean test(@Nullable final CSRFToken csrfToken, @Nullable final String requestCSRFToken) {
            
            if (csrfToken==null || requestCSRFToken==null) {
                return false;
            }
          
            if (csrfToken.getToken().equals(requestCSRFToken)) {
                return true;
            }        
            
            return false;
        }
        
    }

}