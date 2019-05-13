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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.security.auth.Subject;

import net.shibboleth.idp.attribute.ByteAttributeValue;
import net.shibboleth.idp.attribute.EmptyAttributeValue;
import net.shibboleth.idp.attribute.IdPAttribute;
import net.shibboleth.idp.attribute.ScopedStringAttributeValue;
import net.shibboleth.idp.attribute.StringAttributeValue;
import net.shibboleth.idp.authn.AuthenticationResult;
import net.shibboleth.idp.authn.principal.AuthenticationResultPrincipal;
import net.shibboleth.idp.authn.principal.IdPAttributePrincipal;
import net.shibboleth.idp.authn.principal.PasswordPrincipal;
import net.shibboleth.idp.authn.principal.PrincipalSerializer;
import net.shibboleth.idp.authn.principal.ProxyAuthenticationPrincipal;
import net.shibboleth.idp.authn.principal.TestPrincipal;
import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.idp.authn.principal.impl.IdPAttributePrincipalSerializer;
import net.shibboleth.idp.authn.principal.impl.LDAPPrincipalSerializer;
import net.shibboleth.idp.authn.principal.impl.PasswordPrincipalSerializer;
import net.shibboleth.idp.authn.principal.impl.ProxyAuthenticationPrincipalSerializer;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.TestResourceConverter;
import net.shibboleth.utilities.java.support.security.BasicKeystoreKeyStrategy;
import net.shibboleth.utilities.java.support.security.DataSealer;

import org.ldaptive.LdapAttribute;
import org.ldaptive.LdapEntry;
import org.ldaptive.SortBehavior;
import org.ldaptive.jaas.LdapPrincipal;
import org.springframework.core.io.ClassPathResource;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/** {@link DefaultAuthenticationResultSerializer} unit test. */
public class DefaultAuthenticationResultSerializerTest {

    private static final String DATAPATH = "/net/shibboleth/idp/authn/impl/";
    
    private static final String CONTEXT = "_context";
    
    private static final String KEY = "_key";
    
    private static final Instant INSTANT = Instant.ofEpochMilli(1378827849463L);
    
    private static final long ACTIVITY = 1378827556778L;
    
    private DefaultAuthenticationResultSerializer serializer;
    
    @BeforeMethod public void setUp() {
        serializer = new DefaultAuthenticationResultSerializer();
    }

