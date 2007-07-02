/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.opensaml.xml.util.DatatypeHelper;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;

/**
 * The RegexAttributeDefinition allows regular expression based replacements on attribute values, using the regex syntax
 * allowed by {@link java.util.regex.Pattern}.
 */
public class MappedAttributeDefinition extends BaseAttributeDefinition {

    /** Class logger. */
    private static Logger log = Logger.getLogger(MappedAttributeDefinition.class);

    /** Regex string to match the source attribute value with. */
    private String regex;

    /** Regex pattern to match the source attribute value with. */
    private Pattern pattern;

    /** Allow regex to match a substring within the attribute value. */
    private boolean partialMatch;

    /** The replacement string to replace the matched groups in the pattern with. */
    private String replacement;

    /** Perform case-insensitve match. */
    private boolean ignoreCase;

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {
        log.debug("Resolving attribute: (" + getId() + ")");

        Matcher m;
        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId(getId());

        for (Object o : getValuesFromAllDependencies(resolutionContext)) {
            try {
                m = pattern.matcher(o.toString());
                if (partialMatch || m.matches()) {
                    attribute.getValues().add(m.replaceAll(replacement));
                }
            } catch (PatternSyntaxException e) {
                log.debug("RegexAttributeDefinition (" + getId() + ") caught an exception when trying to match value ("
                        + o.toString() + ").  Skipping this value.");
            }
        }

        return attribute;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {
        if (!partialMatch && DatatypeHelper.isEmpty(replacement)) {
            log.error("RegexAttributeDefinition (" + getId()
                    + ") requires either a 'replacement' value or 'partialMatch' set to true.");
            throw new AttributeResolutionException("RegexAttributeDefinition (" + getId()
                    + ") requires either a 'replacement' value or 'partialMatch' set to true.");
        }
    }

    /**
     * Initializes this attribute definition.
     * 
     * @throws AttributeResolutionException if unable to parse regex string
     */
    public void initialize() throws AttributeResolutionException {
        int flags = 0;
        if (ignoreCase) {
            flags = Pattern.CASE_INSENSITIVE;
        }

        try {
            pattern = Pattern.compile(regex, flags);
        } catch (PatternSyntaxException e) {
            log.error("RegexAttributeDefinition (" + getId() + ") contains an invalid regex pattern -- "
                    + e.getMessage());
            throw new AttributeResolutionException("RegexAttributeDefinition (" + getId()
                    + ") contains an invalid regex pattern.", e);
        }
    }

    /**
     * Set whether matching should be case-sensitive.
     * 
     * @param newIgnoreCase whether matching should be case-sensitive
     */
    public void setIgnoreCase(boolean newIgnoreCase) {
        ignoreCase = newIgnoreCase;
    }

    /**
     * Set whether to allow regex to match a substring within the attribute value.
     * 
     * @param newPartialMatch whether to allow regex to match a substring within the attribute value
     */
    public void setPartialMatch(boolean newPartialMatch) {
        partialMatch = newPartialMatch;
    }

    /**
     * Set the regular expression pattern string used for matching attribute values.
     * 
     * @param newRegex new regex string
     */
    public void setRegex(String newRegex) {
        regex = newRegex;
    }

    /**
     * Set the replacement string used to replace matched attribute values.
     * 
     * @param newReplacement new replacement string
     */
    public void setReplacement(String newReplacement) {
        replacement = newReplacement;
    }

}