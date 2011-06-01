/*
 * Licensed to the University Corporation for Advanced Internet Development, Inc.
 * under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache 
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

package net.shibboleth.idp.tou.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import net.shibboleth.idp.tou.ToUAcceptance;

import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/** JDBC implementation. */
public class JDBCStorage implements Storage {

    /** The JDBC template. */
    private SimpleJdbcTemplate jdbcTemplate;

    /** The name of the terms of use acceptance table. */
    private final String acceptanceTable = "ToUAcceptance";

    /** {@see ToUAcceptance} row mapper. */
    private static final class ToUAcceptanceMapper implements RowMapper<ToUAcceptance> {
        @Override
        public ToUAcceptance mapRow(ResultSet rs, int rowNum) throws SQLException {
            final ToUAcceptance touAcceptance =
                    new ToUAcceptance(rs.getString("version"), rs.getString("fingerprint"), new DateTime(
                            rs.getTimestamp("acceptanceDate")));
            return touAcceptance;
        }
    }

    /** The terms of use acceptance mapper. */
    private final ToUAcceptanceMapper touAcceptanceMapper = new ToUAcceptanceMapper();

    /** {@inheritDoc} */
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    /** {@inheritDoc} */
    @Override
    public void createToUAcceptance(final String userId, final ToUAcceptance touAcceptance) {
        final String sql =
                "INSERT INTO " + acceptanceTable + " (userId, version, fingerprint, acceptanceDate)"
                        + " VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, touAcceptance.getVersion(), touAcceptance.getFingerprint(), touAcceptance
                .getAcceptanceDate().toDate());
    }

    /** {@inheritDoc} */
    @Override
    public void updateToUAcceptance(final String userId, final ToUAcceptance touAcceptance) {
        final String sql =
                "UPDATE " + acceptanceTable + " SET fingerprint = ?, acceptanceDate = ?"
                        + " WHERE userId = ? AND version = ?";
        jdbcTemplate.update(sql, touAcceptance.getFingerprint(), touAcceptance.getAcceptanceDate().toDate(), userId,
                touAcceptance.getVersion());
    }

    /** {@inheritDoc} */
    @Override
    public ToUAcceptance readToUAcceptance(final String userId, final String version) {
        final String sql =
                "SELECT version, fingerprint, acceptanceDate" + " FROM " + acceptanceTable
                        + " WHERE userId = ? AND version = ?";
        try {
            return jdbcTemplate.queryForObject(sql, touAcceptanceMapper, userId, version);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsToUAcceptance(final String userId, final String version) {
        final String sql = "SELECT COUNT(*)" + " FROM " + acceptanceTable + " WHERE userId = ? AND version = ?";
        return jdbcTemplate.queryForInt(sql, userId, version) > 0;
    }
}
