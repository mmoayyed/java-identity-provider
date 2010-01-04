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

package edu.internet2.middleware.shibboleth.idp.consent.entities;

import java.util.Set;

/**
 *
 */
public class Attribute {

    private String id;

    private Set<String> values;

    /**
     * @return Returns the id.
     */
    public String getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return Returns the values.
     */
    public Set<String> getValues() {
        return values;
    }

    /**
     * @param values The values to set.
     */
    public void setValues(Set<String> values) {
        this.values = values;
    }

    /**
     * @return Returns the hash of the values.
     */
    public int getValueHash() {
        return values.hashCode();
    }

    /** {@inheritDoc} */
    public boolean equals(Object object) {
        if (object instanceof Attribute) {
            return this.id == ((Attribute) object).getId()
                    && this.getValueHash() == ((Attribute) object).getValueHash();
        }
        return false;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "Attribute [id=" + id + ", values=" + values + "]";
    }

}
