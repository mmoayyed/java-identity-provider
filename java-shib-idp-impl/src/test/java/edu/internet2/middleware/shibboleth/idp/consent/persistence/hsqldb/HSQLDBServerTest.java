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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

/**
 * Tests if the HSQLDB server can be started, if it is running and can be stopped.
 */
@Test
public class HSQLDBServerTest {

    private static final Logger LOG = LoggerFactory.getLogger(HSQLDBServerTest.class);

    private static final String DB_FILE = "file:src/test/resources/hsqldb-test";

    private static final String DB_NAME = "test";

    private HSQLDBServer server;

    @BeforeTest
    public void beforeTest() {
        server = new HSQLDBServer(DB_FILE, DB_NAME);
    }

    @Test
    public void testServerStartStop() {
        server.start();
        assert server.isRunning();
        server.stop();
    }
}
