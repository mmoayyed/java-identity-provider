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

package net.shibboleth.idp.attribute.resolver.spring;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import net.shibboleth.ext.spring.util.AbstractCustomBeanDefinitionParser;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.logic.ResolutionLabelPredicate;
import net.shibboleth.idp.attribute.resolver.spring.impl.InputAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.impl.InputDataConnectorParser;
import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.PredicateSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport;
import net.shibboleth.utilities.java.support.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/** Bean definition parser for a {@link net.shibboleth.idp.attribute.resolver.ResolverPlugin}. */
public abstract class BaseResolverPluginParser extends AbstractCustomBeanDefinitionParser {

    /** An Id for the definition, used for debugging messages and creating names of children. */
    @Nonnull @NotEmpty private String defnId = "<Unnamed Attribute or Connector>";

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseResolverPluginParser.class);

    /**
     * Helper for logging.
     * 
     * @return the definition ID
     */
    @Nonnull @NotEmpty protected String getDefinitionId() {
        return defnId;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    @Override protected void doParse(@Nonnull final Element config, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        super.doParse(config, parserContext, builder);
        final String id = StringSupport.trimOrNull(config.getAttributeNS(null, "id"));
        log.debug("Parsing configuration for {} plugin with id: {}", config.getLocalName(), id);
        builder.addPropertyValue("id", id);
        if (null != id) {
            defnId = id;
        }
        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");

        if (config.hasAttributeNS(null, "activationConditionRef")) {
            if (config.hasAttributeNS(null, "relyingParties") ||
                    config.hasAttributeNS(null, "resolutionPhases") ||
                    config.hasAttributeNS(null, "excludeRelyingParties") ||
                    config.hasAttributeNS(null, "excludeResolutionPhases")) {
                log.warn("relyingParties/resolutionPhases and variants ignored, using activationConditionRef");
            }
            builder.addPropertyReference("activationCondition",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "activationConditionRef")));
        } else {
            final BeanDefinitionBuilder condition = getActivationCondition(config);
            if (condition != null) {
                builder.addPropertyValue("activationCondition", condition.getBeanDefinition());
            }
        }

        if (config.hasAttributeNS(null, "profileContextStrategyRef")) {
            DeprecationSupport.warnOnce(ObjectType.ATTRIBUTE, "profileContextStrategyRef",
                    parserContext.getReaderContext().getResource().getDescription(), "(will be removed)");
            builder.addPropertyReference("profileContextStrategy",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "profileContextStrategyRef")));
        }

        if (config.hasAttributeNS(null, "propagateResolutionExceptions")) {
            builder.addPropertyValue("propagateResolutionExceptions",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "propagateResolutionExceptions")));
        }

        final List<Element> attributeDependencyElements =
                ElementSupport.getChildElements(config, InputAttributeDefinitionParser.ELEMENT_NAME);
        final List<Element> dataConnectorDependencyElements =
                ElementSupport.getChildElements(config, InputDataConnectorParser.ELEMENT_NAME);       
        if ((null != attributeDependencyElements && !attributeDependencyElements.isEmpty()) ||
            (null != dataConnectorDependencyElements && !dataConnectorDependencyElements.isEmpty())) {
            if (failOnDependencies()) {
                log.error("{} Dependencies are not allowed.", getLogPrefix());
                throw new BeanCreationException(getLogPrefix() + " has meaningless Dependencies statements");
            }
            if (warnOnDependencies()) {
                log.warn("{} Dependencies are not allowed.", getLogPrefix());
            }
        }
        builder.addPropertyValue("attributeDependencies", 
                SpringSupport.parseCustomElements(attributeDependencyElements, parserContext, builder));
        builder.addPropertyValue("dataConnectorDependencies", 
                SpringSupport.parseCustomElements(dataConnectorDependencyElements, parserContext, builder));
    }
    
    /**
     * Get the effective activation condition to inject.
     * 
     * @param config configuration element
     * 
     * @return condition bean definition builder, or null
     */
    @Nullable protected BeanDefinitionBuilder getActivationCondition(@Nonnull final Element config) {
        
        BeanDefinitionBuilder rpBuilder = null;
        BeanDefinitionBuilder phasesBuilder = null;
        
        if (config.hasAttributeNS(null, "relyingParties")) {
            if (config.hasAttributeNS(null, "excludeRelyingParties")) {
                log.warn("excludeRelyingParties ignored, using relyingParties");
            }
            rpBuilder = BeanDefinitionBuilder.genericBeanDefinition(RelyingPartyIdPredicate.class);
            rpBuilder.setFactoryMethod("fromCandidates");
            rpBuilder.addConstructorArgValue(
                    SpringSupport.getAttributeValueAsList(config.getAttributeNodeNS(null, "relyingParties")));
        } else if (config.hasAttributeNS(null, "excludeRelyingParties")) {
            final BeanDefinitionBuilder unnegated =
                    BeanDefinitionBuilder.genericBeanDefinition(RelyingPartyIdPredicate.class);
            unnegated.setFactoryMethod("fromCandidates");
            unnegated.addConstructorArgValue(
                    SpringSupport.getAttributeValueAsList(
                            config.getAttributeNodeNS(null, "excludeRelyingParties")));
            rpBuilder = BeanDefinitionBuilder.genericBeanDefinition(PredicateSupport.class);
            rpBuilder.setFactoryMethod("not");
            rpBuilder.addConstructorArgValue(unnegated.getBeanDefinition());
        }
        
        if (config.hasAttributeNS(null, "resolutionPhases")) {
            if (config.hasAttributeNS(null, "excludeResolutionPhases")) {
                log.warn("excludeResolutionPhases ignored, using resolutionPhases");
            }
            phasesBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResolutionLabelPredicate.class);
            phasesBuilder.addConstructorArgValue(
                    SpringSupport.getAttributeValueAsList(config.getAttributeNodeNS(null, "resolutionPhases")));
        } else if (config.hasAttributeNS(null, "excludeResolutionPhases")) {
            final BeanDefinitionBuilder unnegated =
                    BeanDefinitionBuilder.genericBeanDefinition(ResolutionLabelPredicate.class);
            unnegated.setFactoryMethod("byList");
            unnegated.addConstructorArgValue(
                    SpringSupport.getAttributeValueAsList(
                            config.getAttributeNodeNS(null, "excludeResolutionPhases")));
            phasesBuilder = BeanDefinitionBuilder.genericBeanDefinition(PredicateSupport.class);
            phasesBuilder.setFactoryMethod("not");
            phasesBuilder.addConstructorArgValue(unnegated.getBeanDefinition());
        }

        if (rpBuilder != null && phasesBuilder != null) {
            final BeanDefinitionBuilder andBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(PredicateSupport.class);
            andBuilder.setFactoryMethod("and");
            andBuilder.addConstructorArgValue(rpBuilder.getBeanDefinition());
            andBuilder.addConstructorArgValue(phasesBuilder.getBeanDefinition());
            return andBuilder;
        } else if (rpBuilder != null) {
            return rpBuilder;
        } else if (phasesBuilder != null) {
            return phasesBuilder;
        }
        
        return null;
    }
// Checkstyle: CyclomaticComplexity ON
    
    /** Controls parsing of Dependencies. 
     * 
     * If it is considered an invalid configuration for this resolver to have Dependency statements, return true. 
     * The surrounding logic will fail the parse.
     * @return false - by default.
     */
    protected boolean failOnDependencies() {
        return false;
    }

    /** Controls parsing of Dependencies. 
     * 
     * If it is considered an invalid configuration for this resolver to have Dependency statements, return true. 
     * The surrounding logic will issue warning.
     * @return false - by default.
     */
    protected boolean warnOnDependencies() {
        return false;
    }

    /**
     * Return a string which is to be prepended to all log messages.
     * 
     * This is always overridden by upper parsers, but to leave this abstract would break compatibility
     * 
     * @return a basic prefix.
     */
    @Nonnull @NotEmpty protected String getLogPrefix() {
        final StringBuilder builder = new StringBuilder("Unknown Plugin '").append(getDefinitionId()).append("':");
        return builder.toString();
    }

}