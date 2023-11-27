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
package org.apache.maven.plugins.assembly.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugins.assembly.format.AssemblyFormattingException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TypeConversionUtilsTest {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void testModeToInt_InterpretAsOctalWithoutLeadingZero() throws AssemblyFormattingException {
        final int check = Integer.decode("0777");
        final int test = TypeConversionUtils.modeToInt("777", logger);

        assertEquals(check, test);
    }

    @Test
    public void testModeToInt_InterpretValuesWithLeadingZeroAsOctal() throws AssemblyFormattingException {
        final int check = Integer.decode("0777");
        final int test = TypeConversionUtils.modeToInt("0777", logger);

        assertEquals(check, test);
    }

    @Test
    public void testModeToInt_FailOnInvalidOctalValue() {
        try {
            TypeConversionUtils.modeToInt("493", logger);

            fail("'493' is an invalid mode and should trigger an exception.");
        } catch (final AssemblyFormattingException e) {
            // expected.
        }
    }

    @Test
    public void testVerifyModeSanity_WarnOnNonsensicalOctalValue_002() {
        final List<String> messages = new ArrayList<>(2);
        messages.add("World has write access, but User does not.");
        messages.add("World has write access, but Group does not.");

        checkFileModeSanity("002", false, messages);
    }

    @Test
    public void testVerifyModeSanity_WarnOnNonsensicalOctalValue_020() {
        final List<String> messages = new ArrayList<>(1);
        messages.add("Group has write access, but User does not.");

        checkFileModeSanity("020", false, messages);
    }

    @Test
    public void testVerifyModeSanity_ReturnTrueForValidOctalValue_775() {
        checkFileModeSanity("775", true, null);
    }

    public void checkFileModeSanity(
            final String mode, final boolean isSane, final List<String> messagesToCheckIfInsane) {
        Logger logger = mock(Logger.class);
        assertEquals(
                "Mode sanity should be: " + isSane,
                isSane,
                TypeConversionUtils.verifyModeSanity(Integer.parseInt(mode, 8), logger));

        if (!isSane && messagesToCheckIfInsane != null && !messagesToCheckIfInsane.isEmpty()) {
            ArgumentCaptor<String> warnings = ArgumentCaptor.forClass(String.class);
            verify(logger).warn(warnings.capture());
            System.out.println(warnings.getAllValues());
            final String message = warnings.getAllValues().toString();

            for (final String checkMessage : messagesToCheckIfInsane) {

                assertTrue("\'" + checkMessage + "\' is not present in output.",message.contains(checkMessage.replace("\n", " ")));
            }
        }
    }
}
