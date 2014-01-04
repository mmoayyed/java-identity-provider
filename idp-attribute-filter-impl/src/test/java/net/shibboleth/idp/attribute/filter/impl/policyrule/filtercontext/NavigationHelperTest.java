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

package net.shibboleth.idp.attribute.filter.impl.policyrule.filtercontext;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.idp.attribute.resolver.context.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;

import org.opensaml.messaging.context.BaseContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for the {@link NavigationSupport}.
 */
public class NavigationHelperTest {

    @Test public void testNavigation() {
        Assert.assertNull(NavigationSupport.locateResolverContext(new AttributeFilterContext()));
        Assert.assertNull(NavigationSupport.locateResolverContext((new BaseContext() {}).getSubcontext(
                AttributeFilterContext.class, true)));

        AttributeResolutionContext filterContext =
                NavigationSupport.locateResolverContext(DataSources.unPopulatedFilterContext());
        Assert.assertNull(NavigationSupport.locateRecipientContext(filterContext));

        AttributeFilterContext context =
                DataSources.populatedFilterContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);

        filterContext = NavigationSupport.locateResolverContext(context);
        AttributeRecipientContext recipient = NavigationSupport.locateRecipientContext(filterContext);

        Assert.assertEquals(recipient.getAttributeIssuerID(), TestSources.IDP_ENTITY_ID);
        Assert.assertEquals(recipient.getAttributeRecipientID(), TestSources.SP_ENTITY_ID);
        Assert.assertEquals(filterContext.getPrincipal(), TestSources.PRINCIPAL_ID);
    }
}
