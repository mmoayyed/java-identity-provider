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

package net.shibboleth.idp.authn.impl;

import static org.testng.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import javax.security.auth.Subject;

import net.shibboleth.ext.spring.resource.ResourceHelper;
import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.authn.AuthenticationFlowDescriptor;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.idp.authn.principal.GenericPrincipalSerializer;
import net.shibboleth.idp.authn.principal.GenericPrincipalService;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.PasswordPrincipal;
import net.shibboleth.idp.authn.principal.PrincipalServiceManager;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.authn.principal.SealedPrincipalSerializer;
import net.shibboleth.idp.authn.principal.SimplePrincipalSerializer;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.IdPAttributePrincipalSerializer;
import net.shibboleth.idp.authn.principal.impl.LDAPPrincipalSerializer;
import net.shibboleth.idp.authn.principal.impl.ProxyAuthenticationPrincipalSerializer;
import net.shibboleth.idp.authn.testing.TestPrincipal;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.security.DataSealer;
import net.shibboleth.utilities.java.support.security.impl.BasicKeystoreKeyStrategy;

import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.SortBehavior;
import org.ldaptive.jaas.LdapPrincipal;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.profile.testing.RequestContextBuilder;
import org.opensaml.security.x509.X509Support;
import org.springframework.core.io.ClassPathResource;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;

/** {@link DefaultAuthenticationResultSerializer} unit test. */
public class DefaultAuthenticationResultSerializerTest {

    private static final String DATAPATH = "/net/shibboleth/idp/authn/impl/";
    
    private static final String CONTEXT = "_context";
    
    private static final String KEY = "_key";
    
    private static final Instant INSTANT = Instant.ofEpochMilli(1378827849463L);
    
    private static final long ACTIVITY = 1378827556778L;

    private static final String entityCertBase64 = 
            "MIIDjDCCAnSgAwIBAgIBKjANBgkqhkiG9w0BAQUFADAtMRIwEAYDVQQKEwlJbnRl" +
            "cm5ldDIxFzAVBgNVBAMTDmNhLmV4YW1wbGUub3JnMB4XDTA3MDQwOTA2MTIwOVoX" +
            "DTE3MDQwNjA2MTIwOVowMTESMBAGA1UEChMJSW50ZXJuZXQyMRswGQYDVQQDExJm" +
            "b29iYXIuZXhhbXBsZS5vcmcwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIB" +
            "AQDNWnkFmhy1vYa6gN/xBRKkZxFy3sUq2V0LsYb6Q3pe9Qlb6+BzaM5DrN8uIqqr" +
            "oBE3Wp0LtrgKuQTpDpNFBdS2p5afiUtOYLWBDtizTOzs3Z36MGMjIPUYQ4s03IP3" +
            "yPh2ud6EKpDPiYqzNbkRaiIwmYSit5r+RMYvd6fuKvTOn6h7PZI5AD7Rda7VWh5O" +
            "VSoZXlRx3qxFho+mZhW0q4fUfTi5lWwf4EhkfBlzgw/k5gf4cOi6rrGpRS1zxmbt" +
            "X1RAg+I20z6d04g0N2WsK5stszgYKoIROJCiXwjraa8/SoFcILolWQpttVHBIUYl" +
            "yDlm8mIFleZf4ReFpfm+nUYxAgMBAAGjgbIwga8wCQYDVR0TBAIwADAsBglghkgB" +
            "hvhCAQ0EHxYdT3BlblNTTCBHZW5lcmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYE" +
            "FDgRgTkjaKoK6DoZfUZ4g9LDJUWuMFUGA1UdIwROMEyAFNXuZVPeUdqHrULqQW7y" +
            "r9buRpQLoTGkLzAtMRIwEAYDVQQKEwlJbnRlcm5ldDIxFzAVBgNVBAMTDmNhLmV4" +
            "YW1wbGUub3JnggEBMA0GCSqGSIb3DQEBBQUAA4IBAQCPj3Si4Eiw9abNgPBUhBXW" +
            "d6eRYlIHaHcnez6j6g7foAOyuVIUso9Q5c6pvL87lmasK55l09YPXw1qmiH+bHMc" +
            "rwEPODpLx7xd3snlOCi7FyxahxwSs8yfTu8Pq95rWt0LNcfHxQK938Cpnav6jgDo" +
            "2uH/ywAOFFSnoBzGHAfScHMfj8asZ6THosYsklII7FSU8j49GV2utkvGB3mcu4ST" +
            "uLdeRCZmi93vq1D4JVGsXC4UaHjg114+a+9q0XZdz6a1UW4pt1ryXIPotCS62M71" +
            "pkJf5neHUinKAqgoRfPXowudZg1Zl8DjzoOBn+MNHRrR5KYbVGvdHcxoJLCwVB/v";

