
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.opensaml.resource.ClasspathResource;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Test configuration code for attribute resolver.
 */
public class BasicAttributeResolverTest extends BaseConfigTestCase {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(BasicAttributeResolverTest.class);

    /** Application Context. */
    private ApplicationContext ac;

    /** {@inheritDoc} */
    public void setUp() throws Exception {
        super.setUp();
        configResources.add(new ClasspathResource("/shibboleth-2.0-config-internal.xml"));
        ac = createSpringContext();
    }

    /** Test Handle Request. */
    public void testResolverInstantiation() {
        try {
            AttributeResolver resolver = (AttributeResolver) ac.getBean("shibboleth.AttributeResolver");

            ShibbolethAttributeRequestContext context = new ShibbolethAttributeRequestContext();
            context.setPrincipalName("ttrojan");

            SortedSet<String> expected = new TreeSet<String>();
            expected.add("gpburdell");
            expected.add("ttrojan");

            Collection<Attribute> actual = resolver.resolveAttributes(context).values();

            assertEquals(1, actual.size());

            Attribute attribute = actual.iterator().next();
            assertEquals(2, attribute.getValues().size());
            assertEquals(expected, attribute.getValues());
        } catch (Exception e) {
            e.printStackTrace(System.out);
            fail(e.getMessage());
        }
    }
}