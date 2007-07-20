
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ShibbolethSAMLAttributeRequestContext;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;

/**
 * Test configuration code for attribute resolver.
 */
public class MappedAttributeResolverTest extends BaseConfigTestCase {

    /** Test Handle Request. */
    @SuppressWarnings("unchecked")
    public void testResolverInstantiation() throws Exception{
        ApplicationContext ac = createSpringContext(DATA_PATH + "/config/attribute/resolver/service-config.xml");
        AttributeResolver resolver = (AttributeResolver) ac.getBean("resolver-mapped");

        ShibbolethSAMLAttributeRequestContext context = new ShibbolethSAMLAttributeRequestContext();
        context.setPrincipalName("ttrojan");
        
        Map<String, BaseAttribute> actual = resolver.resolveAttributes(context);
        String[] expectedValues;
        
        BaseAttribute affiliation = actual.get("eduPersonAffilation");
        assertEquals(3, affiliation.getValues().size());
        
        expectedValues = new String[] { "member", "parent", "staff" };
        assertEquals(expectedValues, affiliation.getValues().toArray(expectedValues));
        
        
        BaseAttribute firstColor = actual.get("firstColor");
        assertEquals(1, firstColor.getValues().size());
        
        expectedValues = new String[] { "red" };
        assertEquals(expectedValues, firstColor.getValues().toArray(expectedValues));
        
        // test bug SIDP-22
        assertFalse(actual.containsKey("fooBar"));
    }
}