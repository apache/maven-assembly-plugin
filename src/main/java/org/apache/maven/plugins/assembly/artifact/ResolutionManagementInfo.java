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
package org.apache.maven.plugins.assembly.artifact;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * Helper class used to accumulate scopes and modules (with binaries included) that are used in an assembly, for the
 * purposes of creating an aggregated managed-version map with dependency version conflicts resolved.
 *
 * @author jdcasey
 */
class ResolutionManagementInfo {
    private final LinkedHashSet<Artifact> artifacts = new LinkedHashSet<>();

    Set<Artifact> getArtifacts() {
        return artifacts;
    }

    void addArtifacts(final Set<Artifact> a) {
        for (Artifact artifact : a) {
            addOneArtifact(artifact);
        }
        artifacts.addAll(a);
    }

    private void addOneArtifact(Artifact artifact) {
        for (Artifact existing : artifacts) {
            if (existing.equals(artifact)) {
                if (isScopeUpgrade(artifact, existing)) {
                    artifacts.remove(existing);
                    artifacts.add(artifact);
                    return;
                }
            }
        }
    }

    private boolean isScopeUpgrade(Artifact a, Artifact existing) {
        return scopeValue(a.getScope()) > scopeValue(existing.getScope());
    }

    private int scopeValue(final String scope) {
        if (Artifact.SCOPE_COMPILE.equals(scope)) {
            return 5;
        } else if (Artifact.SCOPE_PROVIDED.equals(scope)) {
            return 4;
        } else if (Artifact.SCOPE_RUNTIME.equals(scope)) {
            return 3;
        } else if (Artifact.SCOPE_SYSTEM.equals(scope)) {
            return 2;
        } else if (Artifact.SCOPE_TEST.equals(scope)) {
            return 1;
        }
        return 0;
    }
}
