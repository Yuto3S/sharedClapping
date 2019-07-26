package com.google.location.nearby.apps.rockpaperscissors;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate.Status;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

/** Activity controlling the Rock Paper Scissors game */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SharedClapped";
    private static final String[] REQUIRED_PERMISSIONS =
        new String[] {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WAKE_LOCK,
        };
    private static final int REQUEST_CODE_REQUIRED_PERMISSIONS = 1;
    private static final Strategy STRATEGY = Strategy.P2P_CLUSTER;
    // Our handle to Nearby Connections
    private ConnectionsClient connectionsClient;
    // Our randomly generated name
    private final String codeName = CodenameGenerator.generate();
    final private Connections connections = new Connections();
    private Clap clap;
    private final ArrayList<String> uuidReceived = new ArrayList<>();

    private TextView opponentText;
    private TextView statusText;
    private TextView sharedClapButton;
    private ProgressBar progressBar;
    private TextView connectButton;
    private TextView disconnectButton;
    private ProgressBar playProgressBar;

    public void doClap(String fromEndpointId, String payloadUuid){
        Log.i(TAG, "Propagating to children the UUID: " + payloadUuid);

        Set<String> connectedToEndpointIds = connections.getEndpointIds();
        for(String connectedToEndpointId: connectedToEndpointIds){
            if(!connectedToEndpointId.equals(fromEndpointId)){
                connectionsClient.sendPayload(connectedToEndpointId, Payload.fromBytes(payloadUuid.getBytes()));
            }
        }
        doOwnClap();
    }

    public void doOwnClap(View view){
        doOwnClap();
    }

    public void doOwnClap(){
        if(clap == null)
            clap = new Clap(this, playProgressBar);

        clap.play();
    }

    // Callbacks for receiving payloads
    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String endpointId, Payload payload) {
            String payloadUuid = new String(payload.asBytes());
            Log.i(TAG, "received payload 1 " + new String(payload.asBytes()));

            if (!uuidReceived.contains(payloadUuid)) {
                uuidReceived.add(payloadUuid);
                doClap(endpointId, payloadUuid);
            }
        }

        @Override
        public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
        }

    };

    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback = new EndpointDiscoveryCallback() {
        @Override
        public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
          Log.i(TAG, "onEndpointFound: endpoint found, connecting");
            setStatusText(getString(R.string.status_connecting));
            progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.rgb(0, 255, 255)));
            connectionsClient.requestConnection(codeName, endpointId, connectionLifecycleCallback);
        }

        @Override
        public void onEndpointLost(String endpointId) {}
    };

    // Callbacks for connections to other devices
    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
            Log.i(TAG, "onConnectionInitiated: accepting connection");
            connectionsClient.acceptConnection(endpointId, payloadCallback);
            String endpointName = connectionInfo.getEndpointName();
            connections.addConnection(endpointId, endpointName);
        }

        @Override
        public void onConnectionResult(String endpointId, ConnectionResolution result) {
            if (result.getStatus().isSuccess()) {
                Log.i(TAG, "onConnectionResult: connection successful");
                setStatusText(getString(R.string.status_connected));
                progressBar.setVisibility(View.INVISIBLE);
                sharedClapButton.setEnabled(true);
            } else {
                Log.i(TAG, "onConnectionResult: connection failed");
                connections.removeConnection(endpointId);
                connectionsClient.stopDiscovery();
                connectionsClient.stopAdvertising();
                startDiscovery();
                startAdvertising();
                progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.RED));
                setStatusText(getString(R.string.status_searching));
            }

            setOpponentName(connections.getConnectionsName());
        }

        @Override
        public void onDisconnected(String endpointId) {
            Log.i(TAG, "onDisconnected: disconnected from the opponent");
            connectionsClient.disconnectFromEndpoint(endpointId);
            connections.removeConnection(endpointId);

            if(connections.isEmpty()) {
                progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.RED));
                progressBar.setVisibility(View.VISIBLE);
                setStatusText(getString(R.string.status_searching));
                startDiscovery();
                sharedClapButton.setEnabled(false);
            }
            setOpponentName(connections.getConnectionsName());
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);

        opponentText = findViewById(R.id.opponent_name);
        statusText = findViewById(R.id.status);
        sharedClapButton = findViewById(R.id.shared_clap_button);

        connectButton = findViewById(R.id.connect);
        disconnectButton = findViewById(R.id.disconnect);
        connectButton.setVisibility(View.GONE);

        TextView nameView = findViewById(R.id.name);
        nameView.setText(getString(R.string.codename, codeName));

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.RED));
        playProgressBar = findViewById(R.id.play_progress_bar);
        playProgressBar.setVisibility(View.INVISIBLE);

        connectionsClient = Nearby.getConnectionsClient(this);
        startAdvertising();
        startDiscovery();
        setOpponentName(connections.getConnectionsName());
        setStatusText(getString(R.string.status_searching));
    }

    public void disconnect(View view){
        for(String endpointId: connections.getEndpointIds())
            connectionsClient.disconnectFromEndpoint(endpointId);

        connectionsClient.stopAllEndpoints();
        connectionsClient.stopAdvertising();
        connectionsClient.stopDiscovery();
        connectionsClient = null;
        setStatusText(getString(R.string.status_disconnected));
        progressBar.setVisibility(View.INVISIBLE);
        disconnectButton.setVisibility(View.GONE);
        connectButton.setVisibility(View.VISIBLE);
    }

    public void connect(View view){
        connectionsClient = Nearby.getConnectionsClient(this);
        startAdvertising();
        startDiscovery();
        setStatusText(getString(R.string.status_searching));
        progressBar.setIndeterminateTintList(ColorStateList.valueOf(Color.RED));
        progressBar.setVisibility(View.VISIBLE);
        disconnectButton.setVisibility(View.VISIBLE);
        connectButton.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Permissions.hasPermissions(this, REQUIRED_PERMISSIONS)) {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_REQUIRED_PERMISSIONS);
        }
    }

    /** Handles user acceptance (or denial) of our permission request. */
    @CallSuper
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_CODE_REQUIRED_PERMISSIONS) {
            return;
        }

        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.error_missing_permissions, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }
        recreate();
    }

    /** Starts looking for other players using Nearby Connections. */
    private void startDiscovery() {
        // Note: Discovery may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startDiscovery(getPackageName(), endpointDiscoveryCallback, new DiscoveryOptions.Builder().setStrategy(STRATEGY).build());
    }

    /** Broadcasts our presence using Nearby Connections so other players can find us. */
    private void startAdvertising() {
        // Note: Advertising may fail. To keep this demo simple, we don't handle failures.
        connectionsClient.startAdvertising(codeName, getPackageName(), connectionLifecycleCallback, new AdvertisingOptions.Builder().setStrategy(STRATEGY).build());
    }

    public void sendClap(View view){
        UUID uuid = UUID.randomUUID();
        Log.i("CLAP", "going to send payload" + uuid.toString());

        for(String endpointId: connections.getEndpointIds()){
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(uuid.toString().getBytes()));
        }

        doOwnClap();
    }

    /** Shows a status message to the user. */
    private void setStatusText(String text) {
        statusText.setText(text);
    }

    /** Updates the opponent name on the UI. */
    private void setOpponentName(String opponentName) {
        opponentText.setText(getString(R.string.opponent_name, opponentName));
    }
}
