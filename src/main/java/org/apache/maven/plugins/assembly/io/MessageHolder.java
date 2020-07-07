package org.apache.maven.plugins.assembly.io;

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

/**
 * Message Holder class.
 *
 */
interface MessageHolder
{

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder newMessage();

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder newDebugMessage();

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder newInfoMessage();

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder newWarningMessage();

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder newErrorMessage();

    /**
     * @return {@link MessageHolder}
     */
    MessageHolder newSevereMessage();

    /**
     * @param messagePart message part.
     * @return {@link MessageHolder}
     */
    MessageHolder append( CharSequence messagePart );

    /**
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder append( Throwable error );

    /**
     * @param messagePart Message Part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addMessage( CharSequence messagePart, Throwable error );

    /**
     * @param messagePart message part.
     * @return {@link MessageHolder}
     */
    MessageHolder addMessage( CharSequence messagePart );

    /**
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addMessage( Throwable error );

    /**
     * @param messagePart message part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addDebugMessage( CharSequence messagePart, Throwable error );

    /**
     * @param messagePart messages part.
     * @return {@link MessageHolder}
     */
    MessageHolder addDebugMessage( CharSequence messagePart );

    /**
     * @param error messages part.
     * @return {@link MessageHolder}
     */
    MessageHolder addDebugMessage( Throwable error );

    /**
     * @param messagePart message part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addInfoMessage( CharSequence messagePart, Throwable error );

    /**
     * @param messagePart messages part.
     * @return {@link MessageHolder}
     */
    MessageHolder addInfoMessage( CharSequence messagePart );

    /**
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addInfoMessage( Throwable error );

    /**
     * @param messagePart message part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addWarningMessage( CharSequence messagePart, Throwable error );

    /**
     * @param messagePart message part.
     * @return {@link MessageHolder}
     */
    MessageHolder addWarningMessage( CharSequence messagePart );

    /**
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addWarningMessage( Throwable error );

    /**
     * @param messagePart message part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addErrorMessage( CharSequence messagePart, Throwable error );

    /**
     * @param messagePart message part.
     * @return {@link MessageHolder}
     */
    MessageHolder addErrorMessage( CharSequence messagePart );

    /**
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addErrorMessage( Throwable error );

    /**
     * @param messagePart message part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addSevereMessage( CharSequence messagePart, Throwable error );

    /**
     * @param messagePart message part.
     * @return {@link MessageHolder}
     */
    MessageHolder addSevereMessage( CharSequence messagePart );

    /**
     * @param error The error.
     * @return {@link MessageHolder}
     */
    MessageHolder addSevereMessage( Throwable error );

    /**
     * @return the size.
     */
    int size();

    /**
     * @return count number of messages.
     */
    int countMessages();

    /**
     * @return count number of debug messages.
     */
    int countDebugMessages();

    /**
     * @return count number of info messages
     */
    int countInfoMessages();

    /**
     * @return count number of warning messages
     */
    int countWarningMessages();

    /**
     * @return count number of error messages
     */
    int countErrorMessages();

    /**
     * @return count number of server messages
     */
    int countSevereMessages();

    /**
     * @return true / false.
     */
    boolean isDebugEnabled();

    /**
     * @param enabled enable debug
     */
    void setDebugEnabled( boolean enabled );

    /**
     * @return true if info is enabled false otherwise
     */
    boolean isInfoEnabled();

    /**
     * @param enabled true info enable false otherwise.
     */
    void setInfoEnabled( boolean enabled );

    /**
     * @return true if warning is enabled false otherwise.
     */
    boolean isWarningEnabled();

    /**
     * @param enabled enable warning or disable.
     */
    void setWarningEnabled( boolean enabled );

    /**
     * @return true if error is enabled false otherwise.
     */
    boolean isErrorEnabled();

    /**
     * @param enabled enable error or disable
     */
    void setErrorEnabled( boolean enabled );

    /**
     * @return true if severe is enabled false otherwise.
     */
    boolean isSevereEnabled();

    /**
     * @param enabled enable severe or disable
     */
    void setSevereEnabled( boolean enabled );

    /**
     * @return true if empty, false otherwise
     */
    boolean isEmpty();

    /**
     * @return rendered
     */
    String render();

    /**
     * @param sink {@link MessageSink}
     */
    void render( MessageSink sink );

    /**
     * flush
     */
    void flush();

}
