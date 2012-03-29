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

import javax.annotation.Nonnull;
import javax.security.auth.kerberos.KerberosTicket;

import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.messaging.context.BaseContext;

/**
 * Context, usually attached to {@link AuthenticationRequestContext}, that carries a {@link KerberosTicket} to be
 * validated.
 */
public class KerberosTicketContext extends BaseContext {

    /** Kerberos ticket to be validated. */
    private KerberosTicket ticket;

    /**
     * Gets the Kerberos ticket to be validated.
     * 
     * @return Kerberos ticket to be validated
     */
    @Nonnull public KerberosTicket getTicket() {
        return ticket;
    }
    
    /**
     * Sets the Kerberos ticket to be validated.
     * 
     * @param kerbTicket the Kerberos ticket to be validated
     */
    public void setTicket(@Nonnull final KerberosTicket kerbTicket){
        ticket = Assert.isNotNull(kerbTicket, "Kerberos ticket can not be null");
    }
}