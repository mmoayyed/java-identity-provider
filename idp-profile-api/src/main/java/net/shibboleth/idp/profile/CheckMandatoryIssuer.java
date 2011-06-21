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

package net.shibboleth.idp.profile;

import org.opensaml.messaging.context.BasicMessageMetadataSubcontext;
import org.springframework.webflow.execution.Event;
import org.springframework.webflow.execution.RequestContext;

/** Checks that the incoming message has an issuer. */
public class CheckMandatoryIssuer extends AbstractInboundMessageSubcontextAction<BasicMessageMetadataSubcontext> {

    /** Constructor. The ID of this component is set to the name of this class. */
    public CheckMandatoryIssuer(){
        super(CheckMandatoryIssuer.class.getName());
    }
    
    /**
     * Constructor.
     * 
     * @param componentId unique ID for this component
     */
    public CheckMandatoryIssuer(String componentId) {
        super(componentId);
    }

    /** {@inheritDoc} */
    public Class<BasicMessageMetadataSubcontext> getSubcontextType() {
        return BasicMessageMetadataSubcontext.class;
    }

    /** {@inheritDoc} */
    public Event doExecute(RequestContext springRequestContext, ProfileRequestContext profileRequestContext,
            BasicMessageMetadataSubcontext messageSubcontext) {
        
        if (messageSubcontext.getMessageIssuer() == null) {
            // TODO ERROR
        }

        return ActionSupport.buildEvent(this, ActionSupport.PROCEED_EVENT_ID, null);
    }
}