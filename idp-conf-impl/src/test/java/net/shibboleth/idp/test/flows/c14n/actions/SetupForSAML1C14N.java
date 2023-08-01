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

package net.shibboleth.idp.test.flows.c14n.actions;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.action.ActionSupport;
import org.opensaml.profile.action.EventIds;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.NameIdentifier;

import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.IdPAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.attribute.context.AttributeContext;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.profile.AbstractProfileAction;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.idp.saml.nameid.impl.TransientSAML1NameIdentifierGenerator;
import net.shibboleth.idp.saml.xml.SAMLConstants;
import net.shibboleth.profile.context.RelyingPartyContext;
import net.shibboleth.profile.relyingparty.RelyingPartyConfiguration;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Action to mock up a Subject for C14N.
 */
@SuppressWarnings("javadoc")
public class SetupForSAML1C14N extends AbstractProfileAction {
    
    private String attributeName;

    private TransientSAML1NameIdentifierGenerator generator;
    
    public void setAttributeName(@Nullable final String name) {
        attributeName = name;
    }

    public void setGenerator(@Nullable final TransientSAML1NameIdentifierGenerator gen) {
        generator = gen;
    }
    
    private NameIdentifier encode(IdPAttribute attribute) {
        final Collection<IdPAttributeValue> attributeValues = attribute.getValues();
        if (attributeValues == null || attributeValues.isEmpty()) {
            return null;
        }

        final SAMLObjectBuilder<NameIdentifier> identifierBuilder = (SAMLObjectBuilder<NameIdentifier>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameIdentifier>ensureBuilder(
                        NameIdentifier.DEFAULT_ELEMENT_NAME);

        final NameIdentifier nameId = identifierBuilder.buildObject();
        nameId.setFormat(NameIdentifier.UNSPECIFIED);
        for (final IdPAttributeValue attrValue : attributeValues) {
            if (attrValue == null) {
                continue;
            }
            if (attrValue instanceof StringAttributeValue) {
                final String value = ((StringAttributeValue)attrValue).getValue();
                // Check for empty or all-whitespace, but don't trim.
                if (StringSupport.trimOrNull(value) == null) {
                    continue;
                }
                nameId.setValue(value);
                return nameId;
            }
            continue;
        }
        return null;
    }

    @Override protected void doExecute(@Nonnull final ProfileRequestContext profileRequestContext) {

        final RelyingPartyContext rpc = profileRequestContext.getSubcontext(RelyingPartyContext.class);
        assert rpc!=null;
        
        NameIdentifier nid = null;
        
        if (generator != null) {
            try {
                nid = generator.generate(profileRequestContext, SAMLConstants.SAML1_NAMEID_TRANSIENT);
            } catch (final SAMLException e) {
                
            }
        } else {
            final AttributeContext ac = rpc.getSubcontext(AttributeContext.class);
            assert ac != null;
            nid = encode(ac.getIdPAttributes().get(attributeName));
        }
        
        if (nid == null) {
            ActionSupport.buildEvent(profileRequestContext, EventIds.INVALID_PROFILE_CTX);
            return;
        }

        final NameIdentifierPrincipal nidp = new NameIdentifierPrincipal(nid);
        final Subject sub = new Subject();
        sub.getPrincipals().add(nidp);

        final SubjectCanonicalizationContext scc =
                profileRequestContext.ensureSubcontext(SubjectCanonicalizationContext.class);
        scc.setSubject(sub);
        scc.setRequesterId(rpc.getRelyingPartyId());
        final RelyingPartyConfiguration rpConfig = (RelyingPartyConfiguration) rpc.getConfiguration();
        assert rpConfig!=null;
        scc.setResponderId(rpConfig.getIssuer(profileRequestContext));
    }
    
}
