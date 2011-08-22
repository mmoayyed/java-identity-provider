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

import java.util.Collection;
import java.util.Iterator;

import org.opensaml.messaging.context.Subcontext;
import org.opensaml.messaging.context.SubcontextContainer;
import org.opensaml.util.Assert;

/** Wrapper that allows an {@link IdPSession} to operate as a {@link Subcontext}. */
public class IdPSessionSubcontext extends IdPSession implements Subcontext {

    /** Context that owns this subcontext. */
    private SubcontextContainer owner;
    
    /** IdP session wrapped by this adapter. */
    private IdPSession session;
    
    /**
     * Constructor.
     *
     * @param owningContext context that will own this subcontext
     * @param idpSession IdP session wrapped by this adapter
     */
    public IdPSessionSubcontext(final SubcontextContainer owningContext, final IdPSession idpSession){
        Assert.isNotNull(owningContext, "Owning context can not be null");
        owner = owningContext;
        
        Assert.isNotNull(idpSession, "IdP session can not be null");
        session = idpSession;
    }
    
    /** {@inheritDoc} */
    public SubcontextContainer getOwner() {
        return owner;
    }

    /** {@inheritDoc} */
    public String getId() {
        return session.getId();
    }
    
    /** {@inheritDoc} */
    public byte[] getSecret() {
        return session.getSecret();
    }
    
    /** {@inheritDoc} */
    public long getCreationInstant() {
        return session.getCreationInstant();
    }
    
    /** {@inheritDoc} */
    public long getLastActivityInstant() {
        return session.getLastActivityInstant();
    }
    
    /** {@inheritDoc} */
    public Collection<AuthenticationEvent> getAuthenticateEvents() {
        return session.getAuthenticateEvents();
    }
    
    /** {@inheritDoc} */
    public Collection<AuthenticationEvent> getAuthenticationEvents(String authenticationMethod) {
        return session.getAuthenticationEvents(authenticationMethod);
    }
    
    /** {@inheritDoc} */
    public void addAuthenticationEvent(AuthenticationEvent event) {
        // TODO Auto-generated method stub
        super.addAuthenticationEvent(event);
    }
    
    /** {@inheritDoc} */
    public Collection<ServiceSession> getServiceSessions() {
        return session.getServiceSessions();
    }
    
    /** {@inheritDoc} */
    public ServiceSession getServiceSession(String serviceId) {
        return session.getServiceSession(serviceId);
    }
    
    /** {@inheritDoc} */
    public void addServiceSession(ServiceSession serviceSession) {
        session.addServiceSession(serviceSession);
    }
    
    /** {@inheritDoc} */
    public Collection<AuthenticationEvent> getAuthenticationEventForService(String serviceId,
            String authenticationMethod) {
        return session.getAuthenticationEventForService(serviceId, authenticationMethod);
    }
    
    /** {@inheritDoc} */
    public void addSubcontext(Subcontext subContext) {
        session.addSubcontext(subContext);
    }
    
    /** {@inheritDoc} */
    public void addSubcontext(Subcontext subContext, boolean replace) {
        session.addSubcontext(subContext, replace);
    }
    
    /** {@inheritDoc} */
    public void clearSubcontexts() {
        session.clearSubcontexts();
    }
    
    /** {@inheritDoc} */
    public <T extends Subcontext> boolean containsSubcontext(Class<T> clazz) {
        return session.containsSubcontext(clazz);
    }
    
    /** {@inheritDoc} */
    public <T extends Subcontext> T getSubcontext(Class<T> clazz) {
        return session.getSubcontext(clazz);
    }
    
    /** {@inheritDoc} */
    public <T extends Subcontext> T getSubcontext(Class<T> clazz, boolean autocreate) {
        return session.getSubcontext(clazz, autocreate);
    }
    
    /** {@inheritDoc} */
    public boolean isAutoCreateSubcontexts() {
        return session.isAutoCreateSubcontexts();
    }
    
    /** {@inheritDoc} */
    public Iterator<Subcontext> iterator() {
        return session.iterator();
    }
    
    /** {@inheritDoc} */
    public <T extends Subcontext> void removeSubcontext(Class<T> clazz) {
        session.removeSubcontext(clazz);
    }
    
    /** {@inheritDoc} */
    public void removeSubcontext(Subcontext subcontext) {
        session.removeSubcontext(subcontext);
    }
    
    /** {@inheritDoc} */
    public void setAutoCreateSubcontexts(boolean autoCreate) {
        session.setAutoCreateSubcontexts(autoCreate);
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        return session.hashCode();
    }
    
    /** {@inheritDoc} */
    public boolean equals(Object other) {
        return session.equals(other);
    }
}