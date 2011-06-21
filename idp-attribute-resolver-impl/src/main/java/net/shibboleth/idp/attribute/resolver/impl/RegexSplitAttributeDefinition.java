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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.collections.LazySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Regexp Attribute definition.
 * 
 * The provided regexp is run across each input attribute and the first group is returned as the value.
 */
@ThreadSafe
public class RegexSplitAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(RegexSplitAttributeDefinition.class);

    /** Regular expression used to split values. */
    private final Pattern regExp;

    /**
     * Constructor.
     * 
     * @param id the identifier of the attribute
     * @param regularExpression expression used to split attribute values
     * @param caseSensitive whether the regular expression is case sensitive
     */
    public RegexSplitAttributeDefinition(final String id, final String regularExpression, final boolean caseSensitive) {
        super(id);
        if (!caseSensitive) {
            regExp = Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE);
        } else {
            regExp = Pattern.compile(regularExpression);
        }
    }

    /**
     * Get the regular expression for this resolver.
     * 
     * @return the regexp.
     */
    public Pattern getRegExp() {
        return regExp;
    }

    /** {@inheritDoc} */
    protected Attribute<?> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            return null;
        }

        final Collection<Object> results = new LazySet<Object>();
        for (ResolverPluginDependency dep : depends) {
            Attribute<?> dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                for (Object value : dependentAttribute.getValues()) {
                    if (value instanceof String) {
                        final Matcher matcher = regExp.matcher((String) value);
                        if (matcher.matches()) {
                            results.add(matcher.group(1));
                        } else {
                            log.debug(
                       "Resolver {}: Value {} did not result in any values when split by regular expression {}",
                                    value, new Object[] {getId(), regExp.toString(), regExp.toString(),});
                        }
                    } else {
                        log.info("Ignoring non-string attribute value");
                    }
                }
            }
        }
        Attribute<Object> resultantAttribute = new Attribute<Object>(getId());
        resultantAttribute.setValues(results);
        return resultantAttribute;
    }

}
