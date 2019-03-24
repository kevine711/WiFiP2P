package com.kevinersoy.wifip2p;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.kevinersoy.wifip2p.helper.WifiP2pInfoHelper;
import com.kevinersoy.wifip2p.data.BaseMessage;
import com.kevinersoy.wifip2p.data.ChatMessage;
import com.kevinersoy.wifip2p.connection.ChatClient;
import com.kevinersoy.wifip2p.connection.ChatConnection;
import com.kevinersoy.wifip2p.connection.ChatServer;
import com.kevinersoy.wifip2p.data.LeaveMessage;
import com.kevinersoy.wifip2p.data.JoinMessage;

public class P2pChatService extends Service implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private P2pChatApplication Application;

    public P2pChatService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.Application = (P2pChatApplication) getApplication();
        switch (intent.getAction()) {
            case "STATE_CHANGED":
                this.IsP2pEnabled = intent.getBooleanExtra("P2P_ENABLED", false);
                // update error state
                break;
            case "PEERS_CHANGED":
                Log.d("P2P", "Request peers info");
                this.Application.P2pHandler.Manager.requestPeers(this.Application.P2pHandler.Channel, this);
                break;
            case "CONNECTION_CHANGED":
                Log.d("P2P", "Request conn info");
                this.Application.P2pHandler.Manager.requestConnectionInfo(this.Application.P2pHandler.Channel, this);
                break;
            default:
                break;
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        switch (intent.getAction()) {
            case "PEER_ACTIVITY":
                return this.PeerActivityBinder;
            case "CHAT_ACTIVITY":
                return this.ChatActivityBinder;
            default:
                return null;
        }
    }

    private final PeerBinder PeerActivityBinder = new PeerBinder();
    private final ChatBinder ChatActivityBinder = new ChatBinder();

    public class PeerBinder extends Binder {

        public PeerActivity Activity = null;

        public boolean isRegistered() {
            return this.Activity != null;
        }

        public void registerActivity(PeerActivity activity) {
            this.Activity = activity;
        }

        public void unregisterActivity() {
            this.Activity = null;
        }

        public List<PeerDevice> getDevices() {
            List<PeerDevice> devices = new ArrayList<>();
            for (WifiP2pDevice p2pDevice : P2pChatService.this.Devices) {
                ClientInfo comparable = ClientInfo.GetComparable(p2pDevice.deviceAddress);
                PeerDevice device = new PeerDevice(p2pDevice, P2pChatService.this.Clients.contains(comparable));
                devices.add(device);
            }
            return devices;
        }
    }

    private void updateDevices() {
        if (this.PeerActivityBinder.isRegistered()) {
            this.PeerActivityBinder.Activity.updateDevices();
        } else {
            // inform via notification
        }
    }

    public class ChatBinder extends Binder {

        public ChatActivity Activity = null;

        public boolean isRegistered() {
            return this.Activity != null;
        }

        public void registerActivity(ChatActivity activity) {
            this.Activity = activity;
        }

        public void unregisterActivity() {
            this.Activity = null;
        }

        public int getConnectionState() {
            return 0;
        }

        public List<ClientInfo> getJoinedClients() {
            return P2pChatService.this.Clients;
        }

        public List<BaseMessage> checkoutMessages() {
            int messageCount = P2pChatService.this.Messages.size();
            List<BaseMessage> messages = P2pChatService.this.Messages.subList(0, messageCount - 1);
            P2pChatService.this.Messages.clear();
            return messages;
        }

        public boolean sendMessage(ChatMessage message) {
            if (P2pChatService.this.isConnected()) {
                //P2pChatService.this.Connection.SendMessage(message);
                AsyncTask sendTask = new AsyncTask() {
                    @Override
                    protected Object doInBackground(Object[] objects) {
                        ChatConnection conn = (ChatConnection)objects[0];
                        ChatMessage message = (ChatMessage)objects[1];
                        conn.SendMessage(message);
                        return null;
                    }
                }.execute(P2pChatService.this.Connection, message);
                return true;
            } else {
                return false;
            }
        }
    }

    private int getErrorState() {
        return 0;
    }

    private boolean IsP2pEnabled = false;

    private ChatConnection Connection;

    private boolean isConnected() {
        return ((this.Connection != null) && this.Connection.IsConnected);
    }

    private List<WifiP2pDevice> Devices = new ArrayList<>();

    @Override
    public void onPeersAvailable(WifiP2pDeviceList deviceList) {
        Collection<WifiP2pDevice> foundDevices = deviceList.getDeviceList();
        boolean devicesRemoved = this.Devices.retainAll(foundDevices);
        boolean devicesAdded = this.Devices.addAll(foundDevices);
        if (devicesRemoved || devicesAdded) {
            this.updateDevices();
        }
    }

    private WifiP2pInfo PrevP2pInfo;

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo p2pInfo) {
        Log.d("P2P", "Received info");
        if (!WifiP2pInfoHelper.areInfosEquals(this.PrevP2pInfo, p2pInfo)) {
            Log.d("P2P", "New info");
            // stop possible previous connection
            if (this.Connection != null) {
                this.Connection.Stop();
            }
            if (p2pInfo != null && p2pInfo.groupFormed) {
                // start new connection
                if (p2pInfo.isGroupOwner) {
                    Log.d("P2P", "I'm master");
                    this.Connection = new ChatServer(this.MessageReceiver);
                    Log.d("P2P", "Server started");
                } else {
                    Log.d("P2P", "I'm servant");
                    ClientInfo info = new ClientInfo("deviceAddress", "user");
                    this.Connection = new ChatClient(p2pInfo.groupOwnerAddress, this.MessageReceiver, info);
                    Log.d("P2P", "Client started");
                }
            }
        }
        this.PrevP2pInfo = p2pInfo;
    }

    private List<ClientInfo> Clients = new ArrayList<>();
    private List<BaseMessage> Messages = new ArrayList<>();

    private ReceiveCallback MessageReceiver = new ReceiveCallback() {
        @Override
        public void ReceiveMessage(BaseMessage message) {
            if (message instanceof JoinMessage) {
                ClientInfo client = ((JoinMessage)message).getClient();
                if (!P2pChatService.this.Clients.contains(client)) {
                    P2pChatService.this.Clients.add(client);
                }
            } else if (message instanceof LeaveMessage) {
                ClientInfo client = ((LeaveMessage)message).getClient();
                if (!P2pChatService.this.Clients.contains(client)) {
                    P2pChatService.this.Clients.remove(client);
                }
            }
            P2pChatService.this.Messages.add(message);
            final String s = message.serialize();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(P2pChatService.this, s, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    private void runOnUiThread(Runnable runnable) {
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
