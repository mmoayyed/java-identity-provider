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

package edu.internet2.middleware.shibboleth.idp.consent.logic;

import java.util.Collection;
import java.util.regex.Pattern;

import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class RelyingPartyBlacklist {
    
    private Collection<String> blacklist;
    
    /**
     * @return Returns the blacklist.
     */
    public Collection<String> getBlacklist() {
        return blacklist;
    }

    /**
     * @param blacklist The blacklist to set.
     */
    public void setBlacklist(Collection<String> blacklist) {
        this.blacklist = blacklist;
    }

    public boolean contains(final RelyingParty relyingParty) {
        Pattern pattern;        
        for (String regex : blacklist) {
            pattern = Pattern.compile(regex);
            if (pattern.matcher(relyingParty.getEntityId()).find()) {
                return true;
            }
        }
        return false;
    }
}