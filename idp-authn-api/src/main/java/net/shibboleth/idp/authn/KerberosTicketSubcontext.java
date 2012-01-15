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

import javax.security.auth.kerberos.KerberosTicket;

import net.shibboleth.utilities.java.support.logic.Assert;

import org.opensaml.messaging.context.AbstractSubcontext;
import org.opensaml.messaging.context.SubcontextContainer;

/**
 * Context, usually attached to {@link AuthenticationRequestContext}, that carries a {@link KerberosTicket} to be
 * validated.
 */
public class KerberosTicketSubcontext extends AbstractSubcontext {

    /** Kerberos ticket to be validated. */
    private final KerberosTicket ticket;

    /**
     * Constructor.
     * 
     * @param owner the context which owns this one, may be null
     * @param kerbTicket the Kerberos ticket to be validated, never null
     */
    public KerberosTicketSubcontext(final SubcontextContainer owner, final KerberosTicket kerbTicket) {
        super(owner);
        ticket = Assert.isNotNull(kerbTicket, "Kerberos ticket can not be null");
    }

    /**
     * Gets the Kerberos ticket to be validated.
     * 
     * @return Kerberos ticket to be validated, never null
     */
    public KerberosTicket getTicket() {
        return ticket;
    }
}