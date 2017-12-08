package com.example.nicx.sonus1;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;

public class sonus extends AppCompatActivity {
    private static final int PERMISSION_CODE = 200;
    protected final int MIC_AMPL_HIGH = 32767;
    protected int VOLUME_LOW = 0;
    protected int VOLUME_HIGH = 15;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private static Context context;
    Switch trigger;
    SeekBar volSeekBar;
    ProgressBar currentVolume;
    ProgressBar currentMic;
    AudioManager audioManager;
    ActivityManager activityManager;
    TextView micReading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sonus);

        trigger = (Switch)findViewById(R.id.enable_switch);

        volSeekBar = (SeekBar)findViewById(R.id.volume_seek);

        currentVolume = (ProgressBar)findViewById(R.id.current_volume_progress);

        currentMic = (ProgressBar)findViewById(R.id.current_mic_progress);

        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

        activityManager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);

        sonus.context = getApplicationContext();

        micReading = (TextView) findViewById(R.id.mic_disp);

        int requestCode = PERMISSION_CODE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }

        trigger.setChecked(true);
        currentMic.setMax(MIC_AMPL_HIGH);
        currentVolume.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        currentVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        volSeekBar.setMax(currentVolume.getMax());
        volSeekBar.setProgress(currentVolume.getProgress());
        VOLUME_HIGH = currentVolume.getMax();
        VOLUME_LOW = currentVolume.getProgress();

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (trigger.isChecked() == false) {
                    Toast.makeText(context,"Service Terminated", Toast.LENGTH_LONG).show();
                    onStop();
                } else {
                    Toast.makeText(context,"Service Started", Toast.LENGTH_LONG).show();
                    onStart();
                    /*if (SonusService.getIsServiceRunning() == false) {
                        SonusService.setIsServiceRunning(true);
                        SonusService.setUpdateCurrentVolume(true);
                        startService(new Intent(context, SonusService.class));
                    }*/
                }
            }
        });

        volSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true) {
                    SonusService.setVolumeLow(progress);
                    //currentVolume.setProgress(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    private BroadcastReceiver MICReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                int reading = intent.getIntExtra(SonusService.CURRENT_MIC_PROGRESS, 0);
                currentMic.setProgress(reading);
                double dbReading = intent.getDoubleExtra(SonusService.CURRENT_MIC_DB_PROGRESS, 0.0);;
                DecimalFormat roundOffFormat = new DecimalFormat("#.##");
                dbReading = Double.valueOf(roundOffFormat.format(dbReading));
                micReading.setText(Double.toString(dbReading) + " db");
                int volume = intent.getIntExtra(SonusService.CURRENT_VOLUME_PROGRESS, 0);
                currentVolume.setProgress(volume);
            }
        }
    };

    public static Context getAppContext() {
        return sonus.context;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_CODE:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted  = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) {
            onStop();
        }
        if (!permissionToWriteAccepted ) {
            onStop();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (context != null) {
            IntentFilter intentFilter = new IntentFilter(SonusService.INTENT_TO_UPDATE);
            context.registerReceiver(MICReceiver, intentFilter);
            if (SonusService.getIsServiceRunning() == false) {
                startService(new Intent(this, SonusService.class));
            }
        } else {
            throw new RuntimeException("Unable to start Sonus Demutator");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (trigger.isChecked() == false) {
            //SonusService.setUpdateCurrentVolume(false);
            if (context != null) {
                SonusService.setIsServiceRunning(false);
                unregisterReceiver(MICReceiver);
                stopService(new Intent(this, SonusService.class));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //SonusService.setUpdateCurrentVolume(true);
        if (trigger.isChecked() == false) {
            trigger.setChecked(true);
            if (SonusService.getIsServiceRunning() == false) {
                SonusService.setIsServiceRunning(true);
                startService(new Intent(this, SonusService.class));
            }
        }
    }
}
