/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
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

package net.shibboleth.idp.saml.session.impl;

import java.io.IOException;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.stream.JsonGenerator;

import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLRuntimeException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.NameID;

import net.shibboleth.idp.saml.session.SAML2SPSession;
import net.shibboleth.idp.session.AbstractSPSessionSerializer;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.shared.annotation.ParameterName;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.annotation.constraint.ThreadSafeAfterInit;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.xml.ParserPool;
import net.shibboleth.shared.xml.SerializeSupport;
import net.shibboleth.shared.xml.XMLParserException;

/**
 * A serializer for {@link SAML2SPSession} objects.
 */
@ThreadSafeAfterInit
public class SAML2SPSessionSerializer extends AbstractSPSessionSerializer {

    /** Field name of NameID. */
    @Nonnull @NotEmpty private static final String NAMEID_FIELD = "nam";

    /** Field name of SessionIndex. */
    @Nonnull @NotEmpty private static final String SESSION_INDEX_FIELD = "ix";

    /** Field name of ACS location. */
    @Nonnull @NotEmpty private static final String ACS_LOC_FIELD = "acs";

    /** Field name of Single Logout indicator. */
    @Nonnull @NotEmpty private static final String LOGOUT_PROP_FIELD = "slo";
    
    /** DOM configuration parameters used by LSSerializer to exclude XML declaration. */
    @Nonnull private static final Map<String, Object> NO_XML_DECL_PARAMS;
    
    /** Parser for NameID fields. */
    @Nonnull private ParserPool parserPool;
    
    /**
     * Constructor.
     * 
     * @param offset time to subtract from record expiration to establish session expiration value
     */
    public SAML2SPSessionSerializer(@Nonnull @ParameterName(name="offset") final Duration offset) {
        super(offset);
        
        parserPool = Constraint.isNotNull(XMLObjectProviderRegistrySupport.getParserPool(),
                "ParserPool cannot be null");
    }
    
    /**
     * Set the {@link ParserPool} to use.
     * 
     * @param pool  parser source
     */
    public void setParserPool(@Nonnull final ParserPool pool) {
        checkSetterPreconditions();
        parserPool = Constraint.isNotNull(pool, "ParserPool cannot be null");
    }
   
    /** {@inheritDoc} */
    @Override
    protected void doSerializeAdditional(@Nonnull final SPSession instance, @Nonnull final JsonGenerator generator) {
        final SAML2SPSession saml2Session = (SAML2SPSession) instance;
        
        try {
            generator.write(NAMEID_FIELD, SerializeSupport.nodeToString(
                    XMLObjectSupport.marshall(saml2Session.getNameID()), NO_XML_DECL_PARAMS));
            generator.write(SESSION_INDEX_FIELD, saml2Session.getSessionIndex());
            if (saml2Session.getACSLocation() != null) {
                generator.write(ACS_LOC_FIELD, saml2Session.getACSLocation());
            }
            generator.write(LOGOUT_PROP_FIELD, saml2Session.supportsLogoutPropagation());
        } catch (final MarshallingException e) {
            throw new XMLRuntimeException("Error marshalling and serializing NameID", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull protected SPSession doDeserialize(@Nonnull final JsonObject obj, @Nonnull @NotEmpty final String id, 
            @Nonnull final Instant creation, @Nonnull final Instant expiration) throws IOException {
        
        final JsonString rawNameID = obj.getJsonString(NAMEID_FIELD);
        final JsonString sessionIndex = obj.getJsonString(SESSION_INDEX_FIELD);
        final JsonString acsLocation = obj.getJsonString(ACS_LOC_FIELD);
        final boolean supportsLogoutProp = obj.getBoolean(LOGOUT_PROP_FIELD, true);
        
        if (rawNameID == null || sessionIndex == null) {
            throw new IOException("Serialized SAML2SPSession missing required fields");
        }
        
        try {
            final XMLObject nameID =
                    XMLObjectSupport.unmarshallFromReader(parserPool, new StringReader(rawNameID.getString()));
            if (nameID instanceof NameID) {
                final String sessionIndexString = sessionIndex.getString();
                assert sessionIndexString!= null;
                return new SAML2SPSession(id, creation, expiration, (NameID) nameID, sessionIndexString,
                        acsLocation != null ? acsLocation.getString() : null, supportsLogoutProp);
            }
            throw new IOException("XMLObject stored in NameID field was not a NameID");
        } catch (final XMLParserException | UnmarshallingException e) {
            throw new IOException("Unable to parse or unmarshall NameID field", e);
        }
    }

    static {
        NO_XML_DECL_PARAMS = CollectionSupport.<String,Object>singletonMap("xml-declaration", Boolean.FALSE);
    }
    
}