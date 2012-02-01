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

package net.shibboleth.idp.attribute.resolver.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.shibboleth.utilities.java.support.logic.Assert;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

import com.google.common.base.Optional;

/**
 * A {@link ValueMapping} function that checks if an input contains matches a given {@link Pattern} and, if so, returns
 * a computed return value. The return value is computed by populating a given return value template with matching
 * groups from the {@link Pattern} via {@link Matcher#replaceAll(String)}.
 */
public class RegexValueMapping implements ValueMapping {

    /** Pattern against which the given input is evaluated. */
    private final Pattern target;

    /** Result value pattern to be populated with matching input groups. */
    private final String resultPattern;

    /**
     * Constructor.
     * 
     * @param targetPattern pattern against which the function input is evaluated
     * @param returnValuePattern value pattern to be populated with matching input groups to generate the function
     *            result
     */
    public RegexValueMapping(final Pattern targetPattern, String returnValuePattern) {
        target = Assert.isNotNull(targetPattern, "Target pattern can not be null or empty");
        resultPattern =
                Assert.isNotNull(StringSupport.trimOrNull(returnValuePattern), "Return value can not be null or empty");
    }

    /** {@inheritDoc} */
    public Optional<String> apply(String input) {
        Matcher matcher = target.matcher(input);
        if (matcher.matches()) {
            return Optional.of(matcher.replaceAll(resultPattern));
        }

        return Optional.absent();
    }
}