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

package net.shibboleth.idp.attribute.filter.policyrule.saml.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.Arrays;

import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.filter.PolicyRequirementRule.Tristate;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;

/** {@link RequesterRegistrationAuthorityPolicyRule} unit test. */
@SuppressWarnings("javadoc")
public class RequesterRegistrationAuthorityTest extends BaseMetadataTests {

    private static final String REQUESTED_REG_INFO = "http://www.swamid.se/";

    private static final String INCOMMON_REG_INFO = "https://incommon.org";

    private static final String INCOMMON_SP = "https://wiki.ligo.org/shibboleth-sp";

    private static final String NO_REGINFO_SP = "https://issues.shibboleth.net/shibboleth";

    private final String SWAMID = "https://sp-test.swamid.se/shibboleth";

    private EntitiesDescriptor metadata;

    @BeforeClass(dependsOnMethods = "initXMLObjectSupport") public void initRATest() {
        metadata = unmarshallElement("/net/shibboleth/idp/filter/impl/saml/mdrpi-metadata.xml");
    }

    private EntityDescriptor getEntity(String entityID) {
        for (EntityDescriptor entity : metadata.getEntityDescriptors()) {
            if (entity.getEntityID().equals(entityID)) {
                return entity;
            }
        }
        fail("Could not find " + entityID);
        return null;
    }

    @Test public void swamid() throws Exception {

        AttributeFilterContext context = reqMetadataContext(getEntity(SWAMID), "principal");
        final RequesterRegistrationAuthorityPolicyRule filter = new RequesterRegistrationAuthorityPolicyRule();
        String[] array = {REQUESTED_REG_INFO, "foo",};
        filter.setRegistrars(Arrays.asList(array));

        assertEquals(filter.matches(context), Tristate.TRUE);
        array[0] = INCOMMON_REG_INFO;
        filter.setRegistrars(Arrays.asList(array));
        assertEquals(filter.matches(context), Tristate.FALSE);
    }

    @Test public void ligo() {
        AttributeFilterContext context = reqMetadataContext(getEntity(INCOMMON_SP), "principal");
        final RequesterRegistrationAuthorityPolicyRule filter = new RequesterRegistrationAuthorityPolicyRule();
        String[] array = {REQUESTED_REG_INFO, "foo",};
        filter.setRegistrars(Arrays.asList(array));

        assertEquals(filter.matches(context), Tristate.FALSE);
        array[0] = INCOMMON_REG_INFO;
        filter.setRegistrars(Arrays.asList(array));
        assertEquals(filter.matches(context), Tristate.TRUE);
    }

    @Test public void none()  {
        AttributeFilterContext context = reqMetadataContext(getEntity(NO_REGINFO_SP), "principal");
        final RequesterRegistrationAuthorityPolicyRule filter = new RequesterRegistrationAuthorityPolicyRule();
        String[] array = {REQUESTED_REG_INFO, INCOMMON_REG_INFO, "foo",};
        filter.setRegistrars(Arrays.asList(array));

        filter.setMatchIfMetadataSilent(true);
        assertEquals(filter.matches(context), Tristate.TRUE);
        filter.setMatchIfMetadataSilent(false);
        assertEquals(filter.matches(context), Tristate.FALSE);
    }
    
}