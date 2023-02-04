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

package net.shibboleth.idp.cli.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLine;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Program to check for potential jar clashes.
 */
public final class JarCheckCLI extends AbstractIdPHomeAwareCommandLine<JarCheckArguments> { 
    
    /** Logger. */
    @Nullable private Logger log;

    /** Jar files in edit-webapp. */
    @Nonnull private List<String> webAppJars = Collections.emptyList();

    /** Jar files Plugin Folder. */
    @Nonnull private List<String> pluginJars = Collections.emptyList();

    /** Jar files Distributions . */
    @Nonnull private List<String> distJars = Collections.emptyList();

    /** Populate {@link #webAppJars}, {@link #pluginJars} and {@link #distJars}. 
     * @param args the arguments
     */
    private void loadJarFiles(final JarCheckArguments args) {
        final Path idpHome = Path.of(args.getIdPHome());

        distJars = listJars(idpHome.resolve("dist").resolve("webapp"));
        pluginJars = listJars(idpHome.resolve("dist").resolve("plugin-webapp"));
        webAppJars = listJars(idpHome.resolve("edit-webapp"));
    }

    /** return a list of all the files in the lib file below the webapp folder provided. 
     * @param webapp folder to start at.
     * @return the list of names as a strung
     */
    private List<String> listJars(final Path webapp) {
        final Path libDir = webapp.resolve("WEB-INF").resolve("lib");
        
        if (!Files.exists(libDir) || !Files.isDirectory(libDir)) {
            return Collections.emptyList();
        }
        
        return Arrays.asList(libDir.toFile().list(new FilenameFilter() {
            public boolean accept(final File dir, final String name) {
                final String nameUpper = name.toUpperCase();
                return nameUpper.endsWith(".JAR");
            }
        }));
    }
    
    /** Check for the same file in two places.
     * @param args the arguments to check
     */
    private void listAndExactCheck(final JarCheckArguments args) {
        final Map<String, String> allNames = new HashMap<>(distJars.size() + pluginJars.size() + webAppJars.size());
        final String type = "jar called";

        addAndCheck(allNames, distJars, "distribution", type);
        addAndCheck(allNames, pluginJars, "plugins", type);
        addAndCheck(allNames, webAppJars, "edit-webapp", type);
        
        if (!args.isList()) {
            return;
        }
        
        final List<String> names = new ArrayList<>(allNames.keySet());
        Collections.sort(names);
        System.out.println("Sorted List of jars");
        System.out.println(String.format("  %-22s %s", "Name", "Source"));
        for (final String jarName: names) {
            System.out.println(String.format("  %-22s %s", jarName, allNames.get(jarName)));            
        }
    }

    /** Add the names to the Map, checking for already existing.
     * @param allNames the map
     * @param jars the names
     * @param source where the jar came from
     * @param type what we are checking
     */
    private void addAndCheck(final Map<String, String> allNames, 
            final List<String> jars, 
            final String source, 
            final String type) {
        
        for (final String jar: jars) {
            final String current = allNames.get(jar);
            if (current == null) {
                allNames.put(jar, source);
            } else {
                getLogger().warn("{} {} found in {} and in {}", type, jar, source, current);
            }
        }
    }

    /** {@inheritDoc} */
    protected Class<JarCheckArguments> getArgumentClass() {
        return JarCheckArguments.class;
    }

    /** {@inheritDoc} */
    protected String getVersion() {
        return Version.getVersion();
    }

    /** {@inheritDoc} */
    protected synchronized Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(JarCheckCLI.class);
        }
        return log;
    }

    /** Do a general test.
     * @param args the arguments to check
     */
    private void fileNamesOnly(final JarCheckArguments args) {
        final Map<String, String> allNames = new HashMap<>(distJars.size() + pluginJars.size() + webAppJars.size());
        final String type = "jar fragment called";

        addAndCheck(allNames, normalize(distJars), "distribution", type);
        addAndCheck(allNames, normalize(pluginJars), "plugins", type);
        addAndCheck(allNames, normalize(webAppJars), "edit-webapp", type);
    }
    
    /** make the file names look more 'usual'.
     * @param fileNames the files to look at
     * @return the processed names
     */
    private List<String> normalize(final List<String> fileNames) {
        
       final Pattern patt = Pattern.compile("^(.*)-[0123456789](.*)\\.jar$");
       final List<String> result = new ArrayList<>(fileNames.size());
       
       for (final String name: fileNames ) {
           final Matcher matcher = patt.matcher(name);
           if (matcher.find()) {
               result.add(matcher.group(1));
           }
       }
       return result;
    }

    /** Do a detailed check.
     * @param args the arguments to check
     */
    private void detailed(final JarCheckArguments args) {
        final Map<String, String> allNames = new HashMap<>();
        final Path idpHome = Path.of(args.getIdPHome());
        
        processClassNames(allNames, idpHome.resolve("dist").resolve("webapp"), distJars);
        processClassNames(allNames, idpHome.resolve("dist").resolve("plugin-webapp"), pluginJars);
        processClassNames(allNames, idpHome.resolve("edit-webapp"), webAppJars);
    }

    /** Look at all the class names in the jars provided.
     * @param namesSoFar the class names we have found
     * @param base The directory where the names live 
     * @param jars The Jar Names
     */
    private void processClassNames(final Map<String, String> namesSoFar, final Path base, final List<String> jars) {
        final Path libdir = base.resolve("WEB-INF").resolve("lib");
        for (final String jar: jars) {
            processClassNames(namesSoFar, libdir.resolve(jar));
        }
    }
    
    /** List all the class names in the jar provided.
     * @param namesSoFar the class names we have found
     * @param jar The far file
     */
    private void processClassNames(final Map<String, String> namesSoFar, final Path jar) {
        final String source = jar.toString();
        try (final InputStream inStream = new BufferedInputStream(new FileInputStream(jar.toFile()));
             final ArchiveInputStream classes = new ZipArchiveInputStream(inStream)) {
            
            ArchiveEntry entry = null;
            while ((entry = classes.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                final String claz = entry.getName();
                if (claz == null || claz.startsWith("META-INF") || "module-info.class".equals(claz)) {
                    continue;
                }
                final String current = namesSoFar.get(claz);
                if (current == null) {
                    namesSoFar.put(claz, source);
                } else {
                    getLogger().warn("class {} found in {} and in {}", claz, source, current);
                }
            }
        } catch (final FileNotFoundException  e) {
            getLogger().error("Could not open {}", source, e);
        } catch (final IOException e) {
            getLogger().error("Could not process {}", source, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected int doRun(@Nonnull final JarCheckArguments args) {
        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        loadJarFiles(args);
        listAndExactCheck(args);
        if (args.isDetailed()) {
            detailed(args);
        } else {
            fileNamesOnly(args);
        }
        
        return RC_OK;
    }
    
    /**
     * CLI entry point.
     * 
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        System.exit(new JarCheckCLI().run(args));
    }

}
