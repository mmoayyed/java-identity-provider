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

package net.shibboleth.idp.attribute.resolver.impl.dc.rdbms;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.hsqldb.jdbc.JDBCDataSource;
import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Optional;

import net.shibboleth.idp.attribute.Attribute;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.resolver.AttributeRecipientContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.attribute.resolver.impl.DatabaseTestingSupport;
import net.shibboleth.idp.attribute.resolver.impl.TestSources;
import net.shibboleth.idp.attribute.resolver.impl.dc.ExecutableSearchBuilder;
import net.shibboleth.idp.attribute.resolver.impl.dc.TestCache;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.component.UninitializedComponentException;
import net.shibboleth.utilities.java.support.component.UnmodifiableComponentException;

/**
 * Tests for {@link RdbmsDataConnector}
 */
public class RdbmsDataConnectorTest extends OpenSAMLInitBaseTestCase {

    /** The connector name. */
    private static final String TEST_CONNECTOR_NAME = "rdbmsAttributeConnector";

    private static final String INIT_FILE = "/data/net/shibboleth/idp/attribute/resolver/impl/dc/rdbms/RdbmsStore.sql";

    private static final String DATA_FILE = "/data/net/shibboleth/idp/attribute/resolver/impl/dc/rdbms/RdbmsData.sql";

    private DataSource datasource;

    /** Simple search filter builder that leverages a custom filter. */
    private class TestExecutableStatementBuilder implements ExecutableSearchBuilder<ExecutableStatement> {

        /** {@inheritDoc} */
        @Nonnull public ExecutableStatement build(@Nonnull AttributeResolutionContext resolutionContext)
                throws ResolutionException {
            final AttributeRecipientContext subContext =
                    resolutionContext.getSubcontext(AttributeRecipientContext.class);
            return new ExecutableStatement() {

                private final String query = String
                        .format("SELECT userid, name, homephone, mail FROM people WHERE userid='%s'",
                                subContext.getPrincipal());

                @Nonnull public String getResultCacheKey() {
                    return query;
                }

                @Nonnull public ResultSet execute(@Nonnull Connection connection) throws SQLException {
                    return connection.createStatement().executeQuery(query);
                }
            };
        }
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

    /**
     * Creates a RDBMS data connector using the supplied builder and strategy. Sets defaults values if the parameters
     * are null.
     * 
     * @param builder to build executable statements
     * @param strategy to map results
     * @return rdbms data connector
     */
    protected RdbmsDataConnector createRdbmsDataConnector(ExecutableSearchBuilder builder,
            ResultMappingStrategy strategy) {
        RdbmsDataConnector connector = new RdbmsDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);
        connector.setDataSource(datasource);
        connector.setExecutableSearchBuilder(builder == null ? new TestExecutableStatementBuilder() : builder);
        connector.setMappingStrategy(strategy == null ? new StringResultMappingStrategy() : strategy);
        return connector;
    }

    @Test public void initializeAndGetters() throws ComponentInitializationException, ResolutionException {

        RdbmsDataConnector connector = new RdbmsDataConnector();
        connector.setId(TEST_CONNECTOR_NAME);

        try {
            connector.initialize();
            Assert.fail("No datasource");
        } catch (ComponentInitializationException e) {
            // OK
        }

        connector.setDataSource(new JDBCDataSource());
        try {
            connector.initialize();
            Assert.fail("No statement builder");
        } catch (ComponentInitializationException e) {
            // OK
        }

        ExecutableSearchBuilder statementBuilder = new TestExecutableStatementBuilder();
        connector.setExecutableSearchBuilder(statementBuilder);
        try {
            connector.initialize();
            Assert.fail("Invalid datasource");
        } catch (ComponentInitializationException e) {
            // OK
        }

        connector.setDataSource(datasource);

        StringResultMappingStrategy mappingStrategy = new StringResultMappingStrategy();
        connector.setMappingStrategy(mappingStrategy);

        try {
            connector.doResolve(null);
            Assert.fail("Need to initialize first");
        } catch (UninitializedComponentException e) {
            // OK
        }

        connector.initialize();
        try {
            connector.setDataSource(null);
            Assert.fail("Setter after initialize");
        } catch (UnmodifiableComponentException e) {
            // OK
        }
        Assert.assertEquals(connector.getDataSource(), datasource);
        Assert.assertEquals(connector.getExecutableSearchBuilder(), statementBuilder);
        Assert.assertEquals(connector.getMappingStrategy(), mappingStrategy);
    }

    @Test public void resolve() throws ComponentInitializationException, ResolutionException {
        RdbmsDataConnector connector = createRdbmsDataConnector(null, null);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Optional<Map<String, Attribute>> optional = connector.doResolve(context);
        Assert.assertTrue(optional.isPresent());
        Map<String, Attribute> attrs = optional.get();
        // check total attributes: userid, name, homephone, mail
        Assert.assertTrue(attrs.size() == 4);
        // check userid
        Assert.assertTrue(attrs.get("USERID").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue(TestSources.PRINCIPAL_ID), attrs.get("USERID").getValues()
                .iterator().next());
        // check name
        Assert.assertTrue(attrs.get("NAME").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("Peter Principal"), attrs.get("NAME").getValues().iterator()
                .next());
        // check homephone
        Assert.assertTrue(attrs.get("HOMEPHONE").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("555-111-2222"), attrs.get("HOMEPHONE").getValues().iterator()
                .next());
        // check mail
        Assert.assertTrue(attrs.get("MAIL").getValues().size() == 1);
        Assert.assertEquals(new StringAttributeValue("peter.principal@shibboleth.net"), attrs.get("MAIL").getValues()
                .iterator().next());
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoStatement()
            throws ComponentInitializationException, ResolutionException {
        RdbmsDataConnector connector = createRdbmsDataConnector(new ExecutableSearchBuilder<ExecutableStatement>() {

            @Nonnull public ExecutableStatement build(@Nonnull AttributeResolutionContext resolutionContext)
                    throws ResolutionException {
                return null;
            }
        }, null);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.doResolve(context);
    }

    @Test(expectedExceptions = ResolutionException.class) public void resolveNoResultIsError()
            throws ComponentInitializationException, ResolutionException {
        RdbmsDataConnector connector = createRdbmsDataConnector(null, null);
        connector.setNoResultAnError(true);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        try {
            Optional<Map<String, Attribute>> optional = connector.doResolve(context);
            Assert.assertTrue(optional.isPresent());
        } catch (ResolutionException e) {
            Assert.fail("Resolution exception occurred", e);
        }

        context =
                TestSources.createResolutionContext("NOT_A_PRINCIPAL", TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        connector.doResolve(context);
    }

    @Test public void resolveWithCache() throws ComponentInitializationException, ResolutionException {
        RdbmsDataConnector connector = createRdbmsDataConnector(null, null);
        final TestCache cache = new TestCache();
        connector.setResultsCache(cache);
        connector.initialize();

        AttributeResolutionContext context =
                TestSources.createResolutionContext(TestSources.PRINCIPAL_ID, TestSources.IDP_ENTITY_ID,
                        TestSources.SP_ENTITY_ID);
        Assert.assertTrue(cache.size() == 0);
        Optional<Map<String, Attribute>> optional = connector.doResolve(context);
        Assert.assertTrue(cache.size() == 1);
        Assert.assertEquals(cache.iterator().next(), optional);
    }
}
