
package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.io.IOException;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
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

    /** Constructor. */
    public BasicAttributeResolverTest() {

    }

    /** {@inheritDoc} */
    public void setUp() throws IOException {
        ac = createSpringContext("data/edu/internet2/middleware/shibboleth/common/config/resolver/resolver.xml");
    }

    /**
     * Test Handle Request.
     * 
     */
    public void testResolverInstantiation() {
        AttributeResolver resolver = (AttributeResolver) ac
                .getBean("edu.internet2.middleware.shibboleth.common.config.resolver.AttributeResolverFactoryBean");
        
        ShibbolethAttributeRequestContext context = new ShibbolethAttributeRequestContext();
        context.setPrincipalName("ttrojan");
        
        SortedSet<String> expected = new TreeSet<String>();
        expected.add("gpburdell");
        expected.add("ttrojan");
        
        try {
                Collection<Attribute> actual = resolver.resolveAttributes(context).values();
            
            assertEquals(1, actual.size());
            
            Attribute attribute = actual.iterator().next();
            assertEquals(2, attribute.getValues().size());
            assertEquals(expected, attribute.getValues());
        } catch (AttributeResolutionException e) {
            fail(e.getMessage());
        }
        
    }

}