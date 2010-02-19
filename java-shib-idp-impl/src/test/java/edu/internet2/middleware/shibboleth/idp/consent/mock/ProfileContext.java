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

package edu.internet2.middleware.shibboleth.idp.consent.mock;

import java.util.Collection;

import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;

/**
 * Only a mock for testing
 */
public class ProfileContext {
    
    private RelyingParty relyingParty;
    private Collection<Attribute> releasedAttributes;
    /**
     * @return Returns the relyingParty.
     */
    public RelyingParty getRelyingParty() {
        return relyingParty;
    }
    /**
     * @return Returns the releasedAttributes.
     */
    public Collection<Attribute> getReleasedAttributes() {
        return releasedAttributes;
    }
    /**
     * @param releasedAttributes The releasedAttributes to set.
     */
    public void setReleasedAttributes(Collection<Attribute> releasedAttributes) {
        this.releasedAttributes = releasedAttributes;
    }
    /**
     * @param relyingParty The relyingParty to set.
     */
    public void setRelyingParty(RelyingParty relyingParty) {
        this.relyingParty = relyingParty;
    }

    

    
}
