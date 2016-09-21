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

package net.shibboleth.idp.profile.spring.factory;

import junit.framework.Assert;
import net.shibboleth.ext.spring.context.FilesystemGenericApplicationContext;
import net.shibboleth.ext.spring.util.SchemaTypeAwareXMLBeanDefinitionReader;

import org.opensaml.security.x509.BasicX509Credential;
import org.springframework.context.support.GenericApplicationContext;
import org.testng.annotations.Test;

/**
 *
 */
public class BasicX509CredentialFactoryBeanTest {

    @Test public void bean() {
        final GenericApplicationContext context = new FilesystemGenericApplicationContext();
            context.setDisplayName("ApplicationContext: X509Credential");
            final SchemaTypeAwareXMLBeanDefinitionReader beanDefinitionReader =
                    new SchemaTypeAwareXMLBeanDefinitionReader(context);
    
            beanDefinitionReader.loadBeanDefinitions("net/shibboleth/idp/profile/spring/factory/bean.xml");
    
            context.refresh();
            
             final BasicX509Credential cred1 = context.getBean("Credential", BasicX509Credential.class);
            
             final BasicX509Credential cred2 = context.getBean("EncCredential", BasicX509Credential.class);
             
             Assert.assertEquals("http://example.org/enc", cred2.getEntityId()); 
             
             final byte[] cb1 = cred1.getPrivateKey().getEncoded();
             final byte[] cb2 = cred2.getPrivateKey().getEncoded();
             
             Assert.assertEquals(cb1.length, cb2.length);

             for (int i = 0; i< cb1.length; i++)Assert.assertEquals(cb1[i], cb2[i]);
             
             Assert.assertEquals(cred2.getPublicKey(), cred2.getPublicKey());

    }
}
