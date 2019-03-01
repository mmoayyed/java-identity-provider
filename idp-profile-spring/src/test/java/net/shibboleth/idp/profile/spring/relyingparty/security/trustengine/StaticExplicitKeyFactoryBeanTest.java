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

package net.shibboleth.idp.profile.spring.relyingparty.security.trustengine;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

import net.shibboleth.idp.profile.spring.relyingparty.security.AbstractSecurityParserTest;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.security.credential.Credential;
import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.trust.impl.ExplicitKeyTrustEngine;
import org.opensaml.security.x509.X509Credential;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for StaticExplicitKey factory bean.
 */
public class StaticExplicitKeyFactoryBeanTest extends AbstractSecurityParserTest {
 
    static private String TESTPATH = "trustengine/staticExplicitNative.xml";
    
    @Test public void singleKey() throws IOException, ResolverException {
        final ExplicitKeyTrustEngine engine = (ExplicitKeyTrustEngine) getBean("staticKeySingle", TrustEngine.class, true, TESTPATH);
        
        final StaticCredentialResolver resolver = (StaticCredentialResolver) engine.getCredentialResolver();
        Credential credential  = (Credential) resolver.resolveSingle(null);
        
        Assert.assertNotNull(credential.getPublicKey());
    }
    
    @Test public void singleCert() throws IOException, ResolverException {
        final ExplicitKeyTrustEngine engine = (ExplicitKeyTrustEngine) getBean("staticX509Single", TrustEngine.class, true, TESTPATH);
        
        final StaticCredentialResolver resolver = (StaticCredentialResolver) engine.getCredentialResolver();
        X509Credential credential  = (X509Credential) resolver.resolveSingle(null);
        
        Assert.assertEquals(credential.getEntityCertificateChain().size(), 1);
        Assert.assertTrue(credential.getEntityCertificateChain().contains(credential.getEntityCertificate()));

        Assert.assertEquals(credential.getEntityCertificate().getNotAfter().getTime(), Instant.parse("2024-04-08T13:39:18Z").toEpochMilli());
    }
    
    @Test public void multipleCert() throws IOException, ResolverException {
        final ExplicitKeyTrustEngine engine = (ExplicitKeyTrustEngine) getBean("staticX509Multiple", TrustEngine.class, true, TESTPATH);
        
        final StaticCredentialResolver resolver = (StaticCredentialResolver) engine.getCredentialResolver();
        
        Iterator<Credential> credentials = resolver.resolve(null).iterator();
        
        Assert.assertTrue(credentials.hasNext());
        final X509Credential first = (X509Credential) credentials.next();
        Assert.assertEquals(first.getEntityCertificateChain().size(), 1);

        Assert.assertTrue(credentials.hasNext());
        final X509Credential second = (X509Credential) credentials.next();
        Assert.assertEquals(second.getEntityCertificateChain().size(), 1);
    }

    @Test public void mixed() throws IOException, ResolverException {
        final ExplicitKeyTrustEngine engine = (ExplicitKeyTrustEngine) getBean("staticMixed", TrustEngine.class, true, TESTPATH);
        
        final StaticCredentialResolver resolver = (StaticCredentialResolver) engine.getCredentialResolver();
        
        Iterator<Credential> credentials = resolver.resolve(null).iterator();
        
        Assert.assertTrue(credentials.hasNext());
        final Credential first = (Credential) credentials.next();
        Assert.assertNotNull(first.getPublicKey());
        Assert.assertFalse(first instanceof X509Credential);

        Assert.assertTrue(credentials.hasNext());
        final X509Credential second = (X509Credential) credentials.next();
        Assert.assertEquals(second.getEntityCertificateChain().size(), 1);
        Assert.assertEquals(second.getEntityCertificate().getNotAfter().getTime(), Instant.parse("2024-04-08T13:39:18Z").toEpochMilli());
    }
}