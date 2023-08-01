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

package net.shibboleth.idp.saml.profile.testing;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.shared.component.ComponentInitializationException;

import javax.annotation.Nonnull;

import org.opensaml.profile.context.ProfileRequestContext;
import org.springframework.webflow.execution.Action;

/**
 * A Test {"link {@link Action}. 
 */
public class ActionTestSupportAction extends AbstractProfileAction {

    /** Constructor. 
     * @throws ComponentInitializationException if initialization fails
     */
    public ActionTestSupportAction() throws ComponentInitializationException {
        initialize();
    }

    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull ProfileRequestContext profileRequestContext) {

    }
    
}