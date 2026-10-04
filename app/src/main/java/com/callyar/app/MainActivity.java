package com.callyar.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        EditText name=findViewById(R.id.usernameEditText);
        Button login=findViewById(R.id.loginButton);
        login.setOnClickListener(v -> {
            String n=name.getText().toString().trim();
            if(n.isEmpty()){ name.setError("نام کاربری را وارد کنید"); return; }
            startActivity(new Intent(this, HomeActivity.class).putExtra("username", n));
            finish();
        });
    }
}
