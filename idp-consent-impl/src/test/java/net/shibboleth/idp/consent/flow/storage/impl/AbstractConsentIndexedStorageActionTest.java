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

package net.shibboleth.idp.consent.flow.storage.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import net.shibboleth.idp.consent.storage.impl.CollectionSerializer;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.UnmodifiableComponentException;
import net.shibboleth.shared.logic.FunctionSupport;

import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.storage.StorageRecord;
import org.opensaml.storage.StorageSerializer;
import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link AbstractConsentIndexedStorageAction} unit test. */
@SuppressWarnings("javadoc")
public abstract class AbstractConsentIndexedStorageActionTest extends AbstractConsentStorageActionTest {

    private Object nullObj;

    protected void populateAction() throws Exception {
        super.populateAction();
        ((AbstractConsentIndexedStorageAction) action).setStorageIndexKeyLookupStrategy(FunctionSupport
                .<ProfileRequestContext, String> constant("_index"));
    }

    protected Collection<String> readStorageKeysFromIndex() throws IOException {
        final StorageRecord<?> index = getMemoryStorageService().read("context", "_index");
        assert index!=null;

        final CollectionSerializer collectionSerializer =
                (CollectionSerializer) ((AbstractConsentIndexedStorageAction) action).getStorageKeysSerializer();
        Assert.assertNotNull(collectionSerializer);

        return collectionSerializer.deserialize(0, "context", "_index", index.getValue(), index.getExpiration());
    }
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableStorageIndexKeyStrategy() throws Exception {
        action.initialize();
        ((AbstractConsentIndexedStorageAction) action).setStorageIndexKeyLookupStrategy((Function<ProfileRequestContext, String>) nullObj);
    }
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableStorageKeysSerializerStrategy() throws Exception {
        action.initialize();
        ((AbstractConsentIndexedStorageAction) action).setStorageKeysSerializer((StorageSerializer<Collection<String>>) nullObj);
    }
    
    @SuppressWarnings({ "null", "unchecked" })
    @Test(expectedExceptions = UnmodifiableComponentException.class)
    public void testUnmodifiableStorageKeysStrategy() throws Exception {
        action.initialize();
        ((AbstractConsentIndexedStorageAction) action).setStorageKeysStrategy((Function<Pair<ProfileRequestContext, List<String>>, List<String>>) nullObj);
    }

}
