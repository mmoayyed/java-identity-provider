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

package net.shibboleth.idp.attribute.resolver.impl.dc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBaseTestCase;
import org.opensaml.saml.ext.saml2mdattr.EntityAttributes;
import org.opensaml.saml.saml2.common.Extensions;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.google.common.base.Function;
import com.google.common.base.Optional;

/**
 * Tests for {@link StringSamlAttributeDataConnector}
 */
public class StringSamlAttributeDataConnectorTest extends XMLObjectBaseTestCase {

    private static final String CONNECTOR_ID = "StringSAML";
    private static final String MULTINAME_ID = "MultiName";
    private static final String SAMLNAME_ID = "SamlName";
    private static final HashSet<String> result = new HashSet(Arrays.asList("turquoise", "pink"));
    private FromEntityFunction strategy;

    @BeforeClass public void setitup()
    {
        
        EntityDescriptor entity = unmarshallElement("/data/net/shibboleth/idp/attribute/resolver/impl/dc/entity.xml");
        strategy = new FromEntityFunction(entity);
    }
            
    
    @Test public void testStringEntityAttributes() throws ComponentInitializationException, AttributeResolutionException {
        StringSamlAttributeDataConnector connector = new StringSamlAttributeDataConnector();

        connector.setAttributesStrategy(strategy);
        connector.setId(CONNECTOR_ID);
        connector.setSourceAttributeId(MULTINAME_ID);

        connector.initialize();
        
        Optional<Map<String, Attribute>> res = connector.doResolve(null);

        Assert.assertTrue(res.isPresent());
        Attribute att = res.get().get(CONNECTOR_ID);
        
        Assert.assertNotNull(att);
        
        for (AttributeValue val : att.getValues()) {
            Assert.assertTrue(val instanceof StringAttributeValue);
            StringAttributeValue sval = (StringAttributeValue) val;
            Assert.assertTrue(result.contains(sval.getValue()));
        }
    }
        
    @Test public void testNonStringEntityAttributes() throws ComponentInitializationException, AttributeResolutionException {
            StringSamlAttributeDataConnector connector = new StringSamlAttributeDataConnector();
        connector = new StringSamlAttributeDataConnector();

        connector.setAttributesStrategy(strategy);
        connector.setId(CONNECTOR_ID);
        connector.setSourceAttributeId(SAMLNAME_ID);

        connector.initialize();
        
        Optional<Map<String, Attribute>> res = connector.doResolve(null);

        Assert.assertFalse(res.isPresent());
    }

    /**
    *
    */
    private class FromEntityFunction implements Function<AttributeResolutionContext, List<org.opensaml.saml.saml2.core.Attribute>> {

        final EntityDescriptor entity;

        public FromEntityFunction(EntityDescriptor theEntity) {
            entity = theEntity;
        }

        /** {@inheritDoc} */
        @Nullable
        public List<org.opensaml.saml.saml2.core.Attribute> apply(@Nullable AttributeResolutionContext input) {
            Extensions extensions = entity.getExtensions();
            if (null == extensions) {
                return  null;
            }
            List<XMLObject> kids = extensions.getOrderedChildren();
            
            if (null == kids) {
                return null;
            }
            
            for (XMLObject child: kids) {
                if (child instanceof EntityAttributes) {
                    EntityAttributes entityAttributes = (EntityAttributes) child;
                    
                    return entityAttributes.getAttributes();
                }
            }
            return null;
        }

    }

}
