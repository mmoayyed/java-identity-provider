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

package net.shibboleth.idp.test.flows.saml2;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.StatusResponseType;
import org.testng.Assert;

/**
 * SAML 2 {@link StatusResponseType} validator.
 */
public class SAML2TestStatusResponseTypeValidator {

    /** Expected IdP entity ID. */
    @Nonnull public String idpEntityID = "https://idp.example.org";

    /** Expected SP entity ID. */
    @Nonnull public String spEntityID = "https://sp.example.org";

    /** Expected status code. */
    @Nonnull public String statusCode = StatusCode.SUCCESS;

    /** Expected nested status code when an error occurs. */
    @Nonnull public String statusCodeNested = StatusCode.REQUEST_DENIED;

    /** Expected status message when an error occurs. */
    @Nonnull public String statusMessage = "An error occurred.";

    /** Expected destination. Optional. */
    @Nullable public String destination;

    /**
     * Validate the response.
     * <p>
     * Calls assert methods :
     * <ul>
     * <li>{@link #assertResponse(StatusResponseType)}</li>
     * <li>{@link #assertStatus(Status)}</li>
     * </ul>
     * 
     * @param response the status response type
     */
    public void validateResponse(@Nullable final StatusResponseType response) {
        assertResponse(response);
        assert response!=null;
        assertStatus(response.getStatus());
    }

    /**
     * Assert that :
     * <ul>
     * <li>the response is not null</li>
     * <li>the response ID is not null</li>
     * <li>the response ID is not empty</li>
     * <li>the response issue instant is not null</li>
     * <li>the response version is {@link SAMLVersion#VERSION_20}</li>
     * <li>the response issuer is the expected IdP entity ID</li>
     * <li>the response destination is the expected destination, if not null</li>
     * </ul>
     * 
     * @param response the status response type
     */
    public void assertResponse(@Nullable final StatusResponseType response) {
        assert response!=null;
        final String id = response.getID();
        Assert.assertTrue(id != null && !id.isEmpty());
        Assert.assertNotNull(response.getIssueInstant());
        Assert.assertEquals(response.getVersion(), (SAMLVersion.VERSION_20));
        final Issuer issuer = response.getIssuer();
        assert issuer != null;
        Assert.assertEquals(issuer.getValue(), idpEntityID);
        if (destination != null) {
            Assert.assertEquals(response.getDestination(), destination);
        }
    }

    /**
     * Assert that :
     * <ul>
     * <li>the status is not null</li>
     * <li>the status code is not null</li>
     * <li>the status code is the expected status code</li>
     * <li>the status message is the expected status message if the status code is not success</li>
     * <li>the nested status message is the expected nested status message if the status is not success</li>
     * </ul>
     * 
     * @param status the status
     */
    public void assertStatus(@Nullable final Status status) {
        assert status!=null;
        final StatusCode code = status.getStatusCode();
        assert code != null;
        Assert.assertEquals(code.getValue(), statusCode);
        if (statusCode != StatusCode.SUCCESS) {
            final StatusMessage msg = status.getStatusMessage();
            assert msg != null;
            Assert.assertEquals(msg.getValue(), statusMessage);
            final StatusCode nested = code.getStatusCode();
            if (nested != null) {
                Assert.assertEquals(nested.getValue(), statusCodeNested);
            }
        }
    }

}