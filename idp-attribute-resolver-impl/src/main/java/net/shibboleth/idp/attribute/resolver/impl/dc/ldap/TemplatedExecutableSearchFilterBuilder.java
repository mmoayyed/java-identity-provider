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

package net.shibboleth.idp.attribute.resolver.impl.dc.ldap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.context.AttributeResolutionContext;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullAfterInit;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.ComponentSupport;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.velocity.Template;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.app.event.EventCartridge;
import org.apache.velocity.app.event.ReferenceInsertionEventHandler;
import org.ldaptive.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.common.attribute.provider.V2SAMLProfileRequestContext;

/**
 * An {@link net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder} that generates the search
 * filter to be executed by evaluating a {@link Template} against the currently resolved attributes within a
 * {@link AttributeResolutionContext}.
 */
public class TemplatedExecutableSearchFilterBuilder extends AbstractExecutableSearchFilterBuilder {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(TemplatedExecutableSearchFilterBuilder.class);

    /** Template evaluated to generate a search filter. */
    private Template template;

    /** Template (as Text) to be evaluated. */
    private String templateText;

    /** VelocityEngine. */
    private VelocityEngine engine;

    /** Do we need to make ourself V2 Compatible? */
    private boolean v2Compatibility;

    /**
     * Gets the template to be evaluated.
     * 
     * @return the template
     */
    @NonnullAfterInit public Template getTemplate() {
        return template;
    }

    /**
     * Gets the template text to be evaluated.
     * 
     * @return the template text
     */
    @NonnullAfterInit public String getTemplateText() {
        return templateText;
    }

    /**
     * Sets the template to be evaluated.
     * 
     * @param velocityTemplate template to be evaluated
     */
    public void setTemplateText(@Nullable String velocityTemplate) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        templateText = StringSupport.trimOrNull(velocityTemplate);
    }

    /**
     * Gets the {@link VelocityEngine} to be used.
     * 
     * @return the template
     */
    @Nullable @NonnullAfterInit public VelocityEngine getVelocityEngine() {
        return engine;
    }

    /**
     * Sets the {@link VelocityEngine} to be used.
     * 
     * @param velocityEngine engine to be used
     */
    public synchronized void setVelocityEngine(VelocityEngine velocityEngine) {
        ComponentSupport.ifInitializedThrowUnmodifiabledComponentException(this);

        engine = velocityEngine;
    }

    /**
     * Are we in V2 Compatibility mode?
     * 
     * @return Returns the v2Compat.
     */
    public boolean isV2Compatibility() {
        return v2Compatibility;
    }

    /**
     * What is out V2 Compatibility mode.
     * 
     * @param compat The mode to set.
     */
    public void setV2Compatibility(boolean compat) {
        v2Compatibility = compat;
    }

    /** {@inheritDoc} */
    @Override
    public ExecutableSearchFilter build(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {

        final VelocityContext context = new VelocityContext();
        log.trace("Creating search filter using attribute resolution context {}", resolutionContext);
        context.put("resolutionContext", resolutionContext);

        if (isV2Compatibility()) {
            final V2SAMLProfileRequestContext requestContext =
                    new V2SAMLProfileRequestContext(resolutionContext, resolutionContext.getId());
            log.trace("Adding v2 request context {}", requestContext);
            context.put("requestContext", requestContext);
        }

        final EventCartridge cartridge = new EventCartridge();
        cartridge.addEventHandler(new ReferenceInsertionEventHandler() {

            @Override public Object referenceInsert(final String reference, final Object value) {
                if (value == null) {
                    return null;
                }
                return SearchFilter.encodeValue(value.toString());
            }
        });
        cartridge.attachToContext(context);

        final SearchFilter searchFilter = new SearchFilter(merge(context));
        return super.build(searchFilter);
    }

    /**
     * Invokes {@link Template#merge(org.apache.velocity.context.Context)} on the supplied context.
     * 
     * @param context to merge
     * 
     * @return result of the merge operation
     */
    protected String merge(@Nonnull final VelocityContext context) {
        final String result = template.merge(context);
        log.debug("Template text {} yields {}", templateText, result);
        return result;
    }

    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();

        if (null == engine) {
            throw new ComponentInitializationException(
                    "TemplatedExecutableStatementBuilder: no velocity engine was configured");
        }

        if (null == templateText) {
            throw new ComponentInitializationException(
                    "TemplatedExecutableStatementBuilder: no template text must be non null");
        }

        template = Template.fromTemplate(engine, templateText);
    }
}