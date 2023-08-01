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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

import javax.annotation.Nonnull;

import com.beust.jcommander.JCommander;

import net.shibboleth.shared.annotation.constraint.NotEmpty;


/**
 * Entry point for command line attribute utility.
 */
public final class CLI {

    /** Name of system property for command line argument class. */
    @Nonnull @NotEmpty public static final String ARGS_PROPERTY = "net.shibboleth.idp.cli.arguments";
    
    /** Constructor. */
    private CLI() {
        
    }

    /**
     * Command line entry point.
     * 
     * @param args  command line arguments
     * @throws SecurityException from the object construction
     * @throws ReflectiveOperationException from the object construction
     * @throws IllegalArgumentException from the object construction
     */
    public static void main(@Nonnull final String[] args) throws ReflectiveOperationException,
        SecurityException, IllegalArgumentException {

        // Get name of parameter class to load using system property.
        final String argType = System.getProperty(ARGS_PROPERTY);
        if (argType == null) {
            errorAndExit(ARGS_PROPERTY + " system property not set");
        }

        CommandLineArguments argObject = null;
        
        try {
            final Constructor<?> construct = Class.forName(argType).getConstructor();
            final Object obj = construct.newInstance();
            if (!(obj instanceof CommandLineArguments)) {
                errorAndExit("Argument class was not of the correct type");
            }
            argObject = (CommandLineArguments) obj;
            final JCommander jc = new JCommander(argObject);
            jc.parse(args);
            if (argObject.isUsage()) {
                jc.usage();
                return;
            }
        } catch (final ClassNotFoundException e) {
            errorAndExit("Argument class " + argType + " not found ");
        } catch (final InstantiationException | IllegalAccessException e) {
            final String msg = e.getMessage();
            assert msg != null;
            errorAndExit(msg);
        }
        assert argObject != null;
        
        try {
            argObject.validate();
        } catch (final IllegalArgumentException e) {
            final String msg = e.getMessage();
            assert msg != null;
            errorAndExit(msg);
        }
        
        doRequest(argObject);
    }

    /**
     * Make a request using the arguments established.
     * 
     * @param args  the populated command line arguments
     */
    private static void doRequest(@Nonnull final CommandLineArguments args) {
        URL url = null;
        try {
            url = args.buildURL();
            final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            if (args.getMethod() != null) {
                connection.setRequestMethod(args.getMethod());
            }
            
            final String authorization = args.getBasicAuthHeader();
            if (authorization != null) {
                connection.setRequestProperty("Authorization", authorization);
            }
            
            final Map<String,String> headers = args.getHeaders();
            if (headers != null) {
                for (final Map.Entry<String,String> h : headers.entrySet()) {
                    connection.setRequestProperty(h.getKey(), h.getValue());
                }
            }
            
            try (final InputStream stream = connection.getInputStream()) {
                try (final InputStreamReader reader = new InputStreamReader(stream)) {
                    try (final BufferedReader in = new BufferedReader(reader)) {
                        String line;
                        while((line = in.readLine()) != null) {
                            System.out.println(line);
                        }
                    }
                }
            }
        } catch (final MalformedURLException|ProtocolException e) {
            final String msg = e.getMessage();
            assert msg != null;
            errorAndExit(msg);
        } catch (final IOException e) {
            errorAndExit((url != null ? "(" + url.toString() + ") " : "") + e.getMessage());
        }
    }
    
    /**
     * Logs, as an error, the error message and exits the program.
     * 
     * @param errorMessage error message
     */
    private static void errorAndExit(@Nonnull final String errorMessage) {
        System.err.println(errorMessage);
        System.exit(1);
    }
    
}