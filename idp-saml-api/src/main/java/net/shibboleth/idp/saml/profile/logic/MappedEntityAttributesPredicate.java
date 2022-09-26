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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate;
import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate.Candidate;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import net.shibboleth.idp.attribute.AttributesMapContainer;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue.EmptyType;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.StringSupport;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.Predicate;

/**
 * Extended version of EntityAttributes-driven predicate that uses an optimization to check
 * for mapped attributes in an {@link AttributesMapContainer} structure.
 */
public class MappedEntityAttributesPredicate extends EntityAttributesPredicate {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(MappedEntityAttributesPredicate.class);
    
    /** Delimiter to build string form of scoped values. */
    @Nonnull @NotEmpty private String scopeDelimiter = "@";
    
    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     */
    public MappedEntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates) {
        super(candidates);

        Constraint.isTrue(Iterables.all(candidates, c -> c.getNameFormat() == null),
                "Use of nameFormat property is impermissible with MappedEntityAttributesPredicate");
    }

    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     */
    public MappedEntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim) {
        super(candidates, trim);
        
        Constraint.isTrue(Iterables.all(candidates, c -> c.getNameFormat() == null),
                "Use of nameFormat property is impermissible with MappedEntityAttributesPredicate");
    }
    
    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     * @param all true iff all the criteria must match to be a successful test
     */
    public MappedEntityAttributesPredicate(
            @Nonnull @NonnullElements @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim,
            @ParameterName(name="all") final boolean all) {
        super(candidates, trim, all);
        
        Constraint.isTrue(Iterables.all(candidates, c -> c.getNameFormat() == null),
                "Use of nameFormat property is impermissible with MappedEntityAttributesPredicate");
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
        
        if (getCandidates().isEmpty()) {
            return true;
        } else if (input == null) {
            return false;
        }
        
        final Collection<Candidate> candidates = new ArrayList<>(getCandidates());

        if (doTest(input, input.getEntityID(), candidates)) {
            
            // At least one match. Check if sufficient.
            if (!getMatchAll() || candidates.isEmpty()) {
                return true;
            }
        }
        
        XMLObject parent = input.getParent();
        while (parent instanceof EntitiesDescriptor) {
            if (doTest(parent, ((EntitiesDescriptor) parent).getName(), candidates)) {
                
                // At least one match. Check if sufficient.
                if (!getMatchAll() || candidates.isEmpty()) {
                    return true;
                }
            }
            parent = parent.getParent();
        }
        
        return false;
    }
    
    /**
     * Evaluate the input object's attached object metadata against the supplied candidates.
     * 
     * <p>Any candidates that match will be removed from the input collection.</p>
     * 
     * @param input input object
     * @param name label for logging
     * @param candidates candidates to check
     * 
     * @return true iff the attached object metadata matched at least one input candidate
     */
    private boolean doTest(@Nullable final XMLObject input, @Nullable final String name,
            @Nonnull @NonnullElements final Collection<Candidate> candidates) {
        final List<AttributesMapContainer> containerList =
                input.getObjectMetadata().get(AttributesMapContainer.class);
        if (null == containerList || containerList.isEmpty() || containerList.get(0).get() == null ||
                containerList.get(0).get().isEmpty()) {
            log.trace("No mapped Entity Attributes for {}", name);
            return false;
        }
        
        final Multimap<String,? extends IdPAttribute> entityAttributes = containerList.get(0).get();
        
        log.trace("Checking for match against {} Entity Attributes for {}", entityAttributes.size(),
                name);
        
        // Remove each candidate that matches. Tag values are OR'd for matching purposes.
        // Return true iff at least one candidate matches.
        return candidates.removeIf(new EntityAttributesMatcher(entityAttributes));
    }

    /**
     * Determines whether an {@link Candidate} criterion is satisfied by the mapped
     * attributes in an entity's metadata.
     */
    private class EntityAttributesMatcher implements Predicate<Candidate> {
        
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
                                log.trace("Matched mapped Entity Attribute ({}) value {}", a.getId(), tagvalstr);
                                valflags[tagindex] = true;
                                break;
                            } else if (getTrimTags()) {
                                if (tagvalstr.equals(cvalstr.trim())) {
                                    log.trace("Matched mapped Entity Attribute ({}) value {}", a.getId(), tagvalstr);
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
                                log.trace("Matched mapped Entity Attribute ({}) value {}", a.getId(), cvalstr);
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
