/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import net.shibboleth.idp.authn.context.UserAgentContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.logic.BrowserProfilePredicate;

import jakarta.servlet.http.HttpServletRequest;

/**
 * An action that conditionally populates a {@link UserAgentContext} as a child of the {@link ProfileRequestContext}.
 * By default, the action is activated by a {@link BrowserProfilePredicate} condition such that only browser profiles
 * result in the creation of a {@link UserAgentContext}.
 *
 * @event {@link org.opensaml.profile.action.EventIds#PROCEED_EVENT_ID}
 */
public class PopulateUserAgentContext extends AbstractProfileAction {

    /** Constructor. */
    public PopulateUserAgentContext() {
        setActivationCondition(new BrowserProfilePredicate());
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        final UserAgentContext uac = new UserAgentContext();
        final HttpServletRequest request = getHttpServletRequest();
        assert request!= null;
        uac.setIdentifier(request.getHeader("User-Agent"));
        profileRequestContext.addSubcontext(uac);
    }
}
