package org.wildfly.extension.microprofile.config;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SubsystemExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-config";

    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    protected static final PathElement CONFIG_SOURCE_PATH = PathElement.pathElement("config-source");
    protected static final PathElement CONFIG_SOURCE_PROVIDER_PATH = PathElement.pathElement("config-source-provider");
    public static final PathElement CONFIGURATION_PATH = PathElement.pathElement("configuration");

    private static final String RESOURCE_NAME = SubsystemExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, SubsystemExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(SubsytemParser_1_0.INSTANCE);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new SubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        registration.registerSubModel(new ConfigSourceDefinition());
        registration.registerSubModel(new ConfigSourceProviderDefinition());

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();
        if (registerRuntimeOnly) {
            final ManagementResourceRegistration deployments = subsystem.registerDeploymentModel(new SimpleResourceDefinition(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployment")));
            deployments.registerSubModel(new ConfigurationDefinition());
        }

    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, SubsytemParser_1_0.NAMESPACE, SubsytemParser_1_0.INSTANCE);
    }

}
