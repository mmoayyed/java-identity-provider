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

package edu.internet2.middleware.shibboleth.common.attribute.filtering;

import java.util.ArrayList;
import java.util.List;

/**
 * A policy describing if a set of attribute value filters is applicable.
 */
public class FilterPolicy {

    /** Requirement that must be met for the given value filters to apply. */
    private MatchFunctor applicationReq;

    /** Filters to be used on attribute values. */
    private List<MatchFunctor> valueFilters;

    /** Constructor. */
    public FilterPolicy() {
        valueFilters = new ArrayList<MatchFunctor>();
    }

    /**
     * Gets the requirement that must be met in order for the registered attribute value filters to be applicable.
     * 
     * @return requirement that must be met in order for the registered attribute value filters to be applicable
     */
    public MatchFunctor getApplicationRequirement() {
        return applicationReq;
    }

    /**
     * Sets the requirement that must be met in order for the registered attribute value filters to be applicable.
     * 
     * @param requirement requirement that must be met in order for the registered attribute value filters to be
     *            applicable
     */
    public void setApplicationRequirement(MatchFunctor requirement) {
        applicationReq = requirement;
    }

    /**
     * Gets the attribute value filters that should be applied if this policy's application requirement is met.
     * 
     * @return attribute value filters that should be applied if this policy's application requirement is met
     */
    public List<MatchFunctor> getValueFilters() {
        return valueFilters;
    }
}