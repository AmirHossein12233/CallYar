package com.callyar.app;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_call);
        boolean video=getIntent().getBooleanExtra("video",true);
        TextView status=findViewById(R.id.status);
        status.setText(video ? "تماس تصویری - آماده اتصال" : "تماس صوتی - آماده اتصال");
        findViewById(R.id.endButton).setOnClickListener(v -> finish());
    }
}
