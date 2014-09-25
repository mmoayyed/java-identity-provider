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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A package which is similar to Properties, but allows comments to be preserved. We use the Properties package to parse
 * the non comment lines
 */
public class PropertiesWithComments {

    /** The contents. The Object is either a string (the comment) or a {@link Pair<String, String>} (the property) */
    private List<Object> contents;

    /** The properties bit. */
    private Map<String, Pair<String, String>> properties;

    /**
     * Read the input stream into our structures.
     * 
     * @param input what to read
     * @throws IOException if readline fails
     */
    public void load(InputStream input) throws IOException {
        final Properties parser = new Properties();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        contents = new ArrayList<>();
        properties = new HashMap<>();

        String s = reader.readLine();

        while (s != null) {
            final String what = StringSupport.trimOrNull(s);
            if (what == null) {
                contents.add("");
            } else if (what.startsWith("#")) {
                contents.add(what);
            } else {
                parser.load(new ByteArrayInputStream(s.getBytes()));
                if (!parser.isEmpty()) {
                    final String propName = StringSupport.trimOrNull(parser.stringPropertyNames().iterator().next());
                    if (propName != null) {
                        final Pair<String, String> pair = new Pair<>();
                        pair.setFirst(propName);
                        pair.setSecond(parser.getProperty(propName));
                        properties.put(propName, pair);
                        contents.add(pair);
                    }
                }
                parser.clear();
            }
            s = reader.readLine();
        }
    }
    
    /** Put the output to the supplied stream.
     * @param output where to write
     * @throws IOException is the write fails
     */
    public void store(OutputStream output) throws IOException {
        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output));

        for (Object o : contents) {
            if (o instanceof String) {
                writer.write((String) o);
            } else if (o instanceof Pair) {
                Pair<String,String> p = (Pair)o;
                writer.write(p.getFirst());
                writer.write('=');
                writer.write(p.getSecond());
            }
            writer.newLine();
        }
        writer.flush();
        writer.close();
        output.close();
    }

    
    /** Replace the supplied property or stuff it at the bottom of the list.
     * @param propName the name of the property to replace
     * @param newPropValue the value to  replace
     * @return true if the property was replaced false if it was added
     */
    public boolean replaceProperty(String propName, String newPropValue) {
        
        Pair<String, String> p = properties.get(propName);
        if (null != p) {
            p.setSecond(newPropValue);
            return true;
        }
        p = new Pair<>();
        p.setFirst(propName);
        p.setSecond(newPropValue);
        
        contents.add(p);
        properties.put(propName, p);
        return false;
    }
    
    /** Append a comment to the list.
     * @param what what to add
     */
    public void addComment(String what) {
        contents.add("# "+what);
    }
}
