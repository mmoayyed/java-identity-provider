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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.ui.csrf.impl.SimpleCSRFToken;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;
import net.shibboleth.utilities.java.support.security.impl.SecureRandomIdentifierGenerationStrategy;

/**
 * Tests for the {@link CSRFTokenManager}.
 */
public class CSRFTokenManagerTest {
    
    /** Test token manager instance.*/
    private CSRFTokenManager manager;
    
    
    @BeforeMethod public void setup() {
        manager = new CSRFTokenManager();
    }
    
    /** Test setting an invalid csrf parameter name, which should trigger an exception.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testSetNullCsrfParameterName() {       
        manager.setCsrfParameterName(null);
    }
    
    /** Test setting a valid csrf parameter name, which should not trigger an exception.*/
    @Test public void testSetCsrfParameterName() {       
        manager.setCsrfParameterName("csrf_token");
    }
    
    /** Test setting an invalid token generation strategy, which should trigger an exception.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testSetNullTokenGenerationStrategy() {       
        manager.setTokenGenerationStrategy(null);
    }
    
    /** Test setting a valid token generation strategy, which should not trigger an exception.*/
    @Test public void testSetTokenGenerationStrategy() {       
        manager.setTokenGenerationStrategy(new SecureRandomIdentifierGenerationStrategy(20));
    }
    
    /** Test what happens if you add a custom validation predicate that always returns false.*/
    @Test public void testCustomTokenValidationPredicate() {
        manager.setCsrfTokenValidationPredicate((stored,request) -> {
            return false;
        });
        Assert.assertFalse(manager.isValidCSRFToken(new SimpleCSRFToken("token", "param"), "does-not-matter"));
    }
    
    /** Test token generation using the default instantiation of the token manager.*/
    @Test public void testDefaultTokenGeneration() {
        CSRFToken token = manager.generateCSRFToken();
        Assert.assertNotNull(token);
        Assert.assertNotNull(token.getParameterName());
        Assert.assertNotNull(token.getToken());
    
    }
    
    /** Test token generation using a customised instantiation of the token manager.*/
    @Test public void testCustomisedTokenGeneration() {
       
        manager.setCsrfParameterName("test_name");
        manager.setTokenGenerationStrategy(new SecureRandomIdentifierGenerationStrategy(40));
        CSRFToken token = manager.generateCSRFToken();
        Assert.assertNotNull(token);
        Assert.assertEquals("test_name",token.getParameterName());
        Assert.assertNotNull(token.getToken());
    }

}
