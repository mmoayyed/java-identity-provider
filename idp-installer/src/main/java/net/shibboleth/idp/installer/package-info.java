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
/**
 * Classes available for installation.
 * <b>NOTE</b> that only {@link net.shibboleth.idp.installer.InstallerSupport}
 * is suitable for programmatic extension.  All other classes are either for use
 * only by this package  (i.e. {@link net.shibboleth.idp.installer.PropertiesWithComments})
 * or are final and have limited public methods (the three classes that do the heavy lifting
 * {@link net.shibboleth.idp.installer.V4Install}, {@link net.shibboleth.idp.installer.CopyDistribution},
 * {@link net.shibboleth.idp.installer.BuildWar}
 */

package net.shibboleth.idp.installer;