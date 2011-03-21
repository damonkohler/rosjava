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

package org.ros.internal.node.response;

import com.google.common.base.Preconditions;

import org.ros.internal.transport.ProtocolDescription;
import org.ros.internal.transport.ProtocolNames;
import org.ros.internal.transport.tcp.TcpRosProtocolDescription;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ProtocolDescriptionResultFactory implements ResultFactory<ProtocolDescription> {
  
  @Override
  public ProtocolDescription create(Object value) {
    List<Object> protocolParameters = Arrays.asList((Object[]) value);
    Preconditions.checkState(protocolParameters.size() == 3);
    Preconditions.checkState(protocolParameters.get(0).equals(ProtocolNames.TCPROS));
    InetSocketAddress address =
        InetSocketAddress.createUnresolved((String) protocolParameters.get(1),
            (Integer) protocolParameters.get(2));
    return new TcpRosProtocolDescription(address);
  }
  
}
