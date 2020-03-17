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

package net.shibboleth.idp.profile.spring.failfast;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import java.io.IOException;
import java.util.List;

import org.opensaml.saml.metadata.resolver.MetadataResolver;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.service.ServiceableComponent;

@Ignore
@SuppressWarnings({"unchecked", "javadoc"})
public class MetadataFailFastTest extends AbstractFailFastTest {
    private static int uniquifier;

    @Test public void workingInline() throws IOException {
        
        final Object bean = getBean(propertySource("ServiceConfiguration", makePath("inLineMetadataGood.xml")), "metadataBeansDefaultFF.xml");
        final ReloadableService<MetadataResolver > service = (ReloadableService<MetadataResolver>) bean;
        assertNotNull(service);
        final MetadataResolver resolver = service.getServiceableComponent().getComponent();
        assertNotNull(resolver);
    }

    private void nonWorkingInline(final Boolean failFast) throws IOException {
        nonWorkingMetadata(failFast, propertySource("ServiceConfiguration", makePath("inLineMetadataBad.xml")));
    }

    private void nonWorkingMetadata(final Boolean failFast,
                                    final MockPropertySource propertySource) throws IOException {
        final String beanPath;
        if (failFast == null) {
            beanPath = "metadataBeansDefaultFF.xml";
        } else {
            beanPath = "metadataBeans.xml";
        }
        
        final Object bean = getBean(propertySource, failFast, beanPath);
        final ReloadableService<MetadataResolver > service = (ReloadableService<MetadataResolver>) bean;
        if (null != failFast && failFast) {
            assertNull(service);
            return;
        }
        assertNotNull(service);
        final ServiceableComponent<MetadataResolver> component = service.getServiceableComponent();
        assertNull(component);
    }

    @Test public void nonWorkingInlineFailFast() throws IOException {
        nonWorkingInline(true);
    }

    @Test public void nonWorkingInline() throws IOException {
        nonWorkingInline(false);
    }

    @Test public void nonWorkingInlineDefault() throws IOException {
        nonWorkingInline(null);
    }

    @Test public void workingFile() throws IOException {
        final List<Pair<String, String>> prop = List.of(new Pair<>("ServiceConfiguration", makePath("fileMetadata.xml")),
                new Pair<>("File", makePath("metadataFileGood.xml")));
        
        final Object bean = getBean(propertySource(prop), "metadataBeansDefaultFF.xml");
        final ReloadableService<MetadataResolver > service = (ReloadableService<MetadataResolver>) bean;
        assertNotNull(service);
        final MetadataResolver resolver = service.getServiceableComponent().getComponent();
        assertNotNull(resolver);
    }

    private void badFile(final Boolean failFast) throws IOException {
        final List<Pair<String, String>> prop = List.of(new Pair<>("ServiceConfiguration", makePath("fileMetadata.xml")),
                new Pair<>("File", makePath("metadataFileBad.xml")));
        nonWorkingMetadata(failFast, propertySource(prop));
    }

    @Test public void badFileFailFast() throws IOException {
        badFile(true);
    }

    @Test public void badFile() throws IOException {
        badFile(false);
    }

    @Test public void badFileDefault() throws IOException {
        badFile(null);
    }

    private void nonExistingFile(final Boolean failFast) throws IOException {
        final List<Pair<String, String>> prop = List.of(new Pair<>("ServiceConfiguration", makePath("fileMetadata.xml")),
                new Pair<>("File", makePath("metadatNooneHome.xml")));
        nonWorkingMetadata(failFast, propertySource(prop));
    }

    @Test public void notThereFileFailFast() throws IOException {
        nonExistingFile(true);
    }

    @Test public void notThereFile() throws IOException {
        nonExistingFile(false);
    }

    @Test public void notThereFileDefault() throws IOException {
        nonExistingFile(null);
    }

    @Test public void workingHttp() throws IOException {
        final List<Pair<String, String>> prop = List.of(
                new Pair<>("ServiceConfiguration", makePath("httpMetadata.xml")),
                new Pair<>("Backing", makeTempPath("workingHttpTmp" + uniquifier++ + ".xml")),
                new Pair<>("metadataURL", makeURLPath("metadataFileGood.xml")));

        final Object bean = getBean(propertySource(prop), "metadataBeansDefaultFF.xml");
        final ReloadableService<MetadataResolver > service = (ReloadableService<MetadataResolver>) bean;
        assertNotNull(service);
        final MetadataResolver resolver = service.getServiceableComponent().getComponent();
        assertNotNull(resolver);
    }

    private void badHttp(final Boolean failFast) throws IOException {
        final List<Pair<String, String>> prop = List.of(
                new Pair<>("ServiceConfiguration", makePath("httpMetadata.xml")),
                new Pair<>("Backing", makeTempPath("badHttpTmp" + uniquifier++ + ".xml")),
                new Pair<>("metadataURL", makeURLPath("metadataFileBad.xml")));
        nonWorkingMetadata(failFast, propertySource(prop));
    }

    @Test public void badHttpFailFast() throws IOException {
        badHttp(true);
    }

    @Test public void badHttp() throws IOException {
        badHttp(false);
    }

    @Test public void badHttpDefault() throws IOException {
        badHttp(null);
    }

    private void nonExistingHttp(final Boolean failFast) throws IOException {
        final List<Pair<String, String>> prop = List.of(
                new Pair<>("ServiceConfiguration", makePath("httpMetadata.xml")),
                new Pair<>("Backing", makeTempPath("badHttpTmp" + uniquifier++ + ".xml")),
                new Pair<>("metadataURL", makeURLPath("ItsNotThere.xml")));
        nonWorkingMetadata(failFast, propertySource(prop));
    }

    @Test public void notThereHttpFailFast() throws IOException {
        nonExistingHttp(true);
    }

    @Test public void notThereHttp() throws IOException {
        nonExistingHttp(false);
    }

    @Test public void notThereHttpDefault() throws IOException {
        nonExistingHttp(null);
    }
}
