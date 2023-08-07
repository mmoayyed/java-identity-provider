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

package net.shibboleth.idp.authn.impl;

import java.io.File;
import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import net.shibboleth.shared.resource.Resource;
import net.shibboleth.shared.spring.resource.ResourceHelper;

import org.ldaptive.ssl.SSLContextInitializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test for {@link KeystoreResourceCredentialConfig}.
 */
@SuppressWarnings("javadoc")
public class KeystoreResourceCredentialConfigTest {

    @Nonnull private static final String DATAPATH = "/net/shibboleth/idp/authn/impl/";

    @DataProvider(name = "resources")
    public Object[][] getResources() throws Exception {
        return new Object[][] {
          new Object[] {
              getFileSystemResource(DATAPATH + "truststore.jks"),
              getFileSystemResource(DATAPATH + "keystore.jks"),
          },
          new Object[] {
              ResourceHelper.of(new ClassPathResource(DATAPATH + "truststore.jks")),
              ResourceHelper.of(new ClassPathResource(DATAPATH + "keystore.jks")),
          },
        };
    }

    @Test(dataProvider = "resources") public void createSSLContextInitializer(@Nonnull final Resource truststore,
            @Nonnull final Resource keystore) throws Exception {
        final KeystoreResourceCredentialConfig config = new KeystoreResourceCredentialConfig();
        config.setTruststore(truststore);
        config.setKeystore(keystore);
        config.setKeystorePassword("changeit");

        final SSLContextInitializer init = config.createSSLContextInitializer();
        Assert.assertNotNull(init.getTrustManagers()[0]);
        Assert.assertNotNull(init.getKeyManagers()[0]);
    }

    @Nonnull private static Resource getFileSystemResource(@Nonnull final String path) throws URISyntaxException {
        return ResourceHelper.of(new FileSystemResource(new File(X509ResourceCredentialConfigTest.class.getResource(
                path).toURI())));
    }

}