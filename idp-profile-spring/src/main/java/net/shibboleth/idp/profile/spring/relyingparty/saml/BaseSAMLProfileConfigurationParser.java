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

package net.shibboleth.idp.profile.spring.relyingparty.saml;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.shibboleth.idp.saml.profile.config.BasicSAMLArtifactConfiguration;
import net.shibboleth.idp.saml.profile.config.logic.LegacyEncryptionRequirementPredicate;
import net.shibboleth.idp.saml.profile.config.logic.LegacySigningRequirementPredicate;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import com.google.common.base.Predicate;

/**
 * Parser for all classes which extend {@link net.shibboleth.idp.saml.profile.config.AbstractSAMLProfileConfiguration}
 * and for elements which inherit from <code>saml:SAMLProfileConfigutationType</code>.
 */
public abstract class BaseSAMLProfileConfigurationParser extends AbstractSingleBeanDefinitionParser {

    /** default value when assertionLifetime isn't set. */
    private static final long DEFAULT_ASSERTION_LIFETIME = 300000L;
    
    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(BaseSAMLProfileConfigurationParser.class);

    /** Flag controlling whether to parse artifact configuration. */
    private boolean artifactAware;

    /**
     * Set whether to parse artifact configuration.
     * 
     * @param flag  flag to set
     */
    protected void setArtifactAware(final boolean flag) {
        artifactAware = flag;
    }
    
    /**
     * Construct the builder for the the artifact configuration.
     * 
     * @param element The element under consideration
     * @return the builder.
     * 
     */
    @Nullable protected BeanDefinition getArtifactConfiguration(Element element) {

        final BeanDefinitionBuilder definition =
                BeanDefinitionBuilder.genericBeanDefinition(BasicSAMLArtifactConfiguration.class);

        if (element.hasAttributeNS(null, "artifactType")) {
            definition.addPropertyValue("artifactType", element.getAttributeNS(null, "artifactType"));
        }

        if (element.hasAttributeNS(null, "artifactResolutionServiceURL")) {
            definition.addPropertyValue("artifactResolutionServiceURL",
                    element.getAttributeNS(null, "artifactResolutionServiceURL"));
        } else {
            definition.addPropertyReference("artifactResolutionServiceURL", getProfileBeanNamePrefix()
                    + "ArtifactServiceURL");
        }
        
        if (element.hasAttributeNS(null, "artifactResolutionServiceIndex")) {
            definition.addPropertyValue("artifactResolutionServiceIndex",
                    element.getAttributeNS(null, "artifactResolutionServiceIndex"));
        } else {
            definition.addPropertyReference("artifactResolutionServiceIndex", getProfileBeanNamePrefix()
                    + "ArtifactServiceIndex");
        }

        return definition.getBeanDefinition();
    }

    /**
     * Return the definition describing the predicate associated with the provided value, using the default if none
     * specified.
     * 
     * @param value the value as a string should be "always", "conditional", "never"
     * @param defaultValue the default value if no explicit one.
     * @param claz the predicate type to summon up (one of {@link LegacySigningRequirementPredicate} or
     *            {@link LegacyEncryptionRequirementPredicate}
     * @return the bean for the appropriate predicate.
     */
    @Nonnull private BeanDefinition predicateFor(@Nullable String value, String defaultValue,
            Class<? extends Predicate> claz) {

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(claz);
        final String trimmedValue = StringSupport.trimOrNull(value);
        if (null != trimmedValue) {
            builder.addConstructorArgValue(trimmedValue);
        } else {
            builder.addConstructorArgValue(StringSupport.trimOrNull(defaultValue));
        }

        return builder.getBeanDefinition();
    }

    /**
     * Return the definition of the predicate for encryption derived from the provided string.
     * 
     * @param value the value
     * @param defaultValue the default.
     * @return the definition of an appropriate {@link LegacyEncryptionRequirementPredicate}
     */
    @Nonnull protected BeanDefinition predicateForSigning(@Nullable String value, String defaultValue) {
        return predicateFor(value, defaultValue, LegacySigningRequirementPredicate.class);
    }

    /**
     * Return the definition of the predicate for signing derived from the provided string.
     * 
     * @param value the value
     * @param defaultValue the default.
     * @return the definition of an appropriate {@link LegacyEncryptionRequirementPredicate}
     */
    @Nonnull protected BeanDefinition predicateForEncryption(@Nullable String value, String defaultValue) {
        return predicateFor(value, defaultValue, LegacyEncryptionRequirementPredicate.class);
    }

