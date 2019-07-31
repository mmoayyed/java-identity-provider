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
import java.time.Instant;
import java.util.Collections;

import javax.security.auth.Subject;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.RequestContextBuilder;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.idp.saml.nameid.NameIDCanonicalizationFlowDescriptor;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;
import net.shibboleth.utilities.java.support.security.impl.BasicKeystoreKeyStrategy;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * test for {@link CryptoTransientNameIDDecoder}.
 */
public class CryptoTransientNameIDDecoderTest extends OpenSAMLInitBaseTestCase {

    private final static Duration TIMEOUT = Duration.ofSeconds(5);
    
    private final static String PRINCIPAL = "ThePrincipal";

    private final static String ISSUER = "https://idp.example.org/issuer";

    private final static String RECIPIENT = "https://sp.example.org/recipient";

    private DataSealer dataSealer;

    private CryptoTransientNameIDDecoder decoder;
    
    /**
     * Set up the data sealer. We take advantage of the fact that Spring a {@link ClassPathResource} wraps a files.
     * 
     * @throws IOException
     * @throws DataSealerException
     * @throws ComponentInitializationException
     */
    @BeforeClass public void setupDataSealer() throws IOException, DataSealerException, ComponentInitializationException {

        final Resource keyStore =
                new ClassPathResource("/net/shibboleth/idp/saml/impl/attribute/resolver/SealerKeyStore.jks");
        Assert.assertTrue(keyStore.exists());
        
        final Resource version =
                new ClassPathResource("/net/shibboleth/idp/saml/impl/attribute/resolver/SealerKeyStore.kver");
        Assert.assertTrue(version.exists());

        final BasicKeystoreKeyStrategy kstrategy = new BasicKeystoreKeyStrategy();
        kstrategy.setKeyAlias("secret");
        kstrategy.setKeyPassword("kpassword");
        kstrategy.setKeystorePassword("password");
        kstrategy.setKeystoreResource(ResourceHelper.of(keyStore));
        kstrategy.setKeyVersionResource(ResourceHelper.of(version));
        kstrategy.initialize();
        
        dataSealer = new DataSealer();
        dataSealer.setKeyStrategy(kstrategy);
        dataSealer.initialize();

        decoder = new CryptoTransientNameIDDecoder();
        decoder.setDataSealer(dataSealer);
        decoder.setId("Decoder");
        decoder.initialize();
    }
    
    private String code(final String principalName, final String attributeRecipientID, final Duration timeout)
            throws DataSealerException {
        final String principalTokenId =
                new StringBuilder().append(attributeRecipientID).append("!").append(principalName).toString();
        return dataSealer.wrap(principalTokenId, Instant.now().plus(timeout));
    }

    private String code(final String principalName, final String attributeIssuerID, final String attributeRecipientID)
            throws DataSealerException {
        return code(principalName, attributeRecipientID, TIMEOUT);
    }

    @Test public void testSucess() throws Exception {
        final String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        Assert.assertEquals(decoder.decode(ct, RECIPIENT), PRINCIPAL);
    }

    @Test(expectedExceptions = NameDecoderException.class)
    public void timeout()
            throws SubjectCanonicalizationException, DataSealerException, NameDecoderException {
        final String ct = code(PRINCIPAL, RECIPIENT, Duration.ofMillis(-5));

        decoder.decode(ct, RECIPIENT);
    }

    @Test(expectedExceptions = NameDecoderException.class)
    public void baddata() throws DataSealerException, NameDecoderException {
        final String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        decoder.decode(ct.toUpperCase(), RECIPIENT);
    }

    @Test public void baddata2() throws DataSealerException, NameDecoderException {

        final String principalTokenId =
                new StringBuilder().append(ISSUER).append("!").append(RECIPIENT).append("+").append(PRINCIPAL)
                        .toString();
        final String ct = dataSealer.wrap(principalTokenId, Instant.now().plus(TIMEOUT));

        Assert.assertNull(decoder.decode(ct, RECIPIENT));
    }

    @Test public void badSP() throws DataSealerException, NameDecoderException {
        final String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        Assert.assertNull(decoder.decode(ct, "my" + RECIPIENT));
    }

    @Test public void decode() throws Exception {
        
        final CryptoTransientIdGenerationStrategy strategy = new CryptoTransientIdGenerationStrategy();
        strategy.setDataSealer(dataSealer);
        strategy.setId("strategy");
        strategy.setIdLifetime(TIMEOUT);
        strategy.initialize();

        final TransientSAML2NameIDGenerator generator = new TransientSAML2NameIDGenerator();
        generator.setId("id");
        generator.setTransientIdGenerator(strategy);
        generator.initialize();    
    
        ProfileRequestContext prc =
                new RequestContextBuilder().setInboundMessageIssuer(TestSources.SP_ENTITY_ID).buildProfileRequestContext();
        prc.getSubcontext(SubjectContext.class, true).setPrincipalName(TestSources.PRINCIPAL_ID);
        
        final NameID nameID = generator.generate(prc, generator.getFormat());

        final NameIDCanonicalizationFlowDescriptor desc = new NameIDCanonicalizationFlowDescriptor();
        desc.setId("C14NDesc");
        desc.setFormats(Collections.singleton(generator.getFormat()));
        desc.initialize();
        
        final NameIDCanonicalization canon = new NameIDCanonicalization();
        canon.setDecoder(decoder);
        canon.initialize();

        prc = new ProfileRequestContext();
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class, true);
        final Subject subject = new Subject();

        subject.getPrincipals().add(new NameIDPrincipal(nameID));
        scc.setSubject(subject);
        scc.setAttemptedFlow(desc);

        scc.setRequesterId(TestSources.SP_ENTITY_ID);
        scc.setResponderId(TestSources.IDP_ENTITY_ID);

        canon.execute(prc);

        ActionTestingSupport.assertProceedEvent(prc);

        Assert.assertEquals(scc.getPrincipalName(), TestSources.PRINCIPAL_ID);

    }

}