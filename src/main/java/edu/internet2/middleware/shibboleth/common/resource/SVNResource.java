/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.joda.time.DateTime;
import org.opensaml.util.resource.AbstractFilteredResource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNStatus;
import org.tmatesoft.svn.core.wc.SVNStatusClient;
import org.tmatesoft.svn.core.wc.SVNStatusType;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

/**
 * A resource representing a file fetch from a Subversion server.
 * 
 * This resource will fetch the given resource as follows:
 * <ul>
 * <li>If the revision is a positive number the resource will fetch the resource once during construction time and will
 * never attempt to fetch it again.</li>
 * <li>If the revision number is zero or less, signaling the HEAD revision, every call this resource will cause the
 * resource to check to see if the current working copy is the same as the revision in the remote repository. If it is
 * not the new revision will be retrieved.</li>
 * </ul>
 * 
 * All operations operate on the local working copy.
 * 
 * @since 1.1
 */
public class SVNResource extends AbstractFilteredResource {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SVNResource.class);

    /** SVN Client manager. */
    private final SVNClientManager svnClientMgr;

    /** URL of the remote repository with no trailing slash. */
    private String remoteResource;

    /** Filesystem path for local working copy with no trailing slash. */
    private File localResource;

    /** Revision of the resource. */
    private SVNRevision revision;

    /**
     * Constructor.
     * 
     * @param remote URL of the remote resource
     * @param local file path to where the local working copy will be kept
     * @param revision revision of the resource to retrieve or -1 for HEAD revision
     */
    public SVNResource(String remote, String local, long revision) {
        svnClientMgr = SVNClientManager.newInstance();

        remoteResource = DatatypeHelper.safeTrimOrNullString(remote);
        if (this.remoteResource == null) {
            throw new IllegalArgumentException("Remote repository location may not be null or empty");
        }

        if (DatatypeHelper.isEmpty(local)) {
            throw new IllegalArgumentException("Local repository location may not be null or empty");
        }
        localResource = new File(local);

        if (revision < 0) {
            this.revision = SVNRevision.HEAD;
        } else {
            this.revision = SVNRevision.create(revision);
        }
    }

    /**
     * Constructor.
     * 
     * @param remote URL of the remote resource
     * @param local file path to where the local working copy will be kept
     * @param revision revision of the resource to retrieve or -1 for HEAD revision
     * @param username username used to authenticate to the remote server
     * @param password password used to authenticate to the remote server
     */
    public SVNResource(String remote, String local, long revision, String username, String password) {
        svnClientMgr = SVNClientManager.newInstance();

        if (username != null) {
            if (password != null) {
                BasicAuthenticationManager authnMgr = new BasicAuthenticationManager(username, password);
                svnClientMgr.setAuthenticationManager(authnMgr);
            } else {
                throw new IllegalArgumentException(
                        "Unable to initialize subversion resource.  User name was given but no password was provided.");
            }
        }

        remoteResource = DatatypeHelper.safeTrimOrNullString(remote);
        if (this.remoteResource == null) {
            throw new IllegalArgumentException("Remote resource location may not be null or empty");
        }

        if (DatatypeHelper.isEmpty(local)) {
            throw new IllegalArgumentException("Local repository location may not be null or empty");
        }
        localResource = new File(local);

        if (revision < 0) {
            this.revision = SVNRevision.HEAD;
        } else {
            this.revision = SVNRevision.create(revision);
        }
    }

    /** {@inheritDoc} */
    public boolean exists() throws ResourceException {
        checkoutOrUpdateResource();
        return workingCopyExists();
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws ResourceException {
        checkoutOrUpdateResource();

        try {
            return new FileInputStream(localResource);
        } catch (IOException e) {
            log.error("Unable read local working copy {}", localResource.getAbsolutePath());
            throw new ResourceException("Unable to read local working copy of configuration file "
                    + localResource.getAbsolutePath());
        }
    }

    /** {@inheritDoc} */
    public DateTime getLastModifiedTime() throws ResourceException {
        SVNStatus status = getResourceStatus();
        if (status.getContentsStatus() == SVNStatusType.STATUS_NORMAL) {
            return new DateTime(status.getWorkingContentsDate());
        } else {
            throw new ResourceException("SVN resource is in a state which prevents determining last modified date: "
                    + status.getContentsStatus().toString());
        }
    }

    /** {@inheritDoc} */
    public String getLocation() {
        try {
            return SVNURL.parseURIDecoded(remoteResource).toDecodedString();
        } catch (SVNException e) {
            log.error("Unable to represent remote repository URL as a string");
            return null;
        }
    }

    /**
     * Checks to see if a working copy of the file already exists.
     * 
     * @return true if a working copy of the file exists, false if not
     */
    protected boolean workingCopyExists() {
        return localResource.exists();
    }

    /**
     * Checks out of updates the local resource. Checkout is used of the local resource does not currently exist. An
     * update is performed if the local working copy revision does not match the configured revision. If the configured
     * version is the symbolic version HEAD then an update occurs if the current revision is not the most recent
     * revision within the remote repository.
     * 
     * @throws ResourceException thrown if there is a problem communicating with the remote or local repository
     */
    protected void checkoutOrUpdateResource() throws ResourceException {
        SVNStatus status = getResourceStatus();

        if (!workingCopyExists()) {
            checkoutResource();
            return;
        }

        if (revision == SVNRevision.HEAD) {
            if (!revision.equals(status.getRemoteRevision())) {
                updateResource();
            }
        } else {
            if (!revision.equals(status.getRevision())) {
                updateResource();
            }
        }
    }

    /**
     * Gets the current status of the resource.
     * 
     * @return current status of the resource
     * 
     * @throws ResourceException thrown if there is a problem communicating with the remote or local repository
     */
    protected SVNStatus getResourceStatus() throws ResourceException {
        SVNStatusClient client = svnClientMgr.getStatusClient();

        try {
            return client.doStatus(localResource, true);
        } catch (SVNException e) {
            log.error("Unable to determine current status of SVN resource {}", new Object[] { localResource
                    .getAbsolutePath(), }, e);
            throw new ResourceException("Unable to determine current status of SVN resource");
        }
    }

    /**
     * Checks out the remote resource and stores it locally.
     * 
     * @throws ResourceException thrown if there is a problem communicating with the remote or local repository
     */
    protected void checkoutResource() throws ResourceException {
        SVNUpdateClient client = svnClientMgr.getUpdateClient();
        client.setIgnoreExternals(false);

        try {
            SVNURL remoteResourceLocation = SVNURL.parseURIDecoded(remoteResource);
            client.doCheckout(remoteResourceLocation, localResource, revision, revision, false);
        } catch (SVNException e) {
            log.error("Unable to check out resource {}", new Object[] { remoteResource }, e);
            throw new ResourceException("Unable to check out resource from repository");
        }
    }

    /**
     * Updates the current local working copy, replacing it with the revision from the remote repository.
     * 
     * @throws ResourceException thrown if there is a problem communicating with the remote or local repository
     */
    protected void updateResource() throws ResourceException {
        SVNUpdateClient client = svnClientMgr.getUpdateClient();
        client.setIgnoreExternals(false);

        try {
            client.doUpdate(localResource, revision, false);
        } catch (SVNException e) {
            log.error("Unable to update working copy of resource {} to revision {}", new Object[] { remoteResource,
                    revision.getNumber(), }, e);
            throw new ResourceException("Unable to update working copy of resource");
        }
    }
}