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

package net.shibboleth.idp.session;

import java.security.SecureRandom;

import edu.vt.middleware.crypt.util.HexConverter;

/**
 * A basic {@link SessionFactory} that generates a the session ID and session secret using a {@link SecureRandom} PRNG.
 */
public class BasicSessionFactory implements SessionFactory {

    /** Number of random bits within a session ID. Default value: {@value} */
    private final int sessionIDSize = 32;

    /** A {@link SecureRandom} PRNG to generate session IDs. */
    private final SecureRandom prng;

    /** Converts byte to hex and vice versa. */
    private final HexConverter hexCodec;

    /** Constructor. */
    public BasicSessionFactory() {
        prng = new SecureRandom();
        hexCodec = new HexConverter();
    }

    /** {@inheritDoc} */
    public IdPSession buildSession() {
        // generate a random session ID
        byte[] sid = new byte[sessionIDSize];
        prng.nextBytes(sid);
        final String sessionId = hexCodec.fromBytes(sid);

        // generate a random secret
        final byte[] sessionSecret = new byte[16];
        prng.nextBytes(sessionSecret);

        IdPSession session = new IdPSession();
        session.setId(sessionId);
        session.setSecret(sessionSecret);

        return session;
    }
}