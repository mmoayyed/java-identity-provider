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

package net.shibboleth.idp.attribute.resolver.impl.ad;

import junit.framework.Assert;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.annotations.Test;

import com.google.common.base.Optional;

/**
 * test for {@link PrincipalNameAttributeDefinition}
 */
public class PrincipalNameAttributeDefinitionTest {

    @Test public void nullContextTest() throws ComponentInitializationException {
        PrincipalNameAttributeDefinition defn;
        defn = new PrincipalNameAttributeDefinition();
        defn.setId("id");
        defn.initialize();

        try {
            defn.doAttributeDefinitionResolve(new AttributeResolutionContext());
        } catch (ResolutionException e) {
            // OK
        }
    }

    @Test
    public void nullNameTest() throws ComponentInitializationException{
        PrincipalNameAttributeDefinition defn;
        defn = new PrincipalNameAttributeDefinition();
        defn.setId("id");
        defn.initialize();
        
        try {
            defn.doAttributeDefinitionResolve(TestSources.createResolutionContext("", "issuer", "recipient"));
        } catch (ResolutionException e) {
            // OK
        }
        
    }

    @Test
    public void normalTest() throws ComponentInitializationException, ResolutionException{
        PrincipalNameAttributeDefinition defn;
        defn = new PrincipalNameAttributeDefinition();
        defn.setId("id");
        defn.initialize();
        
        Optional<Attribute> result = defn.doAttributeDefinitionResolve(TestSources.createResolutionContext("principal", "issuer", "recipient"));
        
        Assert.assertEquals(result.get().getValues().size(), 1);
        
        StringAttributeValue value = (StringAttributeValue) result.get().getValues().iterator().next();
        Assert.assertEquals(value.getValue(), "principal");

        
    }
}
