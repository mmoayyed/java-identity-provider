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
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.logic.Constraint;

import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;


/**
 * Adaptor that wraps a {@link ProfileAction} with a Spring Web Flow compatible action implementation
 * so that it can be executed as part of a flow.
 */
@ThreadSafe
public class WebFlowProfileActionAdaptor extends AbstractProfileAction {
    
    /** A POJO bean being adapted.  */
    @Nonnull private final ProfileAction action;
    
    /**
     * Constructor.
     * 
     * @param profileAction the POJO bean to adapt to Web Flow use
     */
    public WebFlowProfileActionAdaptor(@Nonnull final ProfileAction profileAction) {
        action = Constraint.isNotNull(profileAction, "ProfileAction cannot be null");
    }

    /** {@inheritDoc} */
    @Override
    public void execute(@Nonnull final ProfileRequestContext profileRequestContext) {
        action.execute(profileRequestContext);
    }

}