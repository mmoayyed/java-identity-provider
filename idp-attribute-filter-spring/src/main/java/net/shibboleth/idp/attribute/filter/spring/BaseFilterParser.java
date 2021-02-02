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

package net.shibboleth.idp.attribute.filter.spring;

import java.util.Collection;
import java.util.HashSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.namespace.QName;

import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.base.Strings;

import net.shibboleth.ext.spring.util.AbstractCustomBeanDefinitionParser;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

/**
 * Base class for Spring bean definition parsers within the filter engine configuration.
 * 
 * <p>
 * This base class is responsible for generating an ID for the Spring bean that is unique within all the policy
 * components loaded. This in turn underpins our implementation of referencing in the language.
 * </p>
 */
public abstract class BaseFilterParser extends AbstractCustomBeanDefinitionParser {

    /** Namespace The Top level filters. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:afp";

    /** Element name. */
    public static final QName AFP_ELEMENT_NAME = new QName(NAMESPACE,
            "AttributeFilterPolicyGroup");

    /** The PolicyRequirementRule QName. */
    public static final QName POLICY_REQUIREMENT_RULE = new QName(BaseFilterParser.NAMESPACE,
            "PolicyRequirementRule");

    /** PermitValueRule. */
    public static final QName PERMIT_VALUE_RULE = new QName(NAMESPACE,
            "PermitValueRule");

    /** DenyValueRule. */
    public static final QName DENY_VALUE_RULE = new QName(NAMESPACE, "DenyValueRule");
    
    /** The attribute name for the qualified id.*/
    public static final String QUALIFIED_ID = "qualifiedId";

    /** Generator of unique IDs. */
    private static IdentifierGenerationStrategy idGen = new RandomIdentifierGenerationStrategy();

    /** Class logger. */
    private static final Logger LOG = LoggerFactory.getLogger(BaseFilterParser.class);

    /**
     * Generates an ID for a filter engine component. If the given localId is null a random one will be generated.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param localId local id or null
     * 
     * @return unique ID for the component
     */
    @Nonnull @NotEmpty protected String getQualifiedId(@Nonnull final Element configElement,
            @Nonnull final String componentNamespace, @Nullable final String localId) {
        final Element afpgElement = configElement.getOwnerDocument().getDocumentElement();
        final String policyGroupId = StringSupport.trimOrNull(afpgElement.getAttributeNS(null, "id"));

        final StringBuilder qualifiedId = new StringBuilder();
        qualifiedId.append("/");
        qualifiedId.append(BaseFilterParser.AFP_ELEMENT_NAME.getLocalPart());
        qualifiedId.append(":");
        qualifiedId.append(policyGroupId);
        if (!Strings.isNullOrEmpty(componentNamespace)) {
            qualifiedId.append("/");
            qualifiedId.append(componentNamespace);
            qualifiedId.append(":");

            if (Strings.isNullOrEmpty(localId)) {
                qualifiedId.append(idGen.generateIdentifier());
            } else {
                qualifiedId.append(localId);
            }
        }

        return qualifiedId.toString();
    }

