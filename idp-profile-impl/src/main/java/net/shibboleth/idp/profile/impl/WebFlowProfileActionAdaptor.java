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
import javax.annotation.concurrent.ThreadSafe;

import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.utilities.java.support.logic.Constraint;

import org.opensaml.profile.ProfileException;
import org.opensaml.profile.action.ProfileAction;
import org.opensaml.profile.context.ProfileRequestContext;


/**
 * Base class for IdP profile processing steps.
 * 
 * This base class takes care of the following things:
 * <ul>
 * <li>retrieving the {@link ProfileRequestContext} from the current request environment</li>
 * <li>ensuring the {@link javax.servlet.http.HttpServletRequest} and {@link javax.servlet.http.HttpServletResponse} is
 * available on the {@link ProfileRequestContext}</li>
 * <li>tracking performance metrics for the action</li>
 * </ul>
 * 
 * Action implementations should generally override
 * {@link #doExecute(HttpServletRequest, HttpServletResponse, ProfileRequestContext)}, however if an action needs access
 * to the Spring Webflow {@link RequestContext} it may override
 * {@link #doExecute(HttpServletRequest, HttpServletResponse, RequestContext, ProfileRequestContext)} instead. In
 * general, implementations should avoid doing this however.
 * 
 * @param <InboundMessageType> type of in-bound message
 * @param <OutboundMessageType> type of out-bound message
 */
@ThreadSafe
public class WebFlowProfileActionAdaptor<InboundMessageType, OutboundMessageType>
    extends AbstractProfileAction<InboundMessageType, OutboundMessageType> {

    /** A POJO bean being adapted.  */
    private final ProfileAction action;
    
    /**
     * Constructor.
     * 
     * @param profileAction the POJO bean to adapt to Web Flow use
     */
    public WebFlowProfileActionAdaptor(@Nonnull final ProfileAction profileAction) {
        super();
        
        action = Constraint.isNotNull(profileAction, "ProfileAction cannot be null");
    }

    /** {@inheritDoc} */
    @Nonnull protected void doExecute(
            @Nonnull final ProfileRequestContext<InboundMessageType, OutboundMessageType> profileRequestContext)
                    throws ProfileException {

        action.execute(profileRequestContext);
    }
}