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

package net.shibboleth.idp.attribute.transcoding;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.logic.Constraint;
import net.shibboleth.utilities.java.support.logic.ConstraintViolationException;

/**
 * Support functions for working with {@link AttributeTranscoder} framework.
 */
public final class TranscoderSupport {

    /** Constructor. */
    private TranscoderSupport() {
        
    }

    /**
     * Pull an {@link AttributeTranscoder} object out of the rule provided.
     * 
     * @param <T> type of supported target object
     * @param rule transcoding rule
     * 
     * @return an {@link AttributeTranscoder}
     * 
     * @throws ConstraintViolationException if a transcoder cannot be obtained
     */
    @Nonnull public static <T> AttributeTranscoder<T> getTranscoder(@Nonnull final TranscodingRule rule)
            throws ConstraintViolationException {
        Constraint.isNotNull(rule, "Transcoding rule cannot be null");
        
        final AttributeTranscoder<T> transcoder =
                rule.get(AttributeTranscoderRegistry.PROP_TRANSCODER, AttributeTranscoder.class);
        Constraint.isNotNull(transcoder, "AttributeTranscoder not found in properties");
        return transcoder;
    }

}