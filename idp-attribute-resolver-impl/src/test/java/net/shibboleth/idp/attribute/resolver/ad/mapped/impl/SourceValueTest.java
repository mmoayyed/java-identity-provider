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

package net.shibboleth.idp.attribute.resolver.ad.mapped.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 * test for {@link SourceValue}.
 */
public class SourceValueTest {

    Logger log = LoggerFactory.getLogger(SourceValueTest.class);

    @Test public void sourceValue() throws ComponentInitializationException {
        SourceValue value = newSourceValue("value", false, true);

        assertEquals(value.getValue(), "value");
        assertTrue(value.isPartialMatch());
        assertTrue(value.isCaseSensitive());

        log.info("Value = 'value', ignore = true, partial = false", value.toString());

        value = newSourceValue("eulaV", true, false);

        assertEquals(value.getPattern().pattern(), "eulaV");
        assertFalse(value.isPartialMatch());
        assertFalse(value.isCaseSensitive());
        log.info("Value = 'eulaV', ignore = false, partial = true", value.toString());

    }

    @Test public void testDefault() throws ComponentInitializationException {
        assertTrue(new SourceValue().isCaseSensitive());
    }
    
    @SuppressWarnings("deprecation")
    @Test public void deprecated() throws ComponentInitializationException {
        final SourceValue value = new SourceValue();
        assertTrue(value.isCaseSensitive());
        value.setIgnoreCase(true);
        assertFalse(value.isCaseSensitive());
        value.setIgnoreCase(null);
        assertTrue(value.isCaseSensitive());
        value.setCaseSensitive(false);
        assertFalse(value.isCaseSensitive());
        assertTrue(value.isIgnoreCase());
        value.setCaseSensitive(null);
        assertTrue(value.isCaseSensitive());
        assertFalse(value.isIgnoreCase());
    }

    public static SourceValue newSourceValue(final String value, final boolean ignoreCase, final boolean partialMatch)
            throws ComponentInitializationException {

        final SourceValue sourceValue = new SourceValue();
        sourceValue.setValue(value);
        sourceValue.setCaseSensitive(!ignoreCase);
        sourceValue.setPartialMatch(partialMatch);
        sourceValue.initialize();
        return sourceValue;
    }

}