    private PrincipalServiceManager manager;
    
    private DefaultAuthenticationResultSerializer serializer;
    
    private AuthenticationFlowDescriptor flowDescriptor;
    
    @BeforeMethod public void setUp() throws ComponentInitializationException, NoSuchMethodException, SecurityException {

        final SimplePrincipalSerializer<UsernamePrincipal> upSerializer =
                new SimplePrincipalSerializer<>(UsernamePrincipal.class, "U");
        upSerializer.initialize();
        final GenericPrincipalService<UsernamePrincipal> upService =
                new GenericPrincipalService<>(UsernamePrincipal.class, upSerializer);
        upService.setId("username");
        upService.initialize();

        final LDAPPrincipalSerializer lpSerializer = new LDAPPrincipalSerializer();
        lpSerializer.initialize();
        final GenericPrincipalService<LdapPrincipal> lpService =
                new GenericPrincipalService<>(LdapPrincipal.class, lpSerializer);
        lpService.setId("ldap");
        lpService.initialize();

        final IdPAttributePrincipalSerializer attrSerializer = new IdPAttributePrincipalSerializer();
        attrSerializer.initialize();
        final GenericPrincipalService<IdPAttributePrincipal> attrService =
                new GenericPrincipalService<>(IdPAttributePrincipal.class, attrSerializer);
        attrService.setId("attr");
        attrService.initialize();

        final ProxyAuthenticationPrincipalSerializer proxySerializer = new ProxyAuthenticationPrincipalSerializer();
        proxySerializer.initialize();
        final GenericPrincipalService<ProxyAuthenticationPrincipal> proxyService =
                new GenericPrincipalService<>(ProxyAuthenticationPrincipal.class, proxySerializer);
        proxyService.setId("proxy");
        proxyService.initialize();
        
        final ClassPathResource keystoreResource = new ClassPathResource("/net/shibboleth/idp/authn/impl/SealerKeyStore.jks");
        final ClassPathResource versionResource = new ClassPathResource("/net/shibboleth/idp/authn/impl/SealerKeyStore.kver");

        final BasicKeystoreKeyStrategy strategy = new BasicKeystoreKeyStrategy();
        strategy.setKeyAlias("secret");
        strategy.setKeyPassword("kpassword");
        strategy.setKeystorePassword("password");
        strategy.setKeystoreResource(ResourceHelper.of(keystoreResource));
        strategy.setKeyVersionResource(ResourceHelper.of(versionResource));

        final DataSealer sealer = new DataSealer();
        sealer.setKeyStrategy(strategy);

        try {
            strategy.initialize();
            sealer.initialize();
        } catch (ComponentInitializationException e) {
            fail(e.getMessage());
        }

        final SealedPrincipalSerializer<PasswordPrincipal> pwSerializer =
                new SealedPrincipalSerializer<>(PasswordPrincipal.class, "PW");
        pwSerializer.setDataSealer(sealer);
        pwSerializer.initialize();
        final GenericPrincipalService<PasswordPrincipal> pwService =
                new GenericPrincipalService<>(PasswordPrincipal.class, pwSerializer);
        pwService.setId("password");
        pwService.initialize();
        
        manager = new PrincipalServiceManager(List.of(upService, pwService, lpService, attrService, proxyService));
        
        final GenericPrincipalSerializer generic = new GenericPrincipalSerializer();
        generic.initialize();
        serializer = new DefaultAuthenticationResultSerializer(manager, generic);
        flowDescriptor = new AuthenticationFlowDescriptor();
        flowDescriptor.setId("test");
        flowDescriptor.setResultSerializer(serializer);
        flowDescriptor.setReuseCondition(Predicates.alwaysTrue());
    }

