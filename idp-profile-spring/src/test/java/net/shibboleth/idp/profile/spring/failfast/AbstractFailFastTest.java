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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.opensaml.core.OpenSAMLInitBaseTestCase;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Ignore;

import net.shibboleth.ext.spring.util.ApplicationContextBuilder;
import net.shibboleth.ext.spring.util.SpringSupport;
import net.shibboleth.utilities.java.support.collection.Pair;
import net.shibboleth.utilities.java.support.service.ReloadableService;
import net.shibboleth.utilities.java.support.test.repository.RepositorySupport;

/**
 * Base class for testing metadata providers.
 */
@Ignore
@SuppressWarnings({"javadoc"})
public class AbstractFailFastTest extends OpenSAMLInitBaseTestCase {

    private String workspaceDirName;

    static List<GenericApplicationContext> contexts;

    protected void registerContext(final GenericApplicationContext context) {
        synchronized(contexts) {
            contexts.add(context);
        }        
    }

    protected String getPath() {
        return "/net/shibboleth/idp/profile/spring/failfast/";
    }

    protected String getWorkspaceDirName() {
        return workspaceDirName;
    }

    @BeforeSuite public void beforeSuite() throws IOException {
        contexts = new ArrayList<>();
    }

    @BeforeClass public void beforeClass() throws IOException {
        final ClassPathResource resource =
                new ClassPathResource(getPath());
        workspaceDirName = resource.getFile().getAbsolutePath() + "/";
    }

    @SuppressWarnings("resource")
    @AfterSuite public void afterSuite() {
        final Iterator<GenericApplicationContext> contextIterator = contexts.iterator(); 
        while (contextIterator.hasNext()) {
            final GenericApplicationContext context;
            synchronized (contexts) {
                context = contextIterator.next();
            }
            context.close();
        }
    }

    protected ApplicationContext getApplicationContext(final String contextName, final MockPropertySource propSource, final String... files)
            throws IOException {
        final Resource[] resources = new Resource[files.length];

        for (int i = 0; i < files.length; i++) {
            resources[i] = new ClassPathResource(getPath() + files[i]);
        }

        final ApplicationContextBuilder builder = new ApplicationContextBuilder();
        
        builder.setName(contextName);
        
        builder.setPropertySources(Collections.singletonList(propSource));
        
        builder.setServiceConfigurations(Arrays.asList(resources));

        final GenericApplicationContext context = builder.build();
        
        registerContext(context);
        
        return context;
    }

    protected ApplicationContext getApplicationContext(final String contextName, final String... files) throws IOException {
        return getApplicationContext(contextName, null, files);
    }

    protected Object getBean(final MockPropertySource propSource, final String... files) throws IOException {
        return getBean(propSource, true, files);
    }

    protected Object getBean(final MockPropertySource propSource, Boolean failFast, final String... files) throws IOException {
        @SuppressWarnings("rawtypes") final Class<ReloadableService> claz = ReloadableService.class;
        
        if (null == failFast) {
            propSource.setProperty("failFast", "");
        } else if (failFast) {
            propSource.setProperty("failFast", "true");
        } else {
            propSource.setProperty("failFast", "false");
        }
        
        try {
            final ApplicationContext context = getApplicationContext(claz.getCanonicalName(), propSource, files);
            return SpringSupport.getBean(context, claz);
        } catch (Exception e) {
            LoggerFactory.getLogger(AbstractFailFastTest.class).debug("GetAppContext failed",e);
            return null;
        }
    }

    protected Object getBean(final String... files) throws IOException {
        return getBean(null, files);
    }

    protected MockPropertySource propertySource(final String name, final String value) {
        MockPropertySource propSource = new MockPropertySource("localProperties");
        propSource.setProperty(name, value);
        return propSource;
    }

    protected MockPropertySource propertySource(final Collection<Pair<String,String>> values) {
        MockPropertySource propSource = new MockPropertySource("localProperties");
        for (final Pair<String, String> value: values) {
            propSource.setProperty(value.getFirst(), value.getSecond());
        }
        return propSource;
    }

    protected String makePath(final String filePart) {
        return getWorkspaceDirName()  +  filePart;
    }

    protected String makeTempPath(final String filePart) {
        return getWorkspaceDirName()  +  filePart;
    }

    protected String makeURLPath(final String filePart) {
        return RepositorySupport.buildHTTPResourceURL("java-identity-provider", 
                "idp-profile-spring/src/test/resources" + getPath() + filePart,
                false);
    }
}
