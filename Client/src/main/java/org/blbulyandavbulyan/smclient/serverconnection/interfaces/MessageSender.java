package org.blbulyandavbulyan.smclient.serverconnection.interfaces;

import org.blbulyandavbulyan.smgeneral.message.Message;

import java.io.IOException;

public interface MessageSender extends Closeable {
    void sendMessage(Message msg) throws IOException;
}
