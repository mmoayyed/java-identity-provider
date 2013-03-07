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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

/**
 * An extension to the standard {@link XmlBeanDefinitionReader} that defaults some settings.
 * <ul>
 * <li>documentReaderClass property is set to {@link SchemaTypeAwareBeanDefinitionDocumentReader}</li>
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

        setDocumentReaderClass(SchemaTypeAwareBeanDefinitionDocumentReader.class);

        // this also sets namespaceAware to true
        setValidating(false);
    }
}