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

package net.shibboleth.idp.profile.spring.relyingparty.security.credential;

import java.io.IOException;

import net.shibboleth.idp.profile.spring.relyingparty.security.AbstractSecurityParserTest;

import org.opensaml.security.credential.Credential;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.testng.annotations.Test;

/**
 * Test parsing Resource backed Credentials.  These are not support and so the test should fail.
 * Eyaball the logs.
 */
public class ResourceParserTest extends AbstractSecurityParserTest {

    @Test(expectedExceptions={BeanDefinitionStoreException.class,}) public void x509() throws IOException {
        getBean(Credential.class, true, "credential/resourceX509.xml");
    }
    
    @Test(expectedExceptions={BeanDefinitionStoreException.class,}) public void basic() throws IOException {
        getBean(Credential.class, true, "credential/resourceBasic.xml");
    }
}