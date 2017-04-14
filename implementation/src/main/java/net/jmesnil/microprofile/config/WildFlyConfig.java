/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package net.jmesnil.microprofile.config;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.Converter;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class WildFlyConfig implements Config{
    private final List<ConfigSource> configSources;
    protected Map<Type, Converter> converters = new HashMap<>();
    private final ClassLoader classLoader;

    WildFlyConfig(List<ConfigSource> configSources, List<Converter<?>> converters, ClassLoader classLoader) {
        this.configSources = configSources;
        this.classLoader = classLoader;
        //FIXME add converters from the parameters
        this.converters.putAll(Converters.ALL_CONVERTERS);
    }

    @Override
    public <T> T getValue(String name, Class<T> aClass) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getValue(name);
            if (value != null && value.length() > 0) {
                return convert(value, aClass);
            }
        }
        throw new NoSuchElementException("Property " + name + "  not found");
    }

    @Override
    public <T> Optional<T> getOptionalValue(String name, Class<T> aClass) {
        for (ConfigSource configSource : configSources) {
            String value = configSource.getValue(name);
            // treat empty value as null
            if (value != null && value.length() > 0) {
                return Optional.of(convert(value, aClass));
            }
        }
        return Optional.empty();
    }

    @Override
    public Iterable<String> getPropertyNames() {
        Set<String> names = new HashSet<>();
        for (ConfigSource configSource : configSources) {
            names.addAll(configSource.getProperties().keySet());
        }
        return names;
    }

    @Override
    public Iterable<ConfigSource> getConfigSources() {
        return configSources;
    }

    public <T> T convert(String value, Class<T> asType) {
        if (value != null) {
            Converter<T> converter = getConverter(asType);
            return converter.convert(value);
        }

        return null;
    }

    private <T> Converter getConverter(Class<T> asType) {
        Converter converter = converters.get(asType);
        if (converter == null) {
            throw new UnsupportedOperationException("No Converter registered for class " + asType);
        }
        return converter;
    }
}