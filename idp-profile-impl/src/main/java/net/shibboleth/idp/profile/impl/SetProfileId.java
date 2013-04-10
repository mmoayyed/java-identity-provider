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

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.ActionSupport;
import org.opensaml.profile.ProfileException;
import org.opensaml.profile.context.ProfileRequestContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** A profile action that sets the ID of the profile in use. */
public class SetProfileId extends AbstractProfileAction {

    /** ID of the profile in use. */
    private final String profileId;

    /**
     * Constructor.
     * 
     * @param id ID of the profile in use
     */
    public SetProfileId(@Nonnull @NotEmpty final String id) {
        super();

        profileId = Constraint.isNotNull(StringSupport.trimOrNull(id), "Profile ID can not be null or empty");
    }

    /**
     * Gets the ID of the profile in use.
     * 
     * @return ID of the profile in use
     */
    public String getProfileId() {
        return profileId;
    }

    /** {@inheritDoc} */
    protected Event
            doExecute(@Nonnull final RequestContext springRequestContext,
                    @Nonnull final ProfileRequestContext profileRequestContext) throws ProfileException {
        profileRequestContext.setProfileId(profileId);
        return ActionSupport.buildProceedEvent(this);
    }
}