/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.authn;

/**
 * An authentication engine is responsible for authenticating the subject of a request. The actual means by which this
 * may occur can be quite diverse but in general occurs in two stages. The fist stage is the acquisition of the subjects
 * credentials, for example, by request a username/password or find it in the message, by inspecting an X.509
 * certificate available from the message transport, etc. The second step is credential validation which checks to see
 * if a given set of credentials accurately and uniquely identify a subject.
 * 
 * In most cases, a subject is identified by a single credential and thus their identity is established only by means of
 * a single authentication method. However, this is not a requirement. A subject may poses, and an authentication engine
 * may request, more than a single credential and validate the subject's identity using multiple methods.
 */
public interface AuthenticationEngine {

    /**
     * Authenticates the subject according to the rules of the engine implementation.
     * 
     * @param context current authentication context
     * 
     * @throws AuthenticationException thrown if the engine encounters an error attempting to authenticate the subject
     */
    public void authenticateSubject(AuthenticationContext context) throws AuthenticationException;
}