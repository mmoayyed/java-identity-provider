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

package net.shibboleth.idp.attribute.filter.policyrule.impl;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext.Direction;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.logic.Constraint;

/** General {@link PolicyRule} for testing the filtering direction. */
public class DirectionPolicyRule extends AbstractPolicyRule {

    /** Direction to match for a positive evaluation. */
    @NonnullAfterInit private Direction matchDirection;
    
    /**
     * Gets the {@link Direction} to match for a positive evaluation.
     * 
     * @return direction to match
     */
    @NonnullAfterInit public Direction getMatchDirection() {
        return matchDirection;
    }

    /**
     * Sets the {@link Direction} to match for a positive evaluation.
     * 
     * @param match match for a positive evaluation
     */
    public void setMatchDirection(@Nonnull final Direction match) {
        matchDirection = Constraint.isNotNull(match, "Direction cannot be null");
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (matchDirection == null) {
            throw new ComponentInitializationException("Direction cannot be null");
        }
    }
    
    /** {@inheritDoc} */
    public Tristate matches(@Nonnull final AttributeFilterContext filterContext) {
        return matchDirection.equals(filterContext.getDirection()) ? Tristate.TRUE : Tristate.FALSE;
    }

}