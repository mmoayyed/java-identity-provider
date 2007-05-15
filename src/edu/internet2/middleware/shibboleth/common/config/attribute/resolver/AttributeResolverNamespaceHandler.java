
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

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
        registerBeanDefinitionParser(ShibbolethAttributeResolverBeanDefinitionParser.SCHEMA_TYPE,
                new ShibbolethProfileHandlerManagerBeanDefinitionParser());
    }
}