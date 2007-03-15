
package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;
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

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("edu.internet2.middleware.shibboleth").setLevel(Level.DEBUG);
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
        ResolutionContext context = resolver.createResolutionContext("ttrojan", "http://example.com/", null);
        
        SortedSet<String> expected = new TreeSet<String>();
        expected.add("gpburdell");
        expected.add("ttrojan");
        
        try {
            Attribute[] actual = resolver.resolveAttributes(null, context).values().toArray(new Attribute[0]);
            
            assertEquals(1, actual.length);
            assertEquals(2, actual[0].getValues().size());
            assertEquals(expected, actual[0].getValues());
        } catch (AttributeResolutionException e) {
            fail(e.getMessage());
        }
        
    }

}