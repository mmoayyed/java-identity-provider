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

package net.shibboleth.idp.cli;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.shibboleth.idp.Version;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.security.DataSealer;

/**
 * Command line utility for {@link DataSealer}.
 * 
 * @since 4.1.0
 */
public class DataSealerCLI extends AbstractIdPHomeAwareCommandLine<DataSealerArguments> {

    /** Class logger. */
    @Nullable private Logger log;
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Logger getLogger() {
        if (log == null) {
            log = LoggerFactory.getLogger(DataSealerCLI.class);
        }
        return log;
    }
    
    /** {@inheritDoc} */
    @Override
    @Nonnull protected Class<DataSealerArguments> getArgumentClass() {
        return DataSealerArguments.class;
    }

    /** {@inheritDoc} */
    @Override
    @Nonnull @NotEmpty protected String getVersion() {
        return Version.getVersion();
    }
    
    /** {@inheritDoc} */
    @Override
    protected int doRun(@Nonnull final DataSealerArguments args) {
        final int ret = super.doRun(args);
        if (ret != RC_OK) {
            return ret;
        }
        
        try {
            final DataSealer sealer;
            if (args.getDataSealerName() != null) {
                sealer = getApplicationContext().getBean(args.getDataSealerName(), DataSealer.class);
            } else {
                sealer = getApplicationContext().getBean(DataSealer.class);
            }

            switch (args.getOperation()) {
                case WRAP:
                    System.out.println(sealer.wrap(args.getOtherArgs().get(2)));
                    break;
                case UNWRAP:
                    System.out.println(sealer.unwrap(args.getOtherArgs().get(2)));
                    break;
                    
                default:
                    getLogger().error("Invalid operation");
                    return RC_IO;
            }
            
        } catch (final Exception e) {
            if (args.isVerboseOutput()) {
                getLogger().error("Unable to access DataSealer from Spring context", e);
            } else {
                getLogger().error("Unable to access DataSealer from Spring context", e.getMessage());
            }
            return RC_UNKNOWN;
        }
        
        return RC_OK;
    }

    /**
     * CLI entry point.
     * 
     * @param args arguments
     */
    public static void main(@Nonnull final String[] args) {
        System.exit(new DataSealerCLI().run(args));
    }
    
}