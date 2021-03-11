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

package net.shibboleth.idp.profile.spring.factory;

import java.util.Set;

import org.opensaml.security.SecurityException;
import org.opensaml.security.x509.PKIXTrustEvaluator;
import org.opensaml.security.x509.PKIXValidationInformation;
import org.opensaml.security.x509.PKIXValidationOptions;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.impl.BasicX509CredentialNameEvaluator;
import org.opensaml.security.x509.impl.CertPathPKIXTrustEvaluator;
import org.opensaml.security.x509.impl.PKIXX509CredentialTrustEngine;
import org.opensaml.security.x509.impl.StaticPKIXValidationInformationResolver;
import org.opensaml.security.x509.impl.X509CredentialNameEvaluator;
import org.springframework.beans.FatalBeanException;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;

/**
 *
 */
@SuppressWarnings("javadoc")
public class StaticPKIXFactoryBeanTest {

    @Test
    public void defaults() {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        context.setDisplayName("ApplicationContext: X509Credential");
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/profile/spring/factory/static-pkix-factory-defaults.xml");

        context.refresh();

        final PKIXX509CredentialTrustEngine trustEngine = context.getBean("StaticPKIXX509CredentialTrustEngine",
                PKIXX509CredentialTrustEngine.class);
        
        Assert.assertNotNull(trustEngine);
        
        Assert.assertTrue(StaticPKIXValidationInformationResolver.class.isInstance((trustEngine.getPKIXResolver())));
        
        Assert.assertTrue(CertPathPKIXTrustEvaluator.class.isInstance((trustEngine.getPKIXTrustEvaluator())));
        
        Assert.assertTrue(BasicX509CredentialNameEvaluator.class.isInstance((trustEngine.getX509CredentialNameEvaluator())));
    }
    
    @Test
    public void customPropertiesSuccess() {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        context.setDisplayName("ApplicationContext: X509Credential");
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/profile/spring/factory/static-pkix-factory-custom-success.xml");

        context.refresh();

        final PKIXX509CredentialTrustEngine trustEngine = context.getBean("StaticPKIXX509CredentialTrustEngine",
                PKIXX509CredentialTrustEngine.class);
        
        Assert.assertNotNull(trustEngine);
        
        Assert.assertTrue(StaticPKIXValidationInformationResolver.class.isInstance((trustEngine.getPKIXResolver())));
        
        Assert.assertTrue(MockPKIXTrustEvaluator.class.isInstance((trustEngine.getPKIXTrustEvaluator())));
        
        Assert.assertTrue(MockX509CredentialNameEvaluator.class.isInstance((trustEngine.getX509CredentialNameEvaluator())));
    }
    
    @Test(expectedExceptions=FatalBeanException.class)
    public void customPropertiesFailsValidation() {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        context.setDisplayName("ApplicationContext: X509Credential");
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/profile/spring/factory/static-pkix-factory-custom-failsValidation.xml");

        context.refresh();
    }
    
    
    // 
    // Helpers
    //
    
    public static class MockPKIXTrustEvaluator implements PKIXTrustEvaluator {

        /** {@inheritDoc} */
        public boolean validate(PKIXValidationInformation validationInfo, X509Credential untrustedCredential)
                throws SecurityException {
            return false;
        }

        /** {@inheritDoc} */
        public PKIXValidationOptions getPKIXValidationOptions() {
            return null;
        }
        
    }
    
    public static class MockX509CredentialNameEvaluator implements X509CredentialNameEvaluator {

        /** {@inheritDoc} */
        public boolean evaluate(X509Credential credential, Set<String> trustedNames) throws SecurityException {
            return false;
        }
        
    }
}
