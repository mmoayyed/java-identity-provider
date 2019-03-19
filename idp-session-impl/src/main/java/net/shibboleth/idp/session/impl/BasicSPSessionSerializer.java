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

package net.shibboleth.idp.session.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;

import net.shibboleth.idp.session.AbstractSPSessionSerializer;
import net.shibboleth.idp.session.BasicSPSession;
import net.shibboleth.idp.session.SPSession;
import net.shibboleth.utilities.java.support.annotation.ParameterName;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * A serializer for {@link BasicSPSession} objects.
 */
@ThreadSafe
public class BasicSPSessionSerializer extends AbstractSPSessionSerializer {
    
    /**
     * Constructor.
     * 
     * @param offset time to subtract from record expiration to establish session expiration value
     */
    public BasicSPSessionSerializer(@Nonnull @ParameterName(name="offset") final Duration offset) {
        super(offset);
    }

    /** {@inheritDoc} */
    @Override 
    @Nonnull protected SPSession doDeserialize(@Nonnull final JsonObject obj,
            @Nonnull @NotEmpty final String id, @Nonnull final Instant creation,
            @Nonnull final Instant expiration) throws IOException {
        
        return new BasicSPSession(id, creation, expiration);
    }
    
}