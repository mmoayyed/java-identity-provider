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

package net.shibboleth.idp.dependencies;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.beust.jcommander.internal.Nullable;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 *
 */
public class ParsedPom extends OpenSAMLInitBaseTestCase{
    
    /** Compile dependencies - what we care about. */
    private final Map<String, PomArtifact> compileDependencies = new HashMap<>();
    
    /** BOM dependencies. */
    private final Map<String, PomArtifact> bomDependencies = new HashMap<>();

    /** Rum time dependencies. */
    private final Map<String, PomArtifact> runtimeDependencies = new HashMap<>();    

    /** Duplicate dependencies. */
    private final List<Pair<PomArtifact,PomArtifact>> duplicates = new ArrayList<>();    

    /** Generated artifacts. */
    private final Set<PomArtifact> generated = new HashSet<>();

    /** Inherits dependencies. */
    private final Map<String, PomArtifact> managedDependencies;
    
    /** Which the POM.*/
    @Nonnull private final String sourcePomInfo;
    
    /** Properties. */
    private final Properties properties = new Properties();
    
    /** Parent Pom .*/
    private PomArtifact parent;
    
    /** Us. */
    private final PomArtifact us;

    /**
     * Constructor.
     *
     * @param parsers a short-cut to let us parse XML
     * @param pomLoader how to get a pom (for BOM loading)
     * @param pom the {@link Path} to the pom.
     * @param pomName an ID for the pom
     * @param parentPomProperties if present it is properties from the parent (which might be empty), if null we are *only*
     * looking for the parent pom coordinates.
     * @param map Managed dependencies from parent
     * @throws Exception if we have issued locating a bom
     */
    public ParsedPom(@Nonnull final ParserPool parsers,
                     @Nonnull final PomLoader pomLoader,
                     @Nonnull final Path pom,
                     @Nonnull final String pomName,
                     @Nullable final Properties parentPomProperties, 
                     @Nonnull final Map<String, PomArtifact> map)
            throws Exception {

        managedDependencies = new HashMap<>(map);

        sourcePomInfo = pomName;
        Document document;
        try (final InputStream stream = new BufferedInputStream(new FileInputStream(pom.toFile()))) {
            document = parsers.parse(stream);
        }

        final Element el = document.getDocumentElement();
        if (!"project".equals(el.getLocalName())) {
            throw new XMLParserException("Top level element was not <project>");
        }
        final List<Element> par = ElementSupport.getChildElementsByTagName(el, "parent");
        
        if (!par.isEmpty()) {
            parseParent(par.get(0));
        }

        us = new PomArtifact(el, parent);

        if (parentPomProperties == null) {
            return;
        }
        for (final Object p:parentPomProperties.keySet()) {
            String pName = (String) p;
            properties.setProperty(pName, parentPomProperties.getProperty(pName));
        }
        properties.setProperty("project.basedir", "<bogus_base_dir>");
        properties.setProperty("project.build.directory", "<bogus_build_dir>");
        properties.setProperty("project.version", us.getVersion());
        properties.setProperty("project.groupId", us.getGroupId());
        properties.setProperty("project.artifactId", us.getArtifactId());

        final List<Element> props = ElementSupport.getChildElementsByTagName(el, "properties");
        if (!props.isEmpty()) {
            parseProperties(props.get(0));
        }

        for (final Element dependencyMgt: ElementSupport.getChildElementsByTagName(el, "dependencyManagement")) {
            for (final Element dependencies : ElementSupport.getChildElementsByTagName(dependencyMgt, "dependencies")) {
                parseManagedDependencies(dependencies);
            }
        }
        for (final PomArtifact bom : bomDependencies.values()) {
            final ParsedPom parsedBom = new ParsedPom(parsers, pomLoader, pomLoader.downloadPom(bom), bom.toString(), new Properties(), Collections.emptyMap());
            for (PomArtifact dep : parsedBom.getManagedDependencies().values()) {
                addWithCheck(dep, managedDependencies);
            }
        }

        for (final Element dependencies : ElementSupport.getChildElementsByTagName(el, "dependencies")) {
            parseDependencies(dependencies);
        }

        final Set<PomArtifact> moduleCompiles = new HashSet<>();
        final Set<PomArtifact> moduleRuntimes = new HashSet<>();
        for (final Element modules: ElementSupport.getChildElementsByTagName(el, "modules")) {
            for (final Element module: ElementSupport.getChildElementsByTagName(modules, "module")) {
                // Kludge for Jackson
                final Path modulePath = Path.of(module.getTextContent()).resolve("pom.xml");
                if (Files.exists(modulePath)) {
                    final ParsedPom modulePom = new ParsedPom(parsers, pomLoader, modulePath ,module.getTextContent(), properties, managedDependencies);
                    moduleCompiles.addAll(modulePom.getCompileDependencies());
                    moduleRuntimes.addAll(modulePom.getRuntimeDependencies());
                    generated.add(modulePom.getOurInfo());
                }
            }
        }
        for (final PomArtifact dep : moduleCompiles) {
            addWithCheck(dep, compileDependencies);
        }
        for (final PomArtifact dep : moduleRuntimes) {
            addWithCheck(dep, runtimeDependencies);
        }
    }

