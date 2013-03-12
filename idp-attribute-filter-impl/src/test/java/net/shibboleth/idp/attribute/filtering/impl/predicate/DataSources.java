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

package net.shibboleth.idp.attribute.filtering.impl.predicate;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.AttributeValue;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;

/**
 * Strings and such used for testing.
 */
public class DataSources {

    protected final static String TEST_STRING = "nibbleahappywarthog";

    protected final static String TEST_STRING_UPPER = TEST_STRING.toUpperCase();

    protected final static String NON_MATCH_STRING = "ThisIsADifferentString";

    protected final static String TEST_REGEX = "^n.*g";

    protected final static StringAttributeValue STRING_VALUE = new StringAttributeValue(TEST_STRING);

    protected final static StringAttributeValue NON_MATCH_STRING_VALUE = new StringAttributeValue(NON_MATCH_STRING);

    protected final static ScopedStringAttributeValue SCOPED_VALUE_VALUE_MATCH = new ScopedStringAttributeValue(
            TEST_STRING, NON_MATCH_STRING);

    protected final static ScopedStringAttributeValue SCOPED_VALUE_SCOPE_MATCH = new ScopedStringAttributeValue(
            NON_MATCH_STRING, TEST_STRING);

    protected final static ByteAttributeValue BYTE_ATTRIBUTE_VALUE = new ByteAttributeValue(TEST_STRING.getBytes());

    protected final static AttributeValue OTHER_VALUE = new AttributeValue() {

        @Nonnull public Object getValue() {
            return TEST_STRING;
        }
    };

}
