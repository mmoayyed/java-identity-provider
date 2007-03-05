
package edu.internet2.middleware.shibboleth.common.config.resolver;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth resolver namespace.
 */
public class StaticDataConnectorNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:dc:static";
    
    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(StaticDataConnectorBeanDefinitionParser.TYPE_NAME,
                new StaticDataConnectorBeanDefinitionParser());
    }

}