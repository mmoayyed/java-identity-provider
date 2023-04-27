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

import javax.annotation.Nonnull;

import org.opensaml.saml.common.profile.logic.EntityAttributesPredicate.Candidate;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;

/**
 * Deprecated stub for compatibility.
 * 
 * @deprecated
 */
@Deprecated(since="5.0.0", forRemoval=true)
@SuppressWarnings("null")
public class MappedEntityAttributesPredicate
        extends net.shibboleth.saml.profile.context.logic.MappedEntityAttributesPredicate {

    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     */
    public MappedEntityAttributesPredicate(
            @Nonnull @ParameterName(name="candidates") final Collection<Candidate> candidates) {
        super(candidates);
        
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                net.shibboleth.saml.profile.context.logic.MappedEntityAttributesPredicate.class.getName());
    }

    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     */
    public MappedEntityAttributesPredicate(
            @Nonnull @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim) {
        super(candidates, trim);
        
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                net.shibboleth.saml.profile.context.logic.MappedEntityAttributesPredicate.class.getName());
    }
    
    /**
     * Constructor.
     * 
     * @param candidates the {@link Candidate} criteria to check for
     * @param trim true iff the values found in the metadata should be trimmed before comparison
     * @param all true iff all the criteria must match to be a successful test
     */
    public MappedEntityAttributesPredicate(
            @Nonnull @ParameterName(name="candidates") final Collection<Candidate> candidates,
            @ParameterName(name="trim") final boolean trim,
            @ParameterName(name="all") final boolean all) {
        super(candidates, trim, all);
        
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                net.shibboleth.saml.profile.context.logic.MappedEntityAttributesPredicate.class.getName());
    }

}