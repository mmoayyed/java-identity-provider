/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.filter.impl.saml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.shibboleth.utilities.java.support.primitive.StringSupport;


/**
 * Base class for matchers that perform an regular expression match of a given attribute string value against
 * entity attribute value.
 */
public abstract class AbstractEntityAttributeRegexMatcher extends AbstractEntityAttributeMatcher {
    
    /** The value of the entity attribute the entity must have. */
    private Pattern valueRegex;

    /**
     * Gets the value of the entity attribute the entity must have.
     * 
     * @return value of the entity attribute the entity must have
     */
    public Pattern getValueRegex() {
        return valueRegex;
    }

    /**
     * Sets the value of the entity attribute the entity must have.
     * 
     * @param attributeValueRegex value of the entity attribute the entity must have
     */
    public void setValueRegex(Pattern attributeValueRegex) {
        valueRegex = attributeValueRegex;
    }

    /** {@inheritDoc} */
    protected boolean entityAttributeValueMatches(String entityAttributeValue) {
        Matcher valueMatcher = valueRegex.matcher(StringSupport.trim(entityAttributeValue));
        return valueMatcher.matches();
    }

}
