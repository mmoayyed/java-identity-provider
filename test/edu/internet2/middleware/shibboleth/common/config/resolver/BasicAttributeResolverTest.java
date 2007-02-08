
package edu.internet2.middleware.shibboleth.common.config.resolver;

import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;

/**
 * Test configuration code for attribute resolver.
 */
public class BasicAttributeResolverTest extends TestCase {

    /** Application Context. */
    private ApplicationContext ac;

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
        
        assertNotNull(resolver);
    }

}