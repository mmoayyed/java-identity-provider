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

package net.shibboleth.idp.attribute.resolver.spring.dc;

import static org.testng.Assert.assertEquals;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.InitialContextFactoryBuilder;
import javax.naming.spi.NamingManager;
import javax.sql.DataSource;

import org.testng.annotations.Test;

import net.shibboleth.idp.attribute.impl.JDBCPairwiseIdStore;
import net.shibboleth.idp.attribute.resolver.dc.impl.PairwiseIdDataConnector;
import net.shibboleth.idp.attribute.resolver.spring.BaseAttributeDefinitionParserTest;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 *
 */
public class ManagedConnectionTest extends BaseAttributeDefinitionParserTest {

    @Test(enabled=true) public void simple() throws ComponentInitializationException, NamingException {
        if (!NamingManager.hasInitialContextFactoryBuilder()) {
            NamingManager.setInitialContextFactoryBuilder(new ContextFactoryBuilder() );
        }

        final PairwiseIdDataConnector connector = getDataConnector("resolver/containerManagedConnection.xml", PairwiseIdDataConnector.class);
        MyDataSource source = (MyDataSource) ((JDBCPairwiseIdStore) connector.getPairwiseIdStore()).getDataSource();
        assertEquals(source.getEnvironment().size(), 0);
    }

    /** Hard wiring for JNDI.  This is an {@link InitialContextFactoryBuilder} Its only job is to return a {@link MyContextFactory} */
    private class ContextFactoryBuilder implements InitialContextFactoryBuilder {

        /** {@inheritDoc} */
        public InitialContextFactory createInitialContextFactory(Hashtable<?, ?> environment) throws NamingException {
            return new MyContextFactory();
        }
    }

    /** Hard wiring for JNDI.  This is an {@link InitialContextFactory}.  Its only job is to return an {@link MyInitialContext} */
    private class MyContextFactory implements InitialContextFactory {

        /** {@inheritDoc} */
        public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
            return new MyInitialContext(environment);
        }
        
    }

    
    /** Hard wiring for JNDI.  This is a {@link Context}.  Its only job is to return an {@link DataSource} called "myConnnector"
     */
    private class MyInitialContext implements Context {
        
        private final Hashtable<?, ?> environment;
        
        /**
         * Constructor.
         *
         * @param environment
         */
        public MyInitialContext(Hashtable<?, ?> env) {
            environment = env;
        }

        /** {@inheritDoc} */
        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public void bind(Name name, Object obj) throws NamingException {
            
        }

        /** {@inheritDoc} */
        public void bind(String name, Object obj) throws NamingException {
        }

        /** {@inheritDoc} */
        public void close() throws NamingException {
        }

        /** {@inheritDoc} */
        public Name composeName(Name name, Name prefix) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public String composeName(String name, String prefix) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public Context createSubcontext(Name name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public Context createSubcontext(String name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public void destroySubcontext(Name name) throws NamingException {
        }

        /** {@inheritDoc} */
        public void destroySubcontext(String name) throws NamingException {
        }

        /** {@inheritDoc} */
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public String getNameInNamespace() throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public NameParser getNameParser(Name name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public NameParser getNameParser(String name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public Object lookup(Name name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public Object lookup(String name) throws NamingException {
            if ("myConnnector".equals(name)) {
                return new MyDataSource(environment);
            }
            return null;
        }

        /** {@inheritDoc} */
        public Object lookupLink(Name name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public Object lookupLink(String name) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public void rebind(Name name, Object obj) throws NamingException {
        }

        /** {@inheritDoc} */
        public void rebind(String name, Object obj) throws NamingException {
        }

        /** {@inheritDoc} */
        public Object removeFromEnvironment(String propName) throws NamingException {
            return null;
        }

        /** {@inheritDoc} */
        public void rename(Name oldName, Name newName) throws NamingException {
        }

        /** {@inheritDoc} */
        public void rename(String oldName, String newName) throws NamingException {
        }

        /** {@inheritDoc} */
        public void unbind(Name name) throws NamingException {
        }

        /** {@inheritDoc} */
        public void unbind(String name) throws NamingException {
        }
    }
    
    /** Hard wiring for JNDI.  This is a {@link DataSource}.  Its only job is to return hold the environment so
     * we can test it later
     */

    private final class MyDataSource implements DataSource {

        private final Hashtable<?, ?> environment;
        
        /**
         * Constructor.
         *
         * @param environment
         */
        public MyDataSource(Hashtable<?, ?> env) {
            environment = env;
        }
        
        public Hashtable<?, ?> getEnvironment() {
            return environment;
        }

        /** {@inheritDoc} */
        public PrintWriter getLogWriter() throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public int getLoginTimeout() throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        /** {@inheritDoc} */
        public void setLogWriter(PrintWriter out) throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public void setLoginTimeout(int seconds) throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public boolean isWrapperFor(Class<?> arg0) throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public <T> T unwrap(Class<T> arg0) throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public Connection getConnection() throws SQLException {
            throw new SQLException();
        }

        /** {@inheritDoc} */
        public Connection getConnection(String username, String password) throws SQLException {
            throw new SQLException();
        }
        
    }
}
