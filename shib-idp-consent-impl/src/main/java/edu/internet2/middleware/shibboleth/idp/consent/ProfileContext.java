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

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.DateTime;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;


public class ProfileContext {
       
    private final String entityId;
    private final String principalName;
    private final Map<String, BaseAttribute> attributes;
    private final Locale locale;
    private final DateTime accessDate;
    
    public ProfileContext(final String principalName, final String entityId, Map<String, BaseAttribute> attributes, final DateTime accessDate, final Locale locale) {
        this.principalName = principalName;
        this.entityId = entityId;
        this.attributes = attributes;
        this.locale = locale;
        this.accessDate = accessDate;
    }
    
    public String getEntityID() {
        return entityId;
    }

    public Map<String, BaseAttribute> getReleasedAttributes() {
        return attributes;
    }
    
    public String getPrincipalName() {
        return principalName;
    }
    
    public Locale getLocale() {
        return locale;
    }
    
    public DateTime getAccessDate() {
        return accessDate;
    }
}
