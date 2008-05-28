/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.AnyMatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.AttributeValueStringMatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.OrMatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttributeValue;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;

/** Unit test for {@link ShibbolethAttributeFilteringEngine}. */
public class ShibbolethAttributeFilteringEngineTest extends TestCase {

    private Map<String, BaseAttribute> attributes;

    private BaseSAMLProfileRequestContext requestContext;

    /** {@inheritDoc} */
    protected void setUp() throws Exception {
        super.setUp();

        BasicAttribute<String> eduPersonAffiliation = new BasicAttribute<String>();
        eduPersonAffiliation.setId("eduPersonAffiliation");
        eduPersonAffiliation.getValues().add("staff");
        eduPersonAffiliation.getValues().add("employee");
        eduPersonAffiliation.getValues().add("part-time-student");
        eduPersonAffiliation.getValues().add("part-time-staff");

        BasicAttribute<ScopedAttributeValue> eduPersonScopedAffiliation = new BasicAttribute<ScopedAttributeValue>();
        eduPersonScopedAffiliation.setId("eduPersonScopedAffiliation");
        eduPersonScopedAffiliation.getValues().add(new ScopedAttributeValue("staff", "example.org"));
        eduPersonScopedAffiliation.getValues().add(new ScopedAttributeValue("employee", "example.org"));
        eduPersonScopedAffiliation.getValues().add(new ScopedAttributeValue("part-time-staff", "example.org"));
        eduPersonScopedAffiliation.getValues().add(new ScopedAttributeValue("part-time-student", "example.org"));

        attributes = new HashMap<String, BaseAttribute>();
        attributes.put(eduPersonAffiliation.getId(), eduPersonAffiliation);
        attributes.put(eduPersonScopedAffiliation.getId(), eduPersonScopedAffiliation);

        requestContext = new BaseSAMLProfileRequestContext();
        requestContext.setPrincipalName("jsmith");
    }

    /** Tests filtering based on a single simple policy. */
    public void testBasicFilterPolicy() throws Exception {
        // Set up the policy for this test
        AttributeValueStringMatchFunctor staffRule = new AttributeValueStringMatchFunctor();
        staffRule.setMatchString("staff");

        AttributeValueStringMatchFunctor employeeRule = new AttributeValueStringMatchFunctor();
        employeeRule.setMatchString("employee");

        ArrayList<MatchFunctor> allowedValues = new ArrayList<MatchFunctor>();
        allowedValues.add(staffRule);
        allowedValues.add(employeeRule);
        MatchFunctor allowedAffiliationValues = new OrMatchFunctor(allowedValues);

        AttributeRule ePARule = new AttributeRule("eduPersonAffiliation");
        ePARule.setPermitValueRule(allowedAffiliationValues);

        AttributeRule ePSARule = new AttributeRule("eduPersonScopedAffiliation");
        ePSARule.setPermitValueRule(allowedAffiliationValues);

        AttributeFilterPolicy afp = new AttributeFilterPolicy("afptest");
        afp.setPolicyRequirementRule(new AnyMatchFunctor());
        afp.getAttributeRules().add(ePARule);
        afp.getAttributeRules().add(ePSARule);

        // Now run the actual test
        ShibbolethAttributeFilteringEngine filterEngine = new ShibbolethAttributeFilteringEngine();
        filterEngine.getFilterPolicies().add(afp);

        Map<String, BaseAttribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        BaseAttribute eduPersonAffiliation = filteredAttributes.get("eduPersonAffiliation");
        assertNotNull(eduPersonAffiliation);
        assertEquals(2, eduPersonAffiliation.getValues().size());
        assertTrue(eduPersonAffiliation.getValues().contains("staff"));
        assertTrue(eduPersonAffiliation.getValues().contains("employee"));

        BaseAttribute eduPersonScopedAffiliation = filteredAttributes.get("eduPersonAffiliation");
        assertNotNull(eduPersonScopedAffiliation);
        assertEquals(2, eduPersonScopedAffiliation.getValues().size());
        assertTrue(eduPersonScopedAffiliation.getValues().contains("staff"));
        assertTrue(eduPersonScopedAffiliation.getValues().contains("employee"));
    }

