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

package net.shibboleth.idp.attribute.resolver.ad.mapped.impl;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.MoreObjects;

import net.shibboleth.utilities.java.support.component.AbstractInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Represents incoming attribute values and rules used for matching them. The value may include regular expressions.
 */
public class SourceValue extends AbstractInitializableComponent {

    /**
     * Value string. This may contain regular expressions.
     */
    private @Nullable String value;

    /**
     * Whether case should be taken into account when matching.
     */
    private boolean caseSensitive = true;

    /** In the regexp case this contains the compiled pattern. */
    private @Nullable Pattern pattern;

    /**
     * Whether partial matches should be allowed.
     */
    private boolean partialMatch;
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (!partialMatch && value != null) {
            int flags = 0;
            if (!isCaseSensitive()) {
                flags = Pattern.CASE_INSENSITIVE;
            }
            pattern = Pattern.compile(value, flags);
        } else {
            pattern = null;
        }
    }
    
    /**
     * Set whether case is sensitive.
     *
     * @param theCaseSensitive whether case should be ignored when matching.  Null taken as default;
     */
    public void setCaseSensitive(final boolean theCaseSensitive) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        caseSensitive = theCaseSensitive;
    }


    /**
     * Gets whether matching should be case sensitive.
     *
     * @return whether case should be ignored when matching
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Set whether to ignore the case.
     *
     * @param theIgnoreCase whether case should be ignored when matching.  Null defaults to false;
     * @deprecated in V4 - use setCaseSensitive
     */
    @Deprecated public void setIgnoreCase(final boolean theIgnoreCase) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        DeprecationSupport.warnOnce(ObjectType.METHOD, "setIgnoreCase", null, "setCaseSensitive");
            setCaseSensitive(!theIgnoreCase);
    }


    /**
     * Gets whether case should be ignored when matching.
     *
     * @return whether case should be ignored when matching
     * @deprecated in V4 - use isCaseSensitive
     */
    @Deprecated public boolean isIgnoreCase() {
        DeprecationSupport.warnOnce(ObjectType.METHOD, "isIgnoreCase", null, "isCaseSensitive");
        return !isCaseSensitive();
    }

    /**
     * Set whether partial matches should be allowed.
     *
     * @param thePartialMatch whether partial matches should be allowed.  Null defaults to false;
     */
    public void setPartialMatch(final boolean thePartialMatch) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        partialMatch = thePartialMatch;
    }
    
    /**
     * Gets whether partial matches should be allowed.
     * 
     * @return whether partial matches should be allowed
     */
    public boolean isPartialMatch() {
        return partialMatch;
    }
    
    /**
     * Set the value string.
     * 
     * @param theValue value string This may contain regular expressions.
     */
    public void setValue(@Nullable final String theValue) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        value = StringSupport.trimOrNull(theValue);
    }
    

    /**
     * Gets the value string.
     * 
     * @return the value string.
     */
    @Nullable public String getValue() {
        Constraint.isTrue(isPartialMatch(), "getValue is only meaningful for a partialMatch, use getPattern()");
        return value;
    }

    /**
     * get the compiled pattern.  This is compiled in init and hence there is a guard.
     * 
     * @return Returns the pattern.
     */
    @Nonnull public Pattern getPattern() {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        Constraint.isFalse(isPartialMatch(), "getPattern is only meaningful for a non partial Match, use getValue()");
        return pattern;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return MoreObjects.toStringHelper(this).add("value", value).add("IsIgnoreCase", isIgnoreCase())
                .add("isPartialMatch", isPartialMatch()).toString();
    }

}