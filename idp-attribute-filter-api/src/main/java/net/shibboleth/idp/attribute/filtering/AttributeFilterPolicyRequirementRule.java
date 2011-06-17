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

package net.shibboleth.idp.attribute.filtering;

import net.jcip.annotations.ThreadSafe;

/** A rule that determines if a given {@link AttributeFilterPolicy} is active for a given request. */
@ThreadSafe
public interface AttributeFilterPolicyRequirementRule {

    /**
     * Checks if the given rule is satisfied by this request.
     * 
     * @param filterContext current filter request
     * 
     * @return true if the surrounding {@link AttributeFilterPolicy} containing this rule should be active for this
     *         request, false otherwise
     * 
     * @throws AttributeFilteringException thrown if there is a problem determining is this rule is satisfied by the
     *             current request
     */
    public boolean isSatisfied(final AttributeFilterContext filterContext) throws AttributeFilteringException;
}