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

package net.shibboleth.idp.attribute.filtering.impl.policy;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.impl.ContextNavigationSupport;

import org.opensaml.util.criteria.EvaluableCriterion;
import org.opensaml.util.criteria.EvaluationException;

/**
 * Implement the AtttibuteRequesterStringPolicy activation criterion. <br />
 * If the entityID on the incoming message matches the provided string (according to the provided case sensitivity
 * criterion) then we return true.
 */
@ThreadSafe
public class IncomingMessageIssuerStringCriterion extends BaseStringCompare implements
        EvaluableCriterion<AttributeFilterContext> {

    /**
     * {@inheritDoc}
     * 
     * @throws EvaluationException if the attribute name could not be found.
     * */
    public Boolean doEvaluate(final AttributeFilterContext filterContext) throws EvaluationException {

        return isMatch(ContextNavigationSupport.getIncomingIssuer(filterContext));
    }
}
