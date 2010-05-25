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
import java.util.Collection;

import javax.sql.DataSource;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;


import edu.internet2.middleware.shibboleth.idp.consent.components.TermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AgreedTermsOfUse;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Attribute;
import edu.internet2.middleware.shibboleth.idp.consent.entities.AttributeReleaseConsent;
import edu.internet2.middleware.shibboleth.idp.consent.entities.Principal;
import edu.internet2.middleware.shibboleth.idp.consent.entities.RelyingParty;

/**
 *
 */
public class JDBCStorage implements Storage {

    private final Logger logger = LoggerFactory.getLogger(JDBCStorage.class);
    
    private static final class AgreedTermsOfUseMapper implements RowMapper<AgreedTermsOfUse> {
        public AgreedTermsOfUse mapRow(ResultSet rs, int rowNum) throws SQLException {
            final TermsOfUse termsOfUse = new TermsOfUse(rs.getString("version"), rs.getString("fingerprint"));
            final AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse(termsOfUse, new DateTime(rs.getTimestamp("agreeDate")));
            return agreedTermsOfUse;
        }
    }

    private static final class AttributeReleaseConsentMapper implements RowMapper<AttributeReleaseConsent> {
        public AttributeReleaseConsent mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Attribute attribute = new Attribute(rs.getString("attributeId"), rs.getString("attributeValuesHash"));
        	final AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent(attribute, new DateTime(rs.getTimestamp("releaseDate")));
            return attributeReleaseConsent;
        }
    }

    private static final class PrincipalMapper implements RowMapper<Principal> {
        public final Principal mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Principal(
            		rs.getString("uniqueId"),
            		new DateTime(rs.getTimestamp("firstAccess")),
            		new DateTime(rs.getTimestamp("lastAccess")),
            		rs.getBoolean("globalConsent"));
        }
    }

    private static final class RelyingPartyMapper implements RowMapper<RelyingParty> {
        public final RelyingParty mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RelyingParty(rs.getString("entityId"));
        }
    }

    private SimpleJdbcTemplate jdbcTemplate;

    private final PrincipalMapper principalMapper = new PrincipalMapper();

    private final RelyingPartyMapper relyingPartyMapper = new RelyingPartyMapper();

    private final AttributeReleaseConsentMapper attributeReleaseConsentMapper = new AttributeReleaseConsentMapper();

    private final AgreedTermsOfUseMapper agreedTermsOfUseMapper = new AgreedTermsOfUseMapper();

    /** {@inheritDoc} */
    public final AgreedTermsOfUse createAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate) {
        final String sql = " INSERT INTO AgreedTermsOfUse (principalId, version, fingerprint, agreeDate)"
                + " VALUES (?, ?, ?, ?)";
        try {
        	jdbcTemplate.update(sql, principal.getUniqueId(), termsOfUse.getVersion(), termsOfUse.getFingerprint(), agreeDate.toDate()); 	
        } catch (DataAccessException e) {
            logger.warn("AgreedTermsOfUse already exists", e);
        }
        return readAgreedTermsOfUse(principal, termsOfUse);
    }

    /** {@inheritDoc} */
    public final AttributeReleaseConsent createAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime releaseDate) {
        final String sql = " INSERT INTO AttributeReleaseConsent"
                + " (principalId, relyingPartyId, attributeId, attributeValuesHash, releaseDate)"
                + " VALUES (?, ?, ?, ?, ?)";        
        try {
        	jdbcTemplate.update(sql, principal.getUniqueId(), relyingParty.getEntityId(), attribute.getId(), attribute
                .getValuesHash(), releaseDate.toDate());
	    } catch (DataAccessException e) {
	    	logger.warn("AttributeReleaseConsent already exists", e);
	    }
        return readAttributeReleaseConsent(principal, relyingParty, attribute);
    }

    /** {@inheritDoc} */
    public final Principal createPrincipal(final String uniqueId, final DateTime accessDate) {
        final String sql = " INSERT INTO Principal (uniqueId, firstAccess, lastAccess, globalConsent) VALUES (?, ?, ?, ?)";
        try {
            jdbcTemplate.update(sql, uniqueId, accessDate.toDate(), accessDate.toDate(), false);
        } catch (DataAccessException e) {
            logger.warn("Principal already exists {}", uniqueId, e);        }
        return readPrincipal(uniqueId);
    }

    /** {@inheritDoc} */
    public final RelyingParty createRelyingParty(final String entityId) {
        final String sql = " INSERT INTO RelyingParty (entityId) VALUES (?)";
        try {
            jdbcTemplate.update(sql, entityId);
        } catch (DataAccessException e) {
            logger.warn("RelyingParty already exists {}", entityId, e);
        } 
        return readRelyingParty(entityId);   
    }

    /** {@inheritDoc} */
    public boolean deleteAgreedTermsOfUse(final Principal principal, TermsOfUse termsOfUse) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAgreedTermsOfUses(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal) {
    	throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        final String sql = " DELETE FROM AttributeReleaseConsent WHERE principalId = ? AND relyingPartyId = ?";
        return jdbcTemplate.update(sql, principal.getUniqueId(), relyingParty.getEntityId());
    }

    /** {@inheritDoc} */
    public boolean deleteAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute) {
        throw new UnsupportedOperationException();
    }


    /** {@inheritDoc} */
    public boolean deletePrincipal(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean deleteRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean containsPrincipal(final String uniqueId) {
        final String sql = " SELECT COUNT(uniqueId) FROM Principal WHERE uniqueId = ?";
        return jdbcTemplate.queryForInt(sql, uniqueId) > 0;
    }

    /** {@inheritDoc} */
    public boolean containsRelyingParty(final String entityId) {
        final String sql = " SELECT COUNT(entityId) FROM RelyingParty WHERE entityId = ?";
        return jdbcTemplate.queryForInt(sql, entityId) > 0;
    }

    /** {@inheritDoc} */
    public final AgreedTermsOfUse readAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse) {
        final String sql = " SELECT principalId, version, fingerprint, agreeDate FROM AgreedTermsOfUse"
                + " WHERE principalId = ? AND version = ?";
        return jdbcTemplate
                .queryForObject(sql, agreedTermsOfUseMapper, principal.getUniqueId(), termsOfUse.getVersion());
    }

    /** {@inheritDoc} */
    public final Collection<AgreedTermsOfUse> readAgreedTermsOfUses(final Principal principal) {
        final String sql = " SELECT principalId, version, fingerprint, agreeDate FROM AgreedTermsOfUse"
                + " WHERE principalId = ?";
        return jdbcTemplate.query(sql, agreedTermsOfUseMapper, principal.getUniqueId());
    }

    /** {@inheritDoc} */
    public final AttributeReleaseConsent readAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            Attribute attribute) {
        final String sql = " SELECT principalId, relyingPartyId, attributeId, attributeValuesHash, releaseDate"
            + " FROM AttributeReleaseConsent WHERE principalId = ? AND relyingPartyId = ? AND attributeId = ?";
        return jdbcTemplate.queryForObject(sql, attributeReleaseConsentMapper, principal.getUniqueId(), relyingParty.getEntityId(), attribute.getId());
    }

    /** {@inheritDoc} */
    public final Collection<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public final Collection<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        final String sql = " SELECT principalId, relyingPartyId, attributeId, attributeValuesHash, releaseDate"
                + " FROM AttributeReleaseConsent WHERE principalId = ? AND relyingPartyId = ?";
        return jdbcTemplate.query(sql, attributeReleaseConsentMapper, principal.getUniqueId(), relyingParty.getEntityId());
    }

    /** {@inheritDoc} */
    public final Principal readPrincipal(final String uniqueId) {
        final String sql = " SELECT uniqueId, firstAccess, lastAccess, globalConsent FROM Principal"
                + " WHERE uniqueId = ?";
        return jdbcTemplate.queryForObject(sql, principalMapper, uniqueId);
    }

    /** {@inheritDoc} */
    public final RelyingParty readRelyingParty(final String entityId) {
        final String sql = " SELECT entityId FROM RelyingParty WHERE entityId = ?";
        return jdbcTemplate.queryForObject(sql, relyingPartyMapper, entityId);
    }

    /** {@inheritDoc} */
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    /** {@inheritDoc} */
    public boolean updateAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate) {
        final String sql = " UPDATE AgreedTermsOfUse SET fingerprint = ?, agreeDate = ?"
                + " WHERE principalId = ? AND version = ?";
        
        return jdbcTemplate.update(sql, termsOfUse.getFingerprint(), agreeDate.toDate(), principal.getUniqueId(), termsOfUse.getVersion()) > 0;
    }

    /** {@inheritDoc} */
    public boolean updateAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime releaseDate) {
        final String sql = " UPDATE AttributeReleaseConsent SET attributeValuesHash = ?, releaseDate = ?"
                + " WHERE principalId = ? AND relyingPartyId = ? AND attributeId = ?";
        return jdbcTemplate.update(sql, attribute.getValuesHash(), releaseDate.toDate(), principal.getUniqueId(), relyingParty.getEntityId(), attribute.getId()) > 0;
    }

    /** {@inheritDoc} */
    public boolean updatePrincipal(final Principal principal) {
        final String sql = " UPDATE Principal SET lastAccess = ?, globalConsent = ? WHERE uniqueId = ?";
        return jdbcTemplate.update(sql, principal.getLastAccess().toDate(), principal.hasGlobalConsent(), principal.getUniqueId()) > 0;
    }

    /** {@inheritDoc} */
    public boolean updateRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }

}
