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

package edu.internet2.middleware.shibboleth.idp.consent.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;
import edu.internet2.middleware.shibboleth.idp.consent.entities.TermsOfUse;

/**
 *
 */
public class JDBCStorage implements Storage {

    private final Logger logger = LoggerFactory.getLogger(JDBCStorage.class);
    
    private final class AgreedTermsOfUseMapper implements RowMapper<AgreedTermsOfUse> {
        public AgreedTermsOfUse mapRow(ResultSet rs, int rowNum) throws SQLException {
            final AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse();
            final TermsOfUse termsOfUse = new TermsOfUse();
            termsOfUse.setVersion(rs.getString("version"));
            termsOfUse.setFingerprint(rs.getInt("fingerprint"));
            agreedTermsOfUse.setTermsOfUse(termsOfUse);
            agreedTermsOfUse.setAgreeDate(rs.getTimestamp("agreeDate"));
            return agreedTermsOfUse;
        }
    }

    private final class AttributeReleaseConsentMapper implements RowMapper<AttributeReleaseConsent> {
        public AttributeReleaseConsent mapRow(ResultSet rs, int rowNum) throws SQLException {
            final AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent();
            final Attribute attribute = new Attribute();
            attribute.setId(rs.getString("attributeId"));
            attribute.setValueHash(rs.getInt("attributeValueHash"));
            attributeReleaseConsent.setAttribute(attribute);
            attributeReleaseConsent.setReleaseDate(rs.getTimestamp("releaseDate"));
            return attributeReleaseConsent;
        }
    }

