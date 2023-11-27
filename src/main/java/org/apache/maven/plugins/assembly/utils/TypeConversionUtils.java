///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */

package org.apache.maven.plugins.assembly.utils;

import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.slf4j.Logger;

import java.util.List;

public final class TypeConversionUtils {




    private static final int USER_READ = 256;
    private static final int USER_WRITE = 128;
    private static final int USER_EXECUTE = 64;
    private static final int GROUP_READ = 32;
    private static final int GROUP_WRITE = 16;
    private static final int GROUP_EXECUTE = 8;
    private static final int WORLD_READ = 4;
    private static final int WORLD_WRITE = 2;
    private static final int WORLD_EXECUTE = 1;
    private TypeConversionUtils() {}

    public static String[] toStringArray(final List<String> list) {
        return (list != null && !list.isEmpty()) ? list.toArray(new String[0]) : null;
    }
public static int modeToInt(final String mode, final Logger logger) throws AssemblyFormattingException {
        if (mode == null || mode.trim().isEmpty()) {
            return -1;
        }

        try {
            final int value = Integer.parseInt(mode, 8);
            verifyModeSanity(value, logger);
            return value;
        } catch (final NumberFormatException e) {
            throw new AssemblyFormattingException("Failed to parse mode as an octal number: \'" + mode + "\'.", e);
        }
    }

    public static boolean verifyModeSanity(final int mode, final Logger logger) {
        final StringBuilder messages = new StringBuilder();
        messages.append("The mode: ").append(Integer.toString(mode, 8)).append(" contains nonsensical permissions:");


        boolean warn = false;

        warn |= checkAccess(mode, USER_READ, GROUP_READ, WORLD_READ, "read", "Group", "User", messages);
        warn |= checkAccess(mode, USER_WRITE, GROUP_WRITE, WORLD_WRITE, "write", "Group", "User", messages);
        warn |= checkAccess(mode, USER_EXECUTE, GROUP_EXECUTE, WORLD_EXECUTE, "execute/list", "Group", "User", messages);

        if (warn && logger != null) {
            logger.warn(messages.toString());
        }

        return !warn;
    }

    private static boolean checkAccess(
            int mode, int userFlag, int groupFlag, int worldFlag,
            String accessType, String groupLabel, String userLabel, StringBuilder messages) {
        boolean warn = false;

        if ((mode & userFlag) == 0 && (mode & groupFlag) == groupFlag) {
            messages.append(String.format("\n- %s has %s access, but %s does not.", groupLabel, accessType, userLabel));
            warn = true;
        }

        if ((mode & userFlag) == 0 && (mode & worldFlag) == worldFlag) {
            messages.append(String.format("\n- World has %s access, but %s does not.", accessType, userLabel));
            warn = true;
        }

        if ((mode & groupFlag) == 0 && (mode & worldFlag) == worldFlag) {
            messages.append(String.format("\n- World has %s access, but %s does not.", accessType, groupLabel));
            warn = true;
        }

        return warn;
    }
}




