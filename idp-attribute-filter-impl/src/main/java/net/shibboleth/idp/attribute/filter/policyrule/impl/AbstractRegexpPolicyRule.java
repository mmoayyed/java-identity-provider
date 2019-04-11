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

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.logic.Constraint;

/**
 * General {@link net.shibboleth.idp.attribute.filter.Matcher} for regexp comparison of strings in Attribute Filters.
 */
public abstract class AbstractRegexpPolicyRule extends AbstractPolicyRule {

    /** Regular expression to match. */
    private Pattern pattern;

    /**
     * Gets the regular expression to match.
     * 
     * @return regular expression to match
     */
    @NonnullAfterInit public String getRegularExpression() {
        return pattern.pattern();
    }

    /**
     * Sets the {@link Pattern} for matching to match.
     * 
     * @param thePattern the pattern to match
     */
    public void setPattern(@Nonnull final Pattern thePattern) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);
        pattern = Constraint.isNotNull(thePattern, "Pattern supplied to setPattern but not be null");
    }

    /**
     * Matches the given value against the provided regular expression.
     * 
     * @param value the value to evaluate
     * 
     * @return true if the value matches the given match string, false if not
     */
    protected Tristate regexpCompare(@Nullable final String value) {
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);

        final boolean result;
        if (pattern == null || value == null) {
            result = false;
        } else if (pattern.matcher(value).matches()) {
            result = true;
        } else {
            result = false;
        }

        if (result) {
            return Tristate.TRUE;
        }
        return Tristate.FALSE;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        if (null == pattern) {
            throw new ComponentInitializationException(getLogPrefix() + " No regular expression provided");
        }
    }
}
