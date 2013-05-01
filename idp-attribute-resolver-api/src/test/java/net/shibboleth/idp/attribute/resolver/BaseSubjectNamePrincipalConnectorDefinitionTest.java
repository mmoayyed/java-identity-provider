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

package net.shibboleth.idp.attribute.resolver;

import java.util.Arrays;
import java.util.HashSet;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.opensaml.saml.common.binding.SAMLMessageContext;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.opensaml.saml.saml1.core.impl.NameIdentifierBuilder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.impl.AssertionBuilder;
import org.opensaml.saml.saml2.core.impl.NameIDBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * test for {@link BaseSubjectNamePrincipalConnectorDefinition}
 */
public class BaseSubjectNamePrincipalConnectorDefinitionTest extends OpenSAMLInitBaseTestCase  {

    private final static String ID =  "SubjectNamePrincipalConnector";
    
    private final static String FORMAT = "FORMAT";
    
    private final static String ISSUER = "https://example.org/issuer";
    
    private final static String RELYING_PARTY = "https://example.org/RelyingParty";
    
    private final static String IDENTIFIER = "TestInsideTheNameID";

    @Test public void setterGetters() throws ComponentInitializationException {
        
        BaseSubjectNamePrincipalConnectorDefinition defn = new MockSubjectNamePrincipalConnector();
        
        defn.setId(ID);
        try {
            defn.initialize();
            Assert.fail("format missing");
        } catch (ComponentInitializationException e) {
            // OK
        }
        
        defn.setFormat(FORMAT);
        try {
            defn.initialize();
            Assert.fail("strategy missing");
        } catch (ComponentInitializationException e) {
            // OK
        }
        defn.setContextFinderStrategy(new SamlContextFinder(null)); 
        defn.initialize();
        
        Assert.assertTrue(defn.getRelyingParties().isEmpty());
        Assert.assertEquals(defn.getFormat(), FORMAT);
        
        defn = new MockSubjectNamePrincipalConnector();
        defn.setId(ID);
        defn.setFormat(FORMAT);
        defn.setContextFinderStrategy(new SamlContextFinder(null)); 
        defn.setRelyingParties(new HashSet(Arrays.asList(ISSUER, RELYING_PARTY)));
        defn.initialize();
        Assert.assertEquals(defn.getRelyingParties().size(), 2);
        Assert.assertTrue(defn.getRelyingParties().contains(ISSUER));
        Assert.assertTrue(defn.getRelyingParties().contains(RELYING_PARTY));
    }
    
    @Test public void navigationSAML1() throws ResolutionException, ComponentInitializationException {
        
        final NameIdentifier nameId = new NameIdentifierBuilder().buildObject();
        nameId.setFormat(FORMAT);
        nameId.setNameIdentifier(IDENTIFIER);
        final SAMLMessageContext context = new MockMessageContext(ISSUER, nameId);
        
        final BaseSubjectNamePrincipalConnectorDefinition defn = new MockSubjectNamePrincipalConnector();
        defn.setContextFinderStrategy(new SamlContextFinder(context)); 
        defn.setId(ID);
        defn.setFormat(FORMAT);
        defn.initialize();

        Assert.assertEquals(defn.contentOf(null), IDENTIFIER);
        Assert.assertEquals(defn.formatOf(null), FORMAT);
        Assert.assertEquals(defn.issuerIdOf(null), ISSUER);
    }
        
    @Test public void navigationSAML2() throws ResolutionException, ComponentInitializationException {

        final NameID nameId = new NameIDBuilder().buildObject();
        nameId.setFormat(FORMAT);
        nameId.setValue(IDENTIFIER);
        final SAMLMessageContext context = new MockMessageContext(ISSUER, nameId);
        
        final BaseSubjectNamePrincipalConnectorDefinition defn = new MockSubjectNamePrincipalConnector();
        defn.setContextFinderStrategy(new SamlContextFinder(context)); 
        defn.setId(ID);
        defn.setFormat(FORMAT);
        defn.initialize();

        Assert.assertEquals(defn.contentOf(null), IDENTIFIER);
        Assert.assertEquals(defn.formatOf(null), FORMAT);
        Assert.assertEquals(defn.issuerIdOf(null), ISSUER);
    }
    
    @Test public void badNavigation() throws ResolutionException, ComponentInitializationException {

        final Assertion entity = new AssertionBuilder().buildObject();
        final SAMLMessageContext context = new MockMessageContext(ISSUER, entity);
        
        BaseSubjectNamePrincipalConnectorDefinition defn = new MockSubjectNamePrincipalConnector();
        defn.setContextFinderStrategy(new SamlContextFinder(context)); 
        defn.setId(ID);
        defn.setFormat(FORMAT);
        defn.initialize();

        try {
            defn.contentOf(null);
            Assert.fail("Bad nameID Type");
        } catch (ResolutionException e) {
            // OK
        }
        try {
            defn.formatOf(null);
            Assert.fail("Bad nameID Type");
        } catch (ResolutionException e) {
            // OK
        }
        Assert.assertEquals(defn.issuerIdOf(null), ISSUER);

        defn = new MockSubjectNamePrincipalConnector();
        defn.setContextFinderStrategy(new SamlContextFinder(null)); 
        defn.setId(ID);
        defn.setFormat(FORMAT);
        defn.initialize(); 
        try {
            defn.issuerIdOf(null);
            Assert.fail("Missing Context");
        } catch (ResolutionException e) {
            // OK
        }
    }

}
