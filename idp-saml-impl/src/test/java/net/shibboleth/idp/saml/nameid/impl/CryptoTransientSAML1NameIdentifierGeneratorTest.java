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

package net.shibboleth.idp.saml.nameid.impl;

import java.io.IOException;
import java.time.Duration;

import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.security.DataSealer;
import net.shibboleth.shared.security.impl.BasicKeystoreKeyStrategy;
import net.shibboleth.shared.spring.resource.ResourceHelper;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** Unit test for {@link TransientSAML1NameIdentifierGenerator} using crypto-based generator. */
@SuppressWarnings("javadoc")
public class CryptoTransientSAML1NameIdentifierGeneratorTest extends OpenSAMLInitBaseTestCase {

    private static final Duration TIMEOUT = Duration.ofMillis(500);
    
    private DataSealer sealer;
    
    private CryptoTransientIdGenerationStrategy transientGenerator;
    
    private TransientSAML1NameIdentifierGenerator generator;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException, IOException {
        final Resource keyStore =
                new ClassPathResource("/net/shibboleth/idp/saml/impl/nameid/SealerKeyStore.jks");
        Assert.assertTrue(keyStore.exists());
        
        final Resource version =
                new ClassPathResource("/net/shibboleth/idp/saml/impl/nameid/SealerKeyStore.kver");
        Assert.assertTrue(version.exists());

        final BasicKeystoreKeyStrategy kstrategy = new BasicKeystoreKeyStrategy();
        kstrategy.setKeyAlias("secret");
        kstrategy.setKeyPassword("kpassword");
        kstrategy.setKeystorePassword("password");
        kstrategy.setKeystoreResource(ResourceHelper.of(keyStore));
        kstrategy.setKeyVersionResource(ResourceHelper.of(version));
        kstrategy.initialize();
        
        sealer = new DataSealer();
        sealer.setKeyStrategy(kstrategy);
        sealer.initialize();
        
        transientGenerator = new CryptoTransientIdGenerationStrategy();
        transientGenerator.setId("test");
        transientGenerator.setDataSealer(sealer);
        transientGenerator.setIdLifetime(TIMEOUT);
        transientGenerator.initialize();
        
        generator = new TransientSAML1NameIdentifierGenerator();
        generator.setId("test");
        generator.setFormat(NameIdentifier.UNSPECIFIED);
        generator.setTransientIdGenerator(transientGenerator);
        generator.initialize();
    }
    
    @AfterMethod public void tearDown() {
        generator.destroy();
        transientGenerator.destroy();
        sealer.destroy();
    }

    @Test public void testNoPrincipal() throws Exception {        

        final ProfileRequestContext prc = new RequestContextBuilder().buildProfileRequestContext();
        
        final NameIdentifier name = generator.generate(prc, generator.getFormat());
        
        Assert.assertNull(name);
    }

    @Test public void testNoRelyingParty() throws Exception {        

        final ProfileRequestContext prc = new RequestContextBuilder().buildProfileRequestContext();
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.setRelyingPartyId(null);
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("jdoe");
        
        final NameIdentifier name = generator.generate(prc, generator.getFormat());
        
        Assert.assertNull(name);
    }
    
    @Test public void testTransient() throws Exception {        

        final ProfileRequestContext prc = new RequestContextBuilder().buildProfileRequestContext();
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("jdoe");
        
        final NameIdentifier name = generator.generate(prc, generator.getFormat());
        
        assert name!=null;
        Assert.assertEquals(name.getFormat(), generator.getFormat());
        final RelyingPartyConfiguration config = rpc.getConfiguration();
        assert config != null;

        Assert.assertEquals(name.getNameQualifier(), config.getIssuer(prc));

        final String val = name.getValue();
        assert val != null;
        final String decode = sealer.unwrap(val);

        Assert.assertEquals(decode, rpc.getRelyingPartyId() + "!" + "jdoe");

        Thread.sleep(TIMEOUT.multipliedBy(2).toMillis());
        try {
            sealer.unwrap(val);
            Assert.fail("Timeout not set correctly");
        } catch (Exception e) {
            // OK
        }
    }
    
}
