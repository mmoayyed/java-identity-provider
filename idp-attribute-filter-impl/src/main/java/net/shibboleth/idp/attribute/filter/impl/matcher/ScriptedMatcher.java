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

package net.shibboleth.idp.attribute.filter.impl.matcher;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleScriptContext;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.filter.Matcher;
import net.shibboleth.idp.attribute.filter.context.AttributeFilterContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.Unmodifiable;
import net.shibboleth.utilities.java.support.component.AbstractDestructableIdentifiableInitializableComponent;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponent;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.scripting.EvaluableScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

/**
 * A {@link net.shibboleth.idp.attribute.filter.Matcher} that delegates to a JSR-223 script for its actual processing.
 * 
 */
@ThreadSafe
public class ScriptedMatcher extends AbstractDestructableIdentifiableInitializableComponent implements Matcher,
        UnmodifiableComponent {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ScriptedMatcher.class);

    /** Script to be evaluated. */
    private EvaluableScript script;

    /** Log prefix. */
    private String logPrefix;

    /**
     * Constructor.
     * 
     * @param matchingScript script used to determine matching attribute values
     */
    public ScriptedMatcher(@Nonnull final EvaluableScript matchingScript) {
        setScript(matchingScript);
    }

    /** {@inheritDoc} */
    public void setId(@Nullable final String id) {
        super.setId(id);
        // clear cache
        logPrefix = null;
    };

    /**
     * Gets the script to be evaluated.
     * 
     * @return the script to be evaluated
     */
    @Nonnull public EvaluableScript getScript() {
        return script;
    }

    /**
     * Sets the script to be evaluated.
     * 
     * @param matcherScript the script to be evaluated
     */
    protected void setScript(@Nonnull final EvaluableScript matcherScript) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        script = Constraint.isNotNull(matcherScript, "Attribute value matching script can not be null");
    }

    /**
     * Perform the AttributeValueMatching.
     * <p>
     * When the script is evaluated, the following properties will be available via the {@link ScriptContext}:
     * <ul>
     * <li><code>filterContext</code> - the current instance of {@link AttributeFilterContext}</li>
     * <li><code>attribute</code> - the attribute whose values are to be evaluated
     * </ul>
     * The script <strong>MUST</strong> return a {@link Set} containing the {@link IdPAttributeValue} objects that were
     * matched.
     * </p>
     * {@inheritDoc}
     */
    @Nullable @NonnullElements @Unmodifiable public Set<IdPAttributeValue<?>> getMatchingValues(
            @Nonnull final IdPAttribute attribute, @Nonnull final AttributeFilterContext filterContext) {
        Constraint.isNotNull(attribute, "Attribute to be filtered can not be null");
        Constraint.isNotNull(filterContext, "Attribute filter context can not be null");
        ComponentSupport.ifNotInitializedThrowUninitializedComponentException(this);
        ComponentSupport.ifDestroyedThrowDestroyedComponentException(this);

        final EvaluableScript currentScript = script;
        final SimpleScriptContext scriptContext = new SimpleScriptContext();
        scriptContext.setAttribute("filterContext", filterContext, ScriptContext.ENGINE_SCOPE);
        scriptContext.setAttribute("attribute", attribute, ScriptContext.ENGINE_SCOPE);

        try {
            final Object result = currentScript.eval(scriptContext);
            if (null == result) {
                log.error("{} Matcher script did not return a result.", getLogPrefix());
                return null;
            }

            if (result instanceof Set) {
                HashSet<IdPAttributeValue<?>> returnValues = new HashSet<>(attribute.getValues());
                returnValues.retainAll((Set) result);
                return Collections.unmodifiableSet(returnValues);
            } else {
                log.error("{} Matcher script did not return a Set.", getLogPrefix());
                return null;
            }
        } catch (ScriptException e) {
            log.error("{} Error while executing value matching script: {}", getLogPrefix(), e);
            return null;
        }
    }

    /** {@inheritDoc} */
    protected void doDestroy() {
        super.doDestroy();
    }

    /** {@inheritDoc} */
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == script) {
            // never met so long as we have the assert in the constructor
            throw new ComponentInitializationException("No script has been provided");
        }
    }

    // TODO : Do we still need these?
    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!(obj instanceof ScriptedMatcher)) {
            return false;
        }

        ScriptedMatcher other = (ScriptedMatcher) obj;

        return script.equals(other.getScript());
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return Objects.hashCode(script, getId());
    }

    /** {@inheritDoc} */
    public String toString() {
        return Objects.toStringHelper(this).add("Script", getScript()).toString();
    }

    /**
     * return a string which is to be prepended to all log messages.
     * 
     * @return "Scripted Attribute Filter '<filterID>' :"
     */
    protected String getLogPrefix() {
        // local cache of cached entry to allow unsynchronised clearing.
        String prefix = logPrefix;
        if (null == prefix) {
            StringBuilder builder = new StringBuilder("Scripted Attribute Filter '").append(getId()).append("':");
            prefix = builder.toString();
            if (null == logPrefix) {
                logPrefix = prefix;
            }
        }
        return prefix;
    }
}