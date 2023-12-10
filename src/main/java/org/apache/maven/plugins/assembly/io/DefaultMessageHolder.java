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
package org.apache.maven.plugins.assembly.io;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Default Message Holder.
 *
 */
class DefaultMessageHolder implements MessageHolder {

    private List<Message> messages = new ArrayList<>();

    private Message currentMessage;

    private int defaultMessageLevel = MessageLevels.LEVEL_INFO;

    private boolean[] messageLevelStates;

    private MessageSink onDemandSink;

    /**
     * Create instance.
     */
    DefaultMessageHolder() {
        this.messageLevelStates = MessageLevels.getLevelStates(MessageLevels.LEVEL_INFO);
    }

    /** {@inheritDoc} */
    public MessageHolder addMessage(CharSequence messagePart, Throwable error) {
        return addMessage(defaultMessageLevel, messagePart, error);
    }

    /**
     * @param level Level.
     * @param messagePart Message part.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    MessageHolder addMessage(int level, CharSequence messagePart, Throwable error) {
        newMessage(level);
        append(messagePart.toString());
        append(error);

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder addMessage(CharSequence messagePart) {
        return addMessage(defaultMessageLevel, messagePart);
    }

    /**
     * @param level level.
     * @param messagePart message part.
     * @return {@link MessageHolder}
     */
    protected MessageHolder addMessage(int level, CharSequence messagePart) {
        newMessage(level);
        append(messagePart.toString());

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder addMessage(Throwable error) {
        return addMessage(defaultMessageLevel, error);
    }

    /**
     * @param level level.
     * @param error {@link Throwable}
     * @return {@link MessageHolder}
     */
    protected MessageHolder addMessage(int level, Throwable error) {
        newMessage(level);
        append(error);

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder append(CharSequence messagePart) {
        if (currentMessage == null) {
            newMessage();
        }

        currentMessage.append(messagePart.toString());

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder append(Throwable error) {
        if (currentMessage == null) {
            newMessage();
        }

        currentMessage.setError(error);

        return this;
    }

    /** {@inheritDoc} */
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /** {@inheritDoc} */
    public MessageHolder newMessage() {
        newMessage(defaultMessageLevel);

        return this;
    }

    /**
     * @param messageLevel message level.
     */
    protected void newMessage(int messageLevel) {
        if (onDemandSink != null && currentMessage != null) {
            renderTo(currentMessage, onDemandSink);
        }

        currentMessage = new Message(messageLevel, onDemandSink);
        messages.add(currentMessage);
    }

    /** {@inheritDoc} */
    public String render() {
        StringBuilder buffer = new StringBuilder();

        int counter = 1;
        for (Iterator<Message> it = messages.iterator(); it.hasNext(); ) {
            Message message = it.next();

            int ml = message.getMessageLevel();

            if (ml >= messageLevelStates.length || ml < 0) {
                ml = MessageLevels.LEVEL_DEBUG;
            }

            if (!messageLevelStates[ml]) {
                continue;
            }

            CharSequence content = message.render();
            String label = MessageLevels.getLevelLabel(message.getMessageLevel());

            if (content.length() > label.length() + 3) {
                buffer.append('[').append(counter++).append("] ");
                buffer.append(content);

                if (it.hasNext()) {
                    buffer.append("\n\n");
                }
            }
        }

        return buffer.toString();
    }

    /** {@inheritDoc} */
    public int size() {
        return messages.size();
    }

    private static final class Message {
        private StringBuffer message = new StringBuffer();

        private Throwable error;

        private final int messageLevel;

        private final MessageSink onDemandSink;

        Message(int messageLevel, MessageSink onDemandSink) {
            this.messageLevel = messageLevel;

            this.onDemandSink = onDemandSink;
        }

        public Message setError(Throwable pError) {
            this.error = pError;
            return this;
        }

        public Message append(CharSequence pMessage) {
            this.message.append(pMessage.toString());
            return this;
        }

        /**
         * @return message level.
         */
        public int getMessageLevel() {
            return messageLevel;
        }

        /**
         * @return Sequence.
         */
        public CharSequence render() {
            StringBuffer buffer = new StringBuffer();

            if (onDemandSink == null) {
                buffer.append('[')
                        .append(MessageLevels.getLevelLabel(messageLevel))
                        .append("] ");
            }
            if (message != null && message.length() > 0) {
                buffer.append(message);

                if (error != null) {
                    buffer.append('\n');
                }
            }

            if (error != null) {
                buffer.append("Error:\n");

                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                error.printStackTrace(pw);

                buffer.append(sw);
            }

            return buffer;
        }
    }

    /** {@inheritDoc} */
    public MessageHolder addDebugMessage(CharSequence messagePart, Throwable error) {
        return addMessage(MessageLevels.LEVEL_DEBUG, messagePart, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addDebugMessage(CharSequence messagePart) {
        return addMessage(MessageLevels.LEVEL_DEBUG, messagePart);
    }

    /** {@inheritDoc} */
    public MessageHolder addDebugMessage(Throwable error) {
        return addMessage(MessageLevels.LEVEL_DEBUG, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addErrorMessage(CharSequence messagePart, Throwable error) {
        return addMessage(MessageLevels.LEVEL_ERROR, messagePart, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addErrorMessage(CharSequence messagePart) {
        return addMessage(MessageLevels.LEVEL_ERROR, messagePart);
    }

    /** {@inheritDoc} */
    public MessageHolder addErrorMessage(Throwable error) {
        return addMessage(MessageLevels.LEVEL_ERROR, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addInfoMessage(CharSequence messagePart, Throwable error) {
        return addMessage(MessageLevels.LEVEL_INFO, messagePart, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addInfoMessage(CharSequence messagePart) {
        return addMessage(MessageLevels.LEVEL_INFO, messagePart);
    }

    /** {@inheritDoc} */
    public MessageHolder addInfoMessage(Throwable error) {
        return addMessage(MessageLevels.LEVEL_INFO, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addSevereMessage(CharSequence messagePart, Throwable error) {
        return addMessage(MessageLevels.LEVEL_SEVERE, messagePart, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addSevereMessage(CharSequence messagePart) {
        return addMessage(MessageLevels.LEVEL_SEVERE, messagePart);
    }

    /** {@inheritDoc} */
    public MessageHolder addSevereMessage(Throwable error) {
        return addMessage(MessageLevels.LEVEL_SEVERE, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addWarningMessage(CharSequence messagePart, Throwable error) {
        return addMessage(MessageLevels.LEVEL_WARNING, messagePart, error);
    }

    /** {@inheritDoc} */
    public MessageHolder addWarningMessage(CharSequence messagePart) {
        return addMessage(MessageLevels.LEVEL_WARNING, messagePart);
    }

    /** {@inheritDoc} */
    public MessageHolder addWarningMessage(Throwable error) {
        return addMessage(MessageLevels.LEVEL_WARNING, error);
    }

    /** {@inheritDoc} */
    public int countDebugMessages() {
        return countMessagesOfType(MessageLevels.LEVEL_DEBUG);
    }

    /** {@inheritDoc} */
    public int countErrorMessages() {
        return countMessagesOfType(MessageLevels.LEVEL_ERROR);
    }

    /** {@inheritDoc} */
    public int countInfoMessages() {
        return countMessagesOfType(MessageLevels.LEVEL_INFO);
    }

    /** {@inheritDoc} */
    public int countMessages() {
        return size();
    }

    /** {@inheritDoc} */
    public int countSevereMessages() {
        return countMessagesOfType(MessageLevels.LEVEL_SEVERE);
    }

    /** {@inheritDoc} */
    public int countWarningMessages() {
        return countMessagesOfType(MessageLevels.LEVEL_WARNING);
    }

    /**
     * @param messageLevel leve.
     * @return number of messages.
     */
    private int countMessagesOfType(int messageLevel) {
        int count = 0;

        for (Message message : messages) {
            if (messageLevel == message.getMessageLevel()) {
                count++;
            }
        }

        return count;
    }

    /** {@inheritDoc} */
    public boolean isDebugEnabled() {
        return messageLevelStates[MessageLevels.LEVEL_DEBUG];
    }

    /** {@inheritDoc} */
    public boolean isErrorEnabled() {
        return messageLevelStates[MessageLevels.LEVEL_ERROR];
    }

    /** {@inheritDoc} */
    public boolean isInfoEnabled() {
        return messageLevelStates[MessageLevels.LEVEL_INFO];
    }

    /** {@inheritDoc} */
    public boolean isSevereEnabled() {
        return messageLevelStates[MessageLevels.LEVEL_SEVERE];
    }

    /** {@inheritDoc} */
    public boolean isWarningEnabled() {
        return messageLevelStates[MessageLevels.LEVEL_WARNING];
    }

    /** {@inheritDoc} */
    public MessageHolder newDebugMessage() {
        if (isDebugEnabled()) {
            newMessage(MessageLevels.LEVEL_DEBUG);
        }

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder newErrorMessage() {
        if (isErrorEnabled()) {
            newMessage(MessageLevels.LEVEL_ERROR);
        }

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder newInfoMessage() {
        if (isInfoEnabled()) {
            newMessage(MessageLevels.LEVEL_INFO);
        }

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder newSevereMessage() {
        if (isSevereEnabled()) {
            newMessage(MessageLevels.LEVEL_SEVERE);
        }

        return this;
    }

    /** {@inheritDoc} */
    public MessageHolder newWarningMessage() {
        if (isWarningEnabled()) {
            newMessage(MessageLevels.LEVEL_WARNING);
        }

        return this;
    }

    /** {@inheritDoc} */
    public void setDebugEnabled(boolean enabled) {
        messageLevelStates[MessageLevels.LEVEL_DEBUG] = enabled;
    }

    /** {@inheritDoc} */
    public void setErrorEnabled(boolean enabled) {
        messageLevelStates[MessageLevels.LEVEL_ERROR] = enabled;
    }

    /** {@inheritDoc} */
    public void setInfoEnabled(boolean enabled) {
        messageLevelStates[MessageLevels.LEVEL_INFO] = enabled;
    }

    /** {@inheritDoc} */
    public void setSevereEnabled(boolean enabled) {
        messageLevelStates[MessageLevels.LEVEL_SEVERE] = enabled;
    }

    /** {@inheritDoc} */
    public void setWarningEnabled(boolean enabled) {
        messageLevelStates[MessageLevels.LEVEL_WARNING] = enabled;
    }

    /** {@inheritDoc} */
    public void flush() {
        if (onDemandSink != null && currentMessage != null) {
            renderTo(currentMessage, onDemandSink);
            currentMessage = null;
        }
    }

    /** {@inheritDoc} */
    public void render(MessageSink sink) {
        for (Message message : messages) {
            renderTo(message, sink);
        }
    }

    /**
     * @param message {@link Message}
     * @param sink {@link MessageSink}
     */
    protected void renderTo(Message message, MessageSink sink) {
        switch (message.getMessageLevel()) {
            case (MessageLevels.LEVEL_SEVERE):
                sink.severe(message.render().toString());
                break;

            case (MessageLevels.LEVEL_ERROR):
                sink.error(message.render().toString());
                break;

            case (MessageLevels.LEVEL_WARNING):
                sink.warning(message.render().toString());
                break;

            case (MessageLevels.LEVEL_INFO):
                sink.info(message.render().toString());
                break;

            default:
                sink.debug(message.render().toString());
        }
    }
}
