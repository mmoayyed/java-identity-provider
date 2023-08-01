/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.cas.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Default comparator implementation for comparing CAS service URLs. URL comparison is case-insensitive and supports
 * ignoring predefined URL path parameters. The common session marker <em>;jessionid=value</em> is ignored by default.
 *
 * @author Marvin S. Addison
 */
public class DefaultServiceComparator implements Comparator<String> {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(DefaultServiceComparator.class);

    /** Ignored patterns in path part of URL. */
    @Nonnull private final Pattern[] ignoredPatterns;

    /** Creates a new instance that ignores <em>;jsessionid=value</em>. */
    public DefaultServiceComparator() {
        this("jsessionid");
    }

    /**
     * Creates a new instance that ignores the given path parameter names (and any associated values).
     *
     * @param  parameterNames  List of path parameter names to ignore.
     */
    public DefaultServiceComparator(@Nonnull final String ... parameterNames) {
        Constraint.isNotNull(parameterNames, "Parameters names cannot be null");
        ignoredPatterns = new Pattern[parameterNames.length];
        for (int i = 0; i < parameterNames.length; i++) {
            ignoredPatterns[i] = Pattern.compile(";" + parameterNames[i] + "(?:=[^;/]+)?", Pattern.CASE_INSENSITIVE);
        }
    }

    /** {@inheritDoc} */
    public int compare(final String a, final String b) {
        if (a == null || b == null) {
            if (a == null && b == null) {
                return 0;
            } else if (a == null) {
                return -1;
            } else {
                return 1;
            }
        }
        return stripPathParameters(a).compareToIgnoreCase(stripPathParameters(b));
    }

    /**
     * Strips any of the named path parameters (and any associated values) from the given URI.
     *
     * @param uriString String form of URI from which to strip named path parameters.
     *
     * @return URI with named path parameters and any associated values removed.
     */
    private String stripPathParameters(@Nonnull final String uriString) {
        try {
            final URI uri = new URI(uriString);
            String path = uri.getPath();
            for (final Pattern pattern : ignoredPatterns) {
                final Matcher m = pattern.matcher(path);
                path = m.replaceAll("");
            }
            return new URI(uri.getScheme(), uri.getAuthority(), path, uri.getQuery(), uri.getFragment()).toString();
        } catch (final URISyntaxException e) {
            log.warn("Error parsing {}", uriString);
            return uriString;
        }
    }

}