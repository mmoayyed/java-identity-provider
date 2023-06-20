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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Nonnull;

/**
 * A version of {@link BufferedOutputStream} which provides some idea of progress.
 */
public class ProgressReportingOutputStream extends BufferedOutputStream {

    /** How much to transfer before we note it.*/
    private static final int PROGRESS_EVERY = 64 * 1024;

    /** How much have we written so far? */
    private int written;

    /** Do we need to output a terminateing newline? */
    private boolean terminate;

    /** Constructor.
     * @param outStream what to bracket.
     */
    public ProgressReportingOutputStream(@Nonnull final OutputStream outStream) {
        super(outStream);
    }

    /** Constructor.
     * @param outStream what to bracket.
     * @param size buffer size
     */
    public ProgressReportingOutputStream(@Nonnull final OutputStream outStream, final int size) {
        super(outStream, size);
    }

    /** {@inheritDoc} */
    public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
        super.write(b, off, len);
        written += len;
        if (written > PROGRESS_EVERY) {
            if (System.out != null) {
                System.out.print('.');
                System.out.flush();
                terminate = true;
            }
            written -= PROGRESS_EVERY;
        }
    }

    /** {@inheritDoc} */
    public void close() throws IOException {
        super.close();
        if (terminate) {
            System.out.println();
        }
    }

}