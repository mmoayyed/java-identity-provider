/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.attribute.filtering.match.basic;

import javax.xml.namespace.QName;

import org.opensaml.xml.util.DatatypeHelper;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.NumOfAttributeValuesMatchFunctor;
import edu.internet2.middleware.shibboleth.common.config.attribute.filtering.BaseFilterBeanDefinitionParser;

/**
 * Bean definition parser for {@link NumOfAttributeValuesMatchFunctor}s.
 */
public class NumOfAttributeValuesMatchFunctorBeanDefinitionParser extends BaseFilterBeanDefinitionParser {

    /** Schema type. */
    public static final QName SCHEMA_TYPE = new QName(BasicMatchFunctorNamespaceHandler.NAMESPACE,
            "NumberOfAttributeValues");

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return NumOfAttributeValuesMatchFunctor.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element configElement, BeanDefinitionBuilder builder) {
        super.doParse(configElement, builder);

        builder.addConstructorArg(DatatypeHelper
                .safeTrimOrNullString(configElement.getAttributeNS(null, "attributeId")));
        
        builder.addConstructorArg(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, "minimum")));
        
        builder.addConstructorArg(DatatypeHelper.safeTrimOrNullString(configElement.getAttributeNS(null, "maximum")));
    }
}