/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.consent.components;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;

/**
 *
 */

public class AttributeList implements Comparator<Attribute> {
    private List<String> orderedAttributeIds;
    private Collection<String> blacklistedAttributeIds;

    /**
     * @return Returns the ordererdAttributeIds.
     */
    public List<String> getOrderedAttributeIds() {
        return orderedAttributeIds;
    }

    /**
     * @param ordererdAttributeIds The ordererdAttributeIds to set.
     */
    public void setOrderedAttributeIds(List<String> orderedAttributeIds) {
        this.orderedAttributeIds = orderedAttributeIds;
    }

    /**
     * @return Returns the blacklistedAttributeIds.
     */
    public Collection<String> getBlacklistedAttributeIds() {
        return blacklistedAttributeIds;
    }

    /**
     * @param blacklistedAttributeIds The blacklistedAttributeIds to set.
     */
    public void setBlacklistedAttributeIds(Collection<String> blacklistedAttributeIds) {
        this.blacklistedAttributeIds = blacklistedAttributeIds;
    }

    /** {@inheritDoc} */
    public int compare(final Attribute attribute1, final Attribute attribute2) {
        int last = orderedAttributeIds.size();
        
        int rank1 = orderedAttributeIds.indexOf(attribute1.getId());
        int rank2 = orderedAttributeIds.indexOf(attribute2.getId());
        
        if (rank2 < 0) rank2 = last++;
        if (rank1 < 0) rank1 = last++;
        
        return rank1 - rank2;
    }
    
    private boolean isBlacklisted(final Attribute attribute) {
        return blacklistedAttributeIds.contains(attribute.getId());
    }
    
    public Collection<Attribute> removeBlacklisted(final Collection<Attribute> attributes) {
        final Collection<Attribute> attributesNotBlacklisted = new HashSet<Attribute>();
        
        for (Attribute attribute : attributes) {
            if (!isBlacklisted(attribute)) {
                attributesNotBlacklisted.add(attribute);
            }
        }
        
        return attributesNotBlacklisted;
    }
    
    public Collection<Attribute> sortAttributes(final Collection<Attribute> attributes) {
        final SortedSet<Attribute> sortedAttributes = new TreeSet<Attribute>(this);
        sortedAttributes.addAll(attributes);
        return sortedAttributes;
    }
}
