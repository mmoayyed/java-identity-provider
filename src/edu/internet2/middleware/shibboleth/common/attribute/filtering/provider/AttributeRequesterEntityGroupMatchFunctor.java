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
import edu.internet2.middleware.shibboleth.common.attribute.filtering.SAMLFilterContext;

/**
 * A {@link MatchFunctor} that evaluates to true if {@link SAMLFilterContext#getRequesterMetadata()} matches the
 * provided entity group name.
 */
public class AttributeRequesterEntityGroupMatchFunctor extends AbstractEntityGroupMatchFunctor {

    /** {@inheritDoc} */
    public boolean evaluate(FilterContext filterContext) throws FilterProcessingException {
        if (filterContext instanceof SAMLFilterContext) {
            return isEntityInGroup(((SAMLFilterContext) filterContext).getRequesterMetadata());
        }else{
            throw new FilterProcessingException("Given filter context is not a SAMLFilterContext");
        }
    }

    /** {@inheritDoc} */
    public boolean evaluate(FilterContext filterContext, String attributeId, Object attributeValue)
            throws FilterProcessingException {
        if (filterContext instanceof SAMLFilterContext) {
            return isEntityInGroup(((SAMLFilterContext) filterContext).getRequesterMetadata());
        }else{
            throw new FilterProcessingException("Given filter context is not a SAMLFilterContext");
        }
    }
}