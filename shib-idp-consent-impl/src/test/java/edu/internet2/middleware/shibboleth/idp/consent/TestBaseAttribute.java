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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.encoding.AttributeEncoder;

/**
 *
 */
public class TestBaseAttribute<ValueType> extends BaseAttribute<ValueType> {

    private String id;
    private Collection<ValueType> values;
    
    private Map<Locale, String> displayNames;
    private Map<Locale, String> displayDescriptions;
    
    
    public TestBaseAttribute(String id, Collection<ValueType> values) {
        super();
        this.id = id;
        this.values = values;
        displayNames = new HashMap<Locale, String>();
        displayDescriptions = new HashMap<Locale, String>();
    }
    
    public Map<Locale, String> getDisplayDescriptions() {
        return displayDescriptions;
    }

    public Map<Locale, String> getDisplayNames() {
        return displayNames;
    }

    /** {@inheritDoc} */
    public List<AttributeEncoder> getEncoders() {
        return null;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public Comparator<ValueType> getValueComparator() {
        return null;
    }

    /** {@inheritDoc} */
    public Collection<ValueType> getValues() {
        return values;
    }
    
    public void setDisplayName(Locale locale, String name) {
        displayNames.put(locale, name);
    }
    
    public void setDisplayDescription(Locale locale, String description) {
        displayDescriptions.put(locale, description);
    }
    

}