    /** Get the text content of the element, performing property replacement as we go.
     * @param el the element
     * @return the value, with property replacement.
     */
    @Nonnull protected String getElementContent(final Element el) {
        String remainingContents = StringSupport.trimOrNull(el.getTextContent());
        remainingContents = Constraint.isNotNull(remainingContents, "<" + el.getLocalName() +  "> must have content");
        final StringBuilder contents = new StringBuilder();
        for (int index = remainingContents.indexOf("${"); index >= 0; index = remainingContents.indexOf("${")) {
            contents.append(remainingContents.substring(0, index));
            remainingContents = remainingContents.substring(index);
            final int endIndex = remainingContents.indexOf("}");
            if (endIndex <= 1) {
                break;
            }
            final String propName = remainingContents.substring(2, endIndex);
            contents.append(Constraint.isNotNull(properties.getProperty(propName), propName + " is not defined"));
            remainingContents = remainingContents.substring(endIndex+1);
        }
        contents.append(remainingContents);
        return contents.toString();
    }

    /** Parse the dependency part of the pom.
     * @param item what to parse
     */
    private void parseDependencies(final Element item) {
        final List<Element> dependencies = ElementSupport.getChildElementsByTagName(item, "dependency");
        
        for (Element dependency : dependencies) {
            final PomArtifact artifact = new PomArtifact(dependency);
            final List<Element> types = ElementSupport.getChildElementsByTagName(dependency, "type");
            if (!types.isEmpty()) {
                final String type = StringSupport.trimOrNull(types.get(0).getTextContent());
                if ("pom".equals(type)) {
                    addWithCheck(artifact, bomDependencies);
                    continue;
                } else if (!"jar".equals(type)) {
                    // not for us
                    continue;
                }                
            }
            final List<Element> scopes = ElementSupport.getChildElementsByTagName(dependency, "scope");
            if (!scopes.isEmpty()) {
                final String scope = StringSupport.trimOrNull(scopes.get(0).getTextContent());
                if ("runtime".equals(scope)) {
                    addWithCheck(artifact, runtimeDependencies);
                    continue;
                }
                if (!"compile".equals(scope)) {
                    // not for us
                    continue;
                }
            }
            addWithCheck(artifact, compileDependencies);
            continue;
        }
    }

