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

package net.shibboleth.idp.authn.principal.impl;

import java.io.IOException;

import javax.security.auth.x500.X500Principal;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/** Unit test for {@link X500PrincipalSerializer}. */
@SuppressWarnings("javadoc")
public class X500PrincipalSerializerTest {

    X500PrincipalSerializer serializer;
    
    @BeforeClass
    public void setUp() {
        serializer = new X500PrincipalSerializer();
    }
    
    @Test
    public void testRoundTrip() throws IOException {
        
        final X500Principal p1 = new X500Principal("DC=net, DC=shibboleth, CN=jdoe");
        
        final String s = serializer.serialize(p1);
        Assert.assertTrue(serializer.supports(s));
        
        final X500Principal p2 = serializer.deserialize(s);
        Assert.assertEquals(p1, p2);
        assert p2 != null;
        Assert.assertEquals(p2.getName(), "DC=net,DC=shibboleth,CN=jdoe");
    }
    
}