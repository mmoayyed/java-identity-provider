package net.shibboleth.idp.attribute.resolver.impl;

import java.util.Collection;
import java.util.Set;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.ScopedAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionException;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.BaseDataConnector;
import net.shibboleth.idp.attribute.resolver.ResolverPluginDependency;

import org.opensaml.util.collections.LazySet;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test for prescoped attribute definitions.
 */
public class PrecscopedAtributeTest {
    /** The name. */
    private static final String TEST_ATTRIBUTE_NAME = "simple";

    /**
     * Test regexp.
     * 
     * @throws AttributeResolutionException on resolution issues.
     */
    @Test
    public void testPreScoped() throws AttributeResolutionException {

        BaseAttributeDefinition attrDef = new PrescopedAttributeDefinition(TEST_ATTRIBUTE_NAME, "-");
        //
        // The test Data Connector provides an input attribute "at1" with values
        // at1-Data and at1-Connector. We can feed these into the prescoped, looking for '-'
        //
        ScopedAttributeValue res1 = new ScopedAttributeValue("at1", "Data");
        ScopedAttributeValue res2 = new ScopedAttributeValue("at1", "Connector");
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        attrDef.setDependencies(dependencySet);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");
        Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(attrDef);
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);
        Collection f = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME).getValues();

        Assert.assertEquals(f.size(), 2);
        Assert.assertTrue(f.contains(res1));
        Assert.assertTrue(f.contains(res2));
    }

    /**
     * Test the prescoped attribute resolve when there are no matches.
     * 
     * @throws AttributeResolutionException if resolution fails.
     */
    @Test
    public void testPreScopedNoValues() throws AttributeResolutionException {

        BaseAttributeDefinition attrDef = new PrescopedAttributeDefinition(TEST_ATTRIBUTE_NAME, "@");
        //
        // Set the dependency on the data connector
        //
        Set<ResolverPluginDependency> dependencySet = new LazySet<ResolverPluginDependency>();
        dependencySet.add(new ResolverPluginDependency(TestSources.STATIC_CONNECTOR_NAME,
                TestSources.DEPENDS_ON_ATTRIBUTE_NAME));
        attrDef.setDependencies(dependencySet);

        //
        // And resolve
        //
        AttributeResolver resolver = new AttributeResolver("foo");
        Set<BaseDataConnector> connectorSet = new LazySet<BaseDataConnector>();
        connectorSet.add(TestSources.populatedStaticConnectior());

        Set<BaseAttributeDefinition> attributeSet = new LazySet<BaseAttributeDefinition>();
        attributeSet.add(attrDef);
        resolver.setDataConnectors(connectorSet);
        resolver.setAttributeDefinition(attributeSet);

        AttributeResolutionContext context = new AttributeResolutionContext(null);
        resolver.resolveAttributes(context);

        Attribute<?> resultAttribute = context.getResolvedAttributes().get(TEST_ATTRIBUTE_NAME);
        Assert.assertNull(resultAttribute);
    }
}
