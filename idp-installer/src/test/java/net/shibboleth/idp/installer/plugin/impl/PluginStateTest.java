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

package net.shibboleth.idp.installer.plugin.impl;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;

import org.testng.annotations.Test;

import net.shibboleth.idp.plugin.IdPPlugin;
import net.shibboleth.idp.plugin.PluginVersion;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.component.ComponentInitializationException;

/**
 * Tests for {@link PluginState}.
 */
@SuppressWarnings("javadoc")
public class PluginStateTest {
    
    private boolean testSupportState(final PluginVersion pluginVersion, final PluginState state, final String IdpVersion) {
        final PluginVersion idPVersion = new PluginVersion(IdpVersion);
        return state.getPluginInfo().isSupportedWithIdPVersion(pluginVersion, idPVersion);
    }

    @Test
    public void testSimple() throws ComponentInitializationException {

        final IdPPlugin simple = new TestPlugin();
        
        final PluginState state = new PluginState(simple, CollectionSupport.emptyList());
        
        state.initialize();

        final PluginVersion pluginVersion = new PluginVersion(simple.getMajorVersion(), simple.getMinorVersion(), simple.getPatchVersion());
        
        assertEquals(pluginVersion, new PluginVersion("1.2.3"));
        assertEquals(state.getPluginInfo().getAvailableVersions().size(), 3);
        assertTrue(state.getPluginInfo().getAvailableVersions().containsKey(new PluginVersion(1, 2, 3)));
        assertTrue(state.getPluginInfo().getAvailableVersions().containsKey(new PluginVersion(1, 2, 4)));
        assertTrue(state.getPluginInfo().getAvailableVersions().containsKey(new PluginVersion(2,0,0)));
        assertFalse(state.getPluginInfo().getAvailableVersions().containsKey(new PluginVersion(3, 2, 3)));

        assertTrue(testSupportState(pluginVersion, state, "4.1.0"));
        assertTrue(testSupportState(pluginVersion, state, "4.2.0"));
        assertTrue(testSupportState(pluginVersion, state, "4.99.9"));
        assertFalse(testSupportState(pluginVersion, state, "5.0.0"));

        final PluginVersion v124 = new PluginVersion(1,2,3);        
        assertTrue(testSupportState(v124, state,"4.1.0"));
        assertTrue(testSupportState(v124, state, "4.99.9"));
        assertFalse(testSupportState(v124, state, "5.0.0"));
        assertFalse(testSupportState(v124, state, "4.0.0"));

        final PluginVersion v2 = new PluginVersion(2,0,0);
        assertTrue(testSupportState(v2, state, "4.99.1"));
        assertTrue(testSupportState(v2, state, "4.99.999"));
        assertFalse(testSupportState(v2, state, "4.99.0"));
        assertFalse(testSupportState(v2, state, "4.98.999"));
        assertTrue(testSupportState(v2, state, "5.0.0"));
        assertTrue(testSupportState(v2, state, "6.0.0"));
        assertTrue(testSupportState(v2, state, "7.0.0"));
        assertFalse(testSupportState(v2, state, "8"));
    }

    @Test
    public void testTemplating() throws ComponentInitializationException, MalformedURLException {
        final TestPlugin tp = new TestPlugin();
        final PluginState state = new PluginState(tp, tp.getUpdateURLs());
        state.initialize();
        final PluginVersion v123 = new PluginVersion(1,2,3);
        final PluginVersion v124 = new PluginVersion(1,2,4);
        final PluginVersion v2 = new PluginVersion(2,0,0);

        assertEquals(state.getPluginInfo().getUpdateURL(v123), new URL("https://example.org/plugins/"));
        assertEquals(state.getPluginInfo().getUpdateURL(v124), new URL("https://example.org/plugins4/"));
        assertEquals(state.getPluginInfo().getUpdateURL(v2), new URL("https://example.org/plugins2/"));

        assertEquals(state.getPluginInfo().getUpdateBaseName(v123), "base-1.2.3-1.2.3");
        assertEquals(state.getPluginInfo().getUpdateBaseName(v124), "base-1.2.4-1.2.4");
        assertEquals(state.getPluginInfo().getUpdateBaseName(v2), "base-1-2-4");
    }

    @Test
    public void testMulti() throws IOException, Exception {

        final IdPPlugin simple = new TestPlugin() {
            @Override
            public @Nonnull java.util.List<URL> getUpdateURLs() {
                try {
                    return List.of(new URL("http://example.org/dir"), super.getUpdateURLs().get(0));
                } catch (final MalformedURLException e) {
                    fail(e.toString());
                    return super.getUpdateURLs();
                }
            }
        };
        
        final PluginState state = new PluginState(simple, CollectionSupport.emptyList());
        state.initialize();
        
        assertEquals(state.getPluginInfo().getAvailableVersions().size(), 3);
    }
}
