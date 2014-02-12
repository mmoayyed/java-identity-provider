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

package net.shibboleth.idp.saml.impl.nameid;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.authn.SubjectCanonicalizationException;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.idp.saml.impl.TestSources;
import net.shibboleth.idp.saml.impl.attribute.resolver.CryptoTransientIdAttributeDefinition;
import net.shibboleth.idp.saml.impl.nameid.NameIDCanonicalization.ActivationCondition;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.DataSealerException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ActionTestingSupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * test for {@link CryptoTransientNameIDDecoder}.
 */
public class CryptoTransientNameIDDecoderTest extends OpenSAMLInitBaseTestCase {

    private final static long TIMEOUT = 5000;

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

        decoder = new CryptoTransientNameIDDecoder();
        decoder.setDataSealer(dataSealer);
        decoder.setId("Decoder");
        decoder.initialize();
    }

    private String code(String principalName, String attributeIssuerID, String attributeRecipientID, long timeout)
            throws DataSealerException {
        final String principalTokenId =
                new StringBuilder().append(attributeIssuerID).append("!").append(attributeRecipientID).append("!")
                        .append(principalName).toString();
        return dataSealer.wrap(principalTokenId, System.currentTimeMillis() + timeout);
    }

    private String code(String principalName, String attributeIssuerID, String attributeRecipientID)
            throws DataSealerException {
        return code(principalName, attributeIssuerID, attributeRecipientID, TIMEOUT);
    }

    @Test public void testSucess() throws ProfileException, ComponentInitializationException, IOException,
            DataSealerException {
        String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        Assert.assertEquals(decoder.decode(ct, ISSUER, RECIPIENT), PRINCIPAL);
    }

    @Test(expectedExceptions = {NameDecoderException.class,}) public void timeout()
            throws SubjectCanonicalizationException, DataSealerException, NameDecoderException {
        String ct = code(PRINCIPAL, ISSUER, RECIPIENT, -10);

        decoder.decode(ct, ISSUER, RECIPIENT);
    }

    @Test(expectedExceptions = {NameDecoderException.class,}) public void baddata()
            throws SubjectCanonicalizationException, DataSealerException, NameDecoderException {
        String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        decoder.decode(ct.toUpperCase(), ISSUER, RECIPIENT);
    }

    @Test(expectedExceptions = {SubjectCanonicalizationException.class,}) public void baddata2()
            throws SubjectCanonicalizationException, DataSealerException, NameDecoderException {

        final String principalTokenId =
                new StringBuilder().append(ISSUER).append("!").append(RECIPIENT).append("+").append(PRINCIPAL)
                        .toString();
        String ct = dataSealer.wrap(principalTokenId, System.currentTimeMillis() + TIMEOUT);

        decoder.decode(ct, ISSUER, RECIPIENT);
    }

    @Test(expectedExceptions = {SubjectCanonicalizationException.class,}) public void badSP()
            throws SubjectCanonicalizationException, DataSealerException, NameDecoderException {
        String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        decoder.decode(ct, ISSUER, "my" + RECIPIENT);
    }

    @Test(expectedExceptions = {SubjectCanonicalizationException.class,}) public void badIdP()
            throws SubjectCanonicalizationException, DataSealerException, NameDecoderException {
        String ct = code(PRINCIPAL, ISSUER, RECIPIENT);

        decoder.decode(ct, "my" + ISSUER, RECIPIENT);
    }

    @Test public void decode() throws ComponentInitializationException, ResolutionException, DataSealerException,
            InterruptedException, ProfileException {
        final CryptoTransientIdAttributeDefinition defn = new CryptoTransientIdAttributeDefinition();
        defn.setId("defn");
        defn.setDataSealer(dataSealer);
        defn.setIdLifetime(TIMEOUT);
        defn.initialize();

        final AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        final IdPAttribute result = defn.resolve(context);

        final Set<IdPAttributeValue<?>> values = result.getValues();
        Assert.assertEquals(values.size(), 1);
        final String code = ((StringAttributeValue) values.iterator().next()).getValue();

        final NameID nameID = new NameIDBuilder().buildObject();
        nameID.setFormat("https://example.org/");
        nameID.setNameQualifier(TestSources.IDP_ENTITY_ID);
        nameID.setSPNameQualifier(TestSources.SP_ENTITY_ID);
        nameID.setValue(code);

        ActivationCondition condition = new ActivationCondition();
        condition.setFormats(Collections.singleton("https://example.org/"));
        
        final NameIDCanonicalization canon = new NameIDCanonicalization(condition);
        canon.setId("test");
        canon.setDecoder(decoder);
        canon.initialize();

        final ProfileRequestContext prc = new ProfileRequestContext<>();
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class, true);
        final Subject subject = new Subject();

        subject.getPrincipals().add(new NameIDPrincipal(nameID));
        scc.setSubject(subject);

        scc.setRequesterId(TestSources.SP_ENTITY_ID);
        scc.setResponderId(TestSources.IDP_ENTITY_ID);

        canon.execute(prc);

        ActionTestingSupport.assertProceedEvent(prc);

        Assert.assertEquals(scc.getPrincipalName(), TestSources.PRINCIPAL_ID);

    }
}
