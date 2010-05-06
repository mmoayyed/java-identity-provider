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

package edu.internet2.middleware.shibboleth.idp.consent;

import java.util.Collection;
import java.util.Locale;

import org.joda.time.DateTime;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class UserConsentContext {
    
    // TODO
    // Session?
    
    final private Principal principal;

    final private RelyingParty relyingParty;

    final private Collection<Attribute> attributes;
    
    final private DateTime accessDate;
    
    final private Locale locale;
    
    public UserConsentContext(Principal principal, RelyingParty relyingParty, Collection<Attribute> attributes, DateTime accessDate, Locale locale) {
    	this.principal = principal;
    	this.relyingParty = relyingParty;
    	this.attributes = attributes;
    	this.accessDate = accessDate;
    	this.locale = locale;
    }
    
    /**
     * @return Returns the attributes.
     */
    public Collection<Attribute> getAttributes() {
        return attributes;
    }

    /**
     * @return Returns the principal.
     */
    public Principal getPrincipal() {
        return principal;
    }

    /**
     * @return Returns the relyingParty.
     */
    public RelyingParty getRelyingParty() {
        return relyingParty;
    }
    
    /**
     * @return Returns the accessDate.
     */
    public DateTime getAccessDate() {
        return accessDate;
    }
    
    /**
     * @return Returns the locale.
     */
    public Locale getLocale() {
        return locale;
    }
    
    public String toString() {
        return "UserConsentContext [principal=" + principal + ", relyingParty=" + relyingParty + "]";
    }


}
