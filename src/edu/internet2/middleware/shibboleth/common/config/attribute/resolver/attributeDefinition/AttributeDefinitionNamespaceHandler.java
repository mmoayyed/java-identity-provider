
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver.attributeDefinition;

import edu.internet2.middleware.shibboleth.common.config.BaseSpringNamespaceHandler;

/**
 * Spring namespace handler for the Shibboleth simple attribute definition namespace.
 */
public class AttributeDefinitionNamespaceHandler extends BaseSpringNamespaceHandler {

    /** Namespace for this handler. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resolver:ad";

    /** {@inheritDoc} */
    public void init() {
        registerBeanDefinitionParser(SimpleAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new SimpleAttributeDefinitionBeanDefinitionParser());

        registerBeanDefinitionParser(ScopedAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new ScopedAttributeDefinitionBeanDefinitionParser());

        registerBeanDefinitionParser(ScriptedAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new ScriptedAttributeDefinitionBeanDefinitionParser());

        registerBeanDefinitionParser(PrincipalNameAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new PrincipalNameAttributeDefinitionBeanDefinitionParser());

        registerBeanDefinitionParser(PrincipalAuthenticationMethodAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new PrincipalAuthenticationMethodAttributeDefinitionBeanDefinitionParser());
        
        registerBeanDefinitionParser(MappedAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new MappedAttributeDefinitionBeanDefinitionParser());
        
        registerBeanDefinitionParser(TemplateAttributeDefinitionBeanDefinitionParser.TYPE_NAME,
                new TemplateAttributeDefinitionBeanDefinitionParser());
    }
}