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

package net.shibboleth.idp.session;

import org.opensaml.messaging.context.AbstractSubcontextContainer;
import org.opensaml.util.Assert;
import org.opensaml.util.StringSupport;

//TODO implement hashCode/equals - need to implement this for AbstractSubcontextContainer as well

/**
 * Describes a session with a service associated with an {@link IdPSession}.
 * 
 * Properties of this object <strong>must not</strong> be modifiable directly. Instead, use the modification methods
 * available via the {@link SessionManager} that created the associate {@link IdPSession}.
 */
public class ServiceSession extends AbstractSubcontextContainer {

    /** The unique identifier of the service. */
    private String serviceId;

    /**
     * Gets the unique identifier of the service.
     * 
     * @return unique identifier of the service, never null nor empty
     */
    public String getServiceId() {
        return serviceId;
    }

    /**
     * Sets the unique identifier of the service.
     * 
     * @param id unique identifier of the service, never null nor empty
     */
    protected void setServiceId(String id) {
        String trimmedId = StringSupport.trimOrNull(id);
        Assert.isNotNull(trimmedId, "Service ID can not be null nor empty");
        serviceId = trimmedId;
    }
    
    
}