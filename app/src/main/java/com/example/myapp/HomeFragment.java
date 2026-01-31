package com.example.myapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        networkManager = NetworkManager.getInstance();

        // Load saved IP/Port
        SharedPreferences prefs = requireActivity().getSharedPreferences("GyroPrefs", Context.MODE_PRIVATE);
        etIp.setText(prefs.getString("ip", "192.168.1.100"));
        etPort.setText(prefs.getString("port", "5005"));

        view.findViewById(R.id.btn_connect).setOnClickListener(v -> connect());
        view.findViewById(R.id.btn_mode_mouse).setOnClickListener(v -> {
            ((MainActivity) getActivity()).navigateTo(new MouseFragment(), true);
        });
        view.findViewById(R.id.btn_mode_game).setOnClickListener(v -> {
            ((MainActivity) getActivity()).navigateTo(new GameFragment(), true);
        });

        updateStatus();
    }

    private void connect() {
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

        networkManager.connect(ip, port, new NetworkManager.ConnectionCallback() { // Turbo correction: Fixed variable
                                                                                   // name
            @Override
            public void onSuccess() {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Connected!", Toast.LENGTH_SHORT).show();
                    updateStatus();
                });
            }

            @Override
            public void onFailure(String error) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Connection Failed: " + error, Toast.LENGTH_LONG).show();
                    updateStatus();
                });
            }
        });
    }

    private void updateStatus() {
        if (networkManager.isConnected()) {
            tvStatus.setText(R.string.status_connected);
            tvStatus.setTextColor(getResources().getColor(R.color.btn_green, null));
            statusDot.setBackgroundTintList(getResources().getColorStateList(R.color.btn_green, null));
        } else {
            tvStatus.setText(R.string.status_disconnected);
            tvStatus.setTextColor(getResources().getColor(R.color.text_hint, null));
            statusDot.setBackgroundTintList(getResources().getColorStateList(R.color.text_hint, null));
        }
    }
}
