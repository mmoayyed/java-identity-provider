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

import org.hsqldb.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple HSQLDB server which serves one file based database.
 */

public class HSQLDBServer {

    private Logger logger = LoggerFactory.getLogger(HSQLDBServer.class);

    private Server server;

    /**
     * Constructor
     * 
     */
    public HSQLDBServer(String dbPath, String dbName) {
        server = new Server();
        server.setDatabasePath(0, dbPath);
        server.setDatabaseName(0, dbName);
        server.setSilent(true);
    }

    public void start() {
        logger.info("Starting server");
        server.start();
    }

    public void stop() {
        logger.info("Stopping server");
        server.shutdown();
    }

    public boolean isRunning() {
        try {
            server.checkRunning(true);
        } catch (RuntimeException e) {
            return false;
        }
        return true;
    }

}