    private final class PrincipalMapper implements RowMapper<Principal> {
        public Principal mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Principal principal = new Principal();
            principal.setId(rs.getLong("id"));
            principal.setUniqueId(rs.getString("uniqueId"));
            principal.setFirstAccess(rs.getTimestamp("firstAccess"));
            principal.setLastAccess(rs.getTimestamp("lastAccess"));
            principal.setGlobalConsent(rs.getBoolean("globalConsent"));
            return principal;
        }
    }

    private  final class RelyingPartyMapper implements RowMapper<RelyingParty> {
        public RelyingParty mapRow(ResultSet rs, int rowNum) throws SQLException {
            final RelyingParty relyingParty = new RelyingParty();
            relyingParty.setId(rs.getLong("id"));
            relyingParty.setEntityId(rs.getString("entityId"));
            return relyingParty;
        }
    }

    private SimpleJdbcTemplate jdbcTemplate;

    private final PrincipalMapper principalMapper = new PrincipalMapper();

    private final RelyingPartyMapper relyingPartyMapper = new RelyingPartyMapper();

    private final AttributeReleaseConsentMapper attributeReleaseConsentMapper = new AttributeReleaseConsentMapper();

    private final AgreedTermsOfUseMapper agreedTermsOfUseMapper = new AgreedTermsOfUseMapper();

    /** {@inheritDoc} */
    public int createAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final Date agreeDate) {
        final String sql = " INSERT INTO AgreedTermsOfUse" + " (principalId, version, fingerprint, agreeDate)"
                + " VALUES (?, ?, ?, ?)";
        return this.jdbcTemplate.update(sql, principal.getId(), termsOfUse.getVersion(), termsOfUse.getFingerprint(),
                agreeDate);
    }

    /** {@inheritDoc} */
    public int createAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final Date releaseDate) {
        final String sql = " INSERT INTO AttributeReleaseConsent"
                + " (principalId, relyingPartyId, attributeId, attributeValueHash, releaseDate)"
                + " VALUES (?, ?, ?, ?, ?)";
        return this.jdbcTemplate.update(sql, principal.getId(), relyingParty.getId(), attribute.getId(), attribute
                .getValueHash(), releaseDate);
    }

    /** {@inheritDoc} */
    public long createPrincipal(final Principal principal) {
        final String sql = " INSERT INTO Principal" + " (uniqueId, firstAccess, lastAccess, globalConsent)"
                + " VALUES (?, ?, ?, ?)";

        this.jdbcTemplate.update(sql, principal.getUniqueId(), principal.getFirstAccess(), principal
                .getLastAccess(), principal.hasGlobalConsent());
        return findPrincipal(principal);
    }

    /** {@inheritDoc} */
    public long createRelyingParty(final RelyingParty relyingParty) {
        final String sql = " INSERT INTO RelyingParty" + " (entityId)" + " VALUES (?)";

        this.jdbcTemplate.update(sql, relyingParty.getEntityId());
        return findRelyingParty(relyingParty);
    }

    /** {@inheritDoc} */
    public int deleteAgreedTermsOfUse(final Principal principal, TermsOfUse termsOfUse) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAgreedTermsOfUses(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsent(final Principal principal) {
        final String sql = " DELETE FROM AttributeReleaseConsent" + " WHERE principalId = ?";

        return this.jdbcTemplate.update(sql, principal.getId());
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal) {
        final String sql = " DELETE FROM AttributeReleaseConsent" + " WHERE principalId = ?";
        return this.jdbcTemplate.update(sql, principal.getId());
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        final String sql = " DELETE FROM AttributeReleaseConsent" + " WHERE principalId = ? AND relyingPartyId = ?";
        return this.jdbcTemplate.update(sql, principal.getId(), relyingParty.getId());
    }

    /** {@inheritDoc} */
    public int deletePrincipal(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public long findPrincipal(final Principal principal) {
        final String sql = " SELECT id FROM Principal WHERE uniqueId = ?";
        long id = 0;
        try {
            id = jdbcTemplate.queryForLong(sql, principal.getUniqueId());
        } catch (DataAccessException e) {}
        principal.setId(id);
        return id;
    }

    /** {@inheritDoc} */
    public long findRelyingParty(final RelyingParty relyingParty) {
        final String sql = " SELECT id FROM RelyingParty WHERE entityId = ?";
        long id = 0;
        try {
            id = jdbcTemplate.queryForLong(sql, relyingParty.getEntityId());
        } catch (DataAccessException e) {}
        relyingParty.setId(id);
        return id;
    }

    /** {@inheritDoc} */
    public AgreedTermsOfUse readAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse) {
        final String sql = " SELECT principalId, version, fingerprint, agreeDate" + " FROM AgreedTermsOfUse"
                + " WHERE principalId = ? AND version = ?";
        return this.jdbcTemplate
                .queryForObject(sql, agreedTermsOfUseMapper, principal.getId(), termsOfUse.getVersion());
    }

    /** {@inheritDoc} */
    public List<AgreedTermsOfUse> readAgreedTermsOfUses(final Principal principal) {
        final String sql = " SELECT principalId, version, fingerprint, agreeDate" + " FROM AgreedTermsOfUse"
                + " WHERE principalId = ?";
        return this.jdbcTemplate.query(sql, agreedTermsOfUseMapper, principal.getId());
    }

    /** {@inheritDoc} */
    public AttributeReleaseConsent readAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            Attribute attribute) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public List<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public List<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        final String sql = " SELECT principalId, relyingPartyId, attributeId, attributeValueHash, releaseDate"
                + " FROM AttributeReleaseConsent" + " WHERE principalId = ? AND relyingPartyId = ?";
        return this.jdbcTemplate.query(sql, attributeReleaseConsentMapper, principal.getId(), relyingParty.getId());
    }

    /** {@inheritDoc} */
    public Principal readPrincipal(final Principal principal) {
        final String sql = " SELECT id, uniqueId, firstAccess, lastAccess, globalConsent" + " FROM Principal"
                + " WHERE id = ?";
        return this.jdbcTemplate.queryForObject(sql, principalMapper, principal.getId());
    }

    /** {@inheritDoc} */
    public RelyingParty readRelyingParty(final RelyingParty relyingParty) {
        final String sql = " SELECT id, entityId" + " FROM RelyingParty" + " WHERE id = ?";
        return this.jdbcTemplate.queryForObject(sql, relyingPartyMapper, relyingParty.getId());
    }

    /** {@inheritDoc} */
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    /** {@inheritDoc} */
    public int updateAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final Date agreeDate) {
        final String sql = " UPDATE AgreedTermsOfUse" + " SET fingerprint = ?, agreeDate = ?"
                + " WHERE principalId = ? AND version = ?";
        return this.jdbcTemplate.update(sql, termsOfUse.getFingerprint(), agreeDate, principal.getId(), termsOfUse
                .getVersion());
    }

    /** {@inheritDoc} */
    public int updateAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final Date releaseDate) {
        final String sql = " UPDATE AttributeReleaseConsent" + " SET attributeValueHash = ?, releaseDate = ?"
                + " WHERE principalId = ? AND relyingPartyId = ? AND attributeId = ?";
        return this.jdbcTemplate.update(sql, attribute.getValueHash(), releaseDate, principal.getId(), relyingParty
                .getId(), attribute.getId());
    }

    /** {@inheritDoc} */
    public int updatePrincipal(final Principal principal) {
        final String sql = " UPDATE Principal" + " SET lastAccess = ?, globalConsent = ?" + " WHERE id = ?";
        return this.jdbcTemplate
                .update(sql, principal.getLastAccess(), principal.hasGlobalConsent(), principal.getId());
    }

    /** {@inheritDoc} */
    public int updateRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }

}
