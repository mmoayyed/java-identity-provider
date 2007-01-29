
package edu.internet2.middleware.shibboleth.common.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;

/**
 * Spring namespace handler for {@link AttributeResolver}.
 */
public class ResolverNamespaceHandler extends NamespaceHandlerSupport {

    /**
     * Namespace for this handler.
     */
    public static final String NS = "urn:mace:shibboleth:2.0:resolver";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser("AttributeResolver", new AttributeResolverBeanDefinitionParser());
    }

}