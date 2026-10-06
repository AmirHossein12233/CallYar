package com.callyar.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity
        implements CallYarSocket.Listener {

    private TextView txtUsername;
    private TextView txtStatus;
    private TextView txtOnlineCount;
    private LinearLayout usersContainer;

    private CallYarManager callYarManager;

    private String username;

    private final List<String> onlineUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);

        txtUsername = findViewById(R.id.txtUsername);
        txtStatus = findViewById(R.id.txtStatus);
        txtOnlineCount = findViewById(R.id.txtOnlineCount);
        usersContainer = findViewById(R.id.usersContainer);

        username = getIntent().getStringExtra("username");

        if (username == null || username.trim().isEmpty()) {
            finish();
            return;
        }

        username = username.trim();

        txtUsername.setText(username);
        txtStatus.setText("در حال اتصال به سرور...");

        callYarManager = CallYarManager.getInstance();

        callYarManager.setListener(this);

        connectToServer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (callYarManager != null) {
            callYarManager.setListener(this);

            if (!callYarManager.isConnected()) {
                connectToServer();
            }
        }
    }

    private void connectToServer() {

        if (callYarManager == null) {
            return;
        }

        callYarManager.connect(
                username,
                this
        );
    }

    @Override
    public void onConnected() {

        runOnUiThread(() -> {

            txtStatus.setText("متصل به سرور");

            Toast.makeText(
                    HomeActivity.this,
                    "اتصال برقرار شد",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    public void onUsers(String[] users) {

        runOnUiThread(() -> {

            onlineUsers.clear();

            if (users != null) {

                for (String user : users) {

                    if (user == null) {
                        continue;
                    }

                    String cleanUser = user.trim();

                    if (cleanUser.isEmpty()) {
                        continue;
                    }

                    if (cleanUser.equalsIgnoreCase(username)) {
                        continue;
                    }

                    if (!onlineUsers.contains(cleanUser)) {
                        onlineUsers.add(cleanUser);
                    }
                }
            }

            updateUsersList();
        });
    }

    private void updateUsersList() {

        usersContainer.removeAllViews();

        txtOnlineCount.setText(
                "کاربران آنلاین: " + onlineUsers.size()
        );

        if (onlineUsers.isEmpty()) {

            TextView emptyText = new TextView(this);

            emptyText.setText(
                    "کاربر دیگری آنلاین نیست"
            );

            emptyText.setTextColor(
                    0xFF9AA4B2
            );

            emptyText.setTextSize(16);

            emptyText.setGravity(
                    Gravity.CENTER
            );

            emptyText.setPadding(
                    20,
                    30,
                    20,
                    30
            );

            usersContainer.addView(
                    emptyText
            );

            return;
        }

        for (String user : onlineUsers) {

            addUserView(user);
        }
    }

    private void addUserView(String targetUser) {

        LinearLayout userBox =
                new LinearLayout(this);

        userBox.setOrientation(
                LinearLayout.VERTICAL
        );

        userBox.setGravity(
                Gravity.CENTER
        );

        userBox.setPadding(
                20,
                18,
                20,
                18
        );

        userBox.setBackgroundColor(
                0xFF18202A
        );

        LinearLayout.LayoutParams boxParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        boxParams.setMargins(
                0,
                0,
                0,
                15
        );

        userBox.setLayoutParams(
                boxParams
        );

        TextView nameText =
                new TextView(this);

        nameText.setText(
                "👤 " + targetUser
        );

        nameText.setTextColor(
                0xFFFFFFFF
        );

        nameText.setTextSize(20);

        nameText.setGravity(
                Gravity.CENTER
        );

        userBox.addView(
                nameText
        );

        LinearLayout buttons =
                new LinearLayout(this);

        buttons.setOrientation(
                LinearLayout.HORIZONTAL
        );

        buttons.setGravity(
                Gravity.CENTER
        );

        LinearLayout.LayoutParams buttonsParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        buttonsParams.setMargins(
                0,
                15,
                0,
                0
        );

        buttons.setLayoutParams(
                buttonsParams
        );

        TextView voiceButton =
                createCallButton(
                        "📞 تماس صوتی"
                );

        TextView videoButton =
                createCallButton(
                        "📹 تماس تصویری"
                );

        voiceButton.setOnClickListener(v ->
                startCall(
                        targetUser,
                        "voice"
                )
        );

        videoButton.setOnClickListener(v ->
                startCall(
                        targetUser,
                        "video"
                )
        );

        buttons.addView(
                voiceButton
        );

        buttons.addView(
                videoButton
        );

        userBox.addView(
                buttons
        );

        usersContainer.addView(
                userBox
        );
    }

    private TextView createCallButton(
            String text
    ) {

        TextView button =
                new TextView(this);

        button.setText(text);

        button.setTextColor(
                0xFFFFFFFF
        );

        button.setTextSize(15);

        button.setGravity(
                Gravity.CENTER
        );

        button.setPadding(
                20,
                14,
                20,
                14
        );

        button.setBackgroundColor(
                0xFF2196F3
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        params.setMargins(
                5,
                0,
                5,
                0
        );

        button.setLayoutParams(
                params
        );

        return button;
    }

    private void startCall(
            String targetUser,
            String mode
    ) {

        if (targetUser == null ||
                targetUser.trim().isEmpty()) {

            return;
        }

        targetUser = targetUser.trim();

        if (targetUser.equalsIgnoreCase(username)) {

            Toast.makeText(
                    this,
                    "نمی‌توانی با خودت تماس بگیری",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        if (!callYarManager.isConnected()) {

            Toast.makeText(
                    this,
                    "اتصال به سرور برقرار نیست",
                    Toast.LENGTH_SHORT
            ).show();

            txtStatus.setText(
                    "اتصال به سرور برقرار نیست"
            );

            connectToServer();

            return;
        }

        try {

            JSONObject data =
                    new JSONObject();

            data.put(
                    "mode",
                    mode
            );

            data.put(
                    "caller",
                    username
            );

            data.put(
                    "target",
                    targetUser
            );

            JSONObject call =
                    new JSONObject();

            call.put(
                    "type",
                    "call"
            );

            call.put(
                    "target",
                    targetUser
            );

            call.put(
                    "data",
                    data
            );

            callYarManager.sendJson(
                    call
            );

            Intent intent =
                    new Intent(
                            HomeActivity.this,
                            CallActivity.class
                    );

            intent.putExtra(
                    "username",
                    username
            );

            intent.putExtra(
                    "target",
                    targetUser
            );

            intent.putExtra(
                    "mode",
                    mode
            );

            intent.putExtra(
                    "caller",
                    true
            );

            startActivity(intent);

        } catch (JSONException e) {

            Toast.makeText(
                    this,
                    "خطا در ایجاد تماس",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onMessage(
            String message
    ) {

        if (message == null ||
                message.trim().isEmpty()) {

            return;
        }

        runOnUiThread(() -> {

            try {

                JSONObject json =
                        new JSONObject(message);

                String type =
                        json.optString(
                                "type",
                                ""
                        );

                if (!"call".equals(type)) {
                    return;
                }

                String from =
                        json.optString(
                                "from",
                                ""
                        ).trim();

                if (from.isEmpty()) {
                    return;
                }

                JSONObject data =
                        json.optJSONObject(
                                "data"
                        );

                String mode = "voice";

                if (data != null) {

                    mode =
                            data.optString(
                                    "mode",
                                    "voice"
                            );
                }

                Intent intent =
                        new Intent(
                                HomeActivity.this,
                                CallActivity.class
                        );

                intent.putExtra(
                        "username",
                        username
                );

                intent.putExtra(
                        "target",
                        from
                );

                intent.putExtra(
                        "mode",
                        mode
                );

                intent.putExtra(
                        "caller",
                        false
                );

                startActivity(intent);

            } catch (Exception e) {

                Toast.makeText(
                        HomeActivity.this,
                        "خطا در دریافت تماس",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    @Override
    public void onDisconnected() {

        runOnUiThread(() ->
                txtStatus.setText(
                        "اتصال قطع شد"
                )
        );
    }

    @Override
    public void onError(
            String error
    ) {

        runOnUiThread(() ->
                txtStatus.setText(
                        "خطای اتصال"
                )
        );
    }

    @Override
    protected void onDestroy() {

        if (callYarManager != null) {
            callYarManager.clearListener(this);
        }

        super.onDestroy();
    }
}