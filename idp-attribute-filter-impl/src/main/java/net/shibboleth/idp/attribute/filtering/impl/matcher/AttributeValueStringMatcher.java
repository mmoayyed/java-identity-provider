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

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.filtering.AttributeFilterContext;
import net.shibboleth.idp.attribute.filtering.AttributeFilteringException;
import net.shibboleth.idp.attribute.filtering.AttributeValueMatcher;
import net.shibboleth.idp.attribute.filtering.impl.BaseStringCompare;

import org.opensaml.xml.util.LazySet;

/**
 * Implement the Attribute String Value Matcher. <br />
 * If any of the values matches the string then a singleton collection containing that string is returned, otherwise an
 * empty collection is returned.
 */
public class AttributeValueStringMatcher extends BaseStringCompare implements AttributeValueMatcher {

    /**
     * Constructor.
     * 
     * @param match what to compare against.
     * @param isCaseSensitive whether comparison is case sensitive.
     */
    protected AttributeValueStringMatcher(String match, boolean isCaseSensitive) {
        super(match, isCaseSensitive);
    }

    /** {@inheritDoc} */
    public Collection<?> getMatchingValues(Attribute<?> attribute, AttributeFilterContext filterContext)
            throws AttributeFilteringException {
        Set<String> result = new LazySet<String>();
        Collection values = attribute.getValues();
        //
        // We must do this bit by bit in order to enforce the .toString in the
        // call to isMatch (so in the case sensitive side we cannot just call 
        // values.contains())
        // Do this bit by bit
        for (Object value : values) {
            if (isMatch(value)) {
                // Add value, not the patter since the patter might be uppercase.
                result.add(value.toString());
                // All done
                break;
            }
        }
        return Collections.unmodifiableCollection(result);
    }
}
