
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.dataConnector;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth static data connector namespace.
 */
public class DataConnectorNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:dc";
    
    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(StaticDataConnectorBeanDefinitionParser.TYPE_NAME,
                new StaticDataConnectorBeanDefinitionParser());
        registerBeanDefinitionParser(RDBMSDataConnectorBeanDefinitionParser.TYPE_NAME,
                new RDBMSDataConnectorBeanDefinitionParser());
        registerBeanDefinitionParser(LdapDataConnectorBeanDefinitionParser.TYPE_NAME,
                new LdapDataConnectorBeanDefinitionParser());
    }

}