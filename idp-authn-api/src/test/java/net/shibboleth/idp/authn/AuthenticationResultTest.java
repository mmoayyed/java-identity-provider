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

package net.shibboleth.idp.authn;

import java.time.Instant;

import net.shibboleth.idp.authn.principal.UsernamePrincipal;
import net.shibboleth.shared.logic.ConstraintViolationException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AuthenticationResult} unit test. */
public class AuthenticationResultTest {

    /**
     * Tests that everything is properly initialized during object construction.
     * 
     * @throws Exception if something goes wrong
     */
    @Test public void testInstantiation() throws Exception {
        Instant start = Instant.now();
        // this is here to allow the event's creation time to deviate from the 'start' time
        Thread.sleep(50);

        AuthenticationResult event = new AuthenticationResult("test", new UsernamePrincipal("bob"));
        Assert.assertTrue(event.getAuthenticationInstant().isAfter(start));
        Assert.assertEquals(event.getAuthenticationFlowId(), "test");
        
        Assert.assertTrue(event.getSubject().getPrincipals(UsernamePrincipal.class).contains(new UsernamePrincipal("bob")));

        try {
            new AuthenticationResult("", new UsernamePrincipal("bob"));
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }

        try {
            new AuthenticationResult("  ", new UsernamePrincipal("bob"));
            Assert.fail();
        } catch (ConstraintViolationException e) {

        }
    }

}
