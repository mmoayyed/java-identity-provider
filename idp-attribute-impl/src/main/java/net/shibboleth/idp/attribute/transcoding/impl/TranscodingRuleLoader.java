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

package net.shibboleth.idp.attribute.transcoding.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import net.shibboleth.idp.attribute.transcoding.TranscodingRule;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;

/**
 * Wrapper around a {@link Map} representing a rule for transcoding, used to
 * detect and load the rules at runtime from a Spring context.
 */
public class TranscodingRuleLoader {
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(TranscodingRuleLoader.class);
    
    /** Rules loaded. */
    private @Nonnull @NonnullElements final Collection<TranscodingRule> rules;
    
    /**
     * Load rules from all files found below a directory root.
     * 
     * <p>Individual rules that fail to load will be skipped.</p>
     * 
     * @param dir root to search
     * 
     * @throws IOException if an error occurs
     */
    public TranscodingRuleLoader(@Nonnull @ParameterName(name="dir") final Path dir) throws IOException {

        log.debug("Loading rules from directory ({})", dir);
        rules = new ArrayList<>();
        
        try (final DirectoryStream<Path> dirstream  = Files.newDirectoryStream(dir)) {
            for (final Path child : dirstream) {
                final File file =  child.toFile();
                if (file.isDirectory()) {
                    try {
                        rules.addAll(new TranscodingRuleLoader(child).getRules());
                    } catch (final IOException e) {
                        log.error("Failed to load rules from directory ({})", file, e);
                    }
                } else {
                    log.debug("Loading rule from property set in file ({})", file);
                    try {
                        rules.add(TranscodingRule.fromResource(new FileSystemResource(file)));
                    } catch (final IOException e) {
                        log.error("Failed to load rule from file ({})", file, e);
                    }
                }
            }
        }
    }

    /**
     * Get the rules loaded by this object.
     * 
     * @return collection of rules
     */
    @Nonnull @NonnullElements public Collection<TranscodingRule> getRules() {
        return rules;
    }
    
}