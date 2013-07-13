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

package net.shibboleth.idp.attribute.filter.impl.policyrule.saml;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.filter.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.RequestedAttribute;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.filter.impl.policyrule.saml.AttributeInMetadataPolicyRule;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Tests for {@link AttributeInMetadataPolicyRule}
 */
public class AttributeInMetadataPolicyRuleTest {

    private Attribute makeAttribute(String id, List<AttributeValue> values) {
        Attribute attr = new Attribute(id);
        attr.setValues(values);
        return attr;
    }

    private AttributeInMetadataPolicyRule makeMatcher(String id, boolean matchIfMetadataSilent, boolean onlyIfRequired)
            throws ComponentInitializationException {
        AttributeInMetadataPolicyRule matcher = new AttributeInMetadataPolicyRule();
        matcher.setMatchIfMetadataSilent(matchIfMetadataSilent);
        matcher.setOnlyIfRequired(onlyIfRequired);
        matcher.setId(id);
        matcher.initialize();
        return matcher;
    }
    
    private AttributeFilterContext makeContext(String attributeId, RequestedAttribute attribute) {
        
        final AttributeFilterContext context = new AttributeFilterContext();
        
        if (null == attributeId) {
            context.setRequestedAttributes(null);
        } else {
            final Multimap<String, RequestedAttribute> multimap = ArrayListMultimap.create();
            multimap.put(attributeId, attribute);
            context.setRequestedAttributes(multimap);
        }
        return context;
    }
    
    private AttributeFilterContext makeContext(RequestedAttribute attribute) {
        
        if (null == attribute) {
            return makeContext(null, null);
        }
        return makeContext(attribute.getId(), attribute);
    }


    @Test public void getters() throws ComponentInitializationException {
        AttributeInMetadataPolicyRule matcher = makeMatcher("test", true, true);
        Assert.assertTrue(matcher.getMatchIfMetadataSilent());
        Assert.assertTrue(matcher.getOnlyIfRequired());

        matcher = makeMatcher("test", false, false);
        Assert.assertFalse(matcher.getMatchIfMetadataSilent());
        Assert.assertFalse(matcher.getOnlyIfRequired());
    }
    
    @Test public void noRequested() throws AttributeFilterException, ComponentInitializationException {

        final Attribute attr =
                makeAttribute("attr", Lists.newArrayList((AttributeValue) DataSources.STRING_VALUE,
                        DataSources.NON_MATCH_STRING_VALUE));

        Set<AttributeValue> result =
                makeMatcher("test", true, true).getMatchingValues(attr, new AttributeFilterContext());

        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(DataSources.STRING_VALUE));
        Assert.assertTrue(result.contains(DataSources.NON_MATCH_STRING_VALUE));

        result = makeMatcher("test", false, true).getMatchingValues(attr, new AttributeFilterContext());
        Assert.assertTrue(result.isEmpty());
    }
    
    @Test public void wrongRequested() throws AttributeFilterException, ComponentInitializationException {

        final Attribute attr =
                makeAttribute("attr", Lists.newArrayList((AttributeValue) DataSources.STRING_VALUE,
                        DataSources.NON_MATCH_STRING_VALUE));
        
        final AttributeInMetadataPolicyRule matcher = makeMatcher("test", true, true);
        Set<AttributeValue> result = matcher.getMatchingValues(attr, makeContext(null));

        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(DataSources.STRING_VALUE));
        Assert.assertTrue(result.contains(DataSources.NON_MATCH_STRING_VALUE));
        
        result = matcher.getMatchingValues(attr, makeContext(new RequestedAttribute("wrongAttr")));
        Assert.assertTrue(result.isEmpty());
    }
    
    @Test public void isRequiredOnly() throws AttributeFilterException, ComponentInitializationException {

        final Attribute attr =
                makeAttribute("attr", Lists.newArrayList((AttributeValue) DataSources.STRING_VALUE,
                        DataSources.NON_MATCH_STRING_VALUE));
        
        RequestedAttribute required = new RequestedAttribute("attr");
        required.setRequired(false);
        
        AttributeFilterContext context = makeContext(required);
        
        Set<AttributeValue> result = makeMatcher("test", false, false).getMatchingValues(attr, context);
    
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(DataSources.STRING_VALUE));
        Assert.assertTrue(result.contains(DataSources.NON_MATCH_STRING_VALUE));

        result = makeMatcher("test", false, true).getMatchingValues(attr, context);
        Assert.assertTrue(result.isEmpty());
    }
    
    @Test public void values() throws AttributeFilterException, ComponentInitializationException {
    
        final Attribute attr =
                makeAttribute("attr", Lists.newArrayList((AttributeValue) DataSources.STRING_VALUE,
                        DataSources.NON_MATCH_STRING_VALUE));
        
        RequestedAttribute required = new RequestedAttribute("attr");
        required.setRequired(true);
        required.setValues(Collections.singleton((AttributeValue)DataSources.STRING_VALUE));
        
        AttributeFilterContext context = makeContext(required);
        
        Set<AttributeValue> result = makeMatcher("test", false, true).getMatchingValues(attr, context);
        Assert.assertEquals(result.size(), 1);
        Assert.assertTrue(result.contains(DataSources.STRING_VALUE));
    }
    
    @Test public void valuesButNoConvert() throws AttributeFilterException, ComponentInitializationException {
        
        final Attribute attr =
                makeAttribute("attr", Lists.newArrayList((AttributeValue) DataSources.STRING_VALUE,
                        DataSources.NON_MATCH_STRING_VALUE));
        
        AttributeFilterContext context = makeContext("attr", null);
        
        Set<AttributeValue> result = makeMatcher("test", false, true).getMatchingValues(attr, context);
        Assert.assertTrue(result.isEmpty());
    }

    @Test public void multiValues() throws AttributeFilterException, ComponentInitializationException {
        
        final Attribute attr =
                makeAttribute("attr", Lists.newArrayList((AttributeValue) DataSources.STRING_VALUE,
                        DataSources.NON_MATCH_STRING_VALUE));
        
        RequestedAttribute req1 = new RequestedAttribute("attr");
        req1.setRequired(true);
        req1.setValues(Collections.singleton((AttributeValue)DataSources.STRING_VALUE));
        
        RequestedAttribute req2 = new RequestedAttribute("attr");
        req2.setRequired(true);
        req2.setValues(Collections.singleton((AttributeValue)DataSources.NON_MATCH_STRING_VALUE));
        
        final AttributeFilterContext context = new AttributeFilterContext();
        
        final Multimap<String, RequestedAttribute> multimap = ArrayListMultimap.create();
        multimap.put(req1.getId(), req1);
        multimap.put(req2.getId(), req2);
        context.setRequestedAttributes(multimap);
        
        Set<AttributeValue> result = makeMatcher("test", false, true).getMatchingValues(attr, context);
        Assert.assertEquals(result.size(), 2);
        Assert.assertTrue(result.contains(DataSources.STRING_VALUE));
        Assert.assertTrue(result.contains(DataSources.NON_MATCH_STRING_VALUE));
    }

}
