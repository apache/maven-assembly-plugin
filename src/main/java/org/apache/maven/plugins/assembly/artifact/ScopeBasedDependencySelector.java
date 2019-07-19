package org.apache.maven.plugins.assembly.artifact;

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

import org.apache.maven.artifact.Artifact;
import org.sonatype.aether.collection.DependencyCollectionContext;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;

/**
 * Dependency selector which based on the hierarchy of scopes, calculates which dependency should be included or not.
 *
 */
public class ScopeBasedDependencySelector implements DependencySelector
{

    private boolean compileScope;

    private boolean runtimeScope;

    private boolean testScope;

    private boolean providedScope;

    private boolean systemScope;

    public ScopeBasedDependencySelector( String scope )
    {
        switch ( scope )
        {
            case Artifact.SCOPE_PROVIDED:
                providedScope = true;
                break;
            case Artifact.SCOPE_SYSTEM:
                systemScope = true;
                break;
            case Artifact.SCOPE_COMPILE:
                systemScope = true;
                providedScope = true;
                compileScope = true;
                break;
            case Artifact.SCOPE_RUNTIME:
                compileScope = true;
                runtimeScope = true;
                break;
            case Artifact.SCOPE_COMPILE_PLUS_RUNTIME:
                systemScope = true;
                providedScope = true;
                compileScope = true;
                runtimeScope = true;
                break;
            case Artifact.SCOPE_RUNTIME_PLUS_SYSTEM:
                systemScope = true;
                compileScope = true;
                runtimeScope = true;
                break;
            case Artifact.SCOPE_TEST:
                systemScope = true;
                providedScope = true;
                compileScope = true;
                runtimeScope = true;
                testScope = true;
                break;
            default:
                throw new RuntimeException( "Unknown scope specified:" + scope );
        }
    }

    @Override
    public boolean selectDependency ( Dependency dependency )
    {
        switch ( dependency.getScope() )
        {
            case Artifact.SCOPE_SYSTEM:
                return systemScope;
            case Artifact.SCOPE_PROVIDED:
                return providedScope;
            case Artifact.SCOPE_COMPILE:
                return compileScope;
            case Artifact.SCOPE_RUNTIME:
                return runtimeScope;
            case Artifact.SCOPE_TEST:
                return testScope;
            default:
                throw new RuntimeException( "Unknown scope specified:" + dependency.getScope() + " for " + dependency );
        }
    }

    @Override
    public DependencySelector deriveChildSelector ( DependencyCollectionContext context )
    {
        return this;
    }

}
