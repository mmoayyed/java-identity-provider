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

package net.shibboleth.idp.saml.nameid;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

import net.shibboleth.idp.authn.SubjectCanonicalizationFlowDescriptor;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * A class used to describe flow descriptors for {@link net.shibboleth.idp.saml.authn.principal.NameIDPrincipal} and
 * {@link net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal} c14n. This adds the concept of formats to
 * the base class.
 */
public class NameIDCanonicalizationFlowDescriptor extends SubjectCanonicalizationFlowDescriptor {

    /** Store Set of acceptable formats. */
    @Nonnull private Set<String> formats;

    /** Constructor. */
    public NameIDCanonicalizationFlowDescriptor() {
        formats = CollectionSupport.emptySet();
    }
    
    /**
     * Return the set of acceptable formats.
     * 
     * @return Returns the formats. Never empty after initialization.
     */
    @Nonnull @Unmodifiable @NotLive public Collection<String> getFormats() {
        return formats;
    }

    /**
     * Sets the acceptable formats.
     * 
     * @param theFormats The formats to set.
     */
    public void setFormats(@Nonnull final Collection<String> theFormats) {
        formats = CollectionSupport.copyToSet(StringSupport.normalizeStringCollection(
                Constraint.isNotNull(theFormats, "Format collection cannot be null")));
    }
    
}