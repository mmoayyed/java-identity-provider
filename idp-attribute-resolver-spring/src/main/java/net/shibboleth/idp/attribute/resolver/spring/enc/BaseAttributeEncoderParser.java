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

package net.shibboleth.idp.attribute.resolver.spring.enc;

import javax.annotation.Nonnull;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.idp.attribute.resolver.spring.impl.AttributeResolverNamespaceHandler;
import net.shibboleth.idp.profile.logic.RelyingPartyIdPredicate;
import net.shibboleth.idp.profile.logic.ScriptedPredicate;
import net.shibboleth.idp.profile.spring.relyingparty.metadata.ScriptTypeBeanParser;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * Base class for Spring bean definition parser for attribute encoders.
 */
public abstract class BaseAttributeEncoderParser extends AbstractSingleBeanDefinitionParser {

    /** Local name of name attribute. */
    @Nonnull @NotEmpty public static final String NAME_ATTRIBUTE_NAME = "name";

    /** Log4j logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(BaseAttributeEncoderParser.class);

    /** Whether the name property is required or not. */
    private boolean nameRequired;

    /** Constructor. */
    public BaseAttributeEncoderParser() {
        nameRequired = false;
    }

    /**
     * Set whether the name property is required or not.
     * 
     * @param flag flag to set
     */
    public void setNameRequired(final boolean flag) {
        nameRequired = flag;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(final Element config, final ParserContext parserContext,
            final BeanDefinitionBuilder builder) {

        super.doParse(config, parserContext, builder);
        
        final String attributeName = StringSupport.trimOrNull(config.getAttributeNS(null, NAME_ATTRIBUTE_NAME));
        if (nameRequired && attributeName == null) {
            throw new BeanCreationException("Attribute encoder must contain a name property");
        }

        if (config.hasAttributeNS(null, "activationConditionRef")) {
            if (config.hasAttributeNS(null, "relyingParties")) {
                log.warn("relyingParties ignored, using activationConditionRef");
            }
            builder.addPropertyReference("activationCondition",
                    StringSupport.trimOrNull(config.getAttributeNS(null, "activationConditionRef")));
        } else if (config.hasAttributeNS(null, "relyingParties")) {
            final BeanDefinitionBuilder rpBuilder =
                    BeanDefinitionBuilder.genericBeanDefinition(RelyingPartyIdPredicate.class);
            rpBuilder.setFactoryMethod("fromCandidates");
            rpBuilder .addConstructorArgValue(
                    SpringSupport.getAttributeValueAsList(config.getAttributeNodeNS(null, "relyingParties")));
            builder.addPropertyValue("activationCondition", rpBuilder.getBeanDefinition());
        } else {
            final Element child = ElementSupport.getFirstChildElement(config);
            if (child != null && ElementSupport.isElementNamed(child,
                    AttributeResolverNamespaceHandler.NAMESPACE, "ActivationConditionScript")) {
                builder.addPropertyValue("activationCondition",
                        ScriptTypeBeanParser.parseScriptType(ScriptedPredicate.class, child).getBeanDefinition());
            }
        }

        if (config.hasAttributeNS(null, "encodeType")) {
            builder.addPropertyValue("encodeType", StringSupport.trimOrNull(config.getAttributeNS(null, "encodeType")));
        }

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
        builder.addPropertyValue("name", attributeName);

    }

    /** {@inheritDoc} */
    @Override public boolean shouldGenerateId() {
        return true;
    }

    /**
     * {@inheritDoc}. <br/>
     * We parse the attribute "name" and we do not want Spring to. see #IDP-571.
     */
    @Override protected boolean shouldParseNameAsAliases() {
        return false;
    }

}