/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.idp.consent.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.persistence.hsqldb.HSQLDBServer;

/**
 * Tests various SQL commands using the Spring JDBC framework.
 */
@Test
public class JDBCTest {

    private static final Logger LOG = LoggerFactory.getLogger(JDBCTest.class);

    private static final String DB_FILE = "file:src/test/resources/hsqldb-init";

    private static final String DB_NAME = "init";

    private HSQLDBServer server;

    @BeforeClass
    public void beforeClass() {
        server = new HSQLDBServer(DB_FILE, DB_NAME);
        server.start();
        assert server.isRunning();
    }

    @AfterClass
    public void afterClass() {
        server.stop();
    }

    @Test
    public void testSpringJDBCTemplates() {

    }
}
