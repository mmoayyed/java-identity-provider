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
import java.util.TreeSet;

import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;

/**
 *
 */
public class AttributeList extends TreeSet<Attribute> {

    /**
     * 
     */
    private static final long serialVersionUID = -2930469891722360921L;
    
    private final AttributeListConfiguration configuration;
    
    public AttributeList(final AttributeListConfiguration configuration, final Collection<Attribute> attributes) {        
        super(new Comparator<Attribute>() {
            public int compare(Attribute o1, Attribute o2) {
                int last = configuration.size();
                
                int rank1 = configuration.indexOf(o1);
                int rank2 = configuration.indexOf(o2);
                
                if (rank2 < 0) rank2 = last++;
                if (rank1 < 0) rank1 = last++;
                
                return rank1 - rank2;
            }});

        this.configuration = configuration;    
        addAll(attributes);
    }
    
    public boolean add(Attribute e) {
        if (!configuration.isBlacklisted(e)) {
            return super.add(e);
        }
        return false;
    }
}