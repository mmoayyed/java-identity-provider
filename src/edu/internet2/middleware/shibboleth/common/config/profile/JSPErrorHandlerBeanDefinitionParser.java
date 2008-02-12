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

package edu.internet2.middleware.shibboleth.common.config.profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.profile.provider.JSPErrorHandler;

/**
 * Spring bean definition parser for {@link JSPErrorHandler}s.
 */
public class JSPErrorHandlerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final String ELEMENT_NAME = "JSPErrorHandler";

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(JSPErrorHandlerBeanDefinitionParser.class);

    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return JSPErrorHandler.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, BeanDefinitionBuilder builder) {
        log.info("Parsing configuration for JSP error handler.");
        super.doParse(config, builder);

        builder.addConstructorArg(config.getAttributeNS(null, "jspPagePath"));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}