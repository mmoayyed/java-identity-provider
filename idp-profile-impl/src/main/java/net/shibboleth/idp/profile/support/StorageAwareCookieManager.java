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

package net.shibboleth.idp.profile.support;

import java.io.IOException;
import java.time.Instant;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageService;
import org.slf4j.Logger;

import net.shibboleth.shared.annotation.constraint.NonnullAfterInit;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.net.CookieManager;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * An extended {@link CookieManager} that allows use of a {@link StorageService}.
 * 
 * <p>Reads are backed up by a read into the storage service, while writes are passed
 * through to it.</p>
 * 
 * <p>The cookie path and domain, and the username (as obtained by a lookup function)
 * are used to construct the storage context and key to maintain the expected isolation.
 * Notably, the function operates by obtaining the {@link ProfileRequestContext} from the
 * bound request attribute to address the fact that the API does not directly include it.</p>
 * 
 * <p>This is NOT suitable for use cases in which consistency of data is critical, as
 * there are few if any storage options (other than the client itself) that will provide
 * sufficient reliability and locking to avoid problems. It's best used for advisory cookies
 * whose absence does not create issues with security or expected behavior.</p>
 * 
 * @since 5.1.0
 */
public class StorageAwareCookieManager extends CookieManager {

    /** Class logger. */
    @Nonnull private Logger log = LoggerFactory.getLogger(StorageAwareCookieManager.class);
    
    /** Optional storage service to backstop the cookie. */
    @Nullable private StorageService storageService;
    
    /** Lookup strategy for username. */
    @Nullable private Function<ProfileRequestContext,String> usernameLookupStrategy;
    
    /** Storage context based on fixed value and cookie attributes. */
    @NonnullAfterInit private String storageContext;
    
    /**
     * Sets the {@link StorageService} to read/write.
     * 
     * @param ss storage service
     */
    public void setStorageService(@Nullable final StorageService ss) {
        checkSetterPreconditions();
        
        storageService = ss;
    }
    
    /**
     * Get the storage context used to hold the cookies.
     * 
     * @return storage context
     */
    @NonnullAfterInit public String getStorageContext() {
        return storageContext;
    }
    
    /**
     * Sets the lookup strategy to obtain the username for cookie partitioning.
     * 
     * @param strategy lookup strategy
     */
    public void setUsernameLookupStrategy(@Nullable final Function<ProfileRequestContext,String> strategy) {
        checkSetterPreconditions();
        
        usernameLookupStrategy = strategy;
    }
    
    /** {@inheritDoc} */
    @Override
    protected void doInitialize() throws ComponentInitializationException {
        super.doInitialize();
        
        if (storageService != null) {
            if (getMaxAge() == -1) {
                throw new ComponentInitializationException(
                        "StorageService cannot be used for per-session cookie manager.");
            } else if (usernameLookupStrategy == null) {
                throw new ComponentInitializationException("StorageService use requires a username lookup strategy.");
            }
        }
        
        final StringBuilder contextBuilder = new StringBuilder(getClass().getName());
        contextBuilder.append('!');
        if (getCookieDomain() != null) {
            contextBuilder.append(getCookieDomain());
        }
        contextBuilder.append('!');
        if (getCookiePath() != null) {
            contextBuilder.append(getCookiePath());
        }
        
        storageContext = contextBuilder.toString();
    }

    /** {@inheritDoc} */
    @Override
    public void addCookie(@Nonnull final String name, @Nonnull final String value) {
        super.addCookie(name, value);

        final Long exp = Instant.now().plusSeconds(getMaxAge()).toEpochMilli();
        
        final StorageService ss = storageService;
        if (ss != null) {
            try {
                final String cookieName = getPartitionedCookieName(name);
                if (ss.create(storageContext, cookieName, value, exp)) {
                    log.trace("Created new cookie record {}", cookieName);
                } else if (ss.update(storageContext, cookieName, value, exp)) {
                    log.trace("Updated cookie record {}", cookieName);
                }
            } catch (final IOException e) {
                log.warn("Error creating/updating cookie record in storage service", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void unsetCookie(@Nonnull final String name) {
        super.unsetCookie(name);
        
        final StorageService ss = storageService;
        if (ss != null) {
            try {
                final String cookieName = getPartitionedCookieName(name);
                ss.delete(storageContext, cookieName);
                log.trace("Deleted cookie record {}", cookieName);
            } catch (final IOException e) {
                log.warn("Error deleting cookie record from storage service", e);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @Nullable public String getCookieValue(@Nonnull final String name, @Nullable final String defValue) {
        
        final StorageService ss = storageService;
        if (ss != null) {
            try {
                final String val = super.getCookieValue(name, null);
                if (val != null) {
                    return val;
                }

                final String cookieName = getPartitionedCookieName(name);
                final StorageRecord<String> record = ss.read(storageContext, cookieName);
                if (record != null) {
                    log.debug("Backfilling/setting missing cookie {} based on stored record", cookieName);
                    final Long exp = record.getExpiration();
                    if (exp != null) {
                        // Uses protected hook to override max-age to backdate it.
                        super.addCookie(name, record.getValue(), (int) (exp - Instant.now().toEpochMilli()) / 1000);
                    } else {
                        // Won't ever happen, per init checking.
                        super.addCookie(name, record.getValue(), -1);
                    }
                    return record.getValue();
                }
            } catch (final IOException e) {
                log.warn("Error reading cookie record from storage service", e);
            }
        } else {
            return super.getCookieValue(name, defValue);
        }
        
        return defValue;
    }
    
    @Nonnull protected String getPartitionedCookieName(@Nonnull final String cookieName) throws IOException {
        
        final Object obj = getHttpServletRequest().getAttribute(ProfileRequestContext.BINDING_KEY);
        if (obj instanceof ProfileRequestContext prc) {
            final String username = usernameLookupStrategy != null ? usernameLookupStrategy.apply(prc) : null;
            if (username != null) {
                return cookieName + '!' + username;
            }
            throw new IOException("Username was unavailable");
        }
        
        throw new IOException("ProfileRequestContext was unavailable");
    }
    
}