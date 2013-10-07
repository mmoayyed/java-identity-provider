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

package net.shibboleth.idp.saml.impl.session;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;
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
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.Positive;
import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import net.shibboleth.utilities.java.support.xml.XMLParserException;

/**
 * A serializer for {@link SAML2SPSession} objects.
 */
@ThreadSafe
public class SAML2SPSessionSerializer extends AbstractSPSessionSerializer {

    /** Field name of NameID. */
    private static final String NAMEID_FIELD = "nam";

    /** Field name of SessionIndex. */
    private static final String SESSION_INDEX_FIELD = "ix";

    /** DOM configuration parameters used by LSSerializer to exclude XML declaration. */
    private static Map<String, Object> noXMLDeclParams;
    
    /** Parser for NameID fields. */
    @Nonnull private final ParserPool parserPool;
    
    /**
     * Constructor.
     * 
     * @param offset milliseconds to substract from record expiration to establish session expiration value
     */
    protected SAML2SPSessionSerializer(@Duration @NonNegative long offset) {
        super(offset);
        
        parserPool = Constraint.isNotNull(XMLObjectProviderRegistrySupport.getParserPool(),
                "ParserPool cannot be null");
    }

    /** {@inheritDoc} */
    protected void doSerializeAdditional(@Nonnull final SPSession instance, @Nonnull final JsonGenerator generator) {
        SAML2SPSession saml2Session = (SAML2SPSession) instance;
        
        try {
            generator.write(NAMEID_FIELD, SerializeSupport.nodeToString(
                    XMLObjectSupport.marshall(saml2Session.getNameID()), noXMLDeclParams));
            generator.write(SESSION_INDEX_FIELD, saml2Session.getSessionIndex());
        } catch (MarshallingException e) {
            throw new XMLRuntimeException("Error marshalling and serializing NameID", e);
        }
    }

    /** {@inheritDoc} */
    @Nonnull protected SPSession doDeserialize(@Nonnull final JsonObject obj,
            @Nonnull @NotEmpty final String id, @Nonnull @NotEmpty final String flowId,
            @Duration @Positive final long creation, @Duration @Positive final long expiration) throws IOException {
        
        String rawNameID = obj.getString(NAMEID_FIELD);
        String sessionIndex = obj.getString(SESSION_INDEX_FIELD);
        
        try {
            XMLObject nameID = XMLObjectSupport.unmarshallFromReader(parserPool, new StringReader(rawNameID));
            if (nameID instanceof NameID) {
                return new SAML2SPSession(id, flowId, creation, expiration, (NameID) nameID, sessionIndex);
            } else {
                throw new IOException("XMLObject stored in NameID field was not a NameID");
            }
        } catch (XMLParserException | UnmarshallingException e) {
            throw new IOException("Unable to parse or unmarshall NameID field", e);
        }
    }

    static {
        noXMLDeclParams = Collections.<String,Object>singletonMap("xml-declaration", Boolean.FALSE);
    }
    
}