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
import java.util.List;

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
    
    private final class AgreedTermsOfUseMapper implements RowMapper<AgreedTermsOfUse> {
        public AgreedTermsOfUse mapRow(ResultSet rs, int rowNum) throws SQLException {
            final TermsOfUse termsOfUse = new TermsOfUse(rs.getString("version"), rs.getString("fingerprint"));
            final AgreedTermsOfUse agreedTermsOfUse = new AgreedTermsOfUse(termsOfUse, new DateTime(rs.getTimestamp("agreeDate")));
            return agreedTermsOfUse;
        }
    }

    private final class AttributeReleaseConsentMapper implements RowMapper<AttributeReleaseConsent> {
        public AttributeReleaseConsent mapRow(ResultSet rs, int rowNum) throws SQLException {
            final Attribute attribute = new Attribute(rs.getString("attributeId"), rs.getString("attributeValuesHash"));
        	final AttributeReleaseConsent attributeReleaseConsent = new AttributeReleaseConsent(attribute, new DateTime(rs.getTimestamp("releaseDate")));
            return attributeReleaseConsent;
        }
    }

    private final class PrincipalMapper implements RowMapper<Principal> {
        public final Principal mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Principal(
            		rs.getLong("id"),
            		rs.getString("uniqueId"),
            		new DateTime(rs.getTimestamp("firstAccess")),
            		new DateTime(rs.getTimestamp("lastAccess")),
            		rs.getBoolean("globalConsent"));
        }
    }

    private  final class RelyingPartyMapper implements RowMapper<RelyingParty> {
        public final RelyingParty mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new RelyingParty(
            		rs.getLong("id"), rs.getString("entityId"));
        }
    }

    private SimpleJdbcTemplate jdbcTemplate;

    private final PrincipalMapper principalMapper = new PrincipalMapper();

    private final RelyingPartyMapper relyingPartyMapper = new RelyingPartyMapper();

    private final AttributeReleaseConsentMapper attributeReleaseConsentMapper = new AttributeReleaseConsentMapper();

    private final AgreedTermsOfUseMapper agreedTermsOfUseMapper = new AgreedTermsOfUseMapper();

    /** {@inheritDoc} */
    public AgreedTermsOfUse createAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate) {
        final String sql = " INSERT INTO AgreedTermsOfUse (principalId, version, fingerprint, agreeDate)"
                + " VALUES (?, ?, ?, ?)";
        try {
        	this.jdbcTemplate.update(sql, principal.getId(), termsOfUse.getVersion(), termsOfUse.getFingerprint(), agreeDate.toDate()); 	
        } catch (DataAccessException e) {
        	logger.warn("Storage exception {}", e);
        }
        return readAgreedTermsOfUse(principal, termsOfUse);
    }

    /** {@inheritDoc} */
    public AttributeReleaseConsent createAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime releaseDate) {
        final String sql = " INSERT INTO AttributeReleaseConsent"
                + " (principalId, relyingPartyId, attributeId, attributeValuesHash, releaseDate)"
                + " VALUES (?, ?, ?, ?, ?)";        
        try {
        	this.jdbcTemplate.update(sql, principal.getId(), relyingParty.getId(), attribute.getId(), attribute
                .getValuesHash(), releaseDate.toDate());
	    } catch (DataAccessException e) {
	    	logger.warn("Storage exception {}", e);
	    }
        return readAttributeReleaseConsent(principal, relyingParty, attribute);
    }

    /** {@inheritDoc} */
    public Principal createPrincipal(final String uniqueId, final DateTime accessDate) {
        final String sql = " INSERT INTO Principal (uniqueId, firstAccess, lastAccess, globalConsent) VALUES (?, ?, ?, ?)";
        try {
            this.jdbcTemplate.update(sql, uniqueId, accessDate.toDate(), accessDate.toDate(), false);
        } catch (DataAccessException e) {
            logger.warn("Storage exception {}", e);
        }
        long id = findPrincipal(uniqueId);
        return readPrincipal(id);
    }

    /** {@inheritDoc} */
    public RelyingParty createRelyingParty(final String entityId) {
        final String sql = " INSERT INTO RelyingParty (entityId) VALUES (?)";
        try {
            this.jdbcTemplate.update(sql, entityId);
        } catch (DataAccessException e) {
            logger.warn("Storage exception {}", e);
        } 
        long id = findRelyingParty(entityId);
        return readRelyingParty(id);   
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
    public int deleteAttributeReleaseConsents(final Principal principal) {
    	throw new UnsupportedOperationException();
    }
    
    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        final String sql = " DELETE FROM AttributeReleaseConsent WHERE principalId = ? AND relyingPartyId = ?";
        return this.jdbcTemplate.update(sql, principal.getId(), relyingParty.getId());
    }

    /** {@inheritDoc} */
    public int deleteAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute) {
        throw new UnsupportedOperationException();
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
    public long findPrincipal(final String uniqueId) {
        final String sql = " SELECT id FROM Principal WHERE uniqueId = ?";
        long id = 0;
        try {
            id = jdbcTemplate.queryForLong(sql, uniqueId);
        } catch (DataAccessException e) {}
        return id;
    }

    /** {@inheritDoc} */
    public long findRelyingParty(final String entityId) {
        final String sql = " SELECT id FROM RelyingParty WHERE entityId = ?";
        long id = 0;
        try {
            id = jdbcTemplate.queryForLong(sql, entityId);
        } catch (DataAccessException e) {}
        return id;
    }

    /** {@inheritDoc} */
    public AgreedTermsOfUse readAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse) {
        final String sql = " SELECT principalId, version, fingerprint, agreeDate FROM AgreedTermsOfUse"
                + " WHERE principalId = ? AND version = ?";
        return this.jdbcTemplate
                .queryForObject(sql, agreedTermsOfUseMapper, principal.getId(), termsOfUse.getVersion());
    }

    /** {@inheritDoc} */
    public List<AgreedTermsOfUse> readAgreedTermsOfUses(final Principal principal) {
        final String sql = " SELECT principalId, version, fingerprint, agreeDate FROM AgreedTermsOfUse"
                + " WHERE principalId = ?";
        return this.jdbcTemplate.query(sql, agreedTermsOfUseMapper, principal.getId());
    }

    /** {@inheritDoc} */
    public AttributeReleaseConsent readAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            Attribute attribute) {
        final String sql = " SELECT principalId, relyingPartyId, attributeId, attributeValuesHash, releaseDate"
            + " FROM AttributeReleaseConsent" + " WHERE principalId = ? AND relyingPartyId = ? AND attributeId = ?";
        return this.jdbcTemplate.queryForObject(sql, attributeReleaseConsentMapper, principal.getId(), relyingParty.getId(), attribute.getId());
    }

    /** {@inheritDoc} */
    public List<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public List<AttributeReleaseConsent> readAttributeReleaseConsents(final Principal principal, final RelyingParty relyingParty) {
        final String sql = " SELECT principalId, relyingPartyId, attributeId, attributeValuesHash, releaseDate"
                + " FROM AttributeReleaseConsent" + " WHERE principalId = ? AND relyingPartyId = ?";
        return this.jdbcTemplate.query(sql, attributeReleaseConsentMapper, principal.getId(), relyingParty.getId());
    }

    /** {@inheritDoc} */
    public Principal readPrincipal(final long id) {
        final String sql = " SELECT id, uniqueId, firstAccess, lastAccess, globalConsent" + " FROM Principal"
                + " WHERE id = ?";
        return this.jdbcTemplate.queryForObject(sql, principalMapper, id);
    }

    /** {@inheritDoc} */
    public RelyingParty readRelyingParty(final long id) {
        final String sql = " SELECT id, entityId" + " FROM RelyingParty" + " WHERE id = ?";
        return this.jdbcTemplate.queryForObject(sql, relyingPartyMapper, id);
    }

    /** {@inheritDoc} */
    public void setDataSource(final DataSource dataSource) {
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    /** {@inheritDoc} */
    public AgreedTermsOfUse updateAgreedTermsOfUse(final Principal principal, final TermsOfUse termsOfUse, final DateTime agreeDate) {
        final String sql = " UPDATE AgreedTermsOfUse SET fingerprint = ?, agreeDate = ?"
                + " WHERE principalId = ? AND version = ?";
        try {
            this.jdbcTemplate.update(sql, termsOfUse.getFingerprint(), agreeDate.toDate(), principal.getId(), termsOfUse.getVersion());
        } catch (DataAccessException e) {
            logger.warn("Storage exception {}", e);
        }
        return readAgreedTermsOfUse(principal, termsOfUse);
    }

    /** {@inheritDoc} */
    public AttributeReleaseConsent updateAttributeReleaseConsent(final Principal principal, final RelyingParty relyingParty,
            final Attribute attribute, final DateTime releaseDate) {
        final String sql = " UPDATE AttributeReleaseConsent SET attributeValuesHash = ?, releaseDate = ?"
                + " WHERE principalId = ? AND relyingPartyId = ? AND attributeId = ?";
        try {
            this.jdbcTemplate.update(sql, attribute.getValuesHash(), releaseDate.toDate(), principal.getId(), relyingParty.getId(), attribute.getId());
        } catch (DataAccessException e) {
            logger.warn("Storage exception {}", e);
        }        
        return readAttributeReleaseConsent(principal, relyingParty, attribute);
    }

    /** {@inheritDoc} */
    public Principal updatePrincipal(final Principal principal) {
        final String sql = " UPDATE Principal SET lastAccess = ?, globalConsent = ? WHERE id = ?";
        try {
            this.jdbcTemplate.update(sql, principal.getLastAccess().toDate(), principal.hasGlobalConsent(), principal.getId());
        } catch (DataAccessException e) {
            logger.warn("Storage exception {}", e);
        }
        return readPrincipal(principal.getId());
    }

    /** {@inheritDoc} */
    public RelyingParty updateRelyingParty(final RelyingParty relyingParty) {
        throw new UnsupportedOperationException();
    }

}
