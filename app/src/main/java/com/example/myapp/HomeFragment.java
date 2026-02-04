package com.example.myapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private EditText etIp;
    private EditText etPort;
    private TextView tvStatus;
    private View statusDot;
    private Button btnConnect;
    private NetworkManager networkManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etIp = view.findViewById(R.id.et_ip);
        etPort = view.findViewById(R.id.et_port);
        tvStatus = view.findViewById(R.id.tv_status);
        statusDot = view.findViewById(R.id.view_status_dot);
        btnConnect = view.findViewById(R.id.btn_connect);
        networkManager = NetworkManager.getInstance();

        // Load saved IP/Port
        SharedPreferences prefs = requireActivity().getSharedPreferences("GyroPrefs", Context.MODE_PRIVATE);
        etIp.setText(prefs.getString("ip", "192.168.1.100"));
        etPort.setText(prefs.getString("port", "5005"));

        btnConnect.setOnClickListener(v -> toggleConnection());
        view.findViewById(R.id.btn_mode_mouse).setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireActivity(), MouseActivity.class));
        });
        view.findViewById(R.id.btn_mode_game).setOnClickListener(v -> {
            startActivity(new android.content.Intent(requireActivity(), GameActivity.class));
        });

        updateStatus();
    }

    private void toggleConnection() {
        if (networkManager.isConnected()) {
            // Disconnect
            networkManager.disconnect();
            updateStatus();
        } else {
            // Connect
            String ip = etIp.getText().toString();
            String portStr = etPort.getText().toString();

            if (ip.isEmpty() || portStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter IP and Port", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = requireActivity().getSharedPreferences("GyroPrefs", Context.MODE_PRIVATE)
                    .edit();
            editor.putString("ip", ip);
            editor.putString("port", portStr);
            editor.apply();

            int port = Integer.parseInt(portStr);
            tvStatus.setText("Connecting...");
            btnConnect.setEnabled(false); // Prevent multiple clicks

            networkManager.connect(ip, port, new NetworkManager.ConnectionCallback() {
                @Override
                public void onSuccess() {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Connected!", Toast.LENGTH_SHORT).show();
                        btnConnect.setEnabled(true);
                        updateStatus();
                    });
                }

                @Override
                public void onFailure(String error) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Connection Failed: " + error, Toast.LENGTH_LONG).show();
                        btnConnect.setEnabled(true);
                        updateStatus();
                    });
                }
            });
        }
    }

    private void updateStatus() {
        if (networkManager.isConnected()) {
            tvStatus.setText(R.string.status_connected);
            tvStatus.setTextColor(getResources().getColor(R.color.btn_green, null));
            statusDot.setBackgroundTintList(getResources().getColorStateList(R.color.btn_green, null));
            btnConnect.setText("DISCONNECT");
        } else {
            tvStatus.setText(R.string.status_disconnected);
            tvStatus.setTextColor(getResources().getColor(R.color.text_hint, null));
            statusDot.setBackgroundTintList(getResources().getColorStateList(R.color.text_hint, null));
            btnConnect.setText("CONNECT");
        }
    }
}
