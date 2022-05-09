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

package net.shibboleth.idp.consent.logic.impl;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.transcoding.AttributeTranscoderRegistry;
import net.shibboleth.utilities.java.support.service.ReloadableService;

/**
 * Function which returns the locale-aware display name of an attribute, defaulting to the
 * attribute ID if the attribute has no display name.
 */
public class AttributeDisplayNameFunction extends AbstractAttributeDisplayFunction {

    /** Logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeDisplayNameFunction.class);

    /**
     * Constructor.
     * 
     * @param request {@link HttpServletRequest} used to get preferred languages
     * @param defaultLangauages list of fallback languages in order of decreasing preference
     * @param transcoderService the attribute transcoder service
     */
    public AttributeDisplayNameFunction(@Nonnull final HttpServletRequest request,
            @Nullable final List<String> defaultLangauages,
            final ReloadableService<AttributeTranscoderRegistry> transcoderService) {
        super(request, defaultLangauages, transcoderService);
    }

    /** {@inheritDoc} */
    @Override @Nonnull protected Map<Locale, String> getDisplayInfo(
            @Nonnull final AttributeTranscoderRegistry registry,
            @Nonnull final IdPAttribute attribute) {
        @SuppressWarnings("removal")
        final Map<Locale, String>  fromAttribute = attribute.getDisplayNames();
        if (fromAttribute != null && !fromAttribute.isEmpty()) {
            log.debug("Attribute {} carried locally defined names, skipping the registry", attribute.getId());
            return fromAttribute;
        }
        return  registry.getDisplayNames(attribute);
    }
}
