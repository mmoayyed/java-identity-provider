/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcip.annotations.NotThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.collections.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Regexp Attribute definition. */
@NotThreadSafe
public class RegexSplitAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RegexSplitAttributeDefinition.class);

    /** Regular expression used to split values. */
    private Pattern regex;

    /**
     * Constructor.
     * 
     * @param id the identifier of the attribute
     * @param regularExpression expression used to split attribute values
     * @param caseSensitive whether the regular expression is case sensitive
     */
    public RegexSplitAttributeDefinition(String id, String regularExpression, boolean caseSensitive) {
        super(id);
        if (!caseSensitive) {
            regex = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
        } else {
            regex = Pattern.compile(regularExpression);
        }
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        Set<ResolverPluginDependency> depends = getDependencies();
        Attribute<Object> result = new Attribute<Object>(getId());
        Collection<Object> results = new LazySet<Object>();
        Matcher matcher;

        if (null == depends) {
            //
            // No input? No output
            //
            return null;
        }
        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                for (Object value : dependentAttribute.getValues()) {
                    if (value instanceof String) {
                        matcher = regex.matcher((String) value);
                        if (matcher.matches()) {
                            String s = matcher.group(1);
                            results.add(s);
                        } else {
                            log.debug("Value {} did not result in any values when split by regular expression {}",
                                    value, regex.toString());
                        }
                    } else {
                        log.debug("Ignoring non-string attribute value");
                    }
                }
            }
        }
        result.setValues(results);
        return result;
    }

}
