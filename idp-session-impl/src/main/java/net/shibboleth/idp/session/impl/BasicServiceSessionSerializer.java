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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonObject;

import net.shibboleth.idp.session.AbstractServiceSessionSerializer;
import net.shibboleth.idp.session.BasicServiceSession;
import net.shibboleth.idp.session.ServiceSession;
import net.shibboleth.utilities.java.support.annotation.Duration;
import net.shibboleth.utilities.java.support.annotation.constraint.NonNegative;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;

/**
 * A serializer for {@link BasicServiceSession} objects.
 */
@ThreadSafe
public class BasicServiceSessionSerializer extends AbstractServiceSessionSerializer {
    
    /**
     * Constructor.
     * 
     * @param offset milliseconds to substract from record expiration to establish session expiration value
     */
    public BasicServiceSessionSerializer(@Duration @NonNegative final long offset) {
        super(offset);
    }

    /** {@inheritDoc} */
    @Nonnull protected ServiceSession doDeserialize(@Nonnull final JsonObject obj,
            @Nonnull @NotEmpty final String id, @Nonnull @NotEmpty final String flowId,
            final long creation, final long expiration) throws IOException {
        
        return new BasicServiceSession(id, flowId, creation, expiration);
    }
    
}