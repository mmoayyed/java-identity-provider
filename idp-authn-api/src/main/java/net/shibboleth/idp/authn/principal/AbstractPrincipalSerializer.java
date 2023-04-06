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

package net.shibboleth.idp.authn.principal;

import java.io.Reader;
import java.io.Writer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import net.shibboleth.shared.component.AbstractInitializableComponent;

/**
 * Base class for {@link PrincipalSerializer} implementations.
 * 
 * @param <Type> generic type of serialization
 */
@ThreadSafe
public abstract class AbstractPrincipalSerializer<Type> extends AbstractInitializableComponent
        implements PrincipalSerializer<Type> {

    /** JSON generator factory. */
    @Nonnull private final JsonGeneratorFactory generatorFactory;

    /** JSON reader factory. */
    @Nonnull private final JsonReaderFactory readerFactory;

    /**
     * Constructor.
     */
    public AbstractPrincipalSerializer() {
        final JsonProvider provider = JsonProvider.provider();
        assert provider != null;
        final JsonGeneratorFactory gFactory = provider.createGeneratorFactory(null);
        final JsonReaderFactory rFactory = provider.createReaderFactory(null);
        assert gFactory != null && rFactory != null;
        generatorFactory = gFactory;
        readerFactory = rFactory;
    }

    /**
     * Get a {@link JsonGenerator}, synchronized for thread-safety.
     * 
     * @param writer destination for output
     * 
     * @return a generator
     */
    @Nonnull protected synchronized JsonGenerator getJsonGenerator(@Nonnull final Writer writer) {
        final JsonGenerator jsonGenerator = generatorFactory.createGenerator(writer);
        assert jsonGenerator!=null;
        return jsonGenerator;
    }

    /**
     * Get a {@link JsonReader}, synchronized for thread-safety.
     * 
     * @param reader source of input
     * 
     * @return a reader
     */
    @Nonnull protected synchronized JsonReader getJsonReader(@Nonnull final Reader reader) {
        final JsonReader jr = readerFactory.createReader(reader);
        assert jr!=null;
        return jr;
    }

}