package com.kevinersoy.wifip2p;

import com.kevinersoy.wifip2p.data.BaseMessage;

public interface ReceiveCallback {

    void ReceiveMessage(BaseMessage msg);

}
