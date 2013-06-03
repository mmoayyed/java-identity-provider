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

package net.shibboleth.idp.attribute.filter.impl.filtercontext;

import net.shibboleth.idp.attribute.filter.AttributeFilterException;
import net.shibboleth.idp.attribute.filter.MatcherException;
import net.shibboleth.idp.attribute.filter.impl.filtercontext.AttributeIssuerMatcher;
import net.shibboleth.idp.attribute.filter.impl.filtercontext.AttributeRequesterMatcher;
import net.shibboleth.idp.attribute.filter.impl.matcher.DataSources;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for {@link AttributeIssuerMatcher}.
 */
public class AttributeRequesterMatcherTest {

    private AttributeRequesterMatcher getMatcher(boolean caseSensitive) throws ComponentInitializationException {
        AttributeRequesterMatcher matcher = new AttributeRequesterMatcher();
        matcher.setMatchString("requester");
        matcher.setCaseSensitive(caseSensitive);
        matcher.setId("Test");
        matcher.initialize();
        return matcher;

    }

    private AttributeRequesterMatcher getMatcher() throws ComponentInitializationException {
        return getMatcher(true);
    }

    @Test public void testNull() throws ComponentInitializationException {

        try {
            new AttributeRequesterMatcher().doCompare(null);
            Assert.fail();
        } catch (UninitializedComponentException ex) {
            // OK
        }
    }

    @Test(expectedExceptions = {MatcherException.class}) public void testUnpopulated()
            throws ComponentInitializationException, AttributeFilterException {
        getMatcher().doCompare(DataSources.unPopulatedFilterContext());
    }

    @Test(expectedExceptions = {MatcherException.class}) public void testNoRequester()
            throws ComponentInitializationException, AttributeFilterException {
        getMatcher().doCompare(DataSources.populatedFilterContext(null, null, null));
    }

    @Test public void testCaseSensitive() throws ComponentInitializationException {

        final AttributeRequesterMatcher matcher = getMatcher();

        Assert.assertFalse(matcher.doCompare(DataSources.populatedFilterContext(null, null, "wibble")));
        Assert.assertFalse(matcher.doCompare(DataSources.populatedFilterContext(null, null, "REQUESTER")));
        Assert.assertTrue(matcher.doCompare(DataSources.populatedFilterContext(null, null, "requester")));
    }

    @Test public void testCaseInsensitive() throws ComponentInitializationException, AttributeFilterException {

        final AttributeRequesterMatcher matcher = getMatcher(false);

        Assert.assertFalse(matcher.matches(DataSources.populatedFilterContext(null, null, "wibble")));
        Assert.assertTrue(matcher.doCompare(DataSources.populatedFilterContext(null, null, "REQUESTER")));
        Assert.assertTrue(matcher.doCompare(DataSources.populatedFilterContext(null, null, "requester")));
    }
}