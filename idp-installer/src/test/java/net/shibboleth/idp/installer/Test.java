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

package net.shibboleth.idp.installer;

import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import javax.annotation.Nonnull;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.installer.impl.CopyingVisitor;
import net.shibboleth.idp.installer.impl.DeletingVisitor;
import net.shibboleth.idp.installer.impl.InstallerProperties;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;

/**
 *
 */
public class Test {

    /** Class logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(Test.class);

    private static Copy getCopyTask(final Path from, final Path to) {
        final Copy result = new Copy();
        result.setTodir(to.toFile());
        final FileSet fromSet = new FileSet();
        fromSet.setDir(from.toFile());
        result.addFileset(fromSet);
        return result;
    }
    
    /**
     * @param args
     * @throws IOException 
     * @throws ComponentInitializationException 
     */
    public static void main(String[] args) throws IOException, ComponentInitializationException {
        Copy tsk = getCopyTask(Path.of("C:\\Users\\rdw\\Downloads"), Path.of("C:\\Users\\rdw\\Desktop\\fofofo"));
        tsk.setProject(new Project());
        tsk.execute();
    }

}
