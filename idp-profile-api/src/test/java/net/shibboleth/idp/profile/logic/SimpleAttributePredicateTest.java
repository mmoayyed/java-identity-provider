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

package net.shibboleth.idp.profile.logic;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.profile.context.RelyingPartyContext;

import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Unit test for {@link SimpleAttributePredicate}.
 */
@SuppressWarnings("javadoc")
public class SimpleAttributePredicateTest {

    @Test
    public void testString() {
        final ProfileRequestContext prc = createProfileRequestContext("foo", List.of("bar", "baz"));
        SimpleAttributePredicate predicate = new SimpleAttributePredicate();
        
        predicate.setAttributeValueMap(Map.of("foo2", List.of("bar")));
        assertFalse(predicate.test(prc));
        
        predicate.setAttributeValueMap(Map.of("foo2", List.of("*")));
        assertFalse(predicate.test(prc));

        predicate.setAttributeValueMap(Map.of("foo", List.of("*")));
        assertTrue(predicate.test(prc));

        predicate.setAttributeValueMap(Map.of("foo", List.of("bar", "baz")));
        assertTrue(predicate.test(prc));
        
        predicate.setScope("zorkmid");
        assertFalse(predicate.test(prc));
    }
    
    @Test
    public void testScoped() {
        final ProfileRequestContext prc = createProfileRequestContext("foo", List.of("bar@scope1", "baz@scope2"));
        SimpleAttributePredicate predicate = new SimpleAttributePredicate();
        
        predicate.setAttributeValueMap(Map.of("foo", List.of("bar")));
        assertTrue(predicate.test(prc));
        
        predicate.setAttributeValueMap(Map.of("foo", List.of("*")));
        assertTrue(predicate.test(prc));

        predicate.setScope("zorkmid");
        assertFalse(predicate.test(prc));
        
        predicate.setScope("*");
        assertTrue(predicate.test(prc));

        predicate.setAttributeValueMap(Map.of("foo", List.of("bar", "baz")));
        assertTrue(predicate.test(prc));
        
        predicate.setScope("scope1");
        assertTrue(predicate.test(prc));

        predicate.setScope("scope2");
        assertTrue(predicate.test(prc));

        predicate.setAttributeValueMap(Map.of("foo", List.of("bar")));
        assertFalse(predicate.test(prc));
    }
    
    private ProfileRequestContext createProfileRequestContext(final String name, final Collection<String> values) {
        final ProfileRequestContext prc = new ProfileRequestContext();
        final RelyingPartyContext rpc = new RelyingPartyContext();
        final IdPAttribute attribute = new IdPAttribute(name);
        final List<IdPAttributeValue> attributeValues = new ArrayList<>();
        for (final String value : values) {
            final int i = value.indexOf('@');
            if (i == -1) {
                attributeValues.add(new StringAttributeValue(value));
            } else {
                attributeValues.add(new ScopedStringAttributeValue(value.substring(0,i), value.substring(i + 1)));
            }
        }
        attribute.setValues(attributeValues);
        final AttributeContext ac = new AttributeContext();
        ac.setIdPAttributes(Collections.singletonList(attribute));
        ac.setUnfilteredIdPAttributes(Collections.singletonList(attribute));
        rpc.addSubcontext(ac);
        prc.addSubcontext(rpc);
        return prc;
    }
   
}