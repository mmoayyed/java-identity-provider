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
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

/**
 * Tests JDBC storage using the Spring JDBC framework.
 */

@ContextConfiguration("/edu/internet2/middleware/shibboleth/idp/consent/test-context.xml")
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback=true)
public class BaseJDBCTest extends AbstractTransactionalTestNGSpringContextTests {

    private final Logger logger = LoggerFactory.getLogger(BaseJDBCTest.class);

    @Test(groups = {"jdbc.initialization"})
    @Parameters({ "jdbcInitFile" })
    @Rollback(false)
    public void initialization(String initFile) {
        logger.info("start");
        super.executeSqlScript(initFile, false);
        logger.info("stop");
    }
}
