/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
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

package edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.saml;

import java.util.List;
import java.util.regex.Matcher;

import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObject;

import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.FilterProcessingException;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.ShibbolethFilteringContext;
import edu.internet2.middleware.shibboleth.common.attribute.filtering.provider.match.basic.AbstractMatchFunctor;
import edu.internet2.middleware.shibboleth.common.attribute.provider.ScopedAttributeValue;
import edu.internet2.middleware.shibboleth.common.xmlobject.ShibbolethMetadataScope;

/**
 * A match function that ensures that an attribute's value's scope matches a scope given in metadata for the entity or
 * role.
 */
public class AttributeScopeShibMDMatchFunctor extends AbstractMatchFunctor {

    /** {@inheritDoc} */
    public boolean doEvaluatePermitValue(ShibbolethFilteringContext filterContext, String attributeId,
            Object attributeValue) throws FilterProcessingException {
        if (!(attributeValue instanceof ScopedAttributeValue)) {
            return false;
        }

        ScopedAttributeValue scopedValue = (ScopedAttributeValue) attributeValue;

        EntityDescriptor issuerDescriptor = filterContext.getAttributeRequestContext().getAttributeIssuerMetadata();
        List<XMLObject> extensions = issuerDescriptor.getExtensions().getOrderedChildren();
        if (extensions != null) {
            ShibbolethMetadataScope metadataScope;
            Matcher scopeMatcher;
            for (XMLObject extension : extensions) {
                if (extension instanceof ShibbolethMetadataScope) {
                    metadataScope = (ShibbolethMetadataScope) extension;
                    scopeMatcher = metadataScope.getMatchPattern().matcher(scopedValue.getScope());
                    if (scopeMatcher.matches()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    public boolean doEvaluatePolicyRequirement(ShibbolethFilteringContext filterContext)
            throws FilterProcessingException {
        throw new FilterProcessingException("This match functor is not supported in policy requirements");
    }
}