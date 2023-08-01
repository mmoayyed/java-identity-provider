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

package net.shibboleth.idp.cas.flow.impl;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventException;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.cas.protocol.ProtocolError;
import net.shibboleth.idp.cas.service.Service;
import net.shibboleth.idp.cas.service.ServiceContext;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.primitive.LoggerFactory;   

/**
 * Checks the current {@link ServiceContext} to determine whether the service/relying party is authorized to proxy.
 * Possible outcomes:
 * <ul>
 *     <li><code>null</code> on success</li>
 *     <li>{@link ProtocolError#ProxyNotAuthorized ProxyNotAuthorized}</li>
 * </ul>
 *
 * @param <RequestType> request
 * @param <ResponseType> response
 *
 * @author Marvin S. Addison
 */
public class CheckProxyAuthorizationAction<RequestType,ResponseType>
        extends AbstractCASProtocolAction<RequestType,ResponseType> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(CheckProxyAuthorizationAction.class);

    /** CAS service. */
    @NonnullBeforeExec private Service service;
    
    /** {@inheritDoc} */
    @Override
    protected boolean doPreExecute(@Nonnull final ProfileRequestContext profileRequestContext) {
        if (!super.doPreExecute(profileRequestContext)) {
            return false;
        }
        
        try {
            service = getCASService(profileRequestContext);
        } catch (final EventException e) {
            ActionSupport.buildEvent(profileRequestContext, e.getEventID());
            return false;
        }
        
        return true;
    }    
    
    /** {@inheritDoc} */
    @Override
    protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        if (!service.isAuthorizedToProxy()) {
            log.info("{} Service '{}' is not authorized to proxy", getLogPrefix(), service.getName());
            ActionSupport.buildEvent(profileRequestContext, ProtocolError.ProxyNotAuthorized.event(this));
        }
    }

}