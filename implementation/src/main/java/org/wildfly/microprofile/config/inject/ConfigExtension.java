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

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterDeploymentValidation;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.DeploymentException;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessInjectionPoint;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * CDI Extension to produces Config bean.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class ConfigExtension implements Extension {

    private Set<InjectionPoint> injectionPoints = new HashSet<>();

    public ConfigExtension() {
    }

    private void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<ConfigProducer> configBean = bm.createAnnotatedType(ConfigProducer.class);
        bbd.addAnnotatedType(configBean);
    }

    public void collectConfigProducer(@Observes ProcessInjectionPoint<?, ?> pip) {
        ConfigProperty configProperty = pip.getInjectionPoint().getAnnotated().getAnnotation(ConfigProperty.class);
        if (configProperty != null) {
            injectionPoints.add(pip.getInjectionPoint());
        }
    }

    public void registerConfigProducer(@Observes AfterBeanDiscovery abd, BeanManager bm) {
        // excludes type that are already produced by ConfigProducer
        Set<Class> types = injectionPoints.stream()
                .filter(ip -> ip.getType() instanceof Class
                        && ip.getType() != String.class
                        && ip.getType() != Boolean.class
                        && ip.getType() != Boolean.TYPE
                        && ip.getType() != Integer.class
                        && ip.getType() != Integer.TYPE
                        && ip.getType() != Long.class
                        && ip.getType() != Long.TYPE
                        && ip.getType() != Float.class
                        && ip.getType() != Float.TYPE
                        && ip.getType() != Double.class
                        && ip.getType() != Double.TYPE
                        && ip.getType() != Duration.class
                        && ip.getType() != LocalDate.class
                        && ip.getType() != LocalTime.class
                        && ip.getType() != LocalDateTime.class)
                .map(ip -> (Class) ip.getType())
                .collect(Collectors.toSet());
        types.forEach(type -> abd.addBean(new ConfigInjectionBean(bm, type)));
    }

    public void validate(@Observes AfterDeploymentValidation add, BeanManager bm) {
        List<String> deploymentProblems = new ArrayList<>();

        Config config = ConfigProvider.getConfig();
        for (InjectionPoint injectionPoint : injectionPoints) {
            Type type = injectionPoint.getType();
            ConfigProperty configProperty = injectionPoint.getAnnotated().getAnnotation(ConfigProperty.class);
            if (type instanceof Class) {
                String key = configProperty.name();

                if (!config.getOptionalValue(key, (Class)type).isPresent()) {
                    String defaultValue = configProperty.defaultValue();
                    if (defaultValue == null ||
                            defaultValue.length() == 0 ||
                            defaultValue.equals(ConfigProperty.UNCONFIGURED_VALUE)) {
                        deploymentProblems.add("No Config Value exists for " + key);
                    }
                }
            }
        }

        if (!deploymentProblems.isEmpty()) {
            add.addDeploymentProblem(new DeploymentException("Error while validating Configuration\n"
                    + String.join("\n", deploymentProblems)));
        }

    }
}
