package com.callyar.app;

import org.json.JSONObject;

public class CallYarManager {

    private static CallYarManager instance;

    private CallYarSocket socket;

    private String username;

    private CallYarSocket.Listener listener;

    private CallYarManager() {
    }

    public static synchronized CallYarManager getInstance() {

        if (instance == null) {
            instance = new CallYarManager();
        }

        return instance;
    }

    public synchronized void connect(
            String username,
            CallYarSocket.Listener listener
    ) {

        if (username == null ||
                username.trim().isEmpty()) {
            return;
        }

        this.username = username.trim();

        if (listener != null) {
            this.listener = listener;
        }

        // اگر اتصال قبلاً برقرار است، دوباره WebSocket نساز
        if (socket != null &&
                socket.isConnected()) {
            return;
        }

        socket = new CallYarSocket(
                new CallYarSocket.Listener() {

                    @Override
                    public void onConnected() {

                        CallYarSocket.Listener current;

                        synchronized (CallYarManager.this) {
                            current = listener;
                        }

                        if (current != null) {
                            current.onConnected();
                        }
                    }

                    @Override
                    public void onUsers(
                            String[] users
                    ) {

                        CallYarSocket.Listener current;

                        synchronized (CallYarManager.this) {
                            current = listener;
                        }

                        if (current != null) {
                            current.onUsers(users);
                        }
                    }

                    @Override
                    public void onMessage(
                            String message
                    ) {

                        CallYarSocket.Listener current;

                        synchronized (CallYarManager.this) {
                            current = listener;
                        }

                        if (current != null) {
                            current.onMessage(message);
                        }
                    }

                    @Override
                    public void onDisconnected() {

                        CallYarSocket.Listener current;

                        synchronized (CallYarManager.this) {
                            current = listener;
                        }

                        if (current != null) {
                            current.onDisconnected();
                        }
                    }

                    @Override
                    public void onError(
                            String error
                    ) {

                        CallYarSocket.Listener current;

                        synchronized (CallYarManager.this) {
                            current = listener;
                        }

                        if (current != null) {
                            current.onError(error);
                        }
                    }
                }
        );

        socket.connect(
                this.username
        );
    }

    public synchronized void setListener(
            CallYarSocket.Listener listener
    ) {

        this.listener = listener;
    }

    public synchronized void clearListener(
            CallYarSocket.Listener listener
    ) {

        if (this.listener == listener) {
            this.listener = null;
        }
    }

    public synchronized boolean isConnected() {

        return socket != null &&
                socket.isConnected();
    }

    public synchronized void send(
            String message
    ) {

        if (socket == null) {
            return;
        }

        socket.send(message);
    }

    public synchronized void sendJson(
            JSONObject json
    ) {

        if (json == null) {
            return;
        }

        send(
                json.toString()
        );
    }

    public synchronized String getUsername() {

        return username;
    }

    public synchronized void disconnect() {

        if (socket != null) {

            socket.close();

            socket = null;
        }

        username = null;
        listener = null;
    }
}