    /** parse the Managed Dependencies from the provided item
     * @param item  what to parse
     */
    private void parseManagedDependencies(final Element item) {
        final List<Element> dependencies = ElementSupport.getChildElementsByTagName(item, "dependency");
        for (Element dependency : dependencies) {
            final PomArtifact artifact = new PomArtifact(dependency);
            final List<Element> types = ElementSupport.getChildElementsByTagName(dependency, "type");
            if (!types.isEmpty()) {
                final String type = StringSupport.trimOrNull(types.get(0).getTextContent());
                if ("pom".equals(type)) {
                    addWithCheck(artifact, bomDependencies);
                    continue;
                } else if (!"jar".equals(type)) {
                    // not for us
                    continue;
                }
            }
            addWithCheck(artifact, managedDependencies);
        }
    }

    /** Add the artifact to the map, accumulating duplicates.
     * @param artifact what to add
     * @param map wghere to add it
     */
    private void addWithCheck(final PomArtifact artifact, final Map<String, PomArtifact> map) {
        final PomArtifact old = map.put(artifact.getMapKey(),artifact);
        if (old != null) {
            duplicates.add(new Pair<>(old, artifact));
        }
    }

    /** Parse the properties from the pom.
     * @param item the &lt;properties&gt; element
     */
    private void parseProperties(Element item) {
        
        for (final Element child : ElementSupport.getChildElements(item)) {
            final String name = child.getLocalName();
            final String value = getElementContent(child);
            
            properties.setProperty(name, value);
        }
    }

    /** Parse the parent from the pom. 
     * @param item the &lt;parent&gt; element
     */
    private void parseParent(Element item) {
        parent = new PomArtifact(item);
    }

    /** Returns the Compile Dependencies.
     * @return Returns the Compile Dependencies.
     */
    @Nonnull public Collection<PomArtifact> getCompileDependencies() {
        return compileDependencies.values();
    }

    /** Returns the Runtime Dependencies.
     * @return Returns the Runtime Dependencies.
     */
    @Nonnull public Collection<PomArtifact> getRuntimeDependencies() {
        return runtimeDependencies.values();
    }

    /**  Returns the Managed Dependencies.
     * @return Returns the Managed Dependencies.
     */
    @Nonnull public Map<String, PomArtifact> getManagedDependencies() {
        return managedDependencies;
    }

    /** Get artifacts that were duplicated by this build
     * @return Returns the duplicates.
     */
    @Nonnull public List<Pair<PomArtifact, PomArtifact>> getDuplicates() {
        return duplicates;
    }

    /** returns any sub modules created by this module.
     * @return Returns the generated.
     */
    @Nonnull public Set<PomArtifact> getGeneratedArtifacts() {
        return generated;
    }

    /** Return our artifactInformation.
     * @return us.
     */
    public PomArtifact getOurInfo() {
        return us;
    }

    /** Return the parent.
     * @return  the parent.
     */
    public PomArtifact getParent() {
        return parent;
    }

    /** The &lt;properties&gt; contents.
     * @return Returns the properties.
     */
    public Properties getProperties() {
        return properties;
    }
 
    /** Encapsulation of a &lt;dependency&gt; element. */
    public class PomArtifact implements Comparable<PomArtifact>{

        /** What version to give if we cannot find the version. */
        public final static String BAD_VERSION = "VERSION_NOT_DETERMINED"; 

        /** &lt;groupId&gt;.*/
        @Nonnull private final String groupId;

        /** &lt;artifactId&gt;.*/
        @Nonnull private final String artifactId;
        
        /** &lt;version&gt;.*/
        @Nonnull private final String version;

        /** &lt;exclusions&gt;. */
        @Nonnull private final Set<Pair<String, String>> exclusions = new HashSet<>();

        /**
         * Constructor.
         *
         * @param id the &lt;artifactId&gt; 
         * @param group the &lt;groupId&gt;
         * @param ver the &lt;version&gt;
         */
        public PomArtifact(final String  group, final String id, final String ver) {
            artifactId = id;
            groupId = group;
            version = ver;
        }
        
        /**
         * Constructor.
         *
         * @param item element to interrogate.
         */
        public PomArtifact(final Element item) {
            this(item, null);
        }

