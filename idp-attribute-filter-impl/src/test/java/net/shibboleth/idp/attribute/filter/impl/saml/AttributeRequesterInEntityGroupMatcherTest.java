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

package net.shibboleth.idp.attribute.filter.impl.saml;

import junit.framework.Assert;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.annotations.Test;

/**
 * test for {@link AttributeRequesterInEntityGroupMatcher}.
 */
public class AttributeRequesterInEntityGroupMatcherTest extends BaseMetadataTests {
    
    private AttributeRequesterInEntityGroupMatcher getMatcher(String group) throws ComponentInitializationException {
        AttributeRequesterInEntityGroupMatcher matcher = new AttributeRequesterInEntityGroupMatcher();
        matcher.setId("matcher");
        matcher.setEntityGroup(group);
        matcher.initialize();
        return matcher;
    }

    
    @Test public void parent() throws ComponentInitializationException, AttributeFilterException {
        AttributeRequesterInEntityGroupMatcher matcher = getMatcher("http://shibboleth.net");
        
        Assert.assertTrue(matcher.matches(metadataContext(idpEntity, jiraEntity, "Principal")));
        Assert.assertFalse(matcher.matches(metadataContext(null, null, null)));

        matcher = getMatcher("urn:otherstuff");
        Assert.assertFalse(matcher.matches(metadataContext(idpEntity, jiraEntity, "Principal")));
    }
    
    @Test public void getter() throws ComponentInitializationException, AttributeFilterException {
        Assert.assertEquals(getMatcher("http://shibboleth.net").getEntityGroup(), "http://shibboleth.net");
    }

    @Test public void noGroup() throws ComponentInitializationException, AttributeFilterException {
        AttributeRequesterInEntityGroupMatcher matcher = new AttributeRequesterInEntityGroupMatcher();
        matcher.setId("matcher");
        Assert.assertFalse(matcher.isEntityInGroup(metadataContext(null, null, null)));
    }
}
