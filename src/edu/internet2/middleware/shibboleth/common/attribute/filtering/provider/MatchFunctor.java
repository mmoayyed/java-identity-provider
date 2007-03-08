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

import edu.internet2.middleware.shibboleth.common.attribute.filtering.FilterContext;

/**
 * A function that evaluates whether an expressed criteria is met by the current filter context.
 */
public interface MatchFunctor {

    /**
     * Evaluates this matching criteria. This evaluation is used while the filtering engine determiens policy
     * applicability.
     * 
     * @param filterContext current filtering context
     * 
     * @return true if the criteria for this matching function are meant
     * 
     * @throws FilterProcessingException thrown if the function can not be evaluated
     */
    public boolean evaluate(FilterContext filterContext) throws FilterProcessingException;

    /**
     * Evaluates this matching criteria. This evaluation is used while the filtering engine is filtering attribute
     * values.
     * 
     * @param filterContext the current filtering context
     * @param attributeId ID of the attribute being evaluated
     * @param attributeValue value of the attribute being evalauted
     * 
     * @return true if the criteria for this matching function are meant
     * 
     * @throws FilterProcessingException thrown if the function can not be evaluated
     */
    public boolean evaluate(FilterContext filterContext, String attributeId, Object attributeValue)
            throws FilterProcessingException;
}