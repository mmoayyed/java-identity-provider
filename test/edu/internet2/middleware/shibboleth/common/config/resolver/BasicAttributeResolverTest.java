
package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.io.IOException;
import java.util.List;


import junit.framework.TestCase;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.ResolutionContext;

/**
 * Test configuration code for attribute resolver.
 */
public class BasicAttributeResolverTest extends TestCase {

    /** Log4j logger. */
    private static Logger log = Logger.getLogger(BasicAttributeResolverTest.class);
    
    /** Application Context. */
    private ApplicationContext ac;

    /** Constructor. */
    public BasicAttributeResolverTest() {

        BasicConfigurator.resetConfiguration();
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    /** {@inheritDoc} */
    public void setUp() throws IOException {
        ac = new FileSystemXmlApplicationContext(
                "test/data/edu/internet2/middleware/shibboleth/common/config/resolver/resolver.xml");
    }

    /**
     * Test Handle Request.
     * 
     */
    public void testResolverInstantiation() {
        AttributeResolver resolver = (AttributeResolver) ac
                .getBean("edu.internet2.middleware.shibboleth.common.config.resolver.AttributeResolverFactoryBean");
        ResolutionContext context = resolver.createResolutionContext("wnorris", "http://foo.com/", null);
        
        
        log.info("total connectors = (" + resolver.getDataConnectors().size() + ")");
        for (String id : resolver.getDataConnectors().keySet()) {
            DataConnector connector = resolver.getDataConnectors().get(id);
            log.info("connector - (" + id + ") = " + connector);

            try {
                List<Attribute> attributes = connector.resolve(context);
                for (Attribute a : attributes) {
                    log.info("attribute - (" + a.getId() + ")");
                    log.info("values - (" + a.getValues() + ")");
                }

            } catch (AttributeResolutionException e) {
                e.printStackTrace();
            }
        }
        
        assertNotNull(resolver);
    }

}