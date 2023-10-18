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

package net.shibboleth.idp.profile.logic;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.script.ScriptException;

import org.springframework.core.io.Resource;

import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.shared.scripting.EvaluableScript;
import net.shibboleth.shared.spring.resource.ResourceHelper;

/**
 * Deprecated stub for relocated class.
 * 
 * @deprecated
 */
@Deprecated(since="5.0.0", forRemoval=true)
public class ScriptedPredicate extends net.shibboleth.profile.context.logic.ScriptedPredicate {
    
    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate.
     * @param extraInfo debugging information.
     */
    public ScriptedPredicate(@Nonnull @NotEmpty @ParameterName(name="theScript") final EvaluableScript theScript,
            @Nullable @NotEmpty @ParameterName(name="extraInfo") final String extraInfo) {
        super(theScript, extraInfo);
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                net.shibboleth.profile.context.logic.ScriptedPredicate.class.getName());
    }

    /**
     * Constructor.
     * 
     * @param theScript the script we will evaluate.
     */
    public ScriptedPredicate(@Nonnull @NotEmpty @ParameterName(name="theScript") final EvaluableScript theScript) {
        super(theScript);
        DeprecationSupport.warn(ObjectType.CLASS, getClass().getName(), null,
                net.shibboleth.profile.context.logic.ScriptedPredicate.class.getName());
    }
    
    /**
     * Factory to create {@link ScriptedPredicate} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    public static ScriptedPredicate resourceScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull final Resource resource) throws ScriptException, IOException, ComponentInitializationException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(ResourceHelper.of(resource));
        script.initialize();
        return new ScriptedPredicate(script, resource.getDescription());
    }

    /**
     * Factory to create {@link ScriptedPredicate} from a {@link Resource}.
     * 
     * @param resource the resource to look at
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws IOException if the file doesn't exist.
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    public static ScriptedPredicate resourceScript(@Nonnull final Resource resource)
            throws ScriptException, IOException, ComponentInitializationException {
        return resourceScript(DEFAULT_ENGINE, resource);
    }

    /**
     * Factory to create {@link ScriptedPredicate} from inline data.
     * 
     * @param scriptSource the script, as a string
     * @param engineName the language
     * @return the predicate
     * @throws ScriptException if the compile fails
     * @throws ComponentInitializationException if the scripting initialization fails
     */
    public static ScriptedPredicate inlineScript(@Nonnull @NotEmpty final String engineName,
            @Nonnull @NotEmpty final String scriptSource) throws ScriptException, ComponentInitializationException {
        final EvaluableScript script = new EvaluableScript();
        script.setEngineName(engineName);
        script.setScript(scriptSource);
        script.initialize();
        return new ScriptedPredicate(script, "Inline");
    }

    /**
     * Factory to create {@link ScriptedPredicate} from inline data.
     * 
     * @param scriptSource the script, as a string
     * 
     * @return the predicate
     * 
     * @throws ScriptException if the compile fails
     * @throws ComponentInitializationException if the script object fails to initialize
     */
    public static ScriptedPredicate inlineScript(@Nonnull @NotEmpty final String scriptSource)
            throws ScriptException, ComponentInitializationException {
        return inlineScript(DEFAULT_ENGINE, scriptSource);
    }

}