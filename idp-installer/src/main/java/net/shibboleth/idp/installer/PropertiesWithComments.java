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

package net.shibboleth.idp.installer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A package which is similar to Properties, but allows comments to be preserved. We use the Properties package to parse
 * the non-comment lines.
 */
public final class PropertiesWithComments {

    /**
     * The contents.
     * 
     * Each {@link Object} is either a string (a non-property line) or a {@link CommentedProperty}
     * (an optionally commented property definition).
     */
    private List<Object> contents;

    /** The properties bit. */
    private Map<String, CommentedProperty> properties;

    /** Name Replacement info. */
    private final Properties nameReplacement;

    /**  BlackListed property names. */
    @Nonnull private final Set<String> unreplacableNames;

    /** Have we loaded data?.
     *
     * We cannot load the replacement names after the file load.
     * */
    private boolean loadedData;

    /** Legacy Constructor. */
    public PropertiesWithComments() {
        this(Collections.emptySet());
    }

    /** Constructor.
     * @param unreplacable names to warn on.
     */
    public PropertiesWithComments(@Nonnull final Set<String> unreplacable) {
        unreplacableNames = Set.copyOf(unreplacable);
        nameReplacement = new Properties();
    }

    /**
     * Add a property, either as a key/value pair or as a key/comment pair.
     * 
     * @param line what to look at
     * @param isComment whether this is a comment or not.
     * @throws IOException when badness happens.
     */
    private void addCommentedProperty(@Nonnull @NotEmpty final String line, final boolean isComment)
            throws IOException {
        final Properties parser = new Properties();
        final String modifiedLine;

        if (isComment) {
            modifiedLine = line.substring(1);
        } else {
            modifiedLine = line;
        }

        parser.load(new ByteArrayInputStream(modifiedLine.getBytes()));
        if (!parser.isEmpty()) {
            String propName = StringSupport.trimOrNull(parser.stringPropertyNames().iterator().next());
            if (propName != null) {
                
                String outputLine = line;
                final String value = parser.getProperty(propName);
                
                final String newPropName = StringSupport.trimOrNull(nameReplacement.getProperty(propName));

                if (newPropName != null && !newPropName.isEmpty()) {
                    // Change the line
                    if (isComment) {
                        if (newPropName.contains(propName)) {
                            // We can only replace once
                            outputLine = outputLine.replace(propName, newPropName);
                        } else {
                            while (outputLine.contains(propName)) {
                                outputLine = outputLine.replace(propName, newPropName);
                            }
                        }
                    }
                    // and the property name
                    propName = newPropName;
                }
                
                
                final CommentedProperty commentedProperty;

                if (isComment) {
                    commentedProperty = new CommentedProperty(propName, outputLine, true);

                } else {
                    commentedProperty = new CommentedProperty(propName, value, false);

                }
                properties.put(propName, commentedProperty);
                contents.add(commentedProperty);
            }
        } else {
            contents.add(line);
        }
        parser.clear();
    }
    
    /** Read the name replacement data. 
    * 
    * @param input what to read
    * @throws IOException if readline fails
    */
    public void loadNameReplacement(final InputStream input) throws IOException {
        if (loadedData) {
            throw new IOException("Cannot load name replacement after the data");
        }
        nameReplacement.load(input);
    }

    /**
     * Read the input stream into our structures.
     * 
     * @param input what to read
     * @throws IOException if readline fails
     */
    public void load(final InputStream input) throws IOException {
        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            contents = new ArrayList<>();
            properties = new HashMap<>();
    
            String s = reader.readLine();
    
            while (s != null) {
                final String what = StringSupport.trimOrNull(s);
                if (what == null) {
                    contents.add("");
                } else if (what.startsWith("#")) {
                    if (what.contains("=")) {
                        addCommentedProperty(s, true);
                    } else {
                        contents.add(s);
                    }
                } else if (what.startsWith("--") || !what.contains("=")) {                    
                    contents.add(s);
                } else {
                    addCommentedProperty(s, false);
                }
                s = reader.readLine();
            }
            loadedData = true;
        }
    }

    /**
     * Put the output to the supplied stream.
     * 
     * @param output where to write
     * @throws IOException is the write fails
     */
    public void store(final OutputStream output) throws IOException {
        
        try (final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output))) {
            for (final Object o : contents) {
                if (o instanceof String) {
                    writer.write((String) o);
                } else if (o instanceof CommentedProperty) {
                    final CommentedProperty commentedProperty = (CommentedProperty) o;
                    commentedProperty.write(writer);
                }
                writer.newLine();
            }
            writer.flush();
        }
    }

    /** Perform a mass replacement from the supplied {@link Properties}.
     * @param replacements what to replace.
     */
    public void replaceProperties(final Properties replacements) {
        for (final Object propName:replacements.keySet()) {
            if (propName instanceof String) {
                final String name = (String) propName;
                replaceProperty(name, replacements.getProperty(name));
            }
        }
    }

    /**
     * Replace the supplied property or stuff it at the bottom of the list.
     * 
     * @param propName the name of the property to replace
     * @param newPropValue the value to replace
     * @return true if the property was replaced false if it was added
     */
    public boolean replaceProperty(final String propName, final String newPropValue) {

        Constraint.isFalse(unreplacableNames.contains(propName),
                "property '" + propName + "' cannot be replaced");
        CommentedProperty p = properties.get(propName);
        if (null != p) {
            p.setValue(newPropValue);
            return true;
        }
        p = new CommentedProperty(propName, newPropValue, false);
        contents.add(p);
        properties.put(propName, p);
        return false;
    }

    /**
     * Append a comment to the list.
     * 
     * @param what what to add
     */
    public void addComment(final String what) {
        contents.add("# " + what);
    }

    /**
     * A POJO which looks like a property.
     * 
     * It may be a commented property from a line like this "#prop=value" or a property prop=value.
     * 
     */
    private static class CommentedProperty {

        /** The property name. */
        private final String property;

        /** The value - or the entire line if this is a comment. */
        private String value;

        /** Whether this is a comment or a value. */
        private boolean isComment;

        /**
         * Constructor.
         * 
         * @param prop the property name.
         * @param val the value or the entire line if this was a comment.
         * @param comment whether this is a comment.
         */
        CommentedProperty(final String prop, final String val, final boolean comment) {
            property = prop;
            value = val;
            isComment = comment;
        }

        /**
         * Set a new value.
         * 
         * @param newValue what to set
         */
        protected void setValue(final String newValue) {
            value = newValue;
            isComment = false;
        }

        /**
         * Write ourselves to the writer.
         * 
         * @param writer what to write with
         * @throws IOException from the writer
         */
        protected void write(final BufferedWriter writer) throws IOException {

            if (isComment) {
                writer.write(value);
            } else {
                writer.write(property);
                writer.write("=");
                writer.write(value);
            }
        }
    }
}
