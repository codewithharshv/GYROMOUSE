package com.example.myapp;

import android.util.Log;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private static NetworkManager instance;
    private String serverIp;
    private int serverPort;
    private DatagramSocket udpSocket;
    private ExecutorService executorService;
    private volatile boolean isConnected = false;

    private NetworkManager() {
        executorService = Executors.newSingleThreadExecutor();
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public void connect(String ip, int port, ConnectionCallback callback) {
        this.serverIp = ip;
        this.serverPort = port;

        executorService.execute(() -> {
            try {
                // Initialize UDP Socket
                if (udpSocket != null && !udpSocket.isClosed()) {
                    udpSocket.close();
                }
                udpSocket = new DatagramSocket();
                udpSocket.setSoTimeout(2000); // 2 second timeout for handshake

                // Send Handshake
                JSONObject json = new JSONObject();
                json.put("type", "handshake");
                byte[] data = json.toString().getBytes();

                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
                        serverPort);
                udpSocket.send(packet);

                // Wait for response
                byte[] buffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(responsePacket);

                // If we get here, we received a response
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                Log.d(TAG, "Handshake response: " + response);

                isConnected = true;
                if (callback != null) {
                    callback.onSuccess();
                }
                Log.d(TAG, "Connected to " + ip + ":" + port);

                // Reset timeout for normal operation (optional, or keep generic if needed)
                udpSocket.setSoTimeout(0);

            } catch (Exception e) {
                isConnected = false;
                Log.e(TAG, "Connection failed", e);
                if (udpSocket != null) {
                    udpSocket.close();
                    udpSocket = null;
                }
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }

    public void disconnect() {
        isConnected = false;
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
            udpSocket = null;
        }
        Log.d(TAG, "Disconnected");
    }

    public void sendMotion(float dx, float dy) {
        if (!isConnected || udpSocket == null)
            return;

        executorService.execute(() -> {
            try {
                // Protocol: JSON {"type": "mouse_motion", "dx": dx, "dy": dy}
                JSONObject json = new JSONObject();
                json.put("type", "mouse_motion");
                json.put("dx", dx);
                json.put("dy", dy);

                byte[] data = json.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
                        serverPort);
                udpSocket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Error sending motion", e);
            }
        });
    }

    public void sendClick(String type) {
        if (!isConnected)
            return;

        executorService.execute(() -> {
            try {
                // Protocol: JSON {"type": "mouse_click", "action": "L" or "R"}
                JSONObject json = new JSONObject();
                json.put("type", "mouse_click");
                json.put("action", type);

                byte[] data = json.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
                        serverPort);
                udpSocket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Error sending click", e);
            }
        });
    }

    public void sendScroll(int amount) {
        if (!isConnected)
            return;

        executorService.execute(() -> {
            try {
                // Protocol: JSON {"type": "mouse_scroll", "amount": amount}
                JSONObject json = new JSONObject();
                json.put("type", "mouse_scroll");
                json.put("amount", amount);

                byte[] data = json.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
                        serverPort);
                udpSocket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Error sending scroll", e);
            }
        });
    }

    public void sendKey(String key) {
        if (!isConnected)
            return;

        executorService.execute(() -> {
            try {
                // Protocol: JSON {"type": "keyboard", "key": key}
                JSONObject json = new JSONObject();
                json.put("type", "keyboard");
                json.put("key", key);

                byte[] data = json.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
                        serverPort);
                udpSocket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Error sending key", e);
            }
        });
    }

    public boolean isConnected() {
        return isConnected;
    }

    public interface ConnectionCallback {
        void onSuccess();

        void onFailure(String error);
    }

    // Generic JSON Sender
    public void sendJson(String jsonString) {
        if (!isConnected)
            return;

        executorService.execute(() -> {
            try {
                byte[] data = jsonString.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, InetAddress.getByName(serverIp),
                        serverPort);
                udpSocket.send(packet);
            } catch (Exception e) {
                Log.e(TAG, "Error sending JSON", e);
            }
        });
    }
}
