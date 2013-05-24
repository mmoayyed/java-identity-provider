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

package net.shibboleth.attribute.filtering.spring;

import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.security.IdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.security.RandomIdentifierGenerationStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import com.google.common.base.Strings;

// TODO incomplete v2 port
/**
 * Base class for Spring bean definition parsers within the filter engine configuration. This base class is responsible
 * for generating an ID for the Spring bean that is unique within all the policy components loaded.
 */
public abstract class BaseFilterParser extends AbstractSingleBeanDefinitionParser {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(BaseFilterParser.class);

    /** Generator of unique IDs. */
    // TODO correct random identifier ?
    private static IdentifierGenerationStrategy idGen = new RandomIdentifierGenerationStrategy();

    /**
     * Generates an ID for a filter engine component. If the given localId is null a random one will be generated.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param localId local id or null
     * 
     * @return unique ID for the component
     */
    protected String getQualifiedId(Element configElement, String componentNamespace, String localId) {
        Element afpgElement = configElement.getOwnerDocument().getDocumentElement();
        String policyGroupId = StringSupport.trimOrNull(afpgElement.getAttributeNS(null, "id"));

        StringBuilder qualifiedId = new StringBuilder();
        qualifiedId.append("/");
        qualifiedId.append(AttributeFilterPolicyGroupParser.ELEMENT_NAME.getLocalPart());
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

        // TODO remove debug logging
        log.debug("getQualifiedId element {} qualified id '{}'", configElement, qualifiedId.toString());

        return qualifiedId.toString();
    }

    /**
     * Gets the absolute reference given a possibly relative reference.
     * 
     * @param configElement component configuration element
     * @param componentNamespace namespace for the component
     * @param reference reference to convert into absolute form
     * 
     * @return absolute form of the reference
     */
    protected String getAbsoluteReference(Element configElement, String componentNamespace, String reference) {
        if (reference.startsWith("/")) {
            return reference;
        } else {
            return getQualifiedId(configElement, componentNamespace, reference);
        }
    }
}