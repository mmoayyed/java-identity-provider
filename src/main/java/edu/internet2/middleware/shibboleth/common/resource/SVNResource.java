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
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
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
    private final SVNClientManager clientManager;

    /** URL to the remote repository. */
    private SVNURL remoteRepository;

    /** Directory where the working copy will be kept. */
    private File workingCopy;

    /** Revision of the working copy. */
    private SVNRevision revision;

    /** File, within the working copy, represented by this resource. */
    private String resourceFileName;

    /**
     * Constructor.
     * 
     * @param svnClientMgr manager used to create SVN clients
     * @param repositoryUrl URL of the remote repository
     * @param workingCopyDirectory directory that will serve as the root of the local working copy
     * @param revision revision of the resource to retrieve or -1 for HEAD revision
     * @param resourceFile file, within the working copy, represented by this resource
     */
    public SVNResource(SVNClientManager svnClientMgr, SVNURL repositoryUrl, File workingCopyDirectory, long revision,
            String resourceFile) throws ResourceException {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        if (svnClientMgr == null) {
            log.error("SVN client manager may not be null");
            throw new IllegalArgumentException("SVN client manager may not be null");
        }
        clientManager = svnClientMgr;

        if(repositoryUrl == null){
            throw new IllegalArgumentException("SVN repository URL may not be null");
        }
        remoteRepository = repositoryUrl;
        
        try {
            checkWorkingCopyDirectory(workingCopyDirectory);
            workingCopy = workingCopyDirectory;
        } catch (ResourceException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        if (revision < 0) {
            this.revision = SVNRevision.HEAD;
        } else {
            this.revision = SVNRevision.create(revision);
        }

        resourceFileName = DatatypeHelper.safeTrimOrNullString(resourceFile);
        if (resourceFileName == null) {
            log.error("SVN working copy resource file name may not be null or empty");
            throw new IllegalArgumentException("SVN working copy resource file name may not be null or empty");
        }

        checkoutOrUpdateResource();
        if (!getResourceFile().exists()) {
            log.error("Resource file " + resourceFile + " does not exist in SVN working copy directory "
                    + workingCopyDirectory.getAbsolutePath());
            throw new ResourceException("Resource file " + resourceFile
                    + " does not exist in SVN working copy directory " + workingCopyDirectory.getAbsolutePath());
        }
    }

    /** {@inheritDoc} */
    public boolean exists() throws ResourceException {
        return getResourceFile().exists();
    }

    /** {@inheritDoc} */
    public InputStream getInputStream() throws ResourceException {
        try {
            return applyFilter(new FileInputStream(getResourceFile()));
        } catch (IOException e) {
            String erroMsg = "Unable to read resource file " + resourceFileName + " from local working copy "
                    + workingCopy.getAbsolutePath();
            log.error(erroMsg, e);
            throw new ResourceException(erroMsg, e);
        }
    }

    /** {@inheritDoc} */
    public DateTime getLastModifiedTime() throws ResourceException {
        SVNStatusClient client = clientManager.getStatusClient();
        client.setIgnoreExternals(false);

        try {
            SVNStatus status = client.doStatus(getResourceFile(), false);
            if (status.getContentsStatus() == SVNStatusType.STATUS_NORMAL) {
                return new DateTime(status.getWorkingContentsDate());
            } else {
                String errMsg = "Unable to determine last modified time of resource " + resourceFileName
                        + " within working directory " + workingCopy.getAbsolutePath();
                log.error(errMsg);
                throw new ResourceException(errMsg);
            }
        } catch (SVNException e) {
            String errMsg = "Unable to check status of resource " + resourceFileName + " within working directory "
                    + workingCopy.getAbsolutePath();
            log.error(errMsg, e);
            throw new ResourceException(errMsg, e);
        }
    }

    /** {@inheritDoc} */
    public String getLocation() {
        return remoteRepository.toDecodedString() + "/" + resourceFileName;
    }

    /**
     * Checks that the given file exists, or can be created, is a directory, and is read/writable by this process.
     * 
     * @param directory the directory to check
     * 
     * @throws ResourceException thrown if the file is invalid
     */
    protected void checkWorkingCopyDirectory(File directory) throws ResourceException {
        if (directory == null) {
            log.error("SVN working copy directory may not be null");
            throw new ResourceException("SVN working copy directory may not be null");
        }

        if (!directory.exists()) {
            boolean created = directory.mkdirs();
            if (!created) {
                log.error("SVN working copy direction " + directory.getAbsolutePath()
                        + " does not exist and could not be created");
                throw new ResourceException("SVN working copy direction " + directory.getAbsolutePath()
                        + " does not exist and could not be created");
            }
        }

        if (!directory.isDirectory()) {
            log.error("SVN working copy location " + directory.getAbsolutePath() + " is not a directory");
            throw new ResourceException("SVN working copy location " + directory.getAbsolutePath()
                    + " is not a directory");
        }

        if (!directory.canRead()) {
            log.error("SVN working copy directory " + directory.getAbsolutePath() + " can not be read by this process");
            throw new ResourceException("SVN working copy directory " + directory.getAbsolutePath()
                    + " can not be read by this process");
        }

        if (!directory.canWrite()) {
            log.error("SVN working copy directory " + directory.getAbsolutePath()
                    + " can not be written to by this process");
            throw new ResourceException("SVN working copy directory " + directory.getAbsolutePath()
                    + " can not be written to by this process");
        }
    }

    /**
     * Checks out the resource specified by the {@link #remoteRepository} in to the working copy {@link #workingCopy}.
     * If the working copy is empty than an SVN checkout is performed if the working copy already exists then an SVN
     * update is performed.
     * 
     * @throws ResourceException thrown if there is a problem communicating with the remote repository, the revision
     *             does not exist, or the working copy is unusable
     */
    protected void checkoutOrUpdateResource() throws ResourceException {
        SVNUpdateClient client = clientManager.getUpdateClient();
        client.setIgnoreExternals(false);

        File svnMetadataDir = new File(workingCopy, ".svn");
        if (!svnMetadataDir.exists()) {
            try {
                client.doCheckout(remoteRepository, workingCopy, revision, revision, SVNDepth.INFINITY, true);
            } catch (SVNException e) {
                String errMsg = "Unable to check out revsion " + revision.toString() + " from remote repository "
                        + remoteRepository.toDecodedString() + " to local working directory "
                        + workingCopy.getAbsolutePath();
                log.error(errMsg, e);
                throw new ResourceException(errMsg, e);
            }
        } else {
            try {
                client.doUpdate(workingCopy, revision, SVNDepth.INFINITY, true, true);
            } catch (SVNException e) {
                String errMsg = "Unable to update working copy of resoure " + remoteRepository.toDecodedString()
                        + " in working copy " + workingCopy.getAbsolutePath() + " to revsion " + revision.toString();
                log.error(errMsg, e);
                throw new ResourceException(errMsg, e);
            }
        }
    }

    /**
     * Gets {@link File} for the resource.
     * 
     * @return file for the resource
     */
    protected File getResourceFile() {
        return new File(workingCopy, resourceFileName);
    }
}