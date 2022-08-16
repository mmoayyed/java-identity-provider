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

package net.shibboleth.idp.profile.logic;

import net.shibboleth.idp.attribute.DateTimeAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Unit test for {@link DateAttributePredicate}.
 */
public class DateAttributePredicateTest {
    
    private final java.time.format.DateTimeFormatter javaformatter =
            java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

    @DataProvider(name = "test-data-java")
    public Object[][] provideTestDataJava() {
        return new Object[][] {
                // Future date matches
                new Object[] {
                        new DateAttributePredicate("expirationDate", javaformatter),
                        "expirationDate",
                        javaDateStrings(java.time.Duration.ofDays(1)),
                        true,
                },
                // Current date does not match
                new Object[] {
                        new DateAttributePredicate("expirationDate", javaformatter),
                        "expirationDate",
                        javaDateStrings(java.time.Duration.ZERO),
                        false,
                },
                // Past date does not match
                new Object[] {
                        new DateAttributePredicate("expirationDate", javaformatter),
                        "expirationDate",
                        javaDateStrings(java.time.Duration.ofDays(-1)),
                        false,
                },
                // Increase target date by 90 days
                new Object[] {
                        newJavaPredicate("expirationDate", java.time.Duration.ofDays(90)),
                        "expirationDate",
                        javaDateStrings(java.time.Duration.ofDays(91)),
                        true,
                },
                // Decrease target date by 30 days
                // e.g. expiration warning case
                new Object[] {
                        newJavaPredicate("expirationDate", java.time.Duration.ofDays(-30)),
                        "expirationDate",
                        javaDateStrings(java.time.Duration.ofDays(29)),
                        false,
                },
        };
    }


    @Test(dataProvider = "test-data-java")
    public void testJavaTime(
            final DateAttributePredicate predicate,
            final String attribute,
            final String[] values,
            final boolean expected) throws Exception {
        assertEquals(predicate.test(createProfileRequestContext(attribute, values, null)), expected);
    }

    @Test
    public void testDateTimeValues() {
        final DateAttributePredicate predicate = new DateAttributePredicate("test");
        
        assertTrue(predicate.test(createProfileRequestContext("test", null,
                new Instant[] {Instant.now().plus(java.time.Duration.ofMinutes(5))})));
        
        predicate.setOffset(java.time.Duration.ofMinutes(-10));
        
        assertFalse(predicate.test(createProfileRequestContext("test", null,
                new Instant[] {Instant.now().plus(java.time.Duration.ofMinutes(5))})));
    }
    
    
    private ProfileRequestContext createProfileRequestContext(final String name, final String[] values, final Instant[] dtvalues) {
        final ProfileRequestContext prc = new ProfileRequestContext();
        final RelyingPartyContext rpc = new RelyingPartyContext();
        final IdPAttribute attribute = new IdPAttribute(name);
        final List<IdPAttributeValue> attributeValues = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                attributeValues.add(new StringAttributeValue(value));
            }
        }
        if (dtvalues != null) {
            for (Instant value : dtvalues) {
                attributeValues.add(new DateTimeAttributeValue(value));
            }
        }
        attribute.setValues(attributeValues);
        final AttributeContext ac = new AttributeContext();
        ac.setIdPAttributes(Collections.singletonList(attribute));
        ac.setUnfilteredIdPAttributes(Collections.singletonList(attribute));
        rpc.addSubcontext(ac);
        prc.addSubcontext(rpc);
        return prc;
    }

    /**
     * Produces an array of date strings that are offsets from current system time.
     *
     * @param offsets One or more durations that are added to the current system time.
     *
     * @return Array of date strings, one for each provided offset.
     */
    private String[] javaDateStrings(final java.time.Duration ... offsets) {
        final String[] dates = new String[offsets.length];
        for (int i = 0; i < offsets.length; i++) {
            dates[i] = javaformatter.format(ZonedDateTime.now().plus(offsets[i]));
        }
        return dates;
    }
    
    private DateAttributePredicate newJavaPredicate(final String attribute, final java.time.Duration offset) {
        final DateAttributePredicate p = new DateAttributePredicate(attribute, javaformatter);
        p.setOffset(offset);
        return p;
    }
    
}
