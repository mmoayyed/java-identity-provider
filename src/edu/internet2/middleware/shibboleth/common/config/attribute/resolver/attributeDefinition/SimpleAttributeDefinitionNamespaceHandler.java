
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth simple attribute definition namespace.
 */
public class SimpleAttributeDefinitionNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:ad:simple";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(SimpleAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new SimpleAttributeDefinitionBeanDefinitionParser());
        registerBeanDefinitionParser(ScopedAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new ScopedAttributeDefinitionBeanDefinitionParser());
    }

}