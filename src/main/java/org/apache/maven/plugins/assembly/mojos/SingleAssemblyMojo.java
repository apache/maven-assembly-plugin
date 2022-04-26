package org.apache.maven.plugins.assembly.mojos;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * Assemble an application bundle or distribution from an assembly descriptor. This goal is suitable either for binding
 * to the lifecycle or calling directly from the command line (provided all required files are available before the
 * build starts, or are produced by another goal specified before this one on the command line).
 * <br >
 * Note that the parameters {@code descriptors}, {@code descriptorRefs}, and {@code descriptorSourceDirectory}
 * are disjoint, i.e., they are not combined during descriptor location calculation.
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 *
 */
@Mojo( name = "single", requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true )
public class SingleAssemblyMojo
    extends AbstractAssemblyMojo
{
    @Parameter( defaultValue = "${plugin}", readonly = true )
    private PluginDescriptor plugin;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        verifyRemovedParameter( "classifier" );
        verifyRemovedParameter( "descriptor" );
        verifyRemovedParameter( "descriptorId" );
        verifyRemovedParameter( "includeSite" );

        super.execute();
    }

    private void verifyRemovedParameter( String paramName )
    {
        Object pluginConfiguration = plugin.getPlugin().getConfiguration();
        if ( pluginConfiguration instanceof Xpp3Dom )
        {
            Xpp3Dom configDom = (Xpp3Dom) pluginConfiguration;

            if ( configDom.getChild( paramName ) != null )
            {
                throw new IllegalArgumentException( "parameter '" + paramName
                    + "' has been removed from the plugin, please verify documentation." );
            }
        }
    }

    /**
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    @Override
    public MavenProject getProject()
    {
        return project;
    }
}
