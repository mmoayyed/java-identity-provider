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

import java.io.IOException;
import java.util.Collection;

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataParserTest;
import net.shibboleth.utilities.java.support.resolver.CriteriaSet;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.core.criterion.EntityIdCriterion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml.saml2.metadata.Extensions;
import org.testng.Assert;
import org.testng.annotations.Test;

@SuppressWarnings("javadoc")
public class ByReferenceFilterParserTest extends AbstractMetadataParserTest {
    
    @Test
    public void test() throws ResolverException, IOException {
        doTest("filter/entityAttributesMetadataOnly.xml", "filter/entityAttributesByRef.xml", "filter/entityAttributesByRefBeans.xml");
    }

    private void doTest(final String... files) throws ResolverException, IOException {

        final MetadataResolver resolver = getBean(MetadataResolver.class, files);
        
        EntityIdCriterion key = new EntityIdCriterion("https://sp.example.org/sp/shibboleth");
        EntityDescriptor entity = resolver.resolveSingle(new CriteriaSet(key));
        Assert.assertNotNull(entity);

        Extensions exts = entity.getExtensions();
        Assert.assertNotNull(exts);
        Collection<XMLObject> extElements = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
        Assert.assertFalse(extElements.isEmpty());
        EntityAttributes extTags = (EntityAttributes) extElements.iterator().next();
        Assert.assertNotNull(extTags);
        Assert.assertEquals(extTags.getAttributes().size(), 2);
        Assert.assertEquals(extTags.getAttributes().get(0).getName(), "foo");
        Assert.assertEquals(extTags.getAttributes().get(1).getName(), "bar");

        key = new EntityIdCriterion("https://sp2.example.org/sp/shibboleth");
        entity = resolver.resolveSingle(new CriteriaSet(key));
        Assert.assertNotNull(entity);
        exts = entity.getExtensions();
        if (exts != null) {
            extElements = exts.getUnknownXMLObjects(EntityAttributes.DEFAULT_ELEMENT_NAME);
            if (!extElements.isEmpty()) {
                extTags = (EntityAttributes) extElements.iterator().next();
                if (extTags != null) {
                    Assert.assertTrue(extTags.getAttributes().isEmpty());
                }
            }
        }
    }
}