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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.beust.jcommander.Parameter;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.AbstractIdPHomeAwareCommandLineArguments;
import net.shibboleth.profile.installablecomponent.InstallableComponentVersion;
import net.shibboleth.shared.annotation.constraint.NonnullBeforeExec;
import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.logic.Constraint;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.primitive.StringSupport;

/**
 * Arguments for IdP "Updater" CLI.
 */
public class UpdateIdPArguments extends AbstractIdPHomeAwareCommandLineArguments {

    /** Logger. */
    @Nullable private Logger log;

    /** Brief info about available versions. */
    @Parameter(names= {"-l", "--list"})
    private boolean list;

    /** Where to download. */
    @Parameter(names= {"-d", "--downloadDir"})
    @Nullable private String downloadDir;

    /** Truststore to use for signing. */
    @Parameter(names= {"--truststore"})
    @Nullable private String truststore;

    /** Force download version. */
    @Parameter(names= {"-fd", "--forceDownload"})
    @Nullable private String forceDownloadVersion;
 
    /** location to override the default update location. */
    @Parameter(names= {"--updateURL"})
    @Nullable private String updateURL;

    /** For tests "pretend to be that version. */
    @Parameter(names= {"--pretendVersion"})
    @Nullable private String pretendVersion;

    /** {@link #forceDownloadVersion} as a {@link InstallableComponentVersion}. */
    @Nullable private InstallableComponentVersion updateVersion;

    /** The version to upgrade from as a {@link InstallableComponentVersion}. */
    @NonnullBeforeExec private InstallableComponentVersion fromVersion;
    
    /** The path variant of {@link #downloadDir}, non null if this is a {@link OperationType#DOWLOAD}. */
    @Nullable private Path downloadDirPath;
    
    /** Operation enum. */
    public enum OperationType {
        /** Download a version. */
        DOWLOAD,
        /** List all versions. */
        LIST,
        /** Check Installed version. */
        CHECK,
        /** Unknown. */
        UNKNOWN
    };

    /** What to do. */
    @Nonnull private OperationType operation = OperationType.UNKNOWN;

    /** {@inheritDoc} */
    @Nonnull public Logger getLog() {
        if (log == null) {
            log = LoggerFactory.getLogger(UpdateIdPArguments.class);
        }
        assert log != null;
        return log;
    }

    /** get TrustStore (if specified).
     * 
     * @return the trust store
     */
    @Nullable public String getTruststore() {
        return truststore;
    }

    /** Get the download Directory.
     *
     * Only valid for {@link OperationType#DOWLOAD}
     *
     * @return Returns the download directory
     */
    @Nonnull public Path getDownloadLocation() {
        Constraint.isTrue(operation == OperationType.DOWLOAD, 
                "Can only call getInputFileName on a ");
        assert downloadDirPath != null;
        return downloadDirPath;
    }

    /** Are we doing a List?
     * 
     * @return whether we're doing a list
     */
    public boolean isList() {
        return list;
    }

    /** Are we checking the version or not?
     * @return if we are just checking
     */
    public boolean isCheck() {
        return !isList() && downloadDir == null;
    }

    /** Return the version to update to or null.
     * @return the version or null
     */
    @Nullable public InstallableComponentVersion getUpdateToVersion() {
        return updateVersion;
    }
    
    /** Return the version to update from.
     * @return the version 
     */
    @Nonnull public InstallableComponentVersion getUpdateFromVersion() {
        assert fromVersion!=null;
        return fromVersion;
    }


    /** return the update URL or null.
     * @return null or the value supplied
     */
    @Nonnull public List<String> getUpdateURLs() {
        final String u = updateURL;
        if (u != null) {
            return CollectionSupport.singletonList(u);
        }
        return CollectionSupport.emptyList();
    }

    /**
     * Get operation to perform.
     * @return operation
     */
    @Nonnull public OperationType getOperation() {
        return operation;
    }

// Checkstyle: CyclomaticComplexity OFF
    /** {@inheritDoc} */
    public void validate() throws IllegalArgumentException {
        super.validate();
        
        try {
            if (StringSupport.trimOrNull(pretendVersion) != null) {
                fromVersion = new InstallableComponentVersion(pretendVersion);
            } else {
                final String currentVersion = Version.getVersion();
                if (currentVersion == null) {
                    getLog().error("Could not determine current version.");
                    throw new IllegalArgumentException("Could not determine current version.");
                }
                fromVersion = new InstallableComponentVersion(currentVersion);
            }

            if (StringSupport.trimOrNull(forceDownloadVersion) != null) {
                updateVersion = new InstallableComponentVersion(forceDownloadVersion);
                if (downloadDir == null) {
                    getLog().error("Must specify a downloadDir if forceDownload is specified.");
                    throw new IllegalArgumentException("Must specify a downloadDir if forceDownload is specified.");
                }
            }

        } catch (final NumberFormatException e) {
            getLog().error("Error in version specifier");
            throw new IllegalArgumentException("Error in version specifier", e);
        }

        if (list) {
            operation = OperationType.LIST;
            if (downloadDir !=  null) {
                getLog().error("Cannot List and Dowload in the same operation.");
                throw new IllegalArgumentException("Cannot List and Download in the same operation.");
            }
        } else if (downloadDir != null) {
            operation = OperationType.DOWLOAD;
            downloadDirPath = Path.of(downloadDir);
            if (!Files.exists(downloadDirPath)) {
                getLog().error("Download directory {}, does not exist", downloadDir);
                throw new IllegalArgumentException("Download directory does not exist");
            }
            if (!Files.isDirectory(downloadDirPath)) {
                getLog().error("Download location {}, exists, but is not a directory", downloadDir);
                throw new IllegalArgumentException("Download locaition is not a directory");
            }
        } else {
            operation = OperationType.CHECK;
        }
    }
// Checkstyle: CyclomaticComplexity ON

    /** {@inheritDoc} */
    public void printHelp(final @Nonnull PrintStream out) {
        out.println("Update");
        out.println("Provides a way of enquiring whether updates are available for the IdP");
        out.println();
        out.println("   update [options]");
        out.println();
        super.printHelp(out);
        out.println();
        out.println("With no options displays the update status of the IdP");
        out.println();
        out.println(String.format("  %-22s %s", "-d, --downloadDir <directory>",
                "Download the distribution for an available update"));
        out.println(String.format("  %-22s %s", "-fd, --force-download <file>",
                "Specify the version to be downloaded by -d"));
        out.println();
        out.println(String.format("  %-22s %s", "-l, --list", "list all available versions"));
        out.println();
        out.println(String.format("  %-22s %s", "--updateURL <URL>",
                "Explicit location to look for update information (overrides the default)"));
        out.println();
    }

}