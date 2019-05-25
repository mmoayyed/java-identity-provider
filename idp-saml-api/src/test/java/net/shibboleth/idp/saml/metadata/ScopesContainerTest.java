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

package net.shibboleth.idp.saml.metadata;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.testng.annotations.Test;

/**
 * Tests for the {@link ScopesContainer}.
 */
public class ScopesContainerTest {

    @Test public void empty() {
        final ScopesContainer scopes = new ScopesContainer();
        assertFalse(scopes.matchesScope("foo"));
        scopes.setRegexpScopes(null);
        scopes.setSimpleScopes(Collections.EMPTY_SET);
        assertFalse(scopes.matchesScope("foo"));
        scopes.setRegexpScopes(Set.of(""));
        scopes.setSimpleScopes(Collections.singleton((String)null));
        assertFalse(scopes.matchesScope("foo"));
    }
    
    @Test public void stringOnly() {
        final ScopesContainer scopes = new ScopesContainer();
        scopes.setSimpleScopes(Set.of("foo", "bar", "james"));
        assertFalse(scopes.matchesScope("jimmy"));
        assertTrue(scopes.matchesScope("foo"));
        assertTrue(scopes.matchesScope("bar"));
        assertTrue(scopes.matchesScope("james"));
    }
    
    @Test public void regexpOnly() {
        final ScopesContainer scopes = new ScopesContainer();
        scopes.setRegexpScopes(Set.of("^.*fo.*b$"));
        assertFalse(scopes.matchesScope("jimmy"));
        assertFalse(scopes.matchesScope("foo"));
        assertFalse(scopes.matchesScope("foobd"));
        assertTrue(scopes.matchesScope("prefoob"));
        assertTrue(scopes.matchesScope("prefooltfb"));
    }

    @Test public void both() {
        final ScopesContainer scopes = new ScopesContainer();
        scopes.setRegexpScopes(Set.of("^.*fo.*b$"));
        scopes.setSimpleScopes(Set.of("foo", "bar", "james"));
        assertFalse(scopes.matchesScope("jimmy"));
        assertTrue(scopes.matchesScope("james"));
        assertTrue(scopes.matchesScope("foo"));
        assertFalse(scopes.matchesScope("foobd"));
        assertTrue(scopes.matchesScope("prefoob"));
        assertTrue(scopes.matchesScope("prefooltfb"));
    }

}
