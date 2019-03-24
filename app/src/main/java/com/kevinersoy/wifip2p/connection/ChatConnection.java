package com.kevinersoy.wifip2p.connection;

import com.kevinersoy.wifip2p.data.BaseMessage;
import com.kevinersoy.wifip2p.ReceiveCallback;

public abstract class ChatConnection {

    public static final int CHAT_PORT = 8050;

    public boolean IsConnected = false;

    protected boolean ShouldStop = false;

    protected ReceiveCallback Callback;

    protected ChatConnection(ReceiveCallback callback) {
        this.Callback = callback;
    }

    public abstract void SendMessage(BaseMessage message);

    public void Stop() {
        this.IsConnected = false;
        this.ShouldStop = true;
    }

}

