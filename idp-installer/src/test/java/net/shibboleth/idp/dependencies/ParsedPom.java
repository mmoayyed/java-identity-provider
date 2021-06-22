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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final List<PomArtifact> compileDependencies = new ArrayList<>();
    
    /** BOM dependencies. */
    private final List<PomArtifact> bomDependencies = new ArrayList<>();

    /** Rumtime dependencies. */
    private final List<PomArtifact> runtimeDependencies = new ArrayList<>();    

    /** managed dependencies. */
    private final List<PomArtifact> myManagedDependencies = new ArrayList<>();    
    
    /** Inherits dependencies. */
    private final Map<String, PomArtifact> inputManagedDependencies;
    
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
     * @param pom the {@link Path} to the pom.
     * @param pomName an ID for the pom
     * @param parentPomProperties if present it is properties from the parent (which might be empty), if null we are *only*
     * looking for the parent pom coordinates.
     * @param managed Managed dependencies from parent
     * @throws IOException from parsing
     * @throws FileNotFoundException if the file doesn't exist
     * @throws XMLParserException from parsing
     */
    public ParsedPom(@Nonnull final ParserPool parsers,
                     @Nonnull final Path pom,
                     @Nonnull final String pomName,
                     @Nullable final Properties parentPomProperties, 
                     @Nonnull final Collection<PomArtifact> managed)
            throws FileNotFoundException, IOException, XMLParserException {

        inputManagedDependencies = managed.stream().collect(Collectors.toMap(e->e.getGroupId()+"+"+e.getArtifactId(), Function.identity()));
                
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
        final List<Element> props = ElementSupport.getChildElementsByTagName(el, "properties");
        if (!props.isEmpty()) {
            parseProperties(props.get(0));
        }
        if (!properties.contains("project.groupId") && parent != null) {
            properties.setProperty("project.groupId", parent.groupId);
        }
        if (!properties.contains("project.version") && parent != null) {
            properties.setProperty("project.version", parent.version);
        }
        final List<Element> dependencyMgt = ElementSupport.getChildElementsByTagName(el, "dependencyManagement");
        if (!dependencyMgt.isEmpty()) {
            final List<Element> dependencies = ElementSupport.getChildElementsByTagName(dependencyMgt.get(0), "dependencies");
            parseDependencies(dependencies.get(0), true);
        }
        final List<Element> dependencies = ElementSupport.getChildElementsByTagName(el, "dependencies");
        if (!dependencies.isEmpty()) {
            parseDependencies(dependencies.get(0), false);
        }
    }

    @Nonnull protected String getElementContent(final Element el) {
        String contents = StringSupport.trimOrNull(el.getTextContent());
        contents = Constraint.isNotNull(contents, "<" + el.getLocalName() +  "> must have content");
        // Not perfect matching but fits our needs
        if (contents.length() > 3 && contents.startsWith("${") && contents.endsWith("}")) {
            final String propName = contents.substring(2, contents.length()-1);
            contents = Constraint.isNotNull(properties.getProperty(propName), propName + " is not defined");
        }
        return contents;
    }

    /**
     * @param item what to parse
     * @param isManaged to we add to the managed dependencies?
     */
    private void parseDependencies(final Element item, final boolean isManaged) {
        final List<Element> dependencies = ElementSupport.getChildElementsByTagName(item, "dependency");
        
        for (Element dependency : dependencies) {
            final PomArtifact artifact = new PomArtifact(dependency);
            final List<Element> types = ElementSupport.getChildElementsByTagName(dependency, "type");
            if (!types.isEmpty()) {
                final String type = StringSupport.trimOrNull(types.get(0).getTextContent());
                if ("pom".equals(type)) {
                    bomDependencies.add(artifact);
                    continue;
                } else if (!"jar".equals(type)) {
                    // not for us
                    continue;
                }                
            }
            if (isManaged) {
                myManagedDependencies.add(artifact);
            }
            final List<Element> scopes = ElementSupport.getChildElementsByTagName(dependency, "scope");
            if (!scopes.isEmpty()) {
                final String scope = StringSupport.trimOrNull(scopes.get(0).getTextContent());
                if ("runtime".equals(scope)) {
                    runtimeDependencies.add(artifact);
                    continue;
                }
                if (!"compile".equals(scope)) {
                    // not for us
                    continue;
                }
            }
            compileDependencies.add(artifact);
        }
    }

    /**
     * @param item
     */
    private void parseProperties(Element item) {
        
        for (final Element child : ElementSupport.getChildElements(item)) {
            final String name = child.getLocalName();
            final String value = child.getTextContent();
            properties.setProperty(name, value);
        }
    }

    /**
     * @param item
     */
    private void parseParent(Element item) {
        parent = new PomArtifact(item);
    }
    
    /**
     * @return Returns the compileDependencies.
     */
    public List<PomArtifact> getCompileDependencies() {
        return compileDependencies;
    }

    /**
     * @return Returns the bomDependencies.
     */
    public List<PomArtifact> getBomDependencies() {
        return bomDependencies;
    }
    
    /**
     * @return Returns the runtimeDependencies.
     */
    public List<PomArtifact> getRuntimeDependencies() {
        return runtimeDependencies;
    }
    
    /**
     * @return Returns the myManagedDependencies.
     */
    public List<PomArtifact> getManagedDependencies() {
        return myManagedDependencies;
    }

    /** Return our artifactInformation.
     * @return us.
     */
    public PomArtifact getOurInfo() {
        return us;
    }

    /**
     * @return Returns the parent.
     */
    public PomArtifact getParent() {
        return parent;
    }

    /**
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
         * @param id 
         * @param group
         * @param ver
         */
        private PomArtifact(final String id, final String group, final String ver) {
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
                final PomArtifact inherited = inputManagedDependencies.get(groupId+"+"+artifactId);
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

        /** return the same artifact but with an amended version.
         * @param ver the version
         * @return an amended artifact.
         */
        public PomArtifact withVersion(String ver) {
            return new PomArtifact(artifactId, groupId, ver);
        }
    }
}
