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

package net.shibboleth.idp.installer.impl;

import java.io.PrintStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.opensaml.security.httpclient.HttpClientSecurityParameters;
import org.slf4j.Logger;

import com.beust.jcommander.Parameter;

import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.cli.AbstractCommandLineArguments;
import net.shibboleth.shared.primitive.LoggerFactory;

/**
 * Command line argumebnts for the "install" verb.
 */
public class IdPInstallerArguments extends AbstractCommandLineArguments {

    /** Logger. */
    @Nullable private Logger log;

    /** The PluginId - usually used to drive the update. */
    @Parameter(names= {"-s", "--sourceDir"})
    @Nullable private String sourceDir;

    /** The PluginId - usually used to drive the update. */
    @Parameter(names= {"-t", "--targetDir"})
    @Nullable private String targetDir;

    /** Suppress Prompts. */
    @Parameter(names= {"--noPrompt"})
    private boolean noPrompt;

    /** DNS name of the IdP. */
    @Parameter(names= {"-h", "--hostName"})
    private String hostName;

    /** Scope to assert. */
    @Parameter(names= {"--scope"})
    private String scope;

    /** EntityID to install. */
    @Parameter(names= {"-e", "--entityID"})
    private String entityID;

    /** Password for the keystore we will generate. */
    @Parameter(names= {"-kp", "--keystorePassword"})
    private String keystorePassword;

    /** Password for the sealer we will generate. */
    @Parameter(names= {"-sp", "--sealerPassword"})
    private String sealerPassword;

    /** Import Property File. */
    @Parameter(names= {"--propertyFile"})
    private String propertyFile;

    /** Name for the {@link org.apache.hc.client5.http.classic.HttpClient} . */
    @Parameter(names= {"-hc", "--http-client"})
    @Nullable @NotEmpty private String httpClientName;

    /** Name for the {@link HttpClientSecurityParameters} . */
    @Parameter(names= {"-hs", "--http-security"})
    @Nullable @NotEmpty private String httpClientSecurityParametersName;

    /** {@inheritDoc} */
    public @Nonnull Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(IdPInstallerArguments.class);
        }
        assert log != null;
        return log;
    }

    /** Are we doing an unattended install?
     * 
     * @return whether we're doing an unattended install
     */
    public boolean isUnattended() {
        return noPrompt;
    }

    /** Get the target Directory. 
     *  
     * @return {@link #targetDir}
     */
    @Nullable public String getTargetDirectory() {
        return targetDir;
    }

    /** Get the source Directory.
     *
     * @return {@link #sourceDir}
     */
    @Nullable public String getSourceDir() {
        return sourceDir;
    }

    /** Get the propertyFile.
     * @return {@link #propertyFile}
     */
    @Nullable public String getPropertyFile() {
        return propertyFile;
    }

    /**
     * Return the DNS name for the IdP.
     * 
     * @return the DNS name
     */
    @Nullable public String getHostName() {
        return hostName;
    }

    /** Return the scope to assert.
     * @return Returns the scope.
     */
    public String getScope() {
        return scope;
    }

    /** The entityID to create.
     * @return Returns the entityID.
     */
    public String getEntityID() {
        return entityID;
    }

    /** The key store password.
     * @return Returns the keystorePassword.
     */
    public String getKeystorePassword() {
        return keystorePassword;
    }

    /** The sealer password.
     * @return Returns the sealerPassword.
     */
    public String getSealerPassword() {
        return sealerPassword;
    }

    /**
     * Get bean name for the {@link org.apache.hc.client5.http.classic.HttpClient} (if specified).
     *
     * @return the name or null
     */
    @Nullable @NotEmpty public String getHttpClientName() {
        return httpClientName;
    }

    /**
     * Get bean name for the {@link HttpClientSecurityParameters} (if specified).
     *
     * @return the name or null
     */
    @Nullable @NotEmpty public String getHttpClientSecurityParametersName() {
        return httpClientSecurityParametersName;
    }

    /** {@inheritDoc} */
    @Override public void printHelp(final @Nonnull PrintStream out) {
        out.println("Install");
        out.println("Installs or upgrades an IdP");
        out.println();
        out.println(" Install [options] springConfiguration");
        out.println();
        out.println("      springConfiguration      name of Spring configuration resource to use");
        super.printHelp(out);
        out.println();
        out.println(String.format("  %-22s %s", "-t, --targetDir", "Where to install the IdP or location to update"));
        out.println(String.format("  %-22s %s", "--propertyFile", "Property file containing other parameterization"));
        out.println(String.format("  %-22s %s", "-h, --hostName", "DNS name for the IdP"));
        out.println(String.format("  %-22s %s", "-e, --entityID", "Entity ID for the IdP"));
        out.println(String.format("  %-22s %s", "-kp, --keystorePassword", "Password for the generated KeyStore"));
        out.println(String.format("  %-22s %s", "-sp, --sealerPassword", "Password for the generated Data Sealer"));
        out.println(String.format("  %-22s %s", "--noPrompt", "Unattended Install"));

        out.println(String.format("  %-22s %s", "-hc, --http-client",
                "Bean name for an http client (for Module and Plugin Operations"));
        out.println(String.format("  %-22s %s", "-hs, --http-securityt",
                "Bean name for http security parameters (for Module and Plugin Operations"));
        out.println();
    }

}