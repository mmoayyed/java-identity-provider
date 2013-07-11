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

import junit.framework.Assert;
import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.impl.policyrule.saml.AttributeIssuerEntityAttributeExactPolicyRule;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.testng.annotations.Test;

/**
 * test for {@link AttributeIssuerEntityAttributeExactPolicyRule}.
 */
public class AttributeIssuerEntityAttributeExactPolicyRuleTest  extends BaseMetadataTests {

    private AttributeIssuerEntityAttributeExactPolicyRule getMatcher() throws ComponentInitializationException {
        return getMatcher("urn:example.org:policies", "urn:example.org:policy:1234", null);
    }

    private AttributeIssuerEntityAttributeExactPolicyRule getMatcher(String attributeName, String attributeValue,
            String attributeNameFormat) throws ComponentInitializationException {
        AttributeIssuerEntityAttributeExactPolicyRule matcher = new AttributeIssuerEntityAttributeExactPolicyRule();
        matcher.setId("matcher");
        matcher.setAttributeName(attributeName);
        matcher.setValue(attributeValue);
        matcher.setNameFormat(attributeNameFormat);
        matcher.initialize();
        return matcher;
    }
    
    @Test public void simple() throws AttributeFilterException, ComponentInitializationException {

        AttributeIssuerEntityAttributeExactPolicyRule matcher = getMatcher();
        Assert.assertEquals(matcher.matches(metadataContext(jiraEntity, idpEntity, "Principal")), Tristate.FALSE);

        Assert.assertEquals(matcher.matches(metadataContext(idpEntity, jiraEntity, "Principal")), Tristate.TRUE);

    }

}
