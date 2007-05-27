
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import org.springframework.beans.factory.xml.BeanDefinitionParser;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;
import edu.internet2.middleware.shibboleth.common.config.profile.ShibbolethProfileHandlerManagerBeanDefinitionParser;

/**
 * Spring namespace handler for the Shibboleth resolver namespace.
 */
public class AttributeResolverNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver";

    /** {@inheritDoc} */
    public void init() {
        BeanDefinitionParser parser = new ShibbolethProfileHandlerManagerBeanDefinitionParser();
        registerBeanDefinitionParser(ShibbolethAttributeResolverBeanDefinitionParser.SCHEMA_TYPE, parser);
        
        parser = new AttributeResolverBeanDefinitionParser();
        registerBeanDefinitionParser(AttributeResolverBeanDefinitionParser.SCHEMA_TYPE, parser);
        registerBeanDefinitionParser(AttributeResolverBeanDefinitionParser.ELEMENT_NAME, parser);
    }
}