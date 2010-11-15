/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.tou.storage;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import edu.internet2.middleware.shibboleth.idp.tou.ToU;

/**
 *
 */
public class JDBCStorage implements Storage {

    private final Logger logger = LoggerFactory.getLogger(JDBCStorage.class);
    private SimpleJdbcTemplate jdbcTemplate;
 
    /** {@inheritDoc} */
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    /** {@inheritDoc} */
    public void createAcceptedToU(final String userId, final ToU tou, final DateTime acceptanceDate) {
        String sql = "INSERT INTO AcceptedToU " +
        		"(userId, version, fingerprint, acceptanceDate) " +
        		"VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, userId, tou.getVersion(), tou.getFingerprint(), acceptanceDate.toDate());    
        } catch (DuplicateKeyException e) {
            logger.warn("AcceptedToU already exists, update with new ToU");
            sql = "UPDATE AcceptedToU " +
                "SET fingerprint = ?, acceptanceDate = ? " +
                "WHERE userId = ? AND version = ?";
            jdbcTemplate.update(sql, tou.getFingerprint(), acceptanceDate.toDate(), userId, tou.getVersion());
        }
    }

    /** {@inheritDoc} */
    public boolean containsAcceptedToU(final String userId, final ToU tou) {
        final String sql = "SELECT COUNT(*) " +
                "FROM AcceptedToU " +
                "WHERE userId = ? AND version = ? AND fingerprint = ?";
        int result = jdbcTemplate.queryForInt(sql, userId, tou.getVersion(), tou.getFingerprint());
        return result > 0;
    }
}
