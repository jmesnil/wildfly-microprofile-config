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

package org.wildfly.microprofile.config.inject;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Provider;

import org.wildfly.microprofile.config.WildFlyConfig;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class ConfigInjectionBean<T> implements Bean<T>, PassivationCapable {

    private final static Set<Annotation> QUALIFIERS = new HashSet<>();
    static {
        QUALIFIERS.add(new ConfigPropertyLiteral());
    }

    private final BeanManager bm;
    private final Class clazz;

    /**
     * only access via {@link #getConfig(}
     */
    private Config _config;

    public ConfigInjectionBean(BeanManager bm, Class clazz) {
        this.bm = bm;
        this.clazz = clazz;
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {
        return Collections.EMPTY_SET;
    }

    @Override
    public Class<?> getBeanClass() {
        return ConfigInjectionBean.class;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public T create(CreationalContext<T> context) {
        Set<Bean<?>> beans = bm.getBeans(InjectionPoint.class);
        Bean<?> bean = bm.resolve(beans);
        InjectionPoint ip = (InjectionPoint) bm.getReference(bean, InjectionPoint.class,  context);
        if (ip == null) {
            throw new IllegalStateException("Could not retrieve InjectionPoint");
        }
        Annotated annotated = ip.getAnnotated();
        ConfigProperty configProperty = annotated.getAnnotation(ConfigProperty.class);
        String key = ConfigExtension.getConfigKey(ip, configProperty);
        String defaultValue = configProperty.defaultValue();

        if (annotated.getBaseType() instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) annotated.getBaseType();
            Type rawType = paramType.getRawType();

            // handle Provider<T>
            if (rawType instanceof Class && ((Class) rawType).isAssignableFrom(Provider.class) && paramType.getActualTypeArguments().length == 1) {
                Class clazz = (Class) paramType.getActualTypeArguments()[0]; //X TODO check type again, etc
                return (T) new ConfigValueProvider(getConfig(), key, clazz);
            }
        }
        else {
            Class clazz = (Class) annotated.getBaseType();
            if (defaultValue == null || defaultValue.length() == 0) {
                Object value = getConfig().getValue(key, clazz);
                return (T) getConfig().getValue(key, clazz);
            }
            else {
                Config config = getConfig();
                T value = (T) config.getOptionalValue(key, clazz)
                        .orElse(((WildFlyConfig) config).convert(defaultValue, clazz));
                return value;
            }
        }

        throw new IllegalStateException("unhandled ConfigProperty");
    }

    public Config getConfig() {
        if (_config == null) {
            _config = ConfigProvider.getConfig();
        }
        return _config;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> context) {

    }

    @Override
    public Set<Type> getTypes() {
        return Collections.singleton(clazz);
    }

    @Override
    public Set<Annotation> getQualifiers() {
        return QUALIFIERS;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return Dependent.class;
    }

    @Override
    public String getName() {
        return "ConfigInjectionBean_" + clazz;
    }

    @Override
    public Set<Class<? extends Annotation>> getStereotypes() {
        return Collections.EMPTY_SET;
    }

    @Override
    public boolean isAlternative() {
        return false;
    }

    @Override
    public String getId() {
        return "ConfigInjectionBean_" + clazz;
    }

    private static class ConfigPropertyLiteral extends AnnotationLiteral<ConfigProperty> implements ConfigProperty {
        @Override
        public String name() {
            return "";
        }

        @Override
        public String defaultValue() {
            return "";
        }
    }

    public static class ConfigValueProvider<T> implements Provider<T>, Serializable {
        private transient Config config;
        private final String key;
        private final Class<T> type;

        ConfigValueProvider(Config config, String key, Class<T> type) {
            this.config = config;
            this.key = key;
            this.type = type;
        }

        @Override
        public T get() {
            return (T) config.getValue(key, type);
        }

        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            config = ConfigProviderResolver.instance().getConfig();
        }

    }
}
