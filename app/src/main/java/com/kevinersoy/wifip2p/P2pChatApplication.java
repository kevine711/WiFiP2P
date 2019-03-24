package com.kevinersoy.wifip2p;

import android.app.Application;

/**
 * Created by Koerfer on 17.02.2016.
 */
public class P2pChatApplication extends Application {

    public WifiP2pHandler P2pHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        this.P2pHandler = new WifiP2pHandler(this);
    }


}
