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

import org.springframework.beans.factory.parsing.FailFastProblemReporter;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;

import com.google.common.base.Predicate;

/**
 *
 */
public class MockPredicate implements Predicate<AttributeFilterContext>, InitializableComponent, ValidatableComponent,
        DestructableComponent {

    private boolean retVal;

    private boolean initialized;

    private boolean destroyed;

    private boolean validated;
    
    private boolean failValidate;

    private AttributeFilterContext contextUsed;

    public boolean isDestroyed() {
        return destroyed;
    }

    public void destroy() {
        destroyed = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void initialize() throws ComponentInitializationException {
        initialized = true;
    }

    public void validate() throws ComponentValidationException {
        if (failValidate) {
            throw new ComponentValidationException();
        }
        validated = true;

    }

    public boolean getValidated() {
        return validated;
    }

    public void setRetVal(boolean what) {
        retVal = what;
    }

    public boolean apply(AttributeFilterContext input) {
        contextUsed = input;
        return retVal;
    }

    public AttributeFilterContext getContextUsedAndReset() {
        AttributeFilterContext value = contextUsed;
        contextUsed = null;
        return value;
    }

    public void setFailValidate(boolean doFail) {
        failValidate = doFail;
    }
}
