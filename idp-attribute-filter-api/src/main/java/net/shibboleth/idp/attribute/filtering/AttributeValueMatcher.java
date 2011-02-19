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

package net.shibboleth.idp.attribute.filtering;

import java.util.Collection;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;

/** A function that gets the values of an attribute that meets this matchers requirements. */
@ThreadSafe
public interface AttributeValueMatcher {

    /**
     * Determines the values, for the given attribute, that satisfies the requirements of this rule. Note, the value set
     * of the given attribute <strong>MUST NOT</strong> be altered by implementations of this method.
     * 
     * @param attribute attribute whose values will be filtered
     * @param filterContext current filter request
     * 
     * @return attribute values that satisfy this rule
     * 
     * @throws AttributeFilteringException thrown is there is a problem evaluating one or more attribute values against
     *             this rule's criteria
     */
    public Collection<?> getMatchingValues(final Attribute<?> attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException;
}