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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import net.shibboleth.idp.installer.InstallerSupport;
import net.shibboleth.idp.module.IdPModule;
import net.shibboleth.idp.module.ModuleContext;
import net.shibboleth.idp.module.ModuleException;
import net.shibboleth.shared.collection.Pair;

/** Tests for {@link RollbackPluginInstall}. */
@SuppressWarnings("javadoc")
public class RollbackTester {
    
    private Path parent;
    
    @BeforeClass public void setup() throws IOException {
        parent = Files.createTempDirectory("RollBacktest");
    }
    
    @AfterClass public void teardown() {
        InstallerSupport.deleteTree(parent);
    }
    
    @Test public void rollbackTest() throws IOException, ModuleException {
        test(false);
    }

    @Test public void commitTest() throws IOException, ModuleException {
        test(true);
    }
    
    private void test(boolean commit) throws IOException, ModuleException {
        final Path copied = Files.createTempFile(parent, "copied", "file");
        final Path to = Files.createTempFile(parent, "renamed", "file");
        final Path mc = Files.createTempDirectory(parent, "mod");
        final Path from = parent.resolve("fromFile");
        final Pair<Path, Path> renamed = new Pair<>(from, to);
        final IdPModule enabled1 = new TestModule("enabled1", null, null);
        final IdPModule enabled2 = new TestModule("enabled2", null, new ModuleException()); 
        final IdPModule disabled1 = new TestModule("disabled1", null, null);
        final IdPModule disabled2 = new TestModule("disablde2", new ModuleException(), null);
        final String mcs = mc.toString();
        final String parentString = parent.toString();
        assert mcs!=null && parentString!=null;
        final ModuleContext ctx = new ModuleContext(mcs);

        try {
            assertFalse(from.toFile().exists());
            assertTrue(to.toFile().exists());
            assertTrue(copied.toFile().exists());
            
            enabled1.enable(ctx);
            assertTrue(enabled1.isEnabled(ctx));
            
            enabled2.enable(ctx);
            assertTrue(enabled2.isEnabled(ctx));
            
            assertFalse(disabled1.isEnabled(ctx));
            assertFalse(disabled2.isEnabled(ctx));
            
            try (final RollbackPluginInstall rp = new RollbackPluginInstall(new ModuleContext(parentString), new HashMap<>())) {
                rp.getFilesCopied().add(copied);
                rp.getFilesRenamedAway().add(renamed);
                rp.getModulesDisabled().add(disabled1);
                rp.getModulesDisabled().add(disabled2);
                rp.getModulesEnabled().add(enabled1);
                rp.getModulesEnabled().add(enabled2);
                if (commit) {
                    rp.completed();
                }
            }

            if (commit) {
                assertTrue(enabled1.isEnabled(ctx));
                assertTrue(enabled2.isEnabled(ctx));
                assertFalse(disabled1.isEnabled(ctx));
                assertFalse(disabled2.isEnabled(ctx));
                assertFalse(from.toFile().exists());
                assertTrue(to.toFile().exists());
                assertTrue(copied.toFile().exists());            
            } else {
                assertFalse(enabled1.isEnabled(ctx));
                assertTrue(enabled2.isEnabled(ctx)); // threw instead
                assertTrue(disabled1.isEnabled(ctx));
                assertFalse(disabled2.isEnabled(ctx)); // threw instead
                
                assertTrue(from.toFile().exists()); //copied to
                assertTrue(to.toFile().exists()); // copied from
                
                assertFalse(copied.toFile().exists()); //deleted                        
            }
        } finally {
            deleteIt(copied);
            deleteIt(from);
            deleteIt(to);
        }
    }

    private void deleteIt(final Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            p.toFile().deleteOnExit();
        }
    }
}
