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

package net.shibboleth.idp.profile.logic;

import java.util.Collection;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.context.navigate.RelyingPartyIdLookupFunction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.logic.CollectionContainmentPredicate;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.opensaml.profile.context.ProfileRequestContext;

/**
 * Predicate that evaluates a {@link ProfileRequestContext} by looking for relying party ID
 * that matches one of a designated set. The ID is obtained from a lookup function, by default from
 * a {@link net.shibboleth.idp.profile.context.RelyingPartyContext} child of the profile request context.
 */
public class RelyingPartyIdPredicate extends CollectionContainmentPredicate<ProfileRequestContext,String> {

    /**
     * Constructor.
     * 
     * @param candidates hardwired set of values to check against
     */
    public RelyingPartyIdPredicate(@Nonnull @NonnullElements final Collection<String> candidates) {
        super(new RelyingPartyIdLookupFunction(), StringSupport.normalizeStringCollection(candidates));
    }

}