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

package net.shibboleth.idp.profile.spring.relyingparty.metadata.filter;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataParserTest;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2alg.DigestMethod;
import org.opensaml.saml.ext.saml2alg.SigningMethod;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.metadata.resolver.filter.impl.AlgorithmFilter;
import org.opensaml.saml.saml2.metadata.EncryptionMethod;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.opensaml.saml.saml2.metadata.KeyDescriptor;
import org.opensaml.saml.saml2.metadata.RoleDescriptor;
import org.opensaml.xmlsec.encryption.MGF;
import org.opensaml.xmlsec.encryption.support.EncryptionConstants;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class AlgorithmFilterParserTest extends AbstractMetadataParserTest {
    
    @Test
    public void test() throws ResolverException, IOException {
        doTest("filter/algorithm.xml", "filter/algorithmBeans.xml");
    }

    @Test
    public void testFilterScript() throws ResolverException, IOException {
        doTest("filter/algorithmWithScript.xml");
    }

    @Test
    public void testFilterScriptResource() throws ResolverException, IOException {
        doTest("filter/algorithmWithScriptResource.xml");
    }

    private void doTest(final String... files) throws ResolverException, IOException {

        final MetadataResolver resolver = getBean(MetadataResolver.class, files);

        final AlgorithmFilter filter = (AlgorithmFilter) resolver.getMetadataFilter();
        Assert.assertNotNull(filter);
        
        EntityIdCriterion crit = new EntityIdCriterion("https://sp.example.org/sp/shibboleth");
        EntityDescriptor entity = resolver.resolveSingle(new CriteriaSet(crit));
        validate(entity);

        crit = new EntityIdCriterion("https://sp4.example.org/sp/shibboleth");
        entity = resolver.resolveSingle(new CriteriaSet(crit));
        if (entity != null) {
            validate(entity);
        }

        crit = new EntityIdCriterion("https://sp2.example.org/sp/shibboleth");
        entity = resolver.resolveSingle(new CriteriaSet(crit));
        Assert.assertNotNull(entity);
        final Extensions exts = entity.getExtensions();
        if (exts != null) {
            Assert.assertTrue(exts.getUnknownXMLObjects(DigestMethod.DEFAULT_ELEMENT_NAME).isEmpty());
            Assert.assertTrue(exts.getUnknownXMLObjects(SigningMethod.DEFAULT_ELEMENT_NAME).isEmpty());
        }
    }
    
    private void validate(final EntityDescriptor entity) {
        final Extensions exts = entity.getExtensions();
        Assert.assertNotNull(exts);
        
        List<XMLObject> extElements = exts.getUnknownXMLObjects(DigestMethod.DEFAULT_ELEMENT_NAME);
        assertEquals(extElements.size(), 2);
        
        Iterator<XMLObject> digests = extElements.iterator();
        assertEquals(((DigestMethod) digests.next()).getAlgorithm(), SignatureConstants.ALGO_ID_DIGEST_SHA256);
        assertEquals(((DigestMethod) digests.next()).getAlgorithm(), SignatureConstants.ALGO_ID_DIGEST_SHA512);

        extElements = exts.getUnknownXMLObjects(SigningMethod.DEFAULT_ELEMENT_NAME);
        assertEquals(extElements.size(), 2);
        
        Iterator<XMLObject> signings = extElements.iterator();
        assertEquals(((SigningMethod) signings.next()).getAlgorithm(), SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
        assertEquals(((SigningMethod) signings.next()).getAlgorithm(), SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512);

        for (final RoleDescriptor role : entity.getRoleDescriptors()) {
            for (final KeyDescriptor key : role.getKeyDescriptors()) {
                final List<EncryptionMethod> methods = key.getEncryptionMethods();
                assertEquals(methods.size(), 1);
                assertEquals(methods.get(0).getAlgorithm(), EncryptionConstants.ALGO_ID_KEYTRANSPORT_RSAOAEP11);
                
                final List<XMLObject> encDigests = methods.get(0).getUnknownXMLObjects(
                        org.opensaml.xmlsec.signature.DigestMethod.DEFAULT_ELEMENT_NAME);
                assertEquals(encDigests.size(), 1);
                assertEquals(((org.opensaml.xmlsec.signature.DigestMethod) encDigests.get(0)).getAlgorithm(),
                        SignatureConstants.ALGO_ID_DIGEST_SHA256);

                final List<XMLObject> mgfs = methods.get(0).getUnknownXMLObjects(MGF.DEFAULT_ELEMENT_NAME);
                assertEquals(mgfs.size(), 1);
                assertEquals(((MGF) mgfs.get(0)).getAlgorithm(), EncryptionConstants.ALGO_ID_MGF1_SHA256);
            }
        }        
    }

}