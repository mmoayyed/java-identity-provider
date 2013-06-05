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

import java.util.regex.Pattern;

import junit.framework.Assert;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.annotations.Test;

/**
 * test for {@link AttributeIssuerEntityAttributeRegexMatcher}.
 */
public class AttributeIssuerEntityAttributeRegexMatcherTest  extends BaseMetadataTests {

    private AttributeIssuerEntityAttributeRegexMatcher getMatcher() throws ComponentInitializationException {
        Pattern pattern = Pattern.compile("urn\\:example.org\\:policy\\:12.*");
        return getMatcher("urn:example.org:policies", pattern, null);
    }

    private AttributeIssuerEntityAttributeRegexMatcher getMatcher(String attributeName, Pattern attributeValuePattern,
            String attributeNameFormat) throws ComponentInitializationException {
        AttributeIssuerEntityAttributeRegexMatcher matcher = new AttributeIssuerEntityAttributeRegexMatcher();
        matcher.setId("matcher");
        matcher.setAttributeName(attributeName);
        matcher.setValueRegex(attributeValuePattern);
        matcher.setNameFormat(attributeNameFormat);
        matcher.initialize();
        return matcher;
    }
    
    @Test public void simple() throws AttributeFilterException, ComponentInitializationException {

        AttributeIssuerEntityAttributeRegexMatcher matcher = getMatcher();
        Assert.assertFalse(matcher.matches(metadataContext(jiraEntity, idpEntity, "Principal")));

        Assert.assertTrue(matcher.matches(metadataContext(idpEntity, jiraEntity, "Principal")));

    }

}