    @Test public void testInvalid() throws Exception {
        serializer.initialize();
        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "invalid.json"), ACTIVITY);
            fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "noFlowId.json"), ACTIVITY);
            fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "noInstant.json"), ACTIVITY);
            fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "invalidAdditional.json"), ACTIVITY);
            fail();
        } catch (IOException e) {
            
        }
    }
    
    @Test public void testSimple() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        result.getAdditionalData().put("foo", "bar");
        result.getAdditionalData().put("frobnitz", "zorkmid");
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        
        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));
        
        flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "simpleAuthenticationResult.json");
        // assertEquals(s, s2); 
        
        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());
        
        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertEquals(result.getAdditionalData(), result2.getAdditionalData());
        assertTrue(result2.getReuseCondition().test(prc));
    }

    @Test public void testComplex() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        result.getSubject().getPrincipals().add(new TestPrincipal("foo"));
        result.getSubject().getPrincipals().add(new TestPrincipal("bar"));
        
        final String s = flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "complexAuthenticationResult.json");
        assertEquals(s, s2);

        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));

        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());

        
        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertTrue(result2.getReuseCondition().test(prc));
    }

    @Test public void testCreds() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        result.getSubject().getPublicCredentials().add(X509Support.decodeCertificate(entityCertBase64));
        result.getSubject().getPrivateCredentials().add(new PasswordPrincipal("bar"));

        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));

        final String s = flowDescriptor.serialize(result);
                
        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());
        
        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertTrue(result2.getReuseCondition().test(prc));
    }

    @Test public void testSymbolic() throws Exception {
        final GenericPrincipalSerializer generic = new GenericPrincipalSerializer();
        generic.setSymbolics(Collections.singletonMap(TestPrincipal.class.getName(), 1));
        generic.initialize();
        serializer = new DefaultAuthenticationResultSerializer(manager, generic);
        serializer.initialize();
        flowDescriptor.setResultSerializer(serializer);
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        result.getSubject().getPrincipals().add(new TestPrincipal("foo"));
        result.getSubject().getPrincipals().add(new TestPrincipal("bar"));
        
        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));

        final String s = flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "symbolicAuthenticationResult.json");
        assertEquals(s, s2);
        
        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());

        
        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertTrue(result2.getReuseCondition().test(prc));
    }
    

    @Test public void testLdap() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        final LdapEntry entry = new LdapEntry(SortBehavior.SORTED);
        entry.setDn("uid=1234,ou=people,dc=shibboleth,dc=net");
        final LdapAttribute givenName = new LdapAttribute(SortBehavior.SORTED);
        givenName.setName("givenName");
        givenName.addStringValue("Bob", "Robert");
        entry.addAttribute(
                new LdapAttribute("cn", "Bob Cobb"),
                givenName,
                new LdapAttribute("sn", "Cobb"),
                new LdapAttribute("mail", "bob@shibboleth.net"));
        result.getSubject().getPrincipals().add(new LdapPrincipal("bob", entry));

        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));

        final String s = flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "LDAPAuthenticationResult.json");
        assertEquals(s, s2);

        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());

        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertEquals(
                ((LdapPrincipal) result.getSubject().getPrincipals().iterator().next()).getLdapEntry(),
                ((LdapPrincipal) result2.getSubject().getPrincipals().iterator().next()).getLdapEntry());
        assertTrue(result2.getReuseCondition().test(prc));
    }

    @Test public void testIdPAttribute() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        final IdPAttributePrincipal prin = new IdPAttributePrincipal(new IdPAttribute("foo"));
        prin.getAttribute().setValues(List.of(new StringAttributeValue("bar"),
                new ScopedStringAttributeValue("bar2", "scope"), EmptyAttributeValue.ZERO_LENGTH,
                new ByteAttributeValue("foo".getBytes())));
        
        result.getSubject().getPrincipals().add(prin);

        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));
        
        final String s = flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "IdPAttributeAuthenticationResult.json");
        assertEquals(s, s2);

        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());
                

        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertTrue(result2.getReuseCondition().test(prc));
        
        final IdPAttribute attribute =
                ((IdPAttributePrincipal) result2.getSubject().getPrincipals().iterator().next()).getAttribute();
        assertEquals(attribute.getValues().size(), 3);
        assertEquals(((StringAttributeValue) attribute.getValues().get(0)).getValue(), "bar");
        assertEquals(((StringAttributeValue)attribute.getValues().get(1)).getValue(), "bar2");
        assertEquals(((ScopedStringAttributeValue) attribute.getValues().get(1)).getScope(), "scope");
        assertEquals(attribute.getValues().get(2), EmptyAttributeValue.ZERO_LENGTH);
    }

    @Test public void testProxyAuthentication() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        final ProxyAuthenticationPrincipal prin = new ProxyAuthenticationPrincipal(List.of("foo","bar","baz"));
        prin.setProxyCount(10);
        prin.getAudiences().add("zorkmid");
        result.getSubject().getPrincipals().add(prin);

        final ProfileRequestContext prc = getProfileRequestContext(Collections.singletonList(flowDescriptor));
        assertTrue(result.getReuseCondition().test(prc));

        final String s = flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "ProxyAuthenticationResult.json");
        assertEquals(s, s2);

        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());
                

        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertTrue(result2.getReuseCondition().test(prc));
        
        final ProxyAuthenticationPrincipal prin2 = (ProxyAuthenticationPrincipal)result2.getSubject().getPrincipals().iterator().next(); 
        assertEquals(prin.getAuthorities(), prin2.getAuthorities());
        assertEquals(prin.getProxyCount(), prin2.getProxyCount());
        assertEquals(prin.getAudiences(), prin2.getAudiences());
        assertTrue(result2.getReuseCondition().test(prc));
    }

    @Test public void testNestedAuthenticationResult() throws Exception {
        serializer.initialize();
        flowDescriptor.initialize();
        
        final AuthenticationResult result = createResult(flowDescriptor, new Subject());
        
        final AuthenticationFlowDescriptor nestedDescriptor = new AuthenticationFlowDescriptor();
        nestedDescriptor.setId("nested");
        nestedDescriptor.setResultSerializer(serializer);
        nestedDescriptor.initialize();
        
        final AuthenticationResult nested = createResult(nestedDescriptor, new Subject());
        nested.setLastActivityInstant(INSTANT);
        nested.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        
        result.getSubject().getPrincipals().add(new AuthenticationResultPrincipal(nested));

        final ProfileRequestContext prc = getProfileRequestContext(List.of(flowDescriptor, nestedDescriptor));
        assertTrue(result.getReuseCondition().test(prc));

        final String s = flowDescriptor.serialize(result);
        final String s2 = fileToString(DATAPATH + "NestedAuthenticationResult.json");
        assertEquals(s, s2);

        final AuthenticationResult result2 = flowDescriptor.deserialize(1, CONTEXT, KEY, s2,
                Instant.ofEpochMilli(ACTIVITY)
                    .plus(flowDescriptor.getInactivityTimeout())
                    .plus(AuthenticationFlowDescriptor.STORAGE_EXPIRATION_OFFSET)
                    .toEpochMilli());

        assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        assertEquals(result.getSubject(), result2.getSubject());
        assertTrue(result2.getReuseCondition().test(prc));
        
        final AuthenticationResult nested2 =
                ((AuthenticationResultPrincipal) result2.getSubject().getPrincipals().iterator().next()).getAuthenticationResult();
        assertEquals(nested.getAuthenticationFlowId(), nested2.getAuthenticationFlowId());
        assertEquals(nested.getAuthenticationInstant(), nested2.getAuthenticationInstant());
        assertEquals(nested.getLastActivityInstant(), nested2.getLastActivityInstant());
        assertEquals(nested.getSubject(), nested2.getSubject());
        assertTrue(nested2.getReuseCondition().test(prc));
    }

    private AuthenticationResult createResult(AuthenticationFlowDescriptor flow, Subject subject) {
        final AuthenticationResult result = flow.newAuthenticationResult(subject);
        result.setAuthenticationInstant(INSTANT);
        result.setLastActivityInstant(Instant.ofEpochMilli(ACTIVITY));
        return result;
    }
    
    private ProfileRequestContext getProfileRequestContext(final List<AuthenticationFlowDescriptor> flows) {
        final ProfileRequestContext prc = new RequestContextBuilder().buildProfileRequestContext();
        final AuthenticationContext ac = prc.getSubcontext(AuthenticationContext.class, true);
        for (final AuthenticationFlowDescriptor flow : flows) {
            ac.getAvailableFlows().put(flow.getId(), flow);
        }
        return prc;
    }
    
    private String fileToString(String pathname) throws URISyntaxException, IOException {
        try (final FileInputStream stream = new FileInputStream(
                new File(DefaultAuthenticationResultSerializerTest.class.getResource(pathname).toURI()))) {
            int avail = stream.available();
            byte[] data = new byte[avail];
            int numRead = 0;
            int pos = 0;
            do {
              if (pos + avail > data.length) {
                byte[] newData = new byte[pos + avail];
                System.arraycopy(data, 0, newData, 0, pos);
                data = newData;
              }
              numRead = stream.read(data, pos, avail);
              if (numRead >= 0) {
                pos += numRead;
              }
              avail = stream.available();
            } while (avail > 0 && numRead >= 0);
            return new String(data, 0, pos, "UTF-8").trim();
        }
    }
}