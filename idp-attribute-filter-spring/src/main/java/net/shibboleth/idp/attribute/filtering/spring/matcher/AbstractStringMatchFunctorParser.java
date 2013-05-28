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

package net.shibboleth.idp.attribute.filtering.spring.matcher;

import net.shibboleth.idp.attribute.filtering.spring.MatcherParser;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import net.shibboleth.utilities.java.support.xml.AttributeSupport;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.w3c.dom.Element;

// TODO first port from v2
/**
 * Base class for string matching functors.
 */
public abstract class AbstractStringMatchFunctorParser extends MatcherParser {

    /** {@inheritDoc} */
    protected void doParse(Element configElement, BeanDefinitionBuilder builder) {
        super.doParse(configElement, builder);

        builder.addPropertyValue("matchString", StringSupport.trimOrNull(configElement.getAttributeNS(null, "value")));

        boolean ignoreCase = false;
        if (configElement.hasAttributeNS(null, "ignoreCase")) {
            ignoreCase =
                    AttributeSupport.getAttributeValueAsBoolean(configElement.getAttributeNodeNS(null, "ignoreCase"));
        }
        builder.addPropertyValue("caseSensitive", !ignoreCase);
    }
}