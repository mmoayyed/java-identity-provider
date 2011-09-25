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

import org.opensaml.util.collections.LazySet;

/**
 * Implement the Attribute String Value Matcher. <br />
 * If any of the values matches the string then the result is a {@link Collection} with those entries. Otherwise an
 * empty collection is returned. null is never returned. <br />
 * <em> Note </em> If case sensitive is true then this function only returns and empty set or a set with one value.
 */
@ThreadSafe
public class AttributeValueStringMatcher extends BaseStringMatcher implements AttributeValueMatcher {

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(final Attribute<?> attribute, final AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        Set<String> result = null;
        final Collection values = attribute.getValues();

        for (Object value : values) {
            if (isMatch(value)) {
                if (null == result) {
                    // Lazy object allocation
                    result = new LazySet<String>();
                }
                // Add value, not the pattern since the pattern might be in a different case.
                result.add(value.toString());
                if (isCaseSensitive()) {
                    // There can only be one element which matches. Adding other ones
                    // will be a no-op in a Set
                    break;
                }
            }
        }
        if (null == result) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableCollection(result);
    }
}
