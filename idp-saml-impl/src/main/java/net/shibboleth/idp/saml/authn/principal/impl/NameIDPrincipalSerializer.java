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

package net.shibboleth.idp.saml.authn.principal.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml2.core.NameID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.shibboleth.idp.authn.principal.AbstractPrincipalSerializer;
import net.shibboleth.idp.saml.authn.principal.NameIDPrincipal;
import net.shibboleth.shared.annotation.constraint.NotEmpty;

/**
 * Principal serializer for {@link NameIDPrincipal}.
 */
@ThreadSafe
public class NameIDPrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of principal name. */
    @Nonnull @NotEmpty private static final String PRINCIPAL_NAME_FIELD = "NID";

    /** Field name of Format attribute. */
    @Nonnull @NotEmpty private static final String FORMAT_FIELD = "F";

    /** Field name of NameQualifier attribute. */
    @Nonnull @NotEmpty private static final String NAME_QUALIFIER_FIELD = "NQ";

    /** Field name of SPNameQualifier attribute. */
    @Nonnull @NotEmpty private static final String SP_NAME_QUALIFIER_FIELD = "SPNQ";

    /** Field name of SPProvidedID attribute. */
    @Nonnull @NotEmpty private static final String SP_PROVIDED_ID_FIELD = "SPID";
    
    /** Pattern used to determine if input is supported. */
    private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"NID\":.*}$");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(NameIDPrincipalSerializer.class);
    
    /** JSON object bulder factory. */
    @Nonnull private final JsonBuilderFactory objectBuilderFactory;
    
    /** NameID builder. */
    @Nonnull private final SAMLObjectBuilder<NameID> nameIDBuilder;

    /** Constructor. */
    public NameIDPrincipalSerializer() {
        objectBuilderFactory = Json.createBuilderFactory(null);
        nameIDBuilder = (SAMLObjectBuilder<NameID>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameID>getBuilderOrThrow(
                        NameID.DEFAULT_ELEMENT_NAME);
    }
    
    /** {@inheritDoc} */
    public boolean supports(@Nonnull final Principal principal) {
        return principal instanceof NameIDPrincipal;
    }

    /** {@inheritDoc} */
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        
        final NameID nameID = ((NameIDPrincipal) principal).getNameID();
        
        final StringWriter sink = new StringWriter(128);
        try (final JsonGenerator gen = getJsonGenerator(sink)) {
            gen.writeStartObject()
                .write(PRINCIPAL_NAME_FIELD, nameID.getValue());
            
            if (nameID.getFormat() != null) {
                gen.write(FORMAT_FIELD, nameID.getFormat());
            }

            if (nameID.getNameQualifier() != null) {
                gen.write(NAME_QUALIFIER_FIELD, nameID.getNameQualifier());
            }

            if (nameID.getSPNameQualifier() != null) {
                gen.write(SP_NAME_QUALIFIER_FIELD, nameID.getSPNameQualifier());
            }

            if (nameID.getSPProvidedID() != null) {
                gen.write(SP_PROVIDED_ID_FIELD, nameID.getSPProvidedID());
            }

            gen.writeEnd();
        }
        return sink.toString();
    }
        
    /** {@inheritDoc} */
    public boolean supports(@Nonnull @NotEmpty final String value) {
        return JSON_PATTERN.matcher(value).matches();
    }

    /** {@inheritDoc} */
// Checkstyle: CyclomaticComplexity OFF
    @Nullable public NameIDPrincipal deserialize(@Nonnull @NotEmpty final String value) throws IOException {
        
        try (final JsonReader reader = getJsonReader(new StringReader(value))) {
            
            final JsonStructure st = reader.read();
            if (!(st instanceof JsonObject)) {
                throw new IOException("Found invalid data structure while parsing NameIDPrincipal");
            }
            
            final JsonObject obj = (JsonObject) st;
            JsonString str = obj.getJsonString(PRINCIPAL_NAME_FIELD);
            
            if (str != null && !Strings.isNullOrEmpty(str.getString())) {
                final NameID nameID = nameIDBuilder.buildObject();
                nameID.setValue(str.getString());
                
                str = obj.getJsonString(FORMAT_FIELD);
                if (str != null && !Strings.isNullOrEmpty(str.getString())) {
                    nameID.setFormat(str.getString());
                }

                str = obj.getJsonString(NAME_QUALIFIER_FIELD);
                if (str != null && !Strings.isNullOrEmpty(str.getString())) {
                    nameID.setNameQualifier(str.getString());
                }

                str = obj.getJsonString(SP_NAME_QUALIFIER_FIELD);
                if (str != null && !Strings.isNullOrEmpty(str.getString())) {
                    nameID.setSPNameQualifier(str.getString());
                }

                str = obj.getJsonString(SP_PROVIDED_ID_FIELD);
                if (str != null && !Strings.isNullOrEmpty(str.getString())) {
                    nameID.setSPProvidedID(str.getString());
                }

                return new NameIDPrincipal(nameID);
            }
            log.warn("Skipping NameIDPrincipal missing identifier value");
            
            return null;
        } catch (final JsonException e) {
            throw new IOException("Found invalid data structure while parsing NameIDPrincipal", e);
        }
    }
// Checkstyle: CyclomaticComplexity ON
    
   
}