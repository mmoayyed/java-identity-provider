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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.security.auth.Subject;

import org.opensaml.core.testing.OpenSAMLInitBaseTestCase;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.testing.ActionTestingSupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.saml1.core.NameIdentifier;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import net.shibboleth.idp.authn.AuthnEventIds;
import net.shibboleth.idp.authn.context.SubjectCanonicalizationContext;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.saml.authn.principal.NameIdentifierPrincipal;
import net.shibboleth.idp.saml.nameid.NameIDCanonicalizationFlowDescriptor;
import net.shibboleth.idp.saml.nameid.NameIdentifierDecoder;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.collection.CollectionSupport;


/** {@link NameIdentifierCanonicalization} unit test. */
@SuppressWarnings("javadoc")
public class NameIdentifierCanonicalizationTest extends OpenSAMLInitBaseTestCase {

    private ProfileRequestContext prc;
    
    private NameIDCanonicalizationFlowDescriptor flowDescriptor;

    private NameIdentifierCanonicalization action;

    private SAMLObjectBuilder<NameIdentifier> builder;

    @Nonnull private static final String REQUESTER = "TestRequest";

    @Nonnull private static final String RESPONDER = "TestResp";

    @Nonnull private static final String VALUE_PREFIX = "TestPrefix";

    @Nonnull @NotEmpty private static final List<String> formats = CollectionSupport.arrayAsList(NameIdentifier.X509_SUBJECT, NameIdentifier.EMAIL, null);

    @BeforeClass public void initialize() {
        builder = (SAMLObjectBuilder<NameIdentifier>)
                XMLObjectProviderRegistrySupport.getBuilderFactory().<NameIdentifier>ensureBuilder(
                        NameIdentifier.DEFAULT_ELEMENT_NAME);
    }

    @BeforeMethod public void setUp() throws Exception {
        prc = new ProfileRequestContext();
        
        flowDescriptor = new NameIDCanonicalizationFlowDescriptor();
        flowDescriptor.setId("C14NDesc");
        flowDescriptor.setFormats(formats);
        flowDescriptor.initialize();
        
        action = new NameIdentifierCanonicalization();
        action.setDecoder(new NameIdentifierDecoder() {
            public String decode(@Nonnull SubjectCanonicalizationContext scc, @Nonnull NameIdentifier nameIdentifier) {
                if (RESPONDER.equals(scc.getResponderId()) && REQUESTER.equals(scc.getRequesterId())) {
                    return VALUE_PREFIX + nameIdentifier.getValue();
                }
                return null;
            }
        });
        action.initialize();
    }

    private void setSubContext(@Nullable Subject subject, @Nullable String responder, @Nullable String requester) {
        final SubjectCanonicalizationContext scc = prc.ensureSubcontext(SubjectCanonicalizationContext.class);
        if (subject != null) {
            scc.setSubject(subject);
        }
        if (requester != null) {
            scc.setRequesterId(requester);
        }
        if (responder != null) {
            scc.setResponderId(responder);
        }
        scc.setAttemptedFlow(flowDescriptor);
    }

    @Nonnull private NameIdentifier nameId(String value, String format, String nameQualifier) {

        final NameIdentifier id = builder.buildObject();

        id.setValue(value);
        id.setFormat(format);
        id.setNameQualifier(nameQualifier);
        return id;
    }
    
    @Nonnull private NameIdentifier nameId(String value, String format) {

        return nameId(value, format, RESPONDER);
    }

    @Test public void testNoContext() {
        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT_C14N_CTX);
    }

    @Test public void testNoPrincipal() {
        final Subject subject = new Subject();
        setSubContext(subject, null, null);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null && scc.getException()!=null;
    }

    @Test public void testMultiPrincipals() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("value", NameIdentifier.WIN_DOMAIN_QUALIFIED)));
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("value2", NameIdentifier.X509_SUBJECT)));

        setSubContext(subject, null, null);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null && scc.getException()!=null;
    }

    @Test public void testWrongFormat() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("value", NameIdentifier.WIN_DOMAIN_QUALIFIED)));

        setSubContext(subject, RESPONDER, REQUESTER);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
        final SubjectCanonicalizationContext scc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert scc!=null && scc.getException()!=null;
    }

    @Test public void testWrongRequester() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("value", NameIdentifier.EMAIL)));
        setSubContext(subject, RESPONDER, RESPONDER);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }

    @Test public void testWrongResponderNameId() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("value", NameIdentifier.EMAIL)));
        setSubContext(subject, REQUESTER, REQUESTER);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }
    
    @Test public void testWrongResponder() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("value", NameIdentifier.EMAIL, REQUESTER)));
        setSubContext(subject, REQUESTER, REQUESTER);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertEvent(prc, AuthnEventIds.INVALID_SUBJECT);
    }


    @Test public void testSuccess() {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UsernamePrincipal("foo@osu.edu"));
        subject.getPrincipals().add(new NameIdentifierPrincipal(nameId("works", NameIdentifier.EMAIL)));
        setSubContext(subject, RESPONDER, REQUESTER);

        assert prc!=null;
        action.execute(prc);

        assert prc!=null;
        ActionTestingSupport.assertProceedEvent(prc);
        SubjectCanonicalizationContext sc = prc.getSubcontext(SubjectCanonicalizationContext.class);
        assert sc!=null;
        Assert.assertEquals(sc.getPrincipalName(), VALUE_PREFIX+"works");
    }

}