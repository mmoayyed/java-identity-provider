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

package net.shibboleth.idp.tou.storage;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import net.shibboleth.idp.tou.ToUAcceptance;

import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


/**
 *
 */
public class JDBCStorage implements Storage {

    private SimpleJdbcTemplate jdbcTemplate;
    private String acceptanceTable = "ToUAcceptance";
    
    private static final class ToUAcceptanceMapper implements RowMapper<ToUAcceptance> {
        public ToUAcceptance mapRow(ResultSet rs, int rowNum) throws SQLException {
            final ToUAcceptance touAcceptance = new ToUAcceptance(rs.getString("version"), rs.getString("fingerprint"), new DateTime(rs.getTimestamp("acceptanceDate")));
            return touAcceptance;
        }
    }
    private final ToUAcceptanceMapper touAcceptanceMapper = new ToUAcceptanceMapper();

    /** {@inheritDoc} */
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    /** {@inheritDoc} */
    public void createToUAcceptance(String userId, ToUAcceptance touAcceptance) {
        String sql = "INSERT INTO " + acceptanceTable +
            " (userId, version, fingerprint, acceptanceDate)" +
            " VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, touAcceptance.getVersion(), touAcceptance.getFingerprint(), touAcceptance.getAcceptanceDate().toDate());    
    }

    /** {@inheritDoc} */
    public void updateToUAcceptance(String userId, ToUAcceptance touAcceptance) {
        String sql = "UPDATE " + acceptanceTable +
            " SET fingerprint = ?, acceptanceDate = ?" +
            " WHERE userId = ? AND version = ?";
        jdbcTemplate.update(sql, touAcceptance.getFingerprint(), touAcceptance.getAcceptanceDate().toDate(), userId, touAcceptance.getVersion());    
    }

    /** {@inheritDoc} */
    public ToUAcceptance readToUAcceptance(String userId, String version) {
        final String sql = "SELECT version, fingerprint, acceptanceDate" +
            " FROM " + acceptanceTable +
            " WHERE userId = ? AND version = ?";
        try {
            return jdbcTemplate.queryForObject(sql, touAcceptanceMapper, userId, version);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }
    
    /** {@inheritDoc} */
    public boolean containsToUAcceptance(String userId, String version) {
        final String sql = "SELECT COUNT(*)" +
            " FROM " + acceptanceTable +
            " WHERE userId = ? AND version = ?";
        return jdbcTemplate.queryForInt(sql, userId, version) > 0;
    }
}