    /**
     * Gets the absolute reference given a possibly relative reference.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param reference Reference to convert into an absolute
     * 
     * @return absolute form of the reference
     */
    @Nonnull @NotEmpty protected String getAbsoluteReference(@Nonnull final Element configElement,
            @Nonnull @NotEmpty final String componentNamespace, @Nonnull @NotEmpty final String reference) {
        if (reference.startsWith("/")) {
            return reference;
        }
        return getQualifiedId(configElement, componentNamespace, reference);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * Calculate the qualified id once, and set both the id property as well as a qualified id metadata attribute used
     * by the {@link #resolveId(Element, AbstractBeanDefinition, ParserContext)} method.
     * </p>
     * 
     * <p>
     * If we auto-generate a name then we issue a warning so users can (1) correct this, but also so they can make sense
     * of the logging in the filters which uses the id extensively.
     * </p>
     */
    @Override protected void doParse(@Nonnull final Element element, @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder builder) {
        
        super.doParse(element, parserContext, builder);

        builder.setInitMethodName("initialize");
        builder.setDestroyMethodName("destroy");
        
        // We use a prototype scope because it eliminates the overhead during context closure,
        // which is a problem when there are thousands of singletons. This means the destroy()
        // method above will NOT be called for any bean parsed by this class.
        builder.setScope(BeanDefinition.SCOPE_PROTOTYPE);

        final String suppliedId = StringSupport.trimOrNull(element.getAttributeNS(null, "id"));
        final String generatedId = getQualifiedId(element, element.getLocalName(), suppliedId);

        if (suppliedId == null) {
            LOG.trace("Element '{}' did not contain an 'id' attribute.  Generated id '{}' will be used",
                    element.getLocalName(), generatedId);

        } else {
            LOG.debug("Element '{}' 'id' attribute '{}' is mapped to '{}'", element.getLocalName(), suppliedId,
                    generatedId);
        }

        builder.getBeanDefinition().setAttribute(BaseFilterParser.QUALIFIED_ID, generatedId);
    }

    /** {@inheritDoc} */
    @Override @Nonnull @NotEmpty protected String resolveId(@Nonnull final Element configElement,
            @Nonnull final AbstractBeanDefinition beanDefinition, @Nonnull final ParserContext parserContext) {
        return beanDefinition.getAttribute(BaseFilterParser.QUALIFIED_ID).toString();
    }

    /**
     * Is this inside a &lt;PolicyRequirementRule&gt; or an permit or deny rule?.
     * 
     * <p>
     * This is used when parsing the various rules (&lt;MatchFunctorType&gt;) since the bean we summon up depends on
     * where we find ourselves.
     * </p>
     * 
     * @param element the element under question
     * @return true if it is inside a policy requirement rule, false otherwise.
     */
    protected boolean isPolicyRule(@Nonnull final Element element) {

        Element elem = element;
        do {
            if (ElementSupport.isElementNamed(elem, BaseFilterParser.POLICY_REQUIREMENT_RULE)) {
                return true;
            } else if (ElementSupport.isElementNamed(elem, BaseFilterParser.DENY_VALUE_RULE)
                    || ElementSupport.isElementNamed(elem, BaseFilterParser.PERMIT_VALUE_RULE)) {
                return false;
            }
            elem = ElementSupport.getElementAncestor(elem);
        } while (elem != null);
        LOG.warn("Element '{}' : could not find schema defined parent");
        return false;
    }
    
    /**
     * Parse list of elements into bean definitions which are inserted into the parent context.
     * This is like {{@link SpringSupport#parseCustomElements(Collection, ParserContext)} but with
     * qualifiedName warnings (and none of the parent bena stuff - which we do not need)
     *
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     *
     */
    @Nullable public static void parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements, @Nonnull final ParserContext parserContext) {
        if (elements == null) {
            return;
        }
        
        final HashSet<String> beanNames = new HashSet<>(elements.size());
        for (final Element e : elements) {
            if (e != null) {
                final BeanDefinition def = parserContext.getDelegate().parseCustomElement(e, null);
                final Object name = def.getAttribute(QUALIFIED_ID);
                if (name != null && !beanNames.add(name.toString())) {
                   LOG.warn("Duplicate filter element id '{}' found", name);
               }
            }
        }
    }

    /**
     * Parse list of elements into bean definitions.
     * This is like {{@link SpringSupport#parseCustomElements(Collection, ParserContext, BeanDefinitionBuilder)}
     * but with qualifiedName warnings.
     * 
     * @param elements list of elements to parse
     * @param parserContext current parsing context
     * @param parentBuilder the builder we are going to insert into
     * 
     * @return list of bean definitions
     */
    @Nullable public static ManagedList<BeanDefinition> parseCustomElements(
            @Nullable @NonnullElements final Collection<Element> elements,
            @Nonnull final ParserContext parserContext,
            @Nonnull final BeanDefinitionBuilder parentBuilder) {

        if (elements == null) {
            return null;
        }
        Constraint.isNotNull(parentBuilder, "parentBuilder must not be null");

        final ManagedList<BeanDefinition> definitions = new ManagedList<>(elements.size());
        final HashSet<String> beanNames = new HashSet<>(elements.size());
        for (final Element e : elements) {
            if (e != null) {
                final BeanDefinition def = SpringSupport.parseCustomElement(e, parserContext, parentBuilder, false);
                definitions.add(def);
                final Object name = def.getAttribute(QUALIFIED_ID);
                if (name != null && !beanNames.add(name.toString())) {
                    LOG.warn("Duplicate filter element name {} found", name);
                }
            }
        }

        return definitions;
    }

}
