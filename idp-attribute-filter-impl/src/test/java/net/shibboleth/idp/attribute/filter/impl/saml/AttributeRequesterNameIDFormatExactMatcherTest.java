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
 * test for {@link AttributeRequesterNameIDFormatExactMatcher}.
 */
public class AttributeRequesterNameIDFormatExactMatcherTest extends BaseMetadataTests {
    
    private AttributeRequesterNameIDFormatExactMatcher getMatcher(String format) throws ComponentInitializationException {
        AttributeRequesterNameIDFormatExactMatcher matcher = new AttributeRequesterNameIDFormatExactMatcher();
        matcher.setId("matcher");
        matcher.setNameIdFormat(format);
        matcher.initialize();
        return matcher;
    }

    
    @Test public void simple() throws ComponentInitializationException, AttributeFilterException {
        AttributeRequesterNameIDFormatExactMatcher matcher = getMatcher("https://example.org/foo");
        
        Assert.assertEquals(matcher.getNameIdFormat(), "https://example.org/foo");
        
        Assert.assertTrue(matcher.matches(metadataContext(idpEntity, jiraEntity, "Principal")));
        Assert.assertFalse(matcher.matches(metadataContext(null, null, null)));
        Assert.assertFalse(matcher.matches(metadataContext(null, idpEntity, "Principal")));

        matcher = getMatcher("urn:otherstuff");
        Assert.assertFalse(matcher.matches(metadataContext(idpEntity, jiraEntity, "Principal")));
        Assert.assertFalse(matcher.matches(metadataContext(idpEntity, wikiEntity, "Principal")));
    }
    

}
