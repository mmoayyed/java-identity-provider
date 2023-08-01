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

package net.shibboleth.idp.cli;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.Version;
import net.shibboleth.idp.cli.DataSealerArguments.OperationType;
import net.shibboleth.shared.annotation.constraint.NotEmpty;
import net.shibboleth.shared.primitive.LoggerFactory;
import net.shibboleth.shared.security.DataSealer;

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
        assert log!=null;
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
        final String result = Version.getVersion();
        assert result!=null;
        return result;
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
            final String sealerName =  args.getDataSealerName();
            if (sealerName != null) {
                sealer = getApplicationContext().getBean(sealerName, DataSealer.class);
            } else {
                sealer = getApplicationContext().getBean(DataSealer.class);
            }

            final OperationType op = args.getOperation();
            final String arg2 = args.getOtherArgs().get(2);
            assert arg2 != null && op != null;
            switch (op) {
                case WRAP:
                    System.out.println(sealer.wrap(arg2));
                    break;
                case UNWRAP:
                    System.out.println(sealer.unwrap(arg2));
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