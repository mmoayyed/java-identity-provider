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

import java.util.function.BiPredicate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.ui.csrf.impl.SimpleCSRFToken;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.impl.SecureRandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * A thread-safe helper class for dealing with cross-site request forgery tokens. 
 */
@ThreadSafe
public final class CSRFTokenManager {
        
    /** The name of the HTTP parameter that contains the anti-csrf token.*/
   @Nonnull private String csrfParameterName;
   
   /** The strategy used to generate a CSRF token value. */
   @Nonnull private IdentifierGenerationStrategy tokenGenerationStrategy;
   
   /** Predicate to validate the CSRF token.*/
   @Nonnull private BiPredicate<CSRFToken,String> csrfTokenValidationPredicate;
    
    /**
     * public Constructor.
     * 
     * <p>A 20 byte {@link SecureRandomIdentifierGenerationStrategy} is 
     * default to guarantee a strong token entropy.</p>
     *
     */
    public CSRFTokenManager() {
        csrfParameterName = "csrf_token";
        tokenGenerationStrategy = new SecureRandomIdentifierGenerationStrategy(20);
        csrfTokenValidationPredicate = new DefaultCSRFTokenValidationPredicate();
    }
    
    /**
     * Set the CSRF token generation strategy.
     * 
     * @param tokenStrategy CSRF token generation strategy
     */
    public void setTokenGenerationStrategy(@Nonnull final IdentifierGenerationStrategy tokenStrategy) {          
        tokenGenerationStrategy = Constraint.isNotNull(tokenStrategy, "tokenGenerationStrategy cannot be null");
    }
    
    /**
     * Set the CSRF token validation predicate.
     * 
     * @param tokenValidationPredicate the CSRF token validation predicate.
     */
    public void setCsrfTokenValidationPredicate(
            @Nonnull final BiPredicate<CSRFToken,String> tokenValidationPredicate) {       
        csrfTokenValidationPredicate = Constraint.isNotNull(tokenValidationPredicate, 
                "CSRF token validation predicate can not be null");
    }
    
    /**
     * Set the CSRF HTTP parameter name.
     * 
     * @param parameterName CSRF parameter name
     */
    public void setCsrfParameterName(@Nonnull @NotEmpty final String parameterName) {           
        csrfParameterName = Constraint.isNotEmpty(parameterName, "CsrfParameterName cannot be null or empty");
    }
    
    /**
     * Generate a {@link CSRFToken} using the token generation strategy derived token value. Set
     * the HTTP parameter name from the <code>csrfParameterName</code> field.
     * 
     * @return a CSRF token
     */
    @Nonnull public CSRFToken generateCSRFToken() {       
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
