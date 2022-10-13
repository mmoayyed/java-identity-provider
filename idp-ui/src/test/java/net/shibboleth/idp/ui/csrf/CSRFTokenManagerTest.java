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
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.logic.ConstraintViolationException;
import net.shibboleth.shared.security.IdentifierGenerationStrategy;
import net.shibboleth.shared.security.IdentifierGenerationStrategy.ProviderType;

/**
 * Tests for the {@link CSRFTokenManager}.
 */
public class CSRFTokenManagerTest {
    
    /** Test token manager instance.*/
    private CSRFTokenManager manager;
    
    /**
     * Test setup. 
     * @throws ComponentInitializationException 
     */
    @BeforeMethod public void setup() throws ComponentInitializationException {
        manager = new CSRFTokenManager();
        manager.initialize();
    }
    
    /** Test setting an invalid csrf parameter name, which should trigger an exception.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testSetNullCsrfParameterName() {
        manager = new CSRFTokenManager();
        manager.setCsrfParameterName(null);
    }
    
    /** Test setting a valid csrf parameter name, which should not trigger an exception.*/
    @Test public void testSetCsrfParameterName() {
        manager = new CSRFTokenManager();
        manager.setCsrfParameterName("csrf_token");
    }
    
    /** Test setting an invalid token generation strategy, which should trigger an exception.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testSetNullTokenGenerationStrategy() {
        manager = new CSRFTokenManager();
        manager.setTokenGenerationStrategy(null);
    }
    
    /** Test setting a valid token generation strategy, which should not trigger an exception.*/
    @Test public void testSetTokenGenerationStrategy() {
        manager = new CSRFTokenManager();
        manager.setTokenGenerationStrategy(IdentifierGenerationStrategy.getInstance(ProviderType.SECURE));
    }
    
    /** Test what happens if you add a custom validation predicate that always returns false.
     * @throws ComponentInitializationException */
    @Test public void testCustomTokenValidationPredicate() throws ComponentInitializationException {
        manager = new CSRFTokenManager();
        manager.setCsrfTokenValidationPredicate((stored,request) -> {
            return false;
        });
        manager.initialize();
        Assert.assertFalse(manager.isValidCSRFToken(new SimpleCSRFToken("token", "param"), "does-not-matter"));
    }
    
    /** Test token generation using the default instantiation of the token manager.*/
    @Test public void testDefaultTokenGeneration() {
        CSRFToken token = manager.generateCSRFToken();
        Assert.assertNotNull(token);
        Assert.assertNotNull(token.getParameterName());
        Assert.assertNotNull(token.getToken());
    
    }
    
    /** Test token generation using a customised instantiation of the token manager.
     * @throws ComponentInitializationException */
    @Test public void testCustomisedTokenGeneration() throws ComponentInitializationException {
       
        manager = new CSRFTokenManager();
        manager.setCsrfParameterName("test_name");
        manager.setTokenGenerationStrategy(IdentifierGenerationStrategy.getInstance(ProviderType.SECURE));
        manager.initialize();
        CSRFToken token = manager.generateCSRFToken();
        Assert.assertNotNull(token);
        Assert.assertEquals("test_name",token.getParameterName());
        Assert.assertNotNull(token.getToken());
    }

}
