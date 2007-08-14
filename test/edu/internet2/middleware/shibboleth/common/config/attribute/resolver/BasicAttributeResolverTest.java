
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;

/**
 * Test configuration code for attribute resolver.
 */
public class BasicAttributeResolverTest extends BaseConfigTestCase {

    /** Test Handle Request. */
    @SuppressWarnings("unchecked")
    public void testResolverInstantiation() throws Exception {
        ApplicationContext ac = createSpringContext(DATA_PATH + "/config/attribute/resolver/service-config.xml");
        AttributeResolver resolver = (AttributeResolver) ac.getBean("resolver");

        BaseSAMLProfileRequestContext context = new BaseSAMLProfileRequestContext();
        context.setPrincipalName("ttrojan");

        Map<String, BaseAttribute> actual = resolver.resolveAttributes(context);

        assertEquals(3, actual.size());

        BaseAttribute principalName = actual.get("principalName");
        assertEquals(1, principalName.getValues().size());

        BaseAttribute affiliation = actual.get("eduPersonAffilation");
        assertEquals(3, affiliation.getValues().size());

        BaseAttribute entitlement = actual.get("eduPersonEntitlement");
        assertEquals(1, entitlement.getValues().size());
    }
}