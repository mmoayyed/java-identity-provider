
package edu.internet2.middleware.shibboleth.common.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.impl.AttributeResolverImpl;

/**
 * Spring Bean Definition Parser for Shibboleth Attribute Resolver.
 */
public class AttributeResolverBeanDefinitionParser extends AbstractSingleBeanDefinitionParser {

    /** {@inheritDoc} */
    protected Class getBeanClass(Element e) {
        return AttributeResolverImpl.class;
    }

    /** {@inheritDoc} */
    protected boolean shouldGenerateId() {
        return true;
    }

    /** {@inheritDoc} */
    protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
        builder.setInitMethodName("validate");
        super.doParse(element, parserContext, builder);
    }

}