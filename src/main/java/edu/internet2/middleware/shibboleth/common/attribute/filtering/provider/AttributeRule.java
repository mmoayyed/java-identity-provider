/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider;

/**
 * Represents a value filtering rule for a particular attribute.
 */
public class AttributeRule {

    /** Unique ID of the attribute this rule applies to. */
    private String attributeId;
    
    /** Filter that permits the release of attribute values. */
    private MatchFunctor permitValueRule;
    
    /** Filter that denies the release of attribute values. */
    private MatchFunctor denyValueRule;
    
    /**
     * Constructor.
     *
     * @param id unique ID of this rule
     */
    public AttributeRule(String id){
        attributeId = id;
    }
    
    /**
     * Gets the ID of the attribute to which this rule applies.
     * 
     * @return ID of the attribute to which this rule applies
     */
    public String getAttributeId(){
        return attributeId;
    }

    /**
     * Gets the filter that permits the release of attribute values.
     * 
     * @return filter that permits the release of attribute values
     */
    public MatchFunctor getPermitValueRule(){
        return permitValueRule;
    }
    
    /**
     * Sets the filter that permits the release of attribute values.
     * 
     * @param filter filter that permits the release of attribute values
     */
    public void setPermitValueRule(MatchFunctor filter){
        permitValueRule = filter;
    }
    
    /**
     * Gets the filter that denies the release of attribute values.
     * 
     * @return filter that denies the release of attribute values
     */
    public MatchFunctor getDenyValueRule() {
        return denyValueRule;
    }
    
    /**
     * Sets the filter that denies the release of attribute values.
     * 
     * @param filter filter that denies the release of attribute values
     */
    public void setDenyValueRule(MatchFunctor filter) {
        denyValueRule = filter;
    }
}