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

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.core.env.StandardEnvironment;
import org.w3c.dom.Element;

/**
 * An extension to the standard {@link DefaultBeanDefinitionDocumentReader} that uses a
 * {@link SchemaTypeAwareBeanDefinitionParserDelegate} delegate for processing bean definitions.
 */
public class SchemaTypeAwareBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader {

    /** {@inheritDoc} */
    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root,
            BeanDefinitionParserDelegate parentDelegate) {
        BeanDefinitionParserDelegate delegate =
                new SchemaTypeAwareBeanDefinitionParserDelegate(readerContext, new StandardEnvironment());
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

}
