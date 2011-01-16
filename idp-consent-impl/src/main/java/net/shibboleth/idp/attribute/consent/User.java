/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.attribute.consent;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import net.shibboleth.idp.attribute.Attribute;



/**
 *
 */
public class User {
    
    private final String id;
    private boolean globalConsent;
    private final Map<String, Collection<AttributeRelease>> releases;

    public User(final String id, final boolean globalConsent) {
    	this.id = id;
    	this.globalConsent = globalConsent;
    	this.releases = new HashMap<String, Collection<AttributeRelease>>();    	
    }

    /**
     * @return Returns the globalConsent.
     */
    public boolean hasGlobalConsent() {
        return globalConsent;
    }

    /**
     * @param globalConsent The globalConsent to set.
     */
    public void setGlobalConsent(boolean globalConsent) {
        this.globalConsent = globalConsent;
    }

    /**
     * @return Returns the userId.
     */
    public String getId() {
        return id;
    }
    
    public void setAttributeReleases(final String relyingPartyId, final Collection<AttributeRelease> attributeReleases) {
        releases.put(relyingPartyId, attributeReleases);
    }

    public Collection<AttributeRelease> getAttributeReleases(final String relyingPartyId) {
        return releases.get(relyingPartyId);
    }

    public boolean hasApprovedAttributes(final String relyingPartyId,  Collection<Attribute<?>> attributes) {
        Collection<AttributeRelease> attributeReleases = getAttributeReleases(relyingPartyId);
        if (attributeReleases == null) {
            return false;
        }

        boolean approved;
        for (Attribute<?> attribute: attributes) {
            approved = false;
            for (AttributeRelease attributeRelease: attributeReleases) {
                if (attributeRelease.contains(attribute)) {
                        approved = true;
                        break;
                }
            }
            if (!approved) {
                return false;
            }
        }     
        return true;
    }

    @Override
    public String toString() {
        return id;
    }
}