    /**
     * Get the list of audiences from the &lt;Audience&gt; sub-elements.
     * 
     * @param element the element under discussion
     * @return the list of elements (which are subject to property replacement)
     */
    protected List<String> getAudiences(Element element) {
        List<Element> audienceElems =
                ElementSupport.getChildElementsByTagNameNS(element, RelyingPartySAMLNamespaceHandler.NAMESPACE,
                        "Audience");
        List<String> result = new ManagedList<>(audienceElems.size());
        for (Element audienceElement : audienceElems) {
            final String audience = StringSupport.trimOrNull(audienceElement.getTextContent());
            if (null != audience) {
                result.add(audience);
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        super.doParse(element, parserContext, builder);
        
        if (element.hasAttributeNS(null, "assertionLifetime")) {
            // Set as a string and let the converter to the work
            builder.addPropertyValue("assertionLifetime", element.getAttributeNS(null, "assertionLifetime"));
        } else {
            log.debug("assertionLifetime not specified, defaulting to {}", DEFAULT_ASSERTION_LIFETIME);
            builder.addPropertyValue("assertionLifetime", DEFAULT_ASSERTION_LIFETIME);
        }

        if (element.hasAttributeNS(null, "includeConditionsNotBefore")) {
            builder.addPropertyValue("includeConditionsNotBefore",
                    element.getAttributeNS(null, "includeConditionsNotBefore"));
        } else {
            log.debug("includeConditionsNotBefore not specified, defaulting to 'true'");
            builder.addPropertyValue("includeConditionsNotBefore", true);
        }

        if (artifactAware) {
            builder.addPropertyValue("artifactConfiguration", getArtifactConfiguration(element));
        }

        if (element.hasAttributeNS(null, "attributeAuthority")) {
            log.warn("Deprecated attribute 'attributeAuthority=\"{}\"' has been ignored",
                    element.getAttributeNS(null, "attributeAuthority"));
        }

        if (element.hasAttributeNS(null, "securityPolicyRef")) {
            log.warn("Deprecated attribute 'securityPolicyRef=\"{}\"' has been ignored",
                    element.getAttributeNS(null, "securityPolicyRef"));
        }

        if (element.hasAttributeNS(null, "outboundArtifactType")) {
            log.warn("Deprecated attribute 'outboundArtifactType=\"{}\"' has been ignored",
                    element.getAttributeNS(null, "outboundArtifactType"));
        }
        
        if (element.hasAttributeNS(null, "inboundFlowId")) {
            builder.addPropertyValue("inboundSubflowId", element.getAttributeNS(null, "inboundFlowId"));
        } else {
            builder.addPropertyReference("inboundSubflowId", getProfileBeanNamePrefix()+"InboundFlowId");
        }

        builder.addPropertyValue("outboundSubflowId", element.getAttributeNS(null, "outboundFlowId"));
        
        if (element.hasAttributeNS(null, "signingCredentialRef")) {
            log.warn("I do not (yet) know how to handle 'signingCredential=\"{}\"' yet",
                    element.getAttributeNS(null, "signingCredentialRef"));
        }

        builder.addPropertyValue("signAssertionsPredicate",
                predicateForSigning(element.getAttributeNS(null, "signAssertions"), getSignAssertionsDefault()));
        builder.addPropertyValue("signRequestsPredicate",
                predicateForSigning(element.getAttributeNS(null, "signRequests"), "conditional"));
        builder.addPropertyValue("signResponsesPredicate",
                predicateForSigning(element.getAttributeNS(null, "signResponses"), getSignResponsesDefault()));

        builder.addPropertyValue("additionalAudienceForAssertion", getAudiences(element));
    }

    /** {@inheritDoc} */
    @Override protected boolean shouldGenerateId() {
        return true;
    }

    /**
     * Get the prefix for the default beans. This prefix will have on of "ArtifactServiceURL", "ArtifactServiceId" or
     * "InboundFlowId" appended as a bean name.
     * 
     * @return the prefix
     */
    protected abstract String getProfileBeanNamePrefix();

    /**
     * Gets the default value for the signResponses property.
     * 
     * @return default value for the signResponses property
     */
    protected abstract String getSignResponsesDefault();

    /**
     * Gets the default value for the signAssertions property.
     * 
     * @return default value for the signAssertions property
     */
    protected abstract String getSignAssertionsDefault();

}
