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

package net.shibboleth.idp.installer.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.tools.ant.input.InputRequest;
import org.apache.tools.ant.input.SecureInputHandler;

import net.shibboleth.idp.installer.PropertiesWithComments;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import org.slf4j.Logger;


/** Ant helper class to ask for passwords, rejecting zero length passwords and asking for confirmation. */
public class PasswordHandler extends SecureInputHandler {
    
    @Nonnull final private Logger log = LoggerFactory.getLogger(PasswordHandler.class);

    /** Spool the file to a {@link PropertiesWithComments}, read it in again as a {@link Properties} and check
     * for equivalence.
     * @param password what to look at
     * @return if the password can go to a property file OK
     */
    private boolean passwordSavesOK(final @Nonnull @NotEmpty String password) {
        final String propertyName="pass";
        try {
            final PropertiesWithComments saveProps = new PropertiesWithComments();
            // init
            saveProps.load(new ByteArrayInputStream(new byte[0]));

            // set up
            saveProps.replaceProperty(propertyName, password);
            final Properties loadProps = new Properties();

            try (final ByteArrayOutputStream saveStream = new ByteArrayOutputStream()) {
                saveProps.store(saveStream);
    
                // reload
                try(final ByteArrayInputStream loadStream = new ByteArrayInputStream(saveStream.toByteArray())) {
                    loadProps.load(loadStream);
                }
            }
            // test
            return password.equals(loadProps.getProperty(propertyName));
        } catch (final IOException e) {
            log.error("Internal error", e);
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleInput(final InputRequest arg0) {
        while (true) {
            System.console().printf("%s", arg0.getPrompt());
            System.console().flush();
            char[] result  = System.console().readPassword();
            if (null == result || result.length == 0) {
                System.console().printf("Password cannot be zero length\n");
                continue;
            }
            final String firstPass = String.copyValueOf(result);
            assert firstPass != null;
            if (!passwordSavesOK(firstPass)) {
                System.console().printf("Password contains unsafe characters\n");
                continue;
            }
            System.console().printf("Re-enter password: ");
            System.console().flush();
            result  = System.console().readPassword();
            if (null == result || result.length == 0) {
                System.console().printf("Password cannot be zero length\n");
                continue;
            }
            final String secondPass = String.copyValueOf(result);
            if (firstPass.equals(secondPass)) {
                arg0.setInput(firstPass);
                return;
            }
            System.console().printf("Passwords did not match\n");
        }
    }
    
}