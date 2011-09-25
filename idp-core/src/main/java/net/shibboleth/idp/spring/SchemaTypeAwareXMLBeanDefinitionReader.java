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

package net.shibboleth.idp.spring;

import org.opensaml.util.xml.DomTypeSupport;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

/**
 * An extension to the standard {@link XmlBeanDefinitionReader} that defaults some settings.
 * <ul>
 * <li>documentReaderClass property is set to {@link SchemaTypeAwareBeanDefinitionDocmentReader}</li>
 * <li>namespaceAware property is set to true</li>
 * <li>validating property is set to false</li>
 * </ul>
 */
public class SchemaTypeAwareXMLBeanDefinitionReader extends XmlBeanDefinitionReader {

    /**
     * Constructor.
     * 
     * @param beanRegistry the bean definition registry that will be populated by this reader
     */
    public SchemaTypeAwareXMLBeanDefinitionReader(BeanDefinitionRegistry beanRegistry) {
        super(beanRegistry);

        setDocumentReaderClass(SchemaTypeAwareBeanDefinitionDocmentReader.class);

        // this also sets namespaceAware to true
        setValidating(false);
    }

    /**
     * An extension to the standard {@link DefaultBeanDefinitionDocumentReader} that uses the
     * {@link SchemaTypeAwareBeanDefinitionParserDelegate} delegate for processing bean definitions.
     */
    protected class SchemaTypeAwareBeanDefinitionDocmentReader extends DefaultBeanDefinitionDocumentReader {

        /** {@inheritDoc} */
        protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root) {
            BeanDefinitionParserDelegate delegate = new SchemaTypeAwareBeanDefinitionParserDelegate(readerContext);
            delegate.initDefaults(root);
            return delegate;
        }
    }

    /**
     * An extension to the standard {@link BeanDefinitionParserDelegate} that adds support for retrieving
     * {@link NamespaceHandler} by schema type, as well as element QName, when resolving custom elements. In the case
     * where a {@link NamespaceHandler} is registered for both the schema type and element QName for a custom element
     * the schema registered handler is preferred.
     */
    protected class SchemaTypeAwareBeanDefinitionParserDelegate extends BeanDefinitionParserDelegate {

        /**
         * Constructor.
         * 
         * @param readerContext current XML reader context
         */
        public SchemaTypeAwareBeanDefinitionParserDelegate(XmlReaderContext readerContext) {
            super(readerContext);
        }

        /** {@inheritDoc} */
        public BeanDefinition parseCustomElement(Element element) {
            return parseCustomElement(element, null);
        }

        /** {@inheritDoc} */
        public BeanDefinition parseCustomElement(Element element, BeanDefinition containingBd) {
            String namespaceUri = element.getNamespaceURI();
            if (DomTypeSupport.hasXSIType(element)) {
                namespaceUri = DomTypeSupport.getXSIType(element).getNamespaceURI();
            }

            NamespaceHandler handler = getReaderContext().getNamespaceHandlerResolver().resolve(namespaceUri);
            if (handler == null) {
                error("Unable to locate NamespaceHandler for namespace [" + namespaceUri + "]", element);
                return null;
            }

            return handler.parse(element, new ParserContext(getReaderContext(), this, containingBd));
        }
    }
}