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
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.IdentifiableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;

/**
 * An interface that turns a Java {@link Subject} into a canonical principal name.
 * 
 * <p>Implementations of this interface must have knowledge of the various data structures
 * that can be attached to a subject.</p>
 */
public interface SubjectCanonicalizer extends IdentifiableComponent, InitializableComponent, DestructableComponent {

    /**
     * Rationalizes an arbitrarily-extensible {@link Subject} into a single, canonical
     * string representing the underlying security principal.
     * 
     * @param subject   the input to canonicalize
     * 
     * @return the canonical principal name
     * @throws SubjectCanonicalizationException if the subject cannot be turned into a principal name
     */
    @Nonnull @NotEmpty public String canonicalize(@Nonnull final Subject subject)
            throws SubjectCanonicalizationException;
}