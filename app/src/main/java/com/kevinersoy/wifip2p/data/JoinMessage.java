package com.kevinersoy.wifip2p.data;

import com.kevinersoy.wifip2p.ClientInfo;

/**
 * Created by Koerfer on 10.03.2016.
 */
public class JoinMessage extends BaseMessage {

    private ClientInfo Client;

    public JoinMessage(ClientInfo clientInfo) {
        this.Client = clientInfo;
    }

    public ClientInfo getClient() {
        return this.Client;
    }

    public String serialize() {
        return "JOIN" + this.Client.serialize();
    }

    public static JoinMessage Deserialize(String serialized) {
        ClientInfo clientInfo = ClientInfo.Deserialize(serialized);
        return new JoinMessage(clientInfo);
    }

    @Override
    public String toString() {
        return serialize();
    }
}
