package com.callyar.app;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.net.URLEncoder;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class CallYarSocket {

    public interface Listener {
        void onConnected();
        void onUsers(String[] users);
        void onMessage(String message);
        void onDisconnected();
        void onError(String error);
    }

    private static final String SERVER_URL =
            "wss://callyar-server.onrender.com/ws/";

    private final OkHttpClient client;
    private final Handler handler;
    private final Listener listener;

    private WebSocket webSocket;

    private String currentUsername;

    private boolean connected = false;
    private boolean closing = false;

    public CallYarSocket(Listener listener) {

        this.listener = listener;

        client = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .build();

        handler = new Handler(
                Looper.getMainLooper()
        );
    }

    public synchronized void connect(String username) {

        if (username == null) {
            return;
        }

        username = username.trim();

        if (username.isEmpty()) {
            return;
        }

        currentUsername = username;
        closing = false;

        if (webSocket != null) {
            try {
                webSocket.close(
                        1000,
                        "اتصال جدید"
                );
            } catch (Exception ignored) {
            }

            webSocket = null;
        }

        try {

            String encodedUsername =
                    URLEncoder.encode(
                            username,
                            "UTF-8"
                    );

            String url =
                    SERVER_URL + encodedUsername;

            Request request =
                    new Request.Builder()
                            .url(url)
                            .build();

            webSocket =
                    client.newWebSocket(
                            request,
                            new WebSocketListener() {

                                @Override
                                public void onOpen(
                                        WebSocket socket,
                                        Response response
                                ) {

                                    connected = true;

                                    handler.post(() ->
                                            listener.onConnected()
                                    );
                                }

                                @Override
                                public void onMessage(
                                        WebSocket socket,
                                        String text
                                ) {

                                    handler.post(() -> {

                                        try {

                                            JSONObject json =
                                                    new JSONObject(text);

                                            String type =
                                                    json.optString(
                                                            "type"
                                                    );

                                            if ("users".equals(type)) {

                                                String usersText =
                                                        json.optString(
                                                                "users",
                                                                ""
                                                        );

                                                String[] users;

                                                if (usersText
                                                        .trim()
                                                        .isEmpty()) {

                                                    users =
                                                            new String[0];

                                                } else {

                                                    users =
                                                            usersText
                                                                    .split(",");
                                                }

                                                listener.onUsers(users);

                                            } else {

                                                listener.onMessage(
                                                        text
                                                );
                                            }

                                        } catch (Exception e) {

                                            listener.onMessage(
                                                    text
                                            );
                                        }
                                    });
                                }

                                @Override
                                public void onClosing(
                                        WebSocket socket,
                                        int code,
                                        String reason
                                ) {

                                    try {

                                        socket.close(
                                                1000,
                                                null
                                        );

                                    } catch (Exception ignored) {
                                    }
                                }

                                @Override
                                public void onClosed(
                                        WebSocket socket,
                                        int code,
                                        String reason
                                ) {

                                    connected = false;

                                    handler.post(() ->
                                            listener.onDisconnected()
                                    );
                                }

                                @Override
                                public void onFailure(
                                        WebSocket socket,
                                        Throwable throwable,
                                        Response response
                                ) {

                                    connected = false;

                                    String error =
                                            throwable.getMessage();

                                    if (error == null ||
                                            error.trim().isEmpty()) {

                                        error =
                                                "خطای اتصال به سرور";
                                    }

                                    String finalError =
                                            error;

                                    handler.post(() ->
                                            listener.onError(
                                                    finalError
                                            )
                                    );
                                }
                            }
                    );

        } catch (Exception e) {

            connected = false;

            String error =
                    e.getMessage();

            if (error == null ||
                    error.trim().isEmpty()) {

                error = "خطای اتصال";
            }

            listener.onError(error);
        }
    }

    public synchronized boolean isConnected() {
        return connected && webSocket != null;
    }

    public synchronized void send(
            String message
    ) {

        if (message == null ||
                message.trim().isEmpty()) {

            return;
        }

        if (webSocket == null ||
                !connected) {

            return;
        }

        webSocket.send(message);
    }

    public synchronized void close() {

        closing = true;
        connected = false;

        if (webSocket != null) {

            try {

                webSocket.close(
                        1000,
                        "بستن اتصال"
                );

            } catch (Exception ignored) {
            }

            webSocket = null;
        }
    }
}