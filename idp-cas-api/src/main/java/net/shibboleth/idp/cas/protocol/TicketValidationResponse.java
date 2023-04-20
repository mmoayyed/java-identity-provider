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

package net.shibboleth.idp.cas.protocol;

import net.shibboleth.idp.cas.attribute.Attribute;
import net.shibboleth.shared.annotation.constraint.NonnullElements;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.NotLive;
import net.shibboleth.shared.annotation.constraint.Unmodifiable;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.StringSupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Service ticket validation response protocol message.
 *
 * @author Marvin S. Addison
 */
public class TicketValidationResponse extends AbstractProtocolResponse {

    /** Subject principal on ticket validation success. */
    @Nullable private String userName;

    /** User attributes. */
    @Nonnull @NonnullElements private final List<Attribute> attributes;

    /** Proxy granting ticket IOU. */
    @Nullable private String pgtIou;

    /** Proxies traversed. */
    @Nonnull @NonnullElements private final List<String> proxies;

    /** Constructor. */
    public TicketValidationResponse() {
        attributes = new ArrayList<>();
        proxies = new ArrayList<>();
    }

    /**
     * Get the non-null subject principal on ticket validation success.
     * 
     * @return non-null subject principal on ticket validation success
     */
    @Nullable @NotEmpty public String getUserName() {
        return userName;
    }

    /**
     * Set the non-null subject principal on ticket validation success.
     * 
     * @param user non-null subject principal on ticket validation success
     */
    public void setUserName(@Nonnull @NotEmpty final String user) {
        userName = Constraint.isNotNull(user, "Username cannot be null");
    }

    /**
     * Get the immutable collection of user attributes.
     * 
     * @return immutable collection of user attributes
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public Collection<Attribute> getAttributes() {
        final Collection<Attribute> result = CollectionSupport.copyToList(attributes);
        assert result!=null;
        return result;
    }

    /**
     * Add an attribute to the attribute collection.
     * 
     * @param attribute the attribute
     */
    public void addAttribute(@Nonnull final Attribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Get the proxy granting ticket IOU.
     * 
     * @return proxy granting ticket IOU
     */
    @Nullable public String getPgtIou() {
        return pgtIou;
    }

    /**
     * Set the proxy granting ticket IOU.
     * 
     * @param iou proxy granting ticket IOU
     */
    public void setPgtIou(@Nullable final String iou) {
        pgtIou = StringSupport.trimOrNull(iou);
    }

    /**
     * Get the immutable list of proxies traversed in order of most recent to last recent.
     * 
     * @return immutable list of proxies traversed in order of most recent to last recent
     */
    @Nonnull @NonnullElements @NotLive @Unmodifiable public List<String> getProxies() {
        final List<String> result =  CollectionSupport.copyToList(proxies);
        assert result!=null;
        return result;
    }

    /**
     * Adds a proxy to the list of proxies traversed.
     *
     * @param proxy Name of a proxying service, typically a URI.
     */
    public void addProxy(@Nonnull final String proxy) {
        proxies.add(proxy);
    }
}
