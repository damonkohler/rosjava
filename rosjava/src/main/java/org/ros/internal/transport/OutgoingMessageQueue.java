/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.transport;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.ros.concurrent.CancellableLoop;
import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageSerializer;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class OutgoingMessageQueue<T> {

  private static final boolean DEBUG = false;
  private static final Log log = LogFactory.getLog(OutgoingMessageQueue.class);

  private static final int MESSAGE_BUFFER_CAPACITY = 8192;

  private final MessageSerializer<T> serializer;
  private final CircularBlockingQueue<ChannelBuffer> buffers;
  private final ChannelGroup channelGroup;
  private final Writer writer;

  private boolean latchMode;
  private ChannelBuffer latchedBuffer;

  private final class Writer extends CancellableLoop {
    @Override
    public void loop() throws InterruptedException {
      writeMessageToChannel(buffers.take());
    }
  }

  public OutgoingMessageQueue(MessageSerializer<T> serializer,
      ScheduledExecutorService executorService) {
    this.serializer = serializer;
    buffers = new CircularBlockingQueue<ChannelBuffer>(MESSAGE_BUFFER_CAPACITY);
    channelGroup = new DefaultChannelGroup();
    writer = new Writer();
    latchMode = false;
    executorService.execute(writer);
  }

  public void setLatchMode(boolean enabled) {
    latchMode = enabled;
  }

  public boolean getLatchMode() {
    return latchMode;
  }

  private void writeMessageToChannel(ChannelBuffer buffer) {
    if (DEBUG) {
      log.info(String.format("Writing %d bytes.", buffer.readableBytes()));
    }
    channelGroup.write(buffer);
  }

  /**
   * @param message
   *          the message to add to the queue
   */
  public void put(T message) {
    ByteBuffer serializedMessage = serializer.serialize(message);
    ChannelBuffer buffer = ChannelBuffers.wrappedBuffer(serializedMessage);
    try {
      buffers.put(buffer);
    } catch (InterruptedException e) {
      throw new RosRuntimeException(e);
    }
    latchedBuffer = buffer;
  }

  /**
   * Stop writing messages and close all outgoing connections.
   */
  public void shutdown() {
    writer.cancel();
    channelGroup.close().awaitUninterruptibly();
  }

  /**
   * @see CircularBlockingQueue#setLimit(int)
   */
  public void setLimit(int limit) {
    buffers.setLimit(limit);
  }

  /**
   * @see CircularBlockingQueue#getLimit
   */
  public int getLimit() {
    return buffers.getLimit();
  }

  /**
   * @param channel
   *          added to this {@link OutgoingMessageQueue}'s {@link ChannelGroup}
   */
  public void addChannel(Channel channel) {
    if (!writer.isRunning()) {
      log.warn("Failed to add channel. Cannot add channels after shutdown.");
      return;
    }
    if (DEBUG) {
      log.info("Adding channel: " + channel);
    }
    channelGroup.add(channel);
    if (latchMode && latchedBuffer != null) {
      if (DEBUG) {
        log.info("Writing latched message: " + latchedBuffer);
      }
      writeMessageToChannel(latchedBuffer);
    }
  }

  /**
   * @return the number of {@link Channel}s which have been added to this queue
   */
  public int getNumberOfChannels() {
    return channelGroup.size();
  }

  @VisibleForTesting
  ChannelGroup getChannelGroup() {
    return channelGroup;
  }
}
