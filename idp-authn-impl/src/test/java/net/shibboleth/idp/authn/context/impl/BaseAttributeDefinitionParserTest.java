/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.authn.context.impl;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nonnull;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.AfterMethod;

import net.shibboleth.idp.attribute.resolver.AttributeDefinition;
import net.shibboleth.idp.attribute.resolver.DataConnector;
import net.shibboleth.idp.attribute.resolver.impl.AttributeResolverImpl;
import net.shibboleth.idp.attribute.resolver.spring.ad.BaseAttributeDefinitionParser;
import net.shibboleth.idp.attribute.resolver.spring.ad.impl.SimpleAttributeDefinitionParser;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.spring.config.IdentifiableBeanPostProcessor;
import net.shibboleth.shared.spring.config.StringToDurationConverter;
import net.shibboleth.shared.spring.config.StringToIPRangeConverter;
import net.shibboleth.shared.spring.config.StringToResourceConverter;
import net.shibboleth.shared.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.shared.spring.custom.SchemaTypeAwareXMLBeanDefinitionReader;

/**
 * Base class for tests for {@link SimpleAttributeDefinitionParser} and by extension {@link BaseAttributeDefinitionParser}.
 * 
 * Note that several helper classes are marked private.  This is purely to discourage accidental use of non validating
 * parsers with no need. 
 */
@SuppressWarnings("javadoc")
public abstract class BaseAttributeDefinitionParserTest extends OpenSAMLInitBaseTestCase {

    public static final String BEAN_FILE_PATH = "net/shibboleth/idp/attribute/resolver/spring/";

    public static final String ATTRIBUTE_FILE_PATH = BEAN_FILE_PATH + "ad/";

    public static final String DATACONNECTOR_FILE_PATH = BEAN_FILE_PATH + "dc/";

    public static final String ENCODER_FILE_PATH = BEAN_FILE_PATH + "enc/";

    public static final String PRINCIPALCONNECTOR_FILE_PATH = BEAN_FILE_PATH + "pc/";
    
    protected GenericApplicationContext pendingTeardownContext = null;
    
    @AfterMethod public void tearDownTestContext() {
        if (null == pendingTeardownContext ) {
            return;
        }
        pendingTeardownContext.close();
        pendingTeardownContext = null;
    }
    
    protected void setTestContext(final GenericApplicationContext context) {
        tearDownTestContext();
        pendingTeardownContext = context;
    }

    private void loadFile(final String fileName, final GenericApplicationContext context, final boolean supressValid) {
        final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        if (supressValid) {
           beanDefinitionReader.setValidating(false);
        }

        beanDefinitionReader.loadBeanDefinitions(fileName, BEAN_FILE_PATH + "customBean.xml");
    }

    protected void loadFile(final String fileName, final GenericApplicationContext context) {
        loadFile(fileName, context, false);
    }

    @Nonnull protected <Type> Type getBean(final String fileName, final Class<Type> claz, final GenericApplicationContext context,
            final boolean supressValid) {

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(
                new StringToIPRangeConverter(),
                new StringToResourceConverter(),
                new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        loadFile(fileName, context, supressValid);

        context.refresh();

        final Collection<Type> beans = context.getBeansOfType(claz).values();
        assertEquals(beans.size(), 1);
        final Type result = beans.iterator().next();
        assert result != null;
        return result;
    }

    @Nonnull protected <Type> Type getBean(final String fileName, final Class<Type> claz, final GenericApplicationContext context) {
        return getBean(fileName, claz, context, false);
    }

    @Nonnull protected <Type extends AttributeDefinition> Type getAttributeDefn(final String fileName, final Class<Type> claz,
            final GenericApplicationContext context) {

        return getBean(ATTRIBUTE_FILE_PATH + fileName, claz, context);
    }

    @Nonnull private <Type extends AttributeDefinition> Type getAttributeDefn(final String fileName, final Class<Type> claz,
            final GenericApplicationContext context, final boolean supressValidation) {

        return getBean(ATTRIBUTE_FILE_PATH + fileName, claz, context, supressValidation);
    }

    @Nonnull protected <Type extends AttributeDefinition> Type getAttributeDefn(final String fileName, final String beanFileName,
            final Class<Type> claz) {
        return getAttributeDefn(fileName, beanFileName, claz, false);

    }

    @Nonnull private <Type extends AttributeDefinition> Type getAttributeDefn(final String fileName, final String beanFileName,
            final Class<Type> claz, final boolean supressValidation) {

        final GenericApplicationContext context = new GenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + claz);
        final XmlBeanDefinitionReader configReader = new SchemaTypeAwareXMLBeanDefinitionReader(context);

        final ConversionServiceFactoryBean service = new ConversionServiceFactoryBean();
        service.setConverters(new HashSet<>(Arrays.asList(
                new StringToIPRangeConverter(),
                new StringToResourceConverter(),
                new StringToDurationConverter())));
        service.afterPropertiesSet();

        context.getBeanFactory().setConversionService(service.getObject());

        configReader.loadBeanDefinitions(BEAN_FILE_PATH + beanFileName);

        return getAttributeDefn(fileName, claz, context, supressValidation);
    }

    @Nonnull protected <Type extends AttributeDefinition> Type getAttributeDefn(final String fileName, final Class<Type> claz) {
        return getAttributeDefn(fileName, claz, false);

    }

    @Nonnull protected <Type extends AttributeDefinition> Type getAttributeDefn(final String fileName, final Class<Type> claz,
            final boolean supressValid) {

        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + claz);

        return getAttributeDefn(fileName, claz, context, supressValid);
    }

    @Nonnull protected <Type extends DataConnector> Type getDataConnector(final String fileName, final Class<Type> claz) {
        return getDataConnector(fileName, claz, false);
    }
    
    @Nonnull private <Type extends DataConnector> Type
            getDataConnector(final String fileName, final Class<Type> claz, final boolean supressValid) {

        final GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().addBeanPostProcessor(new IdentifiableBeanPostProcessor());
        setTestContext(context);
        context.setDisplayName("ApplicationContext: " + claz);

        return getBean(DATACONNECTOR_FILE_PATH + fileName, claz, context, supressValid);
    }

    @Nonnull static public AttributeResolverImpl getResolver(final ApplicationContext appContext) throws ComponentInitializationException {
        final Map<String, AttributeDefinition> attributesMap = appContext.getBeansOfType(AttributeDefinition.class);
        final Collection<AttributeDefinition> definitions = attributesMap.values();
        final Map<String, DataConnector> dataConnectorsMap = appContext.getBeansOfType(DataConnector.class);
        final Collection<DataConnector> connectors = dataConnectorsMap.values();
        assert connectors != null && definitions != null; 

        @Nonnull final AttributeResolverImpl resolver = new AttributeResolverImpl();
        resolver.setAttributeDefinitions(definitions);
        resolver.setDataConnectors(connectors);
        resolver.setId("testResolver");
        resolver.initialize();
        return resolver;
    }

}
