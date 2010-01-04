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

package edu.internet2.middleware.shibboleth.idp.consent.persistence.hsqldb;

import org.hsqldb.util.SqlTool;
import org.hsqldb.util.SqlTool.SqlToolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import edu.internet2.middleware.shibboleth.idp.consent.persistence.hsqldb.HSQLDBServer;

/**
 * Tests if the database initialization script for HSQLDB works.
 */
@Test
public class HSQLDBInitTest {

    private static final Logger LOG = LoggerFactory.getLogger(HSQLDBInitTest.class);

    private static final String DB_FILE = "file:src/test/resources/hsqldb-init";

    private static final String DB_NAME = "init";

    private static final String DB_URL = "jdbc:hsqldb:hsql://localhost/init";

    private static final String DB_USER = "sa";

    private static final String DB_PASSWORD = "";

    private static final String DB_SETUP_SQL_FILE = "src/main/resources/hsqldb-init.sql";

    private HSQLDBServer server;

    @BeforeTest
    public void beforeTest() {
        server = new HSQLDBServer(DB_FILE, DB_NAME);
        server.start();
        assert server.isRunning();
    }

    @AfterTest
    public void afterTest() {
        server.stop();
    }

    @Test
    public void testDBInit() {

        String[] args = { "--autoCommit", "--inlineRc",
                "url=" + DB_URL + ",user=" + DB_USER + ",password=" + DB_PASSWORD, DB_SETUP_SQL_FILE };

        try {
            SqlTool.objectMain(args);
        } catch (SqlToolException e) {
            assert false : e.getMessage();
        }

    }
}
