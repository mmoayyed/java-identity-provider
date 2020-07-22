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

package net.shibboleth.idp.plugin;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.springframework.core.io.ClassPathResource;

/**
 *
 */
public class TestPlugin extends AbstractPluginDescription {

    /** {@inheritDoc} */
    @Override
    public String getPluginId() {
        // TODO Auto-generated method stub
        return "net.shibboleth.plugin.test";
    }

    /** {@inheritDoc} */
    @Override
    public List<URL> getUpdateURLs() {
        ClassPathResource resource = new ClassPathResource("/net/shibboleth/idp/plugin/plugins.props");
        try {
            return Collections.singletonList(resource.getURL());
        } catch (IOException e) {
            return Collections.EMPTY_LIST;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getMajorVersion() {
        return 1;
    }

    /** {@inheritDoc} */
    @Override
    public int getMinorVersion() {
        return 2;
    }
    

    /** {@inheritDoc} */
    @Override
    public int getPatchVersion() {
        return 3;
    }
}
