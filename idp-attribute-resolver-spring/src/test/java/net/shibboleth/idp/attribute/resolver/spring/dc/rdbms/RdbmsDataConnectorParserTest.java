/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.spring.dc.rdbms;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.impl.MappingStrategy;
import net.shibboleth.idp.attribute.resolver.dc.impl.Validator;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.ExecutableStatement;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.RdbmsDataConnector;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.idp.testing.DatabaseTestingSupport;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.mchange.v2.c3p0.ComboPooledDataSource;

/** Test for {@link RdbmsDataConnectorParser}. */
public class RdbmsDataConnectorParserTest {

    private static final String INIT_FILE = "/net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/RdbmsStore.sql";

    private static final String DATA_FILE = "/net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/RdbmsData.sql";

    private DataSource datasource;

    /**
     * Creates an HSQLDB database instance.
     * 
     * @throws ClassNotFoundException if the database driver cannot be found
     * @throws SQLException if the database cannot be initialized
     */
    @BeforeTest public void setupDatabaseServer() throws ClassNotFoundException, SQLException {

        datasource = DatabaseTestingSupport.GetMockDataSource(INIT_FILE, "RDBMSDataConnectorStore");
        DatabaseTestingSupport.InitializeDataSourceFromFile(DATA_FILE, datasource);
    }
    
    @Test public void v2Config() throws ComponentInitializationException, ServiceException, ResolutionException {
        RdbmsDataConnector dataConnector =
                getRdbmsDataConnector(false, "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-attribute-resolver-v2.xml");
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    @Test public void v2PropsConfig() throws ComponentInitializationException, ServiceException, ResolutionException {
        RdbmsDataConnector dataConnector =
                getRdbmsDataConnector(
                        true,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-attribute-resolver-v2-props.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/PropertyPlaceholder.xml");
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    @Test public void springConfig() throws ComponentInitializationException, ServiceException, ResolutionException {
        RdbmsDataConnector dataConnector =
                getRdbmsDataConnector(true, "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-attribute-resolver-spring.xml");
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    @Test public void springPropsConfig() throws ComponentInitializationException, ServiceException, ResolutionException {
        RdbmsDataConnector dataConnector =
                getRdbmsDataConnector(true, "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-attribute-resolver-spring-props.xml");
        Assert.assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    protected RdbmsDataConnector getRdbmsDataConnector(boolean supressValidation, final String... beanDefinitions) {
        GenericApplicationContext context = new GenericApplicationContext();
        context.setDisplayName("ApplicationContext: " + RdbmsDataConnectorParserTest.class);

        XmlBeanDefinitionReader configReader = new XmlBeanDefinitionReader(context);

        configReader.loadBeanDefinitions("net/shibboleth/idp/attribute/resolver/spring/velocity.xml");
        
        SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                new SchemaTypeAwareXMLBeanDefinitionReader(context);

        if (supressValidation) {
            beanDefinitionReader.setValidating(false);
        }
        beanDefinitionReader.loadBeanDefinitions(beanDefinitions);
        context.refresh();

        return (RdbmsDataConnector) context.getBean("myDatabase");
    }

    protected void doTest(final RdbmsDataConnector dataConnector) throws ResolutionException {

        String id = dataConnector.getId();
        AssertJUnit.assertEquals("myDatabase", id);

        ComboPooledDataSource dataSource = (ComboPooledDataSource) dataConnector.getDataSource();
        AssertJUnit.assertNotNull(dataSource);
        AssertJUnit.assertEquals("jdbc:hsqldb:mem:RDBMSDataConnectorStore", dataSource.getJdbcUrl());
        AssertJUnit.assertEquals("SA", dataSource.getUser());
        AssertJUnit.assertEquals(3, dataSource.getAcquireIncrement());
        AssertJUnit.assertEquals(24, dataSource.getAcquireRetryAttempts());
        AssertJUnit.assertEquals(5000, dataSource.getAcquireRetryDelay());
        AssertJUnit.assertEquals(true, dataSource.isBreakAfterAcquireFailure());
        AssertJUnit.assertEquals(1, dataSource.getMinPoolSize());
        AssertJUnit.assertEquals(5, dataSource.getMaxPoolSize());
        AssertJUnit.assertEquals(300, dataSource.getMaxIdleTime());
        AssertJUnit.assertEquals(360, dataSource.getIdleConnectionTestPeriod());

        Validator validator = dataConnector.getValidator();
        AssertJUnit.assertNotNull(validator);

        ExecutableSearchBuilder<ExecutableStatement> searchBuilder = dataConnector.getExecutableSearchBuilder();
        AssertJUnit.assertNotNull(searchBuilder);

        MappingStrategy mappingStrategy = dataConnector.getMappingStrategy();
        AssertJUnit.assertNotNull(mappingStrategy);

        Cache<String, Map<String, IdPAttribute>> resultCache = dataConnector.getResultsCache();
        AssertJUnit.assertNotNull(resultCache);
    }
}
