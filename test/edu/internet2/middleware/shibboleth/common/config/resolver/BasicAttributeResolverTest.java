
package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.Attribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.DataConnector;
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
        ResolutionContext context = resolver.createResolutionContext("wnorris", "http://foo.com/", null);
        
        
        log.debug("total connectors = (" + resolver.getDataConnectors().size() + ")");
        for (String id : resolver.getDataConnectors().keySet()) {
            DataConnector connector = resolver.getDataConnectors().get(id);
            log.debug("connector (" + id + ") = " + connector);

            try {
                Map<String, Attribute> attributes = connector.resolve(context);
                for (String attributeId : attributes.keySet()) {
                    log.debug("    (" + attributeId + ") => (" + attributes.get(attributeId).getValues() + ")");
                }

            } catch (AttributeResolutionException e) {
                e.printStackTrace();
            }
        }
        
        assertNotNull(resolver);
    }

}