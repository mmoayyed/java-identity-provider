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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePropertySource;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.cache.Cache;
import com.google.common.collect.Collections2;

import net.shibboleth.ext.spring.resource.PreferFileSystemResourceLoader;
import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.dc.impl.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.DataSourceValidator;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.ExecutableStatement;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.RDBMSDataConnector;
import net.shibboleth.idp.attribute.resolver.dc.rdbms.impl.StringResultMappingStrategy;
import net.shibboleth.idp.attribute.resolver.spring.dc.rdbms.impl.RDBMSDataConnectorParser;
import net.shibboleth.idp.testing.DatabaseTestingSupport;

/** Test for {@link RDBMSDataConnectorParser}. */
public class RDBMSDataConnectorParserTest {

    private static final String INIT_FILE = "/net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/RdbmsStore.sql";

    private static final String DATA_FILE = "/net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/RdbmsData.sql";

    private DataSource datasource;

    private GenericApplicationContext pendingTeardownContext = null;
    
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
    
    @Test public void simpleConnector() throws Exception {
        final RDBMSDataConnector dataConnector =
                getRdbmsDataConnector("net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/resolver/rdbms-attribute-resolver-v2-simple.xml");
        assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringResultMappingStrategy mappingStrategy = (StringResultMappingStrategy) dataConnector.getMappingStrategy();
        assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");
    }

    
    @Test public void v2Config() throws Exception {
        final RDBMSDataConnector dataConnector =
                getRdbmsDataConnector("net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/resolver/rdbms-attribute-resolver-v2.xml");
        assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringResultMappingStrategy mappingStrategy = (StringResultMappingStrategy) dataConnector.getMappingStrategy();
        assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");
    }


    @Test public void hybridConfig() throws Exception {
        final RDBMSDataConnector dataConnector =
                getRdbmsDataConnector(
                        "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/resolver/rdbms-attribute-resolver-v2-hybrid.xml",
                        "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-attribute-resolver-spring-context.xml");
        assertNotNull(dataConnector);
        doTest(dataConnector);
        final StringResultMappingStrategy mappingStrategy = (StringResultMappingStrategy) dataConnector.getMappingStrategy();
        assertEquals(mappingStrategy.getResultRenamingMap().size(), 1);
        assertEquals(mappingStrategy.getResultRenamingMap().get("homephone"), "phonenumber");
    }

    @Test public void v2PropsConfig() throws Exception {
        final Resource props = new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-v2.properties");
        final RDBMSDataConnector dataConnector =
                getRdbmsDataConnector(props,
                        "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/resolver/rdbms-attribute-resolver-v2-props.xml");
        assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    @Test public void springConfig() throws Exception {
        final RDBMSDataConnector dataConnector =
                getRdbmsDataConnector("net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/resolver/rdbms-attribute-resolver-spring.xml");
        assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    @Test public void springPropsConfig() throws Exception {
        final Resource props = new ClassPathResource("net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/rdbms-v3.properties");

        final RDBMSDataConnector dataConnector =
                getRdbmsDataConnector(props, "net/shibboleth/idp/attribute/resolver/spring/dc/rdbms/resolver/rdbms-attribute-resolver-spring-props.xml");
        assertNotNull(dataConnector);
        doTest(dataConnector);
    }

    protected RDBMSDataConnector getRdbmsDataConnector(final String... beanDefinitions) throws IOException {
        return getRdbmsDataConnector(null, beanDefinitions);
    }

    protected RDBMSDataConnector getRdbmsDataConnector(final Resource properties, final String... beanDefinitions) throws IOException {
        
        final ResourceLoader loader = new PreferFileSystemResourceLoader();
        
        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        builder.setName("ApplicationContext: " + RDBMSDataConnectorParserTest.class);

        final Collection<String> defs = new ArrayList<>(Arrays.asList(beanDefinitions));
        defs.add("net/shibboleth/idp/attribute/resolver/spring/externalBeans.xml");

        builder.setServiceConfigurations(Collections2.transform(defs, s -> loader.getResource(s)));

        if (properties != null) {
            builder.setPropertySources(Collections.singletonList(new ResourcePropertySource(properties)));
        }
        
        final GenericApplicationContext context = builder.build();
        
        setTestContext(context);

        return (RDBMSDataConnector) context.getBean("myDatabase");
    }

    protected void doTest(final RDBMSDataConnector dataConnector) throws ResolutionException {

        final String id = dataConnector.getId();
        assertEquals("myDatabase", id);
        assertEquals(Duration.ofMinutes(5), dataConnector.getNoRetryDelay());

        final BasicDataSource dataSource = (BasicDataSource) dataConnector.getDataSource();
        assertNotNull(dataSource);
        assertEquals(dataSource.getUrl(), "jdbc:hsqldb:mem:RDBMSDataConnectorStore");
        assertEquals(dataSource.getUsername(), "SA");
        assertEquals(dataSource.getMaxTotal(), 20);
        assertEquals(dataSource.getMaxIdle(), 5);
        assertEquals(dataSource.getMaxWaitMillis(), 5000);

        assertFalse(dataConnector.isConnectionReadOnly());
        final DataSourceValidator validator = (DataSourceValidator) dataConnector.getValidator();
        assertNotNull(validator);
        assertTrue(validator.isThrowValidateError());
        assertNotNull(validator.getDataSource());

        final ExecutableSearchBuilder<ExecutableStatement> searchBuilder = dataConnector.getExecutableSearchBuilder();
        assertNotNull(searchBuilder);

        final StringResultMappingStrategy mappingStrategy = (StringResultMappingStrategy) dataConnector.getMappingStrategy();
        AssertJUnit.assertNotNull(mappingStrategy);
        AssertJUnit.assertTrue(mappingStrategy.isNoResultAnError());
        AssertJUnit.assertTrue(mappingStrategy.isMultipleResultsAnError());

        final Cache<String, Map<String, IdPAttribute>> resultCache = dataConnector.getResultsCache();
        assertNotNull(resultCache);
    }
}
