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

package org.wildfly.extension.microprofile.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.OBJECT;
import static org.jboss.dmr.ModelType.STRING;
import static org.wildfly.extension.microprofile.config.SubsystemExtension.CONFIGURATION_PATH;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class ConfigurationDefinition extends SimpleResourceDefinition {

    private static final ResourceDescriptionResolver RESOURCE_DESCRIPTION_RESOLVER = SubsystemExtension.getResourceDescriptionResolver(CONFIGURATION_PATH.getKey());

    ConfigurationDefinition() {
        super(new Parameters(CONFIGURATION_PATH, RESOURCE_DESCRIPTION_RESOLVER)
                .setRuntime(true));
    }

    private static OperationDefinition EVAL_PROPERTY = new SimpleOperationDefinitionBuilder("eval-property", RESOURCE_DESCRIPTION_RESOLVER)
            .addParameter(SimpleAttributeDefinitionBuilder.create(NAME, STRING )
                    .setRequired(true)
                    .build())
            .setReplyType(LIST)
            .setReplyValueType(OBJECT)
            .setRuntimeOnly()
            .build();

    private static OperationDefinition LIST_CONFIG_SOURCES = new SimpleOperationDefinitionBuilder("list-config-sources", RESOURCE_DESCRIPTION_RESOLVER)
            .setReplyType(LIST)
            .setReplyValueType(OBJECT)
            .setRuntimeOnly()
            .build();

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(LIST_CONFIG_SOURCES, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress deploymentAddress = context.getCurrentAddress().getParent().getParent();
                String deploymentName = deploymentAddress.getLastElement().getValue();
                Config config;
                try {
                    config = getConfig(context.getServiceRegistry(false), deploymentName);
                } catch (ModuleLoadException e) {
                    throw new OperationFailedException("Could not load configuration for deployment " + deploymentName);
                }

                ModelNode result = context.getResult();
                result.setEmptyList();

                for (ConfigSource cs : config.getConfigSources()) {
                    ModelNode m = new ModelNode();
                    m.get("name").set(cs.getName());
                    m.get("ordinal").set(cs.getOrdinal());
                    result.add(m);
                }
            }
        });
        resourceRegistration.registerOperationHandler(EVAL_PROPERTY, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                PathAddress deploymentAddress = context.getCurrentAddress().getParent().getParent();
                String deploymentName = deploymentAddress.getLastElement().getValue();
                Config config;
                try {
                    config = getConfig(context.getServiceRegistry(false), deploymentName);
                } catch (ModuleLoadException e) {
                    throw new OperationFailedException("Could not load configuration for deployment " + deploymentName);
                }

                ModelNode result = context.getResult();
                result.setEmptyList();

                String propName = operation.get(NAME).asString();
                for (ConfigSource cs : config.getConfigSources()) {
                    // skip the config source if the property name is not found in it
                    if (propName != null && !cs.getPropertyNames().contains(propName)) {
                        continue;
                    }
                    ModelNode m = new ModelNode();
                    m.get("value").set(cs.getValue(propName));
                    m.get("config-source", "name").set(cs.getName());
                    m.get("config-source", "ordinal").set(cs.getOrdinal());
                    result.add(m);
                }
            }
        });
    }

    private static Config getConfig(ServiceRegistry serviceRegistry, String deploymentName) throws ModuleLoadException, OperationFailedException {
        String moduleName = "deployment." + deploymentName;
        ServiceController<?> service = serviceRegistry.getService(Services.JBOSS_SERVICE_MODULE_LOADER);
        if (service == null) {
            throw new OperationFailedException("Could not find Service Module Loader");
        }
        ServiceModuleLoader moduleLoader = ServiceModuleLoader.class.cast(service.getValue());
        Module module = moduleLoader.loadModule(moduleName);
        // FIXME this should work with both Eclipse MicroProfile and JSR Config APIs
        Config config = ConfigProvider.getConfig(module.getClassLoader());
        return config;
    }
}