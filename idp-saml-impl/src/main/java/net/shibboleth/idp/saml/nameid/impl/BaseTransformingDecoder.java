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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/** Regular expression, etc. transform of an identifier. */
public abstract class BaseTransformingDecoder extends AbstractIdentifiableInitializableComponent {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseTransformingDecoder.class);
    
    /** Match patterns and replacement strings to apply. */
    @Nonnull private List<Pair<Pattern,String>> transforms;
    
    /** Convert to uppercase prior to transforms? */
    private boolean uppercase;
    
    /** Convert to lowercase prior to transforms? */
    private boolean lowercase;
    
    /** Constructor. */
    public BaseTransformingDecoder() {
        transforms = CollectionSupport.emptyList();
    }
    
    /**
     * Controls conversion to uppercase prior to applying any transforms.
     * 
     * @param flag  uppercase flag
     * 
     * @since 4.1.0
     */
    public void setUppercase(final boolean flag) {
        checkSetterPreconditions();
        uppercase = flag;
    }

    /**
     * Controls conversion to lowercase prior to applying any transforms.
     * 
     * @param flag lowercase flag
     * 
     * @since 4.1.0
     */
    public void setLowercase(final boolean flag) {
        checkSetterPreconditions();
        lowercase = flag;
    }
    
    /**
     * A collection of regular expression and replacement pairs.
     * 
     * @param newTransforms collection of replacement transforms
     */
    public void setTransforms(@Nonnull final Collection<Pair<String,String>> newTransforms) {
        checkSetterPreconditions();
        Constraint.isNotNull(newTransforms, "Transforms collection cannot be null");
        
        transforms = new ArrayList<>();
        for (final Pair<String,String> p : newTransforms) {
            final Pattern pattern = Pattern.compile(StringSupport.trimOrNull(p.getFirst()));
            transforms.add(new Pair<>(pattern, Constraint.isNotNull(
                    StringSupport.trimOrNull(p.getSecond()), "Replacement expression cannot be null")));
        }
    }
    
    /**
     * Apply configured transforms to input identifier.
     * 
     * @param id the identifier to transform
     * @return transformed value
     */
    @Nullable protected String decode(@Nonnull @NotEmpty final String id) {
        checkComponentActive();
        String s = id;
        
        if (lowercase) {
            log.debug("Converting input string '{}' to lowercase", s);
            s = s.toLowerCase();
        } else if (uppercase) {
            log.debug("Converting input string '{}' to uppercase", s);
            s = s.toUpperCase();
        }
        
        if (transforms.isEmpty()) {
            return s;
        }
        
        for (final Pair<Pattern,String> p : transforms) {
            @Nonnull final Pattern first = Constraint.isNotNull(p.getFirst(), "Transforms did not contain pattern");
            final Matcher m = first.matcher(s);
            log.debug("Applying replacement expression '{}' against input '{}'", first.pattern(), s);
            s = m.replaceAll(p.getSecond());
            log.debug("Result of replacement is '{}'", s);
        }
        
        return s;
    }
    
}