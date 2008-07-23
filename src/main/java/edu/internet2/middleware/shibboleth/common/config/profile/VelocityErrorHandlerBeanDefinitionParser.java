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

import edu.internet2.middleware.shibboleth.common.profile.provider.VelocityErrorHandler;

/**
 * Spring bean definition parser for {@link VelocityErrorHandler}s.
 */
public class VelocityErrorHandlerBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** Element name. */
    public static final String ELEMENT_NAME = "VelocityErrorHandler";

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(VelocityErrorHandlerBeanDefinitionParser.class);
    
    /** {@inheritDoc} */
    protected Class getBeanClass(Element arg0) {
        return VelocityErrorHandler.class;
    }

    /** {@inheritDoc} */
    protected void doParse(Element config, BeanDefinitionBuilder builder) {
        log.info("Parsing configuration for velocity error handler.");
        super.doParse(config, builder);        
        
        if(config.hasAttributeNS(null, "errorTemplatePath")){
            builder.addConstructorArgValue(config.getAttributeNS(null, "errorTemplatePath"));
        }else{
            builder.addConstructorArgValue(config.getAttributeNS(null, "/error.vm"));
        }

        builder.addConstructorArgReference(config.getAttributeNS(null, "velocityEngine"));
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }
}