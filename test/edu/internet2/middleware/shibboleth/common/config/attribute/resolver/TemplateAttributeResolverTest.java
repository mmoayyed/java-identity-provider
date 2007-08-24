
package edu.internet2.middleware.shibboleth.common.config.attribute.resolver;

import java.util.Arrays;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolver;
import edu.internet2.middleware.shibboleth.common.config.BaseConfigTestCase;
import edu.internet2.middleware.shibboleth.common.profile.provider.BaseSAMLProfileRequestContext;

/**
 * Test configuration code for attribute resolver.
 */
public class TemplateAttributeResolverTest extends BaseConfigTestCase {

    /** Test Handle Request. */
    @SuppressWarnings("unchecked")
    public void testTemplateAttributeDefinition() throws Exception {
        ApplicationContext ac = createSpringContext(DATA_PATH + "/config/attribute/resolver/service-config.xml");
        AttributeResolver resolver = (AttributeResolver) ac.getBean("resolver-template");

        BaseSAMLProfileRequestContext context = new BaseSAMLProfileRequestContext();
        context.setPrincipalName("gpburdell");

        Map<String, BaseAttribute> actual = resolver.resolveAttributes(context);
        String[] expectedValues;

        BaseAttribute enrollment = actual.get("courseEnrollment");
        assertEquals(2, enrollment.getValues().size());

        expectedValues = new String[] { "urn:mace:example.edu:enrollment:20073:eng101:a3",
                "urn:mace:example.edu:enrollment:20073:math203:2", };        
        assertEquals(Arrays.asList(expectedValues), enrollment.getValues());
    }
}