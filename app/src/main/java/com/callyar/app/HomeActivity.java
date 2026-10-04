package com.callyar.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class HomeActivity extends AppCompatActivity {
    private static final int REQ=10;
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_home);
        String username=getIntent().getStringExtra("username");
        TextView title=findViewById(R.id.title);
        title.setText("سلام "+username);
        Button voice=findViewById(R.id.voiceButton);
        Button video=findViewById(R.id.videoButton);
        voice.setOnClickListener(v -> openCall(false));
        video.setOnClickListener(v -> openCall(true));
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED ||
           ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO},REQ);
    }
    private void openCall(boolean video){
        startActivity(new Intent(this,CallActivity.class).putExtra("video",video));
    }
}
