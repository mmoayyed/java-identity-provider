
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethSAMLAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Test configuration code for attribute resolver.
 */
public class BasicAttributeResolverTest extends BaseConfigTestCase {

    /** Test Handle Request. */
    public void testResolverInstantiation() throws Exception{
        ApplicationContext ac = createSpringContext(DATA_PATH + "/config/attribute/resolver/service-config.xml");
        AttributeResolver resolver = (AttributeResolver) ac.getBean("resolver");

        ShibbolethSAMLAttributeRequestContext context = new ShibbolethSAMLAttributeRequestContext();
        context.setPrincipalName("ttrojan");

        SortedSet<String> expected = new TreeSet<String>();
        expected.add("gpburdell");
        expected.add("ttrojan");

        Collection<BaseAttribute> actual = resolver.resolveAttributes(context).values();

        assertEquals(1, actual.size());

        BaseAttribute attribute = actual.iterator().next();
        assertEquals(2, attribute.getValues().size());
        assertEquals(expected, attribute.getValues());
    }
}