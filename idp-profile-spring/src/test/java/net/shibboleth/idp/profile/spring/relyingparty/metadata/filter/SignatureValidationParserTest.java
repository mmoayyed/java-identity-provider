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

import net.shibboleth.idp.profile.spring.relyingparty.metadata.AbstractMetadataParserTest;
import net.shibboleth.utilities.java.support.resolver.ResolverException;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.Sets;


/**
 * test for parser for SignatureValidation filter
 */
public class SignatureValidationParserTest extends AbstractMetadataParserTest {

    @Test public void correctSig() throws IOException, ResolverException {
        MetadataResolver resolver =
                getBean(MetadataResolver.class, true, "filter/switch-metadata.xml", "filter/switch.xml", "beans.xml");

        final Iterable<EntityDescriptor> result = resolver.resolve(criteriaFor("urn:mace:switch.ch:SWITCHaai:ethz.ch"));
        Assert.assertTrue(result.iterator().hasNext());
        Assert.assertEquals(Sets.newHashSet(result).size(), 1);
    }

    // OSJ-71
    @Test(enabled=false) public void wrongSig() throws IOException, ResolverException {
        MetadataResolver resolver =
                getBean(MetadataResolver.class, true, "filter/switch-metadata.xml", "filter/other.xml", "beans.xml");

        final Iterable<EntityDescriptor> result = resolver.resolve(criteriaFor("urn:mace:switch.ch:SWITCHaai:ethz.ch"));
        Assert.assertFalse(result.iterator().hasNext());
        Assert.assertEquals(Sets.newHashSet(result).size(), 0);
    }
    
    // OSJ-71
    @Test(enabled=false) public void noSigCheck() throws IOException, ResolverException {
        MetadataResolver resolver =
                getBean(MetadataResolver.class, true, "filter/signingCertCheck.xml", "filter/switch.xml", "beans.xml");

        final Iterable<EntityDescriptor> result = resolver.resolve(criteriaFor("https://sp.example.org/sp/shibboleth"));
        Assert.assertFalse(result.iterator().hasNext());
        Assert.assertEquals(Sets.newHashSet(result).size(), 0);
    }

    @Test public void noSigNoCheck() throws IOException, ResolverException {
        MetadataResolver resolver =
                getBean(MetadataResolver.class, true, "filter/signingNoCertCheck.xml", "filter/switch.xml", "beans.xml");

        final Iterable<EntityDescriptor> result = resolver.resolve(criteriaFor("https://sp.example.org/sp/shibboleth"));
        Assert.assertTrue(result.iterator().hasNext());
        Assert.assertEquals(Sets.newHashSet(result).size(), 1);
    }

}
