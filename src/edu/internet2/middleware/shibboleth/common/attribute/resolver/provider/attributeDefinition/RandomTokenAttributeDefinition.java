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

package edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.attributeDefinition;

import java.io.Serializable;
import java.security.NoSuchAlgorithmException;

import org.joda.time.DateTime;
import org.opensaml.common.IdentifierGenerator;
import org.opensaml.common.impl.SecureRandomIdentifierGenerator;
import org.opensaml.util.storage.ExpiringObject;
import org.opensaml.util.storage.StorageService;

import edu.internet2.middleware.shibboleth.common.attribute.BaseAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.provider.BasicAttribute;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.AttributeResolutionException;
import edu.internet2.middleware.shibboleth.common.attribute.resolver.provider.ShibbolethResolutionContext;
import edu.internet2.middleware.shibboleth.common.profile.provider.SAMLProfileRequestContext;

/**
 * An attribute definition that generates random information to use as its value. Useful for transient subject
 * identifiers.
 */
public class RandomTokenAttributeDefinition extends BaseAttributeDefinition {

    /** Store used to map tokens to principals. */
    private StorageService<String, TokenEntry> tokenStore;

    /** Generater of randome, hex-encdoded, tokens. */
    private IdentifierGenerator tokenGenerator;

    /** Size, in bytes, of the token. */
    private int tokenSize;

    /** Length, in milliseconds, tokens are valid. */
    private long tokenLifetime;

    /**
     * Constructor.
     * 
     * @param store store used to map tokens to principals
     * 
     * @throws NoSuchAlgorithmException thrown if the SHA1PRNG, used as the default random number generation algo, is
     *             not supported
     */
    public RandomTokenAttributeDefinition(StorageService<String, TokenEntry> store) throws NoSuchAlgorithmException {
        tokenGenerator = new SecureRandomIdentifierGenerator();
        tokenStore = store;
        tokenSize = 16;
        tokenLifetime = 1000 * 60 * 60 * 4;
    }

    /**
     * Constructor.
     * 
     * @param generator generator used to create tokens
     * @param store store used to map tokens to principals
     */
    public RandomTokenAttributeDefinition(IdentifierGenerator generator, StorageService<String, TokenEntry> store) {
        tokenGenerator = generator;
        tokenStore = store;
        tokenSize = 16;
        tokenLifetime = 1000 * 60 * 60 * 4;
    }

    /** {@inheritDoc} */
    protected BaseAttribute doResolve(ShibbolethResolutionContext resolutionContext)
            throws AttributeResolutionException {

        SAMLProfileRequestContext requestContext = resolutionContext.getAttributeRequestContext();
        String token = tokenGenerator.generateIdentifier(tokenSize);

        TokenEntry tokenEntry = new TokenEntry(tokenLifetime, requestContext.getInboundMessageIssuer(), requestContext
                .getPrincipalName(), token);
        tokenStore.put(token, tokenEntry);

        BasicAttribute<String> attribute = new BasicAttribute<String>();
        attribute.setId(getId());
        attribute.getValues().add(token);

        return attribute;
    }

    /**
     * Gets the size, in bytes, of the token.
     * 
     * @return size, in bytes, of the token
     */
    public int getTokenSize() {
        return tokenSize;
    }

    /**
     * Sets the size, in bytes, of the token.
     * 
     * @param size size, in bytes, of the token
     */
    public void setTokenSize(int size) {
        tokenSize = size;
    }

    /**
     * Gets the time, in milliseconds, tokens are valid.
     * 
     * @return time, in milliseconds, tokens are valid
     */
    public long getTokenLifetime() {
        return tokenLifetime;
    }

    /**
     * Sets the time, in milliseconds, tokens are valid.
     * 
     * @param lifetime time, in milliseconds, tokens are valid
     */
    public void setTokenLiftetime(long lifetime) {
        tokenLifetime = lifetime;
    }

    /** {@inheritDoc} */
    public void validate() throws AttributeResolutionException {

    }

    /**
     * Storage service entry used to store information associated with a token.
     */
    public class TokenEntry implements ExpiringObject, Serializable {

        /** Serial version UID. */
        private static final long serialVersionUID = 4267659016907251921L;

        /** Time this entry expires. */
        private DateTime expirationTime;

        /** Relying party the token was issed to. */
        private String relyingPartyId;

        /** Principal the token was issused for. */
        private String principalName;

        /** Random token. */
        private String token;

        /**
         * Constructor.
         * 
         * @param lifetime lifetime of the token in milliseconds
         * @param relyingParty relying party the token was issued to
         * @param principal principal the token was issued for
         * @param randomToken the token
         */
        public TokenEntry(long lifetime, String relyingParty, String principal, String randomToken) {
            expirationTime = new DateTime().plus(lifetime);
            relyingPartyId = relyingParty;
            principalName = principal;
            token = randomToken;
        }

        /**
         * Gets the principal the token was issued for.
         * 
         * @return principal the token was issued for
         */
        public String getPrincipalName() {
            return principalName;
        }

        /**
         * Gets the relying party the token was issued to.
         * 
         * @return relying party the token was issued to
         */
        public String getRelyingPartyId() {
            return relyingPartyId;
        }

        /**
         * Gets the random token.
         * 
         * @return random token
         */
        public String getToken() {
            return token;
        }

        /** {@inheritDoc} */
        public DateTime getExpirationTime() {
            return expirationTime;
        }

        /** {@inheritDoc} */
        public boolean isExpired() {
            return expirationTime.isBeforeNow();
        }
    }
}