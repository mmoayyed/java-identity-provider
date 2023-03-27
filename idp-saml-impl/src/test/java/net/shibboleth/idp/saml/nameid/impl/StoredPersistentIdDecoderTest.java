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
import java.sql.SQLException;

import javax.sql.DataSource;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.testing.SAML2ActionTestingSupport;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.idp.profile.testing.RequestContextBuilder;
import net.shibboleth.idp.saml.impl.testing.TestSources;
import net.shibboleth.idp.saml.nameid.NameDecoderException;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.testing.DatabaseTestingSupport;

/** Test for {@link StoredPersistentIdDecoder}. */
@SuppressWarnings("javadoc")
public class StoredPersistentIdDecoderTest extends OpenSAMLInitBaseTestCase {

    private DataSource testSource;
    
    private ProfileRequestContext prc;
    
    private PersistentSAML2NameIDGenerator generator;
    
    private StoredPersistentIdDecoder decoder;
    
    @BeforeClass public void setUpSource() {
        testSource = DatabaseTestingSupport.GetMockDataSource(PersistentSAML2NameIDGeneratorTest.INIT_FILE, "StoredIDDataConnectorStore");
    }
    
    @AfterClass public void teardown() {
        DatabaseTestingSupport.InitializeDataSource(PersistentSAML2NameIDGeneratorTest.DELETE_FILE, testSource);
    }

    @BeforeMethod public void setUp() throws SQLException, IOException, ComponentInitializationException {
        
        final JDBCPairwiseIdStore store = new JDBCPairwiseIdStore();
        store.setDataSource(testSource);
        store.initialize();
        
        generator = new PersistentSAML2NameIDGenerator();
        generator.setId("test");
        generator.setPersistentIdStore(store);
        generator.setAttributeSourceIds(CollectionSupport.singletonList("SOURCE"));
        
        decoder = new StoredPersistentIdDecoder();
        decoder.setId("test");
        decoder.setPersistentIdStore(store);
        decoder.initialize();

        prc = new RequestContextBuilder().setInboundMessageIssuer(TestSources.SP_ENTITY_ID)
                .setOutboundMessageIssuer(TestSources.IDP_ENTITY_ID).buildProfileRequestContext();
    }
    
    @Test
    public void testMissingID() throws Exception {

        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        scc.setRequesterId(TestSources.SP_ENTITY_ID);
        scc.setResponderId(TestSources.IDP_ENTITY_ID);
        
        final Subject subject = SAML2ActionTestingSupport.buildSubject("foo");
        final NameID n = subject.getNameID();
        assert n != null;
        Assert.assertNull(decoder.decode(scc, n));
    }

    @Test(expectedExceptions={NameDecoderException.class})
    public void testNoQualifiers() throws Exception {

        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        
        final Subject subject = SAML2ActionTestingSupport.buildSubject("foo");
        final NameID n = subject.getNameID();
        assert n != null;
        decoder.decode(scc, n);
    }
    
    @Test
    public void testBadQualifier() throws Exception {
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final IdPAttribute source = new IdPAttribute("SOURCE");
        source.setValues(CollectionSupport.singletonList(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)));
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(
                CollectionSupport.singleton(source));
        final NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertNotNull(id.getValue());
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);

        id.setNameQualifier(null);
        id.setSPNameQualifier(null);
        
        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        scc.setRequesterId("Bad");
        scc.setResponderId(TestSources.IDP_ENTITY_ID);
        
        Assert.assertNull(decoder.decode(scc, id));
    }
    
    @Test
    public void testStoredIdDecode() throws Exception {
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final IdPAttribute source = new IdPAttribute("SOURCE");
        source.setValues(CollectionSupport.singletonList(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)));
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(
                CollectionSupport.singleton(source));
        final NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id != null;
        Assert.assertNotNull(id.getValue());
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), TestSources.SP_ENTITY_ID);

        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        scc.setRequesterId(TestSources.SP_ENTITY_ID);
        scc.setResponderId(TestSources.IDP_ENTITY_ID);
        
        final String decoded = decoder.decode(scc, id);
        Assert.assertEquals(decoded, "foo");
    }
    
    @Test
    public void testAffiliation() throws Exception {
        generator.setSPNameQualifier("http://affiliation.org");
        generator.initialize();
        
        prc.ensureSubcontext(SubjectContext.class).setPrincipalName("foo");
        Assert.assertNull(generator.generate(prc, NameID.PERSISTENT));
        
        final IdPAttribute source = new IdPAttribute("SOURCE");
        source.setValues(CollectionSupport.singletonList(new StringAttributeValue(TestSources.COMMON_ATTRIBUTE_VALUE_STRING)));
        final RelyingPartyContext rpc = prc.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        rpc.ensureSubcontext(AttributeContext.class).setUnfilteredIdPAttributes(
                CollectionSupport.singleton(source));
        final NameID id = generator.generate(prc, NameID.PERSISTENT);
        assert id!=null;
        Assert.assertNotNull(id.getValue());
        Assert.assertEquals(id.getFormat(), NameID.PERSISTENT);
        Assert.assertEquals(id.getNameQualifier(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(id.getSPNameQualifier(), "http://affiliation.org");

        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        scc.setRequesterId(TestSources.SP_ENTITY_ID);
        scc.setResponderId(TestSources.IDP_ENTITY_ID);
        
        final String decoded = decoder.decode(scc, id);
        Assert.assertEquals(decoded, "foo");
    }

}