/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.resource.Resource;
import net.shibboleth.shared.scripting.AbstractScriptEvaluator;
import net.shibboleth.shared.scripting.EvaluableScript;

/**
 * An action which calls out to a supplied script.
 * 
 * <p>
 * The return value must be an event ID to signal. As this is a generic wrapper, the action may return any event
 * depending on the context of the activity, and may manipulate the profile context tree as required.
 * </p>
 * 
 * @event {@link EventIds#INVALID_PROFILE_CTX}
 */
public class ScriptedAction extends AbstractProfileAction {

    /** The default language is Javascript. */
    @Nonnull @NotEmpty public static final String DEFAULT_ENGINE = "JavaScript";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(ScriptedAction.class);

    /** Evaluator. */
    @Nonnull private final ActionScriptEvaluator scriptEvaluator;

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate
     */
    protected ScriptedAction(@Nonnull final EvaluableScript theScript) {
        scriptEvaluator = new ActionScriptEvaluator(theScript);
    }

    /**
     * Return the custom (externally provided) object.
     * 
     * @return the custom object
     */
    @Nullable public Object getCustomObject() {
        return scriptEvaluator.getCustomObject();
    }

    /**
     * Set the custom (externally provided) object.
     * 
     * @param object the custom object
     */
    public void setCustomObject(@Nullable final Object object) {
        checkSetterPreconditions();
        scriptEvaluator.setCustomObject(object);
    }

    /**
     * Set whether to hide exceptions in script execution (default is false).
     * 
     * @param flag flag to set
     * 
     * @since 3.4.0
     */
    public void setHideExceptions(final boolean flag) {
        checkSetterPreconditions();
        scriptEvaluator.setHideExceptions(flag);
    }
    
    /** {@inheritDoc} */
    @Override protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        scriptEvaluator.setLogPrefix(getLogPrefix());
    }

    /** {@inheritDoc} */
    @Override public void doExecute(@Nonnull final ProfileRequestContext profileContext) {

        final String result = scriptEvaluator.execute(profileContext);
        if (result == null) {
            log.debug("{} signaled proceed event", getLogPrefix());
            ActionSupport.buildProceedEvent(profileContext);
        } else {
            log.debug("{} signaled event: {}", getLogPrefix(), result);
            ActionSupport.buildEvent(profileContext, result);
        }
    }

    /**
     * Factory to create {@link ScriptedAction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    @Nonnull public static ScriptedAction resourceScript(
            @Nonnull @NotEmpty @ParameterName(name="engineName") final String engineName,
            @Nonnull @ParameterName(name="resource") final Resource resource)
            throws ScriptException, IOException, ComponentInitializationException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(resource);
        script.initialize();
        return new ScriptedAction(script);
    }

    /**
     * Factory to create {@link ScriptedAction} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    @Nonnull public static ScriptedAction resourceScript(
            @Nonnull @ParameterName(name="resource") final Resource resource)
                    throws ScriptException, IOException, ComponentInitializationException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedAction} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    @Nonnull public static ScriptedAction inlineScript(
            @Nonnull @NotEmpty @ParameterName(name="engineName") final String engineName,
            @Nonnull @NotEmpty @ParameterName(name="scriptSource") final String scriptSource)
                    throws ScriptException, ComponentInitializationException {
        final EvaluableScript script = new EvaluableScript();
                script.setEngineName(engineName);
                script.setScript(scriptSource);
                script.initialize();
        return new ScriptedAction(script);
    }

    /**
     * Factory to create {@link ScriptedAction} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    @Nonnull public static ScriptedAction inlineScript(
            @Nonnull @NotEmpty @ParameterName(name="scriptSource") final String scriptSource)
                    throws ScriptException, ComponentInitializationException {
        return inlineScript(DEFAULT_ENGINE, scriptSource);
    }

    /**
     * Evaluator bound to the Action semantic.
     */
    private class ActionScriptEvaluator extends AbstractScriptEvaluator {

        /**
         * Constructor.
         * 
         * @param theScript the script we will evaluate.
         */
        public ActionScriptEvaluator(@Nonnull final EvaluableScript theScript) {
            super(theScript);
            setOutputType(String.class);
            setReturnOnError(EventIds.INVALID_PROFILE_CTX);
        }
        
        /** {@inheritDoc} */
        @Override
        @Nullable public Object getCustomObject() {
            return super.getCustomObject();
        }
        
        /**
         * Execution hook for the script.
         * 
         * @param profileContext profile request context
         * 
         * @return the resulting event
         */
        @Nullable public String execute(@Nullable final ProfileRequestContext profileContext) {
            return (String) evaluate(profileContext);
        }

        /** {@inheritDoc} */
        @Override
        protected void prepareContext(@Nonnull final ScriptContext scriptContext, @Nullable final Object... input) {
            scriptContext.setAttribute("profileContext", input != null ? input[0] : null, ScriptContext.ENGINE_SCOPE);
        }
    }
    
}