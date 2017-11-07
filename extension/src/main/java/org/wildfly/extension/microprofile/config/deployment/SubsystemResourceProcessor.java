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

package org.wildfly.extension.microprofile.config.deployment;

import static org.wildfly.extension.microprofile.config.SubsystemExtension.CONFIGURATION_PATH;

import org.jboss.as.controller.PathElement;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.microprofile.config.SubsystemExtension;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SubsystemResourceProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(DeploymentPhaseContext context) throws DeploymentUnitProcessingException {
        final DeploymentResourceSupport deploymentResourceSupport = context.getDeploymentUnit().getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubsystemModel(SubsystemExtension.SUBSYSTEM_NAME);
        final PathElement configuration = PathElement.pathElement(CONFIGURATION_PATH.getKey(), "default");
        deploymentResourceSupport.getDeploymentSubModel(SubsystemExtension.SUBSYSTEM_NAME, configuration);
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {

    }
}
