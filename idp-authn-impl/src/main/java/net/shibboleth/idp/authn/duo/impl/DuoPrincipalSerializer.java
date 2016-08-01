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

package net.shibboleth.idp.authn.duo.impl;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.Principal;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.stream.JsonGenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import net.shibboleth.idp.authn.duo.DuoPrincipal;
import net.shibboleth.idp.authn.principal.AbstractPrincipalSerializer;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * Principal serializer for {@link DuoPrincipal}.
 */
@ThreadSafe
public class DuoPrincipalSerializer extends AbstractPrincipalSerializer<String> {

    /** Field name of {@link DuoPrincipal}. */
    @Nonnull @NotEmpty private static final String DUO_FIELD = "DUO";

    /** Pattern used to determine if input is supported. */
    @Nonnull private static final Pattern JSON_PATTERN = Pattern.compile("^\\{\"DUO\":.*\\}$");

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DuoPrincipalSerializer.class);

    /** {@inheritDoc} */
    @Override
    public boolean supports(@Nonnull final Principal principal) {
        return principal instanceof DuoPrincipal;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty public String serialize(@Nonnull final Principal principal) throws IOException {
        final StringWriter sink = new StringWriter(32);
        final JsonGenerator gen = getJsonGenerator(sink);
        gen.writeStartObject()
            .write(DUO_FIELD, principal.getName())
            .writeEnd();
        gen.close();
        return sink.toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean supports(@Nonnull @NotEmpty final String value) {
        return JSON_PATTERN.matcher(value).matches();
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public DuoPrincipal deserialize(@Nonnull @NotEmpty final String value) throws IOException {
        final JsonReader reader = getJsonReader(new StringReader(value));
        JsonStructure st = null;
        try {
            st = reader.read();
        } finally {
            reader.close();
        }
        if (!(st instanceof JsonObject)) {
            throw new IOException("Found invalid data structure while parsing DuoPrincipal");
        }
        final JsonString str = ((JsonObject) st).getJsonString(DUO_FIELD);
        if (str != null) {
            final String username = str.getString();
            if (!Strings.isNullOrEmpty(username)) {
                return new DuoPrincipal(username);
            }
        }
        return null;
    }

}
