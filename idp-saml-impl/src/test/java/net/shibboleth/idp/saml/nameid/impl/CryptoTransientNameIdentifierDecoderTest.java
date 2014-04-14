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
import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.attribute.resolver.impl.TransientIdAttributeDefinition;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.saml.nameid.NameIDCanonicalizationFlowDescriptor;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.impl.NameIdentifierBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * test for {@link CryptoTransientNameIdentifierDecoder}.
 */
public class CryptoTransientNameIdentifierDecoderTest extends OpenSAMLInitBaseTestCase {

    private final static long TIMEOUT = 5000;

    private DataSealer dataSealer;

    private CryptoTransientNameIdentifierDecoder decoder;

    /**
     * Set up the data sealer. We take advantage of the fact that Spring a {@link ClassPathResource} wraps a files.
     * 
     * @throws IOException
     * @throws DataSealerException
     * @throws ComponentInitializationException
     */
    @BeforeClass public void setupDataSealer() throws IOException, DataSealerException,
            ComponentInitializationException {

        final Resource keyStore =
                new ClassPathResource("/net/shibboleth/idp/saml/impl/attribute/resolver/SealerKeyStore.jks");
        Assert.assertTrue(keyStore.exists());

        final String keyStorePath = keyStore.getFile().getAbsolutePath();

        dataSealer = new DataSealer();
        dataSealer.setCipherKeyAlias("secret");
        dataSealer.setCipherKeyPassword("kpassword");

        dataSealer.setKeystorePassword("password");
        dataSealer.setKeystorePath(keyStorePath);

        dataSealer.initialize();

        decoder = new CryptoTransientNameIdentifierDecoder();
        decoder.setDataSealer(dataSealer);
        decoder.setId("Decoder");
        decoder.initialize();
    }

    @Test public void decode() throws ComponentInitializationException, ResolutionException, DataSealerException,
            InterruptedException, ProfileException {
        final CryptoTransientIdGenerationStrategy strategy = new CryptoTransientIdGenerationStrategy();
        strategy.setDataSealer(dataSealer);
        strategy.setId("strategy");
        strategy.setIdLifetime(TIMEOUT);
        strategy.initialize();

        final TransientIdAttributeDefinition defn = new TransientIdAttributeDefinition(strategy);
        defn.setId("defn");
        defn.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        final IdPAttribute result = defn.resolve(context);

        final Set<IdPAttributeValue<?>> values = result.getValues();
        Assert.assertEquals(values.size(), 1);
        final String code = ((StringAttributeValue) values.iterator().next()).getValue();

        final NameIdentifier nameID = new NameIdentifierBuilder().buildObject();
        nameID.setFormat("https://example.org/");
        nameID.setNameQualifier(TestSources.IDP_ENTITY_ID);
        nameID.setNameIdentifier(code);

        NameIDCanonicalizationFlowDescriptor desc = new NameIDCanonicalizationFlowDescriptor();
        desc.setId("C14NDesc");
        desc.setFormats(Collections.singleton("https://example.org/"));
        desc.initialize();
        
        final NameIdentifierCanonicalization canon = new NameIdentifierCanonicalization();
        canon.setId("test");
        canon.setDecoder(decoder);
        canon.initialize();

        final ProfileRequestContext prc = new ProfileRequestContext<>();
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class, true);
        final Subject subject = new Subject();

        subject.getPrincipals().add(new NameIdentifierPrincipal(nameID));
        scc.setSubject(subject);
        scc.setAttemptedFlow(desc);

        scc.setRequesterId(TestSources.SP_ENTITY_ID);
        scc.setResponderId(TestSources.IDP_ENTITY_ID);

        canon.execute(prc);

        ActionTestingSupport.assertProceedEvent(prc);

        Assert.assertEquals(scc.getPrincipalName(), TestSources.PRINCIPAL_ID);

    }
}
