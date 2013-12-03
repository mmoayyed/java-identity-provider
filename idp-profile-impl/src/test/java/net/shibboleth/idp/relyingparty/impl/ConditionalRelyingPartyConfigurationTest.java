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

package net.shibboleth.idp.relyingparty.impl;

import java.util.Collections;

import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** Unit test for {@link ConditionalRelyingPartyConfiguration}. */
public class ConditionalRelyingPartyConfigurationTest {

    @Test public void testConstruction() {
        ConditionalRelyingPartyConfiguration config =
                new ConditionalRelyingPartyConfiguration("foo", "http://idp.example.org", Collections.EMPTY_LIST,
                        Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertEquals(config.getId(), "foo");
        Assert.assertEquals(config.getResponderEntityId(), "http://idp.example.org");
        Assert.assertSame(config.getActivationCondition(), Predicates.<ProfileRequestContext> alwaysFalse());
        Assert.assertTrue(config.getProfileConfigurations().isEmpty());

        try {
            config =
                    new ConditionalRelyingPartyConfiguration("foo", "http://idp.example.org", Collections.EMPTY_LIST,
                            null);
            Assert.fail();
        } catch (ConstraintViolationException e) {
            // expected this
        }
    }
}