        /**
         * Constructor.
         *
         * @param item element to interrogate.
         * @param parentArtifact to inherit from
         */
        public PomArtifact(final Element item, final @Nullable PomArtifact parentArtifact) {
            
            final List<Element> grps  = ElementSupport.getChildElementsByTagName(item, "groupId");
            if (grps.size() > 0) {
                groupId = getElementContent(grps.get(0));
            } else if (parentArtifact != null) {
                groupId = parentArtifact.getGroupId();
            } else {
                Constraint.isGreaterThan(0, grps.size(), "<groupId> should exist in dependency");
                groupId = null;
            }
            
            final List<Element> arts  = ElementSupport.getChildElementsByTagName(item, "artifactId");
            Constraint.isGreaterThan(0, arts.size(), "<artifactId> should exist in dependency");
            artifactId = getElementContent(arts.get(0));
            
            final List<Element> vers  = ElementSupport.getChildElementsByTagName(item, "version");
            if (vers.size() > 0) {
                version = getElementContent(vers.get(0));
            } else if (parentArtifact != null) {
                version = parentArtifact.getVersion();
            } else {
                final PomArtifact inherited = managedDependencies.get(groupId+"+"+artifactId);
                if (inherited != null) {
                    version = inherited.getVersion();
                } else {
                    version = BAD_VERSION;
                }
            }
            
            List<Element> excls  = ElementSupport.getChildElementsByTagName(item, "exclusions"); 
            if (excls.size() > 0) {
                excls  = ElementSupport.getChildElementsByTagName(excls.get(0), "exclusion");
                for (Element e : excls) {
                    List<Element> els = ElementSupport.getChildElementsByTagName(e, "groupId");
                    Constraint.isGreaterThan(0, els.size(), "<groupId> should exist in exclusion");
                    final String grp = getElementContent(els.get(0));
                    els = ElementSupport.getChildElementsByTagName(e, "artifactId");
                    Constraint.isGreaterThan(0, els.size(), "<artifactId> should exist in exclusion");
                    final String art = getElementContent(els.get(0));
                    exclusions.add(new Pair<>(grp, art));
                }
            }
        }

        /**
         * @return Returns the groupId.
         */
        public String getGroupId() {
            return groupId;
        }

        /**
         * @return Returns the artifactId.
         */
        public String getArtifactId() {
            return artifactId;
        }

        /**
         * @return Returns the version.
         */
        public String getVersion() {
            return version;
        }
        
        /**
         * @return the pom source.
         */
        public String getSourcePomFilename() {
            return sourcePomInfo;
        }
        
        /** Get the key we use in out maps.
         * @return the key - derives from groupId and EntityId 
         */
        public String getMapKey() {
            return getGroupId()+"+"+getArtifactId();
        }
        
        /**
         * @return Returns the exclusions.
         */
        public Set<Pair<String, String>> getExclusions() {
            return exclusions;
        }

        /** {@inheritDoc} */
        public int compareTo(final PomArtifact o) {
            return getArtifactId().compareTo(o.getArtifactId());
        }
        
        /** {@inheritDoc} */
        public boolean equals(final Object obj) {
            if (obj != null && obj instanceof PomArtifact ) {
                final PomArtifact him = (PomArtifact) obj;
                return  him.getArtifactId().equals(getArtifactId()) &&
                        him.getGroupId().equals(getGroupId()) &&
                        him.getVersion().equals(getVersion());
            }
            return false;
        }
        
        /** {@inheritDoc} */
        public int hashCode() {
            return Objects.hash(artifactId, groupId, version);
        }
        
        /** {@inheritDoc} */
        public String toString() {
            return artifactId + "-" + version;
        }

        /** return the same artifact but with an amended version.
         * @param ver the version
         * @return an amended artifact.
         */
        public PomArtifact withVersion(String ver) {
            return new PomArtifact(groupId, artifactId, ver);
        }
    }
}
