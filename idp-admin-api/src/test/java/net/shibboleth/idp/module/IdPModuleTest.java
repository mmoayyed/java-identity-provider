package net.shibboleth.idp.module;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

import org.opensaml.security.credential.impl.StaticCredentialResolver;
import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.opensaml.security.httpclient.impl.SecurityEnhancedHttpClientSupport;
import org.opensaml.security.trust.TrustEngine;
import org.opensaml.security.trust.impl.ExplicitKeyTrustEngine;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.security.x509.X509Credential;
import org.opensaml.security.x509.X509Support;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.io.ByteStreams;

import net.shibboleth.idp.module.IdPModule.ModuleResource;
import net.shibboleth.utilities.java.support.httpclient.HttpClientBuilder;
import net.shibboleth.utilities.java.support.repository.RepositorySupport;

/**
 * Unit tests exercising module code.
 */
public class IdPModuleTest {

    private static final String XML_DATA = "<test>foo</test>\n";
    private static final String XML_OTHER_DATA = "<test>bar</test>\n";
    private static final String VEL_DATA = "## something\n";
    private static final String VEL_OTHER_DATA = "## something else\n";
    
    private Path testHome;
    private IdPModule testModule;
    private ModuleContext context;
    
    @BeforeMethod
    public void setUp() throws Exception {
        final ServiceLoader<IdPModule> loader = ServiceLoader.load(IdPModule.class);
        final Optional<Provider<IdPModule>> opt =
                loader.stream().filter(p -> TestModule.class.equals(p.type())).findFirst();
        Assert.assertTrue(opt.isPresent());
        
        testModule = opt.get().get();
        
        testHome = Files.createTempDirectory("test-idp-home-");
        context = new ModuleContext(testHome);
        
        final HttpClientBuilder builder = new HttpClientBuilder();
        builder.setTLSSocketFactory(SecurityEnhancedHttpClientSupport.buildTLSSocketFactory(true, false));
        context.setHttpClient(builder.buildClient());
        
        final HttpClientSecurityParameters params = new HttpClientSecurityParameters();
        params.setTLSTrustEngine(buildExplicitKeyTrustEngine());
        context.setHttpClientSecurityParameters(params);
    }
    
    @AfterMethod
    public void tearDown() throws IOException {
        if (testHome != null) {
            Files.walkFileTree(testHome, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException
                {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e)
                    throws IOException
                {
                    if (e == null) {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    // directory iteration failed
                    throw e;
                }
            });
            testHome = null;
        }
    }
    
    @Test
    public void testBadModules() {
        final ServiceLoader<IdPModule> loader = ServiceLoader.load(IdPModule.class);

        Optional<Provider<IdPModule>> opt =
                loader.stream().filter(p -> BadModule.class.equals(p.type())).findFirst();
        Assert.assertTrue(opt.isPresent());
        try {
            opt.get().get();
            Assert.fail("BadModule should have failed");
        } catch (final ServiceConfigurationError e) {
            Assert.assertTrue(e.getCause() instanceof ModuleException);
        }

        opt = loader.stream().filter(p -> BadModule2.class.equals(p.type())).findFirst();
        Assert.assertTrue(opt.isPresent());
        try {
            opt.get().get();
            Assert.fail("BadModule2 should have failed");
        } catch (final ServiceConfigurationError e) {
            Assert.assertTrue(e.getCause() instanceof ModuleException);
        }
    }
    
    @Test
    public void testModule() {
        Assert.assertEquals(testModule.getId(), "idp.test");
        Assert.assertEquals(testModule.getName(), "Test module");
        Assert.assertEquals(testModule.getURL().toString(), "https://wiki.shibboleth.net/confluence/display/IDP4/Home");
        
        final Iterator<ModuleResource> resources = testModule.getResources().iterator();
        Assert.assertEquals(testModule.getResources().size(), 2);
        
        ModuleResource resource = resources.next();
        Assert.assertEquals(resource.getSource(), "/net/shibboleth/idp/module/test.xml");
        Assert.assertEquals(resource.getDestination(), Path.of("conf/test.xml"));
        
        resource = resources.next();
        Assert.assertEquals(resource.getSource(),
                RepositorySupport.buildHTTPSResourceURL("java-identity-provider", "idp-admin-api/src/test/resources/net/shibboleth/idp/module/test.vm"));
        Assert.assertEquals(resource.getDestination(), Path.of("views/test.vm"));
    }