    @Test public void testInvalid() throws Exception {
        serializer.initialize();
        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "invalid.json"), ACTIVITY);
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "noFlowId.json"), ACTIVITY);
            Assert.fail();
        } catch (IOException e) {
            
        }

        try {
            serializer.deserialize(1, CONTEXT, KEY, fileToString(DATAPATH + "noInstant.json"), ACTIVITY);
            Assert.fail();
        } catch (IOException e) {
            
        }
    }
    
    @Test public void testSimple() throws Exception {
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        
        final String s = serializer.serialize(result);
        final String s2 = fileToString(DATAPATH + "simpleAuthenticationResult.json");
        Assert.assertEquals(s, s2);
        
        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);
        
        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
    }

    @Test public void testComplex() throws Exception {
        serializer.initialize();
        
        AuthenticationResult result = createResult("test", new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        result.getSubject().getPrincipals().add(new TestPrincipal("foo"));
        result.getSubject().getPrincipals().add(new TestPrincipal("bar"));
        
        String s = serializer.serialize(result);
        String s2 = fileToString(DATAPATH + "complexAuthenticationResult.json");
        Assert.assertEquals(s, s2);
        
        AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);
        
        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
    }

    @Test public void testCreds() throws Exception {
        final ClassPathResource keystoreResource = new ClassPathResource("/net/shibboleth/idp/authn/impl/SealerKeyStore.jks");
        final ClassPathResource versionResource = new ClassPathResource("/net/shibboleth/idp/authn/impl/SealerKeyStore.kver");

        final BasicKeystoreKeyStrategy strategy = new BasicKeystoreKeyStrategy();
        strategy.setKeyAlias("secret");
        strategy.setKeyPassword("kpassword");
        strategy.setKeystorePassword("password");
        strategy.setKeystoreResource(TestResourceConverter.of(keystoreResource));
        strategy.setKeyVersionResource(TestResourceConverter.of(versionResource));

        final DataSealer sealer = new DataSealer();
        sealer.setKeyStrategy(strategy);

        try {
            strategy.initialize();
            sealer.initialize();
        } catch (ComponentInitializationException e) {
            Assert.fail(e.getMessage());
        }

        final PasswordPrincipalSerializer pwSerializer = new PasswordPrincipalSerializer();
        pwSerializer.setDataSealer(sealer);
        pwSerializer.initialize();
        serializer.setPrincipalSerializers(Collections.<PrincipalSerializer<String>>singletonList(pwSerializer));
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        result.getSubject().getPrivateCredentials().add(new PasswordPrincipal("bar"));
        
        final String s = serializer.serialize(result);
        
        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s, ACTIVITY);
        
        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
    }

    @Test public void testSymbolic() throws Exception {
        serializer.getGenericPrincipalSerializer().setSymbolics(Collections.singletonMap(TestPrincipal.class.getName(), 1));
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
        result.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        result.getSubject().getPrincipals().add(new TestPrincipal("foo"));
        result.getSubject().getPrincipals().add(new TestPrincipal("bar"));
        
        final String s = serializer.serialize(result);
        final String s2 = fileToString(DATAPATH + "symbolicAuthenticationResult.json");
        Assert.assertEquals(s, s2);
        
        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);
        
        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
    }
    

    @Test public void testLdap() throws Exception {
        final LDAPPrincipalSerializer lpSerializer = new LDAPPrincipalSerializer();
        serializer.setPrincipalSerializers(Collections.<PrincipalSerializer<String>>singletonList(lpSerializer));
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
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

        final String s = serializer.serialize(result);
        final String s2 = fileToString(DATAPATH + "LDAPAuthenticationResult.json");
        Assert.assertEquals(s, s2);

        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);

        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
        Assert.assertEquals(
                ((LdapPrincipal) result.getSubject().getPrincipals().iterator().next()).getLdapEntry(),
                ((LdapPrincipal) result2.getSubject().getPrincipals().iterator().next()).getLdapEntry());
    }

    @Test public void testIdPAttribute() throws Exception {
        final IdPAttributePrincipalSerializer attrSerializer = new IdPAttributePrincipalSerializer();
        serializer.setPrincipalSerializers(Collections.<PrincipalSerializer<String>>singletonList(attrSerializer));
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
        final IdPAttributePrincipal prin = new IdPAttributePrincipal(new IdPAttribute("foo"));
        prin.getAttribute().setValues(Arrays.asList(new StringAttributeValue("bar"),
                new ScopedStringAttributeValue("bar2", "scope"), EmptyAttributeValue.ZERO_LENGTH,
                new ByteAttributeValue("foo".getBytes())));
        
        result.getSubject().getPrincipals().add(prin);

        final String s = serializer.serialize(result);
        final String s2 = fileToString(DATAPATH + "IdPAttributeAuthenticationResult.json");
        Assert.assertEquals(s, s2);

        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);

        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
        
        final IdPAttribute attribute =
                ((IdPAttributePrincipal) result2.getSubject().getPrincipals().iterator().next()).getAttribute();
        Assert.assertEquals(attribute.getValues().size(), 3);
        Assert.assertEquals(((StringAttributeValue) attribute.getValues().get(0)).getValue(), "bar");
        Assert.assertEquals(((StringAttributeValue)attribute.getValues().get(1)).getValue(), "bar2");
        Assert.assertEquals(((ScopedStringAttributeValue) attribute.getValues().get(1)).getScope(), "scope");
        Assert.assertEquals(attribute.getValues().get(2), EmptyAttributeValue.ZERO_LENGTH);
    }

    @Test public void testProxyAuthentication() throws Exception {
        final ProxyAuthenticationPrincipalSerializer proxySerializer = new ProxyAuthenticationPrincipalSerializer();
        serializer.setPrincipalSerializers(Collections.<PrincipalSerializer<String>>singletonList(proxySerializer));
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
        final ProxyAuthenticationPrincipal prin = new ProxyAuthenticationPrincipal();
        prin.getAuthorities().addAll(Arrays.asList("foo","bar","baz"));
        result.getSubject().getPrincipals().add(prin);

        final String s = serializer.serialize(result);
        final String s2 = fileToString(DATAPATH + "ProxyAuthenticationResult.json");
        Assert.assertEquals(s, s2);

        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);

        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
        
        final Collection<String> authorities =
                ((ProxyAuthenticationPrincipal) result2.getSubject().getPrincipals().iterator().next()).getAuthorities();
        Assert.assertEquals(authorities, prin.getAuthorities());
    }

    @Test public void testNestedAuthenticationResult() throws Exception {
        serializer.initialize();
        
        final AuthenticationResult result = createResult("test", new Subject());
        final AuthenticationResult nested = createResult("nested", new Subject());
        nested.setLastActivityInstant(INSTANT);
        nested.getSubject().getPrincipals().add(new UsernamePrincipal("bob"));
        
        result.getSubject().getPrincipals().add(new AuthenticationResultPrincipal(nested));

        final String s = serializer.serialize(result);
        final String s2 = fileToString(DATAPATH + "NestedAuthenticationResult.json");
        Assert.assertEquals(s, s2);

        final AuthenticationResult result2 = serializer.deserialize(1, CONTEXT, KEY, s2, ACTIVITY);

        Assert.assertEquals(result.getAuthenticationFlowId(), result2.getAuthenticationFlowId());
        Assert.assertEquals(result.getAuthenticationInstant(), result2.getAuthenticationInstant());
        Assert.assertEquals(result.getLastActivityInstant(), result2.getLastActivityInstant());
        Assert.assertEquals(result.getSubject(), result2.getSubject());
        
        final AuthenticationResult nested2 =
                ((AuthenticationResultPrincipal) result2.getSubject().getPrincipals().iterator().next()).getAuthenticationResult();
        Assert.assertEquals(nested.getAuthenticationFlowId(), nested2.getAuthenticationFlowId());
        Assert.assertEquals(nested.getAuthenticationInstant(), nested2.getAuthenticationInstant());
        Assert.assertEquals(nested.getLastActivityInstant(), nested2.getLastActivityInstant());
        Assert.assertEquals(nested.getSubject(), nested2.getSubject());
    }

    private AuthenticationResult createResult(String flowId, Subject subject) {
        final AuthenticationResult result = new AuthenticationResult(flowId, subject);
        result.setAuthenticationInstant(INSTANT);
        result.setLastActivityInstant(Instant.ofEpochMilli(ACTIVITY));
        return result;
    }
    
    private String fileToString(String pathname) throws URISyntaxException, IOException {
        try (FileInputStream stream = new FileInputStream(
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