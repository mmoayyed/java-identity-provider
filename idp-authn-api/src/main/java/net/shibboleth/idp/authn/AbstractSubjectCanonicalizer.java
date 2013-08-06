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

package net.shibboleth.idp.authn;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;

/**
 * Base class for {@link SubjectCanonicalizer} implementations.
 */
public abstract class AbstractSubjectCanonicalizer extends AbstractDestructableIdentifiableInitializableComponent
    implements SubjectCanonicalizer {

    /**
     * Constructor.
     * 
     * Initializes the ID of this action to the class name.
     */
    public AbstractSubjectCanonicalizer() {
        super();

        setId(getClass().getName());
    }

    /** {@inheritDoc} */
    public synchronized void setId(String componentId) {
        super.setId(componentId);
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String canonicalize(@Nonnull final Subject subject)
            throws SubjectCanonicalizationException {
        return doCanonicalize(subject);
    }
    
    /**
     * Implementations must override this method.
     * 
     * @param subject   the input to canonicalize
     * 
     * @return the canonical principal name
     * @throws SubjectCanonicalizationException if the subject cannot be turned into a principal name
     */
    @Nonnull @NotEmpty public abstract String doCanonicalize(@Nonnull final Subject subject)
            throws SubjectCanonicalizationException;
    
    /**
     * Return a prefix for logging messages for this component.
     * 
     * @return a string for insertion at the beginning of any log messages
     */
    @Nonnull protected String getLogPrefix() {
        return "Subject Canonicalizer " + getId() + ":";
    }

}