    @Test
    public void testEnableNoTree() throws ModuleException, IOException {
        testModule.enable(context);
        String xml = Files.readString(testHome.resolve("conf/test.xml"));
        Assert.assertEquals(xml, XML_DATA);
        
        String vel = Files.readString(testHome.resolve("views/test.vm"));
        Assert.assertEquals(vel, VEL_DATA);
    }

    @Test
    public void testDisableNoTree() throws ModuleException {
        testModule.disable(context, true);
        testModule.disable(context, false);
    }

    @Test
    public void testEnableClean() throws ModuleException, IOException {
        
        Files.createDirectory(testHome.resolve("conf"));
        Files.createDirectory(testHome.resolve("views"));
        
        testModule.enable(context);
        
        String xml = Files.readString(testHome.resolve("conf/test.xml"));
        Assert.assertEquals(xml, XML_DATA);
        
        String vel = Files.readString(testHome.resolve("views/test.vm"));
        Assert.assertEquals(vel, VEL_DATA);
        
        testModule.disable(context, false);
        Assert.assertEquals(testHome.resolve("conf").toFile().listFiles().length, 0);
        Assert.assertEquals(testHome.resolve("views").toFile().listFiles().length, 0);
    }

    @Test
    public void testEnableExistingSame() throws IOException, ModuleException {
        Files.createDirectory(testHome.resolve("conf"));
        Files.createDirectory(testHome.resolve("views"));
        
        try (final OutputStream os =
                Files.newOutputStream(Files.createFile(testHome.resolve("conf/test.xml")))) {
            os.write(XML_DATA.getBytes());
        }

        try (final OutputStream os =
                Files.newOutputStream(Files.createFile(testHome.resolve("views/test.vm")))) {
            os.write(VEL_DATA.getBytes());
        }

        testModule.enable(context);

        String xml = Files.readString(testHome.resolve("conf/test.xml"));
        Assert.assertEquals(xml, XML_DATA);
        Assert.assertFalse(testHome.resolve("conf/test.xml.idpsave").toFile().exists());
        
        String vel = Files.readString(testHome.resolve("views/test.vm"));
        Assert.assertEquals(vel, VEL_DATA);
        Assert.assertFalse(testHome.resolve("views/test.vm.idpnew").toFile().exists());
    }

    @Test
    public void testEnableExistingDifferent() throws IOException, ModuleException {
        Files.createDirectory(testHome.resolve("conf"));
        Files.createDirectory(testHome.resolve("views"));
        
        try (final OutputStream os =
                Files.newOutputStream(Files.createFile(testHome.resolve("conf/test.xml")))) {
            os.write(XML_OTHER_DATA.getBytes());
        }

        try (final OutputStream os =
                Files.newOutputStream(Files.createFile(testHome.resolve("views/test.vm")))) {
            os.write(VEL_OTHER_DATA.getBytes());
        }

        testModule.enable(context);

        String xml = Files.readString(testHome.resolve("conf/test.xml"));
        Assert.assertEquals(xml, XML_DATA);
        xml = Files.readString(testHome.resolve("conf/test.xml.idpsave"));
        Assert.assertEquals(xml, XML_OTHER_DATA);
        
        String vel = Files.readString(testHome.resolve("views/test.vm"));
        Assert.assertEquals(vel, VEL_OTHER_DATA);
        vel = Files.readString(testHome.resolve("views/test.vm.idpnew"));
        Assert.assertEquals(vel, VEL_DATA);
    }
    
    private static TrustEngine<? super X509Credential> buildExplicitKeyTrustEngine() throws URISyntaxException, CertificateException, IOException {
        
        final InputStream certStream = IdPModuleTest.class.getResourceAsStream("/net/shibboleth/idp/module/repo-entity.crt");
        final X509Certificate entityCert = X509Support.decodeCertificate(ByteStreams.toByteArray(certStream));
        final X509Credential entityCredential = new BasicX509Credential(entityCert);
        return new ExplicitKeyTrustEngine(new StaticCredentialResolver(entityCredential));
        
    }
}