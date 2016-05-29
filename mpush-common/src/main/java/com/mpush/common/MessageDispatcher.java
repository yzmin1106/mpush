/*
 * (C) Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *   ohun@live.cn (夜色)
 */

package com.mpush.common;

import com.mpush.api.MessageHandler;
import com.mpush.api.PacketReceiver;
import com.mpush.api.connection.Connection;
import com.mpush.api.protocol.Command;
import com.mpush.api.protocol.Packet;
import com.mpush.common.message.ErrorMessage;
import com.mpush.tools.common.Profiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.mpush.common.ErrorCode.DISPATCH_ERROR;
import static com.mpush.common.ErrorCode.UNSUPPORTED_CMD;

/**
 * Created by ohun on 2015/12/22.
 *
 * @author ohun@live.cn
 */
public final class MessageDispatcher implements PacketReceiver {
    public static final Logger LOGGER = LoggerFactory.getLogger(MessageDispatcher.class);
    private final Map<Byte, MessageHandler> handlers = new HashMap<>();

    public void register(Command command, MessageHandler handler) {
        handlers.put(command.cmd, handler);
    }

    @Override
    public void onReceive(Packet packet, Connection connection) {
        MessageHandler handler = handlers.get(packet.cmd);
        if (handler != null) {
            try {
                Profiler.enter("start handle:" + handler.getClass().getSimpleName());
                handler.handle(packet, connection);
            } catch (Throwable throwable) {
                LOGGER.error("dispatch message ex, packet={}, connect={}, body={}"
                        , packet, connection, Arrays.toString(packet.body), throwable);
                ErrorMessage
                        .from(packet, connection)
                        .setErrorCode(DISPATCH_ERROR)
                        .close();
            } finally {
                Profiler.release();
            }
        } else {
            LOGGER.error("dispatch message failure unsupported cmd, packet={}, connect={}, body={}"
                    , packet, connection);
            ErrorMessage
                    .from(packet, connection)
                    .setErrorCode(UNSUPPORTED_CMD)
                    .close();
        }
    }
}
