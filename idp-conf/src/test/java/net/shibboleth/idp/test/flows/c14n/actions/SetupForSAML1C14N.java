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

package net.shibboleth.idp.test.flows.c14n.actions;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.security.auth.Subject;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.NameIdentifier;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.profile.context.RelyingPartyContext;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * Action to mock up a Subject for C14N.
 */
public class SetupForSAML1C14N extends AbstractProfileAction {
    
    private String attributeName;

    /**
     * @return Returns the attributeName.
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * @param attributeName The attributeName to set.
     */
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    private NameIdentifier encode(IdPAttribute attribute) {
        final Collection<IdPAttributeValue<?>> attributeValues = attribute.getValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            return null;
        }

        final SAMLObjectBuilder<NameIdentifier> identifierBuilder =  (SAMLObjectBuilder<NameIdentifier>) XMLObjectProviderRegistrySupport.getBuilderFactory().getBuilder(
                NameIdentifier.DEFAULT_ELEMENT_NAME);

        final NameIdentifier nameId = identifierBuilder.buildObject();
        final String format;
        if ("Principal".equals(attributeName)) {
            format="urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
        } else {
            format="urn:mace:shibboleth:1.0:nameIdentifier";
        }
        nameId.setFormat(format);
        for (final IdPAttributeValue attrValue : attributeValues) {
            if (attrValue == null || attrValue.getValue() == null) {
                continue;
            }
            final Object value = attrValue.getValue();
            if (value instanceof String) {
                // Check for empty or all-whitespace, but don't trim.
                if (StringSupport.trimOrNull((String) value) == null) {
                    continue;
                }
                nameId.setValue((String) value);
                return nameId;
            } else {
                continue;
            }
        }
        return null;
    }

    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpc = profileRequestContext.getSubcontext(RelyingPartyContext.class);

        final AttributeContext ac = rpc.getSubcontext(AttributeContext.class, false);
        final NameIdentifier nid = encode(ac.getIdPAttributes().get(getAttributeName()));
        if (nid == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }

        final NameIdentifierPrincipal nidp = new NameIdentifierPrincipal(nid);
        final Subject sub = new Subject();
        sub.getPrincipals().add(nidp);

        final SubjectCanonicalizationContext scc =
                profileRequestContext.getSubcontext(SubjectCanonicalizationContext.class, true);
        scc.setSubject(sub);
        scc.setRequesterId(rpc.getRelyingPartyId());
        scc.setResponderId(rpc.getConfiguration().getResponderId());
    }
}