    /** Tests filtering based on a two policy, each of which releases one a value for each of the test attributes. */
    public void testMultiplePolicies() throws Exception {
        // Set up the policy for this test
        AttributeValueStringMatchFunctor staffAllow = new AttributeValueStringMatchFunctor();
        staffAllow.setMatchString("staff");

        AttributeValueStringMatchFunctor employeeAllow = new AttributeValueStringMatchFunctor();
        employeeAllow.setMatchString("employee");

        // First policy
        AttributeFilterPolicy afp1 = new AttributeFilterPolicy("afp1");
        afp1.setPolicyRequirementRule(new AnyMatchFunctor());
        AttributeRule ePARule1 = new AttributeRule("eduPersonAffiliation");
        ePARule1.setPermitValueRule(staffAllow);
        afp1.getAttributeRules().add(ePARule1);
        AttributeRule ePSARule1 = new AttributeRule("eduPersonScopedAffiliation");
        ePSARule1.setPermitValueRule(staffAllow);
        afp1.getAttributeRules().add(ePSARule1);

        // Second policy
        AttributeFilterPolicy afp2 = new AttributeFilterPolicy("afp2");
        afp2.setPolicyRequirementRule(new AnyMatchFunctor());
        AttributeRule ePARule2 = new AttributeRule("eduPersonAffiliation");
        ePARule2.setPermitValueRule(employeeAllow);
        afp2.getAttributeRules().add(ePARule2);
        AttributeRule ePSARule2 = new AttributeRule("eduPersonScopedAffiliation");
        ePSARule2.setPermitValueRule(employeeAllow);
        afp2.getAttributeRules().add(ePSARule2);

        // Now run the actual test
        ShibbolethAttributeFilteringEngine filterEngine = new ShibbolethAttributeFilteringEngine();
        filterEngine.getFilterPolicies().add(afp1);
        filterEngine.getFilterPolicies().add(afp2);

        Map<String, BaseAttribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        BaseAttribute eduPersonAffiliation = filteredAttributes.get("eduPersonAffiliation");
        assertNotNull(eduPersonAffiliation);
        assertEquals(2, eduPersonAffiliation.getValues().size());
        assertTrue(eduPersonAffiliation.getValues().contains("staff"));
        assertTrue(eduPersonAffiliation.getValues().contains("employee"));

        BaseAttribute eduPersonScopedAffiliation = filteredAttributes.get("eduPersonAffiliation");
        assertNotNull(eduPersonScopedAffiliation);
        assertEquals(2, eduPersonScopedAffiliation.getValues().size());
        assertTrue(eduPersonScopedAffiliation.getValues().contains("staff"));
        assertTrue(eduPersonScopedAffiliation.getValues().contains("employee"));
    }

    /** Test running a policy with a deny value rule. */
    public void testDenyRules() throws Exception {
        // Set up the policy for this test
        AttributeValueStringMatchFunctor staffAllow = new AttributeValueStringMatchFunctor();
        staffAllow.setMatchString("staff");

        AttributeValueStringMatchFunctor employeeAllow = new AttributeValueStringMatchFunctor();
        employeeAllow.setMatchString("employee");

        ArrayList<MatchFunctor> allowedValues = new ArrayList<MatchFunctor>();
        allowedValues.add(staffAllow);
        allowedValues.add(employeeAllow);
        MatchFunctor allowedAffiliationValues = new OrMatchFunctor(allowedValues);

        // First policy
        AttributeFilterPolicy afp1 = new AttributeFilterPolicy("afp1");
        afp1.setPolicyRequirementRule(new AnyMatchFunctor());
        AttributeRule ePARule1 = new AttributeRule("eduPersonAffiliation");
        ePARule1.setPermitValueRule(allowedAffiliationValues);
        afp1.getAttributeRules().add(ePARule1);
        AttributeRule ePSARule1 = new AttributeRule("eduPersonScopedAffiliation");
        ePSARule1.setPermitValueRule(allowedAffiliationValues);
        afp1.getAttributeRules().add(ePSARule1);

        // Second policy
        AttributeFilterPolicy afp2 = new AttributeFilterPolicy("afp2");
        afp2.setPolicyRequirementRule(new AnyMatchFunctor());
        AttributeRule ePARule2 = new AttributeRule("eduPersonAffiliation");
        ePARule2.setDenyValueRule(employeeAllow);
        afp2.getAttributeRules().add(ePARule2);
        AttributeRule ePSARule2 = new AttributeRule("eduPersonScopedAffiliation");
        ePSARule2.setDenyValueRule(employeeAllow);
        afp2.getAttributeRules().add(ePSARule2);

        // Now run the actual test
        ShibbolethAttributeFilteringEngine filterEngine = new ShibbolethAttributeFilteringEngine();
        filterEngine.getFilterPolicies().add(afp1);
        filterEngine.getFilterPolicies().add(afp2);

        Map<String, BaseAttribute> filteredAttributes = filterEngine.filterAttributes(attributes, requestContext);

        BaseAttribute eduPersonAffiliation = filteredAttributes.get("eduPersonAffiliation");
        assertNotNull(eduPersonAffiliation);
        assertEquals(1, eduPersonAffiliation.getValues().size());
        assertTrue(eduPersonAffiliation.getValues().contains("staff"));

        BaseAttribute eduPersonScopedAffiliation = filteredAttributes.get("eduPersonAffiliation");
        assertNotNull(eduPersonScopedAffiliation);
        assertEquals(1, eduPersonScopedAffiliation.getValues().size());
        assertTrue(eduPersonScopedAffiliation.getValues().contains("staff"));
    }
}