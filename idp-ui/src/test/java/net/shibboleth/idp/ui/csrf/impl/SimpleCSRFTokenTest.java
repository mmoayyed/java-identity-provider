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

package net.shibboleth.idp.ui.csrf.impl;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * Test the {@link SimpleCSRFToken}.
 */
public class SimpleCSRFTokenTest {
    
    /** Test token construction with valid parameters.*/
    @Test public void testTokenConstruction() {
        
        final String tokenValue = UUID.randomUUID().toString();
        final String paramName = "csrf_token";
        SimpleCSRFToken token = new SimpleCSRFToken(tokenValue, paramName);
        Assert.assertEquals(tokenValue, token.getToken());
        Assert.assertEquals(paramName, token.getParameterName());
    }
    
    /** Test token construction with an invalid null token value parameter.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testNullValueConstruction() {        
       
        final String paramName = "csrf_token";
        new SimpleCSRFToken(null, paramName);
      
    }
    
    /** Test token construction with an invalid null parameter name parameter.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testNullParamConstruction() {        
        
        final String tokenValue = UUID.randomUUID().toString();
        new SimpleCSRFToken(tokenValue, null);
      
    }
    
    /** Test token construction with all invalid null parameters.*/
    @Test(expectedExceptions=ConstraintViolationException.class) public void testNullValueAndParamConstruction() {        
    
        new SimpleCSRFToken(null, null);
      
    }

}
