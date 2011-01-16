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

package net.shibboleth.idp.attribute.consent.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.sql.DataSource;

import net.shibboleth.idp.attribute.consent.AttributeRelease;
import net.shibboleth.idp.attribute.consent.User;

import org.joda.time.DateTime;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;



/**
 *
 */
public class JDBCStorage implements Storage {
    
    private static final class AttributeReleaseMapper implements RowMapper<AttributeRelease> {
        public AttributeRelease mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new AttributeRelease(rs.getString("attributeId"), rs.getString("valuesHash"), new DateTime(rs.getTimestamp("consentDate")));
        }
    }

    private static final class UserMapper implements RowMapper<User> {
        public final User mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new User(rs.getString("id"), rs.getBoolean("globalConsent"));
        }
    }

    private SimpleJdbcTemplate jdbcTemplate;
    private final UserMapper userMapper = new UserMapper();
    private final AttributeReleaseMapper attributeReleaseMapper = new AttributeReleaseMapper();

    private String userTable = "User";
    private String attributeReleaseTable = "AttributeRelease";
    
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }
    
    /** {@inheritDoc} */
    public boolean containsUser(String userId) {
        final String sql = " SELECT COUNT(*)" +
            " FROM " + userTable +
            " WHERE id = ?";    
        return jdbcTemplate.queryForInt(sql, userId) > 0;
    }

    /** {@inheritDoc} */
    public User readUser(String userId) {
        final String sql = "SELECT id, globalConsent" +
            " FROM " + userTable +
            " WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userMapper, userId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }


    /** {@inheritDoc} */
    public void updateUser(User user) {
        final String sql = "UPDATE " + userTable +
            " SET globalConsent = ?" +
            " WHERE id = ?";
        jdbcTemplate.update(sql, user.hasGlobalConsent(), user.getId()); 
    }


    /** {@inheritDoc} */
    public void createUser(User user) {
        final String sql = "INSERT INTO " + userTable +
            " (id, globalConsent)" +
            " VALUES (?, ?)";
        jdbcTemplate.update(sql, user.getId(), user.hasGlobalConsent()); 
    }


    /** {@inheritDoc} */
    public Collection<AttributeRelease> readAttributeReleases(String userId, String relyingPartyId) {
        final String sql = "SELECT attributeId, valuesHash, consentDate" +
            " FROM " + attributeReleaseTable +
            " WHERE userId = ? AND relyingPartyId = ?";
        try {
            return jdbcTemplate.query(sql, attributeReleaseMapper, userId, relyingPartyId);
        } catch (EmptyResultDataAccessException e) {
            return Collections.EMPTY_SET;
        }
    }


    /** {@inheritDoc} */
    public void deleteAttributeReleases(String userId, String relyingPartyId) {
        final String sql = "DELETE" +
            " FROM " + attributeReleaseTable +
            " WHERE userId = ? AND relyingPartyId = ?";
        jdbcTemplate.update(sql, userId, relyingPartyId);
    }


    /** {@inheritDoc} */
    public boolean containsAttributeRelease(String userId, String relyingPartyId, String attributeId) {
        final String sql = " SELECT COUNT(*)" +
            " FROM " + attributeReleaseTable +
            " WHERE userId = ? AND relyingPartyId = ? AND attributeId =?";    
        return jdbcTemplate.queryForInt(sql, userId, relyingPartyId, attributeId) > 0;
    }


    /** {@inheritDoc} */
    public void updateAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease) {
        final String sql = "UPDATE " + attributeReleaseTable +
            " SET valuesHash = ?, consentDate = ?" +
            " WHERE userId = ? AND relyingPartyId = ? AND attributeId = ?";
        jdbcTemplate.update(sql, attributeRelease.getValuesHash(), attributeRelease.getDate().toDate(), userId, relyingPartyId, attributeRelease.getAttributeId());
    }


    /** {@inheritDoc} */
    public void createAttributeRelease(String userId, String relyingPartyId, AttributeRelease attributeRelease) {
        final String sql = "INSERT INTO " + attributeReleaseTable +
            " (userId, relyingPartyId, attributeId, valuesHash, consentDate)" +
            " VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, userId, relyingPartyId, attributeRelease.getAttributeId(), attributeRelease.getValuesHash(), attributeRelease.getDate().toDate());        
    }
}
