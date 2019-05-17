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

package net.shibboleth.idp.saml.profile.logic;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.Predicate;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Extended version of EntityAttributes-driven predicate that adds an optimization to check
 * for mapped attributes in an {@link AttributesMapContainer} structure before backing off
 * to brute force examination of the metadata by the superclass.
 */
public class EntityAttributesPredicate extends org.opensaml.saml.common.profile.logic.EntityAttributesPredicate {

    /** Determines whether we can check the mapped container. */
    private final boolean nonOptimized;
    
    /** Delimiter to build string form of scoped values. */
    @Nonnull @NotEmpty private String scopeDelimiter = "@";
    
    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     */
    public EntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates) {
        super(candidates);

        // matchAll is false, so a single Candidate with no NameFormat might work.
        nonOptimized = Iterables.all(candidates, c -> c.getNameFormat() != null);
    }

    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     */
    public EntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim) {
        super(candidates, trim);
        
        // matchAll is false, so a single Candidate with no NameFormat might work.
        nonOptimized = Iterables.all(candidates, c -> c.getNameFormat() != null);
    }
    
    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     * @param all true iff all the criteria must match to be a successful test
     */
    public EntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim,
            @ParameterName(name="all") final boolean all) {
        super(candidates, trim, all);
        
        // matchAll is true, so a single Candidate with a NameFormat prevents optimization
        nonOptimized = Iterables.any(candidates, c -> c.getNameFormat() != null);
    }
    
    /**
     * Set delimiter for constructing scoped values for comparison.
     * 
     * <p>Defaults to '@'.</p>
     * 
     * @param delimiter delimiter to use
     */
    public void setScopeDelimiter(@Nonnull @NotEmpty final String delimiter) {
        scopeDelimiter = Constraint.isNotNull(StringSupport.trimOrNull(delimiter),
                "Scope delimiter cannot be null or empty");
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean test(@Nullable final EntityDescriptor input) {
        
        if (input == null) {
            return false;
        } else if (nonOptimized) {
            return super.test(input);
        }

        final List<AttributesMapContainer> containerList =
                input.getObjectMetadata().get(AttributesMapContainer.class);
        if (null != containerList && !containerList.isEmpty() && containerList.get(0).get() != null &&
                !containerList.get(0).get().isEmpty()) {
            
            // If we find a matching tag, we win. Each tag is treated in OR fashion.
            final EntityAttributesMatcher matcher = new EntityAttributesMatcher(containerList.get(0).get());
            
            if (getMatchAll()) {
                if (Iterables.all(getCandidates(), matcher::test)) {
                    return true;
                }
            } else {
                if (Iterables.tryFind(getCandidates(), matcher::test).isPresent()) {
                    return true;
                }
            }
        }
        
        return super.test(input);
    }

    /**
     * Determines whether an {@link Candidate} criterion is satisfied by the mapped
     * attributes in an entity's metadata.
     */
    private class EntityAttributesMatcher implements Predicate<Candidate> {
        
        /** Class logger. */
        @Nonnull private final Logger log = LoggerFactory.getLogger(EntityAttributesPredicate.class);
        
        /** Population to evaluate for a match. */
        @Nonnull private final Multimap<String,? extends IdPAttribute> attributes;
        
        /**
         * Constructor.
         *
         * @param attrs population to evaluate for a match
         */
        public EntityAttributesMatcher(@Nonnull @NonnullElements final Multimap<String,? extends IdPAttribute> attrs) {
            attributes = Constraint.isNotNull(attrs, "Extension attributes cannot be null");
        }

// Checkstyle: CyclomaticComplexity OFF
        /** {@inheritDoc} */
        public boolean test(@Nonnull final Candidate input) {
            
            if (input.getNameFormat() != null) {
                return false;
            }
            
            final List<String> tagvals = input.getValues();
            final List<Pattern> tagexps = input.getRegexps();

            // Track whether we've found every match we need (possibly with arrays of 0 size).
            final boolean[] valflags = new boolean[tagvals.size()];
            final boolean[] expflags = new boolean[tagexps.size()];

            // Check each attribute/tag in the populated set.
            for (final IdPAttribute a : attributes.get(input.getName())) {

                // Check each tag value's simple content for a value match.
                for (int tagindex = 0; tagindex < tagvals.size(); ++tagindex) {
                    final String tagvalstr = tagvals.get(tagindex);

                    for (final IdPAttributeValue cval : a.getValues()) {
                        final String cvalstr = attributeValueToString(cval);
                        if (tagvalstr != null && cvalstr != null) {
                            if (tagvalstr.equals(cvalstr)) {
                                valflags[tagindex] = true;
                                break;
                            } else if (getTrimTags()) {
                                if (tagvalstr.equals(cvalstr.trim())) {
                                    valflags[tagindex] = true;
                                    break;
                                }
                            }
                        }
                    }
                }

                // Check each tag regular expression for a match.
                for (int tagindex = 0; tagindex < tagexps.size(); ++tagindex) {

                    for (final IdPAttributeValue cval : a.getValues()) {
                        final String cvalstr = attributeValueToString(cval);
                        if (tagexps.get(tagindex) != null && cvalstr != null) {
                            if (tagexps.get(tagindex).matcher(cvalstr).matches()) {
                                expflags[tagindex] = true;
                                break;
                            }
                        }
                    }
                }
            }

            for (final boolean flag : valflags) {
                if (!flag) {
                    return false;
                }
            }

            for (final boolean flag : expflags) {
                if (!flag) {
                    return false;
                }
            }

            return true;
        }
// Checkstyle: CyclomaticComplexity ON
     
        /**
         * Convert an IdPAttributeValue to a String if the type is recognized.
         * 
         * @param value object to convert
         * @return the converted value, or null
         */
        @Nullable private String attributeValueToString(@Nonnull final IdPAttributeValue value) {
            
            if (value instanceof ScopedStringAttributeValue) {
                return ((ScopedStringAttributeValue) value).getValue() + scopeDelimiter +
                        ((ScopedStringAttributeValue) value).getScope();
            } else if (value instanceof StringAttributeValue) {
                return ((StringAttributeValue) value).getValue();
            } else if (value instanceof EmptyAttributeValue) {
                final EmptyType empty = ((EmptyAttributeValue) value).getValue();
                return EmptyType.ZERO_LENGTH_VALUE.equals(empty) ? "" : null;
            }
            return null;
        }
    }

}
