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

package net.shibboleth.idp.attribute.filter;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

/** A simple, mock implementation of {@link Matcher}. */
public class MockPolicyRequirementRule extends AbstractIdentifiableInitializableComponent implements
        PolicyRequirementRule, InitializableComponent, DestructableComponent, ValidatableComponent {

    /** state variable */
    private boolean initialized;

    /** state variable */
    private boolean destroyed;

    /** state variable */
    private boolean validated;

    /** to return from matcher(). */
    private Tristate retVal;

    /** do we fail when validate is called? do we fail when we are called? */
    private boolean fails;

    /** what was passed to getMatchingValues(). */
    private AttributeFilterContext contextUsed;

    public MockPolicyRequirementRule() {
        setId("Mock");
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        if (fails) {
            throw new ComponentValidationException();
        }
        validated = true;
    }

    public boolean getValidated() {
        return validated;
    }

    /** {@inheritDoc} */
    public boolean isDestroyed() {
        return destroyed;
    }

    /** {@inheritDoc} */
    public void destroy() {
        destroyed = true;
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return initialized;
    }

    /** {@inheritDoc} */
    public void doInitialize() {
        initialized = true;
    }

    public void setRetVal(Tristate what) {
        retVal = what;
    }

    public AttributeFilterContext getContextUsedAndReset() {
        AttributeFilterContext value = contextUsed;
        contextUsed = null;
        return value;
    }

    public void setFailValidate(boolean doFail) {
        fails = doFail;
    }

    /** {@inheritDoc} */
    public Tristate matches(@Nonnull AttributeFilterContext filterContext) {
        if (fails) {
            return Tristate.FAIL;
        }
        contextUsed = filterContext;
        return retVal;
    }
}