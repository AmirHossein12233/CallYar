package com.callyar.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private Button loginButton;

    private SharedPreferences preferences;

    private static final String PREFS_NAME = "CallYarPrefs";
    private static final String KEY_USERNAME = "username";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        // نام کاربری ذخیره‌شده
        String savedUsername = preferences.getString(
                KEY_USERNAME,
                ""
        );

        // اگر قبلاً نام کاربری ذخیره شده
        if (!savedUsername.trim().isEmpty()) {

            Intent intent = new Intent(
                    MainActivity.this,
                    HomeActivity.class
            );

            intent.putExtra(
                    "username",
                    savedUsername
            );

            startActivity(intent);

            finish();

            return;
        }

        // اگر نامی ذخیره نشده، صفحه ورود را نشان بده
        setContentView(R.layout.activity_main);

        usernameEditText =
                findViewById(R.id.usernameEditText);

        loginButton =
                findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {

            String username =
                    usernameEditText
                            .getText()
                            .toString()
                            .trim();

            if (username.isEmpty()) {

                Toast.makeText(
                        MainActivity.this,
                        "لطفاً نام کاربری را وارد کنید",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            // ذخیره نام کاربری
            preferences
                    .edit()
                    .putString(
                            KEY_USERNAME,
                            username
                    )
                    .apply();

            // رفتن به صفحه اصلی
            Intent intent =
                    new Intent(
                            MainActivity.this,
                            HomeActivity.class
                    );

            intent.putExtra(
                    "username",
                    username
            );

            startActivity(intent);

            finish();
        });
    }
}
