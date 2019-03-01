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

package net.shibboleth.idp.cas.session.impl;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;

public class CASSPSessionSerializerTest {

    private CASSPSessionSerializer serializer = new CASSPSessionSerializer(Duration.ZERO);

    @Test
    public void testSerializeDeserialize() throws Exception{
        final Instant exp = Instant.ofEpochMilli(1410539474000000000L);
        final CASSPSession original = new CASSPSession(
                "https://foo.example.com/shibboleth",
                Instant.ofEpochMilli(1410532279838046000L),
                exp,
                "ST-1234126-ABC1346DEADBEEF");
        final String serialized = serializer.serialize(original);
        final CASSPSession deserialized =
                (CASSPSession) serializer.deserialize(1, "context", "key", serialized, exp.toEpochMilli());
        assertEquals(deserialized.getId(), original.getId());
        assertEquals(deserialized.getCreationInstant(), original.getCreationInstant());
        assertEquals(deserialized.getExpirationInstant(), original.getExpirationInstant());
        assertEquals(deserialized.getTicketId(), original.getTicketId());
    }

}