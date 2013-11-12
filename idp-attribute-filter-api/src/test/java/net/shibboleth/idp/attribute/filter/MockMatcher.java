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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.utilities.java.support.component.AbstractIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;
import net.shibboleth.utilities.java.support.component.DestructableComponent;
import net.shibboleth.utilities.java.support.component.InitializableComponent;
import net.shibboleth.utilities.java.support.component.ValidatableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Objects;

/** A simple, mock implementation of {@link Matcher}. */
public class MockMatcher extends AbstractIdentifiableInitializableComponent implements Matcher, InitializableComponent, DestructableComponent, ValidatableComponent{ 

    /** ID of the attribute to which this matcher applies. */
    private String matchingAttribute;

    /** Values, of the attribute, considered to match this matcher. */
    private Collection matchingValues;
    
    /** state variable */
    private boolean initialized;

    /** state variable */
    private boolean destroyed;
    
    /** state variable */
    private boolean validated;
    
    /** do we fail when validate is called? do we fail when we are called?*/
    private boolean fails;

    /** what was passed to getMatchingValues(). */
    private AttributeFilterContext contextUsed;

    public MockMatcher() {
        setId("Mock");
    }

    /**
     * Sets the ID of the attribute to which this matcher applies.
     * 
     * @param id ID of the attribute to which this matcher applies
     */
    public void setMatchingAttribute(String id) {
        matchingAttribute = Constraint.isNotNull(StringSupport.trimOrNull(id), "attribute ID can not be null or empty");
        if (!initialized) {
            setId("Mock " + id);
        }
    }

    /**
     * Sets the values, of the attribute, considered to match this matcher. If null then all attribute values are
     * considered to be matching.
     * 
     * @param values values, of the attribute, considered to match this matcher
     */
    public void setMatchingValues(Collection values) {
        matchingValues = values;
    }

    /** {@inheritDoc} */
    public Set<IdPAttributeValue<?>> getMatchingValues(IdPAttribute attribute, AttributeFilterContext filterContext) {
        if (fails) {
            return null;
        }
        if (!Objects.equal(attribute.getId(), matchingAttribute)) {
            return Collections.EMPTY_SET;
        }

        if (matchingValues == null) {
            return attribute.getValues();
        }

        HashSet<IdPAttributeValue<?>> values = new HashSet<>();
        for (IdPAttributeValue value : attribute.getValues()) {
            if (matchingValues.contains(value)) {
                values.add(value);
            }
        }

        return values;
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
    public void doInitialize()  {
        initialized = true;
    }


    public AttributeFilterContext getContextUsedAndReset() {
        AttributeFilterContext value = contextUsed;
        contextUsed = null;
        return value;
    }

    public void setFailValidate(boolean doFail) {
        fails = doFail;
    }

    /** {@inheritDoc}
    //TODO remove
    public boolean matches(@Nonnull AttributeFilterContext filterContext) throws AttributeFilterException {
        if (fails) {
            throw new MatcherException("oops");
        }
        contextUsed = filterContext;
        return retVal;
    }*/
}