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

package net.shibboleth.idp.attribute.filtering.impl.matcher;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.utilities.java.support.collection.LazySet;

/**
 * Implement the Attribute Regular Expression Value Matcher. <br />
 * The result is a {@link Collection} with those value which match the regular expression. If none match then an empty
 * collection is returned. null is never returned. <br />
 */
@ThreadSafe
public class AttributeValueRegexMatcher extends BaseRegexMatcher implements AttributeValueMatcher {

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(final Attribute<?> attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        Set result = null;
        final Collection values = attribute.getValues();

        for (Object value : values) {
            if (isMatch(value)) {
                if (null == result) {
                    // Lazy object allocation
                    result = new LazySet();
                }
                // Add value, not the pattern since the pattern might be in a different case.
                result.add(value);
            }
        }
        if (null == result) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }
}
