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
import java.util.regex.PatternSyntaxException;

import net.jcip.annotations.ThreadSafe;
import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;
import net.shibboleth.utilities.java.support.collection.LazySet;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

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
    private Pattern compiledRegExp;

    /** Text of regular expression. */
    private String regExp;

    /** Whether the regexp is case sensitive. */
    private boolean caseSensitive;

    /**
     * Set whether the regexp is case sensitive or not. This cannot be modified after initialization.
     * 
     * @param isCaseSensitive the case sensitivity flag.
     */
    public synchronized void setCaseSensitive(final boolean isCaseSensitive) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        caseSensitive = new Boolean(isCaseSensitive);
    }

    /**
     * returns the case sensitivity.
     * 
     * @return whether we are case sensitive or not. defaults to false
     */
    public boolean getCaseSensitive() {
        return caseSensitive;
    }

    /**
     * Sets the pattern we are matching with. This cannot be modified after initialization.
     * 
     * @param newRegExp the pattern.
     */
    public synchronized void setRegExp(final String newRegExp) {
        ifInitializedThrowUnmodifiabledComponentException(getId());
        ifDestroyedThrowDestroyedComponentException(getId());

        regExp = StringSupport.trimOrNull(newRegExp);
    }

    /**
     * Returns the regexp we are using.
     * 
     * @return the pattern. Never returns null after initialization.
     */
    public String getRegExp() {
        return regExp;
    }

    /**
     * Returns the regexp we are using.
     * 
     * @return the compiled pattern. Always returns null before initialization and never after.
     */
    public Pattern getCompiledRegExp() {
        return compiledRegExp;
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == regExp) {
            throw new ComponentInitializationException("Regexp Split Attribute definition " + getId()
                    + " is being initialized, without a valid regexp being set");
        }

        try {
            if (!caseSensitive) {
                compiledRegExp = Pattern.compile(regExp, Pattern.CASE_INSENSITIVE);
            } else {
                compiledRegExp = Pattern.compile(regExp);
            }
        } catch (PatternSyntaxException e) {
            throw new ComponentInitializationException(e);
        }
        if (null == compiledRegExp) {
            throw new ComponentInitializationException("Regexp Split Attribute definition " + getId()
                    + " was not compiled correctly");
        }
    }

    /** {@inheritDoc} */
    protected Optional<Attribute> doAttributeResolution(final AttributeResolutionContext resolutionContext)
            throws AttributeResolutionException {
        final Set<ResolverPluginDependency> depends = getDependencies();
        if (null == depends) {
            return null;
        }

        final Collection<Object> results = new LazySet<Object>();
        for (ResolverPluginDependency dep : depends) {
            Attribute dependentAttribute = dep.getDependentAttribute(resolutionContext);
            if (null != dependentAttribute) {
                for (Object value : dependentAttribute.getValues()) {
                    if (value instanceof String) {
                        final Matcher matcher = compiledRegExp.matcher((String) value);
                        if (matcher.matches()) {
                            results.add(matcher.group(1));
                        } else {
                            log.debug("Resolver {}: Value {} did not result in any values"
                                    + " when split by regular expression {}", value,
                                    new Object[] {getId(), regExp.toString(), regExp.toString(),});
                        }
                    } else {
                        log.info("Ignoring non-string attribute value");
                    }
                }
            }
        }
        Attribute resultantAttribute = new Attribute(getId());
        resultantAttribute.setValues(results);
        return Optional.of(resultantAttribute);
    }
}
