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

import java.util.Set;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * Simple {@link SubjectCanonicalizer} implementations that returns a {@link UsernamePrincipal}
 * attached to the {@link Subject}.
 * 
 * <p>An exception is raised if more than one such Principal is attached.</p>
 */
public class SimpleSubjectCanonicalizer extends AbstractSubjectCanonicalizer {

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String doCanonicalize(@Nonnull final Subject subject)
            throws SubjectCanonicalizationException {
        Constraint.isNotNull(subject, "Subject cannot be null");
        
        Set<UsernamePrincipal> usernames = subject.getPrincipals(UsernamePrincipal.class);
        if (usernames == null || usernames.isEmpty()) {
            throw new SubjectCanonicalizationException("No UsernamePrincipals were found");
        } else if (usernames.size() > 1) {
            throw new SubjectCanonicalizationException("Multiple UsernamePrincipals were found");
        }
        
        return usernames.iterator().next().getName();
    }
    
}