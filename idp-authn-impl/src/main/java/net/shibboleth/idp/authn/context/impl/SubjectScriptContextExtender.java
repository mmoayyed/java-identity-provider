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

package net.shibboleth.idp.authn.context.impl;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.script.ScriptContext;
import javax.security.auth.Subject;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.idp.attribute.filter.FilterScriptContextExtender;
import net.shibboleth.idp.attribute.resolver.scripted.ResolverScriptContextExtender;
import net.shibboleth.idp.authn.context.SubjectContext;
import net.shibboleth.shared.component.AbstractInitializableComponent;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An extender that supplements an IdP {@link ScriptContext} with {@link Subject} information. 
 *
 * @since 5.0.0
 */
public class SubjectScriptContextExtender extends AbstractInitializableComponent
        implements ResolverScriptContextExtender, FilterScriptContextExtender {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(SubjectScriptContextExtender.class);
    
    /** Strategy used to locate the {@link SubjectContext} to use. */
    @Nonnull private Function<ProfileRequestContext,SubjectContext> subjectContextLookupStrategy;
    
    /** Constructor. */
    public SubjectScriptContextExtender() {
        subjectContextLookupStrategy = new ChildContextLookup<>(SubjectContext.class);
    }
    
    /**
     * Set the strategy used to locate the {@link SubjectContext}.
     * 
     * @param strategy strategy used to locate the {@link SubjectContext}
     */
    public void setSubjectContextLookupStrategy(
            @Nonnull final Function<ProfileRequestContext,SubjectContext> strategy) {
        checkSetterPreconditions();
        subjectContextLookupStrategy = Constraint.isNotNull(strategy, "SubjectContext lookup strategy cannot be null");
    }
    
    /** {@inheritDoc} */
    public void extendContext(@Nonnull final ScriptContext scriptContext) {
        checkSetterPreconditions();
        final ProfileRequestContext prc = (ProfileRequestContext) scriptContext.getAttribute("profileContext");
        
        final SubjectContext sc = subjectContextLookupStrategy.apply(prc);
        if (null == sc) {
            log.debug("SubjectScriptContextExtender could not locate SubjectContext");
        } else {
            final List<Subject> subjects = sc.getSubjects();
            scriptContext.setAttribute(
                    "subjects", subjects.toArray(new Subject[subjects.size()]), ScriptContext.ENGINE_SCOPE);
        }
    }

}