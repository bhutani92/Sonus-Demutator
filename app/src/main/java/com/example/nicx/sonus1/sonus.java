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
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
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

        context = getApplicationContext();

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
        micReading.setText(Double.toString(0.0));
        VOLUME_HIGH = currentVolume.getMax();
        VOLUME_LOW = currentVolume.getProgress();

        trigger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (trigger.isChecked() == false) {
                    SonusService.setIsServiceRunning(false);
                    micReading.setText(Double.toString(0.0));
                    currentMic.setProgress(0);
                } else {
                    if (SonusService.getIsServiceRunning() == false) {
                        SonusService.setIsServiceRunning(true);
                        SonusService.setIsBackgroundUI(false);
                        onStart();
                    }
                }
            }
        });

        volSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true) {
                    SonusService.setVolumeLow(progress);
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)){
            System.out.println("Volume Down Pressed");
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volume -= 1;

            if (volume < 0) {
                volume = 0;
            }

            SonusService.setVolumeLow(volume);

            currentVolume.setProgress(volume);
            volSeekBar.setProgress(volume);
            return true;
        } else if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)) {
            System.out.println("Volume Up Pressed");
            int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            volume += 1;

            if (volume > VOLUME_HIGH) {
                volume = VOLUME_HIGH;
            }

            SonusService.setVolumeLow(volume);

            currentVolume.setProgress(volume);
            volSeekBar.setProgress(volume);
            return true;
        }
        return false;
    }

    private BroadcastReceiver MICReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                int reading = intent.getIntExtra(SonusService.CURRENT_MIC_PROGRESS, 0);
                currentMic.setProgress(reading);
                double dbReading = intent.getDoubleExtra(SonusService.CURRENT_MIC_DB_PROGRESS, 0.0);;
                //dbReading = 20 * Math.log10(dbReading / MIC_AMPL_HIGH);
                dbReading = Math.round(dbReading * 100.0) / 100.0;
                micReading.setText(Double.toString(dbReading));
                int volume = intent.getIntExtra(SonusService.CURRENT_VOLUME_PROGRESS, 0);
                currentVolume.setProgress(volume);
            }
        }
    };

    public static Context getAppContext() {
        return context;
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
        System.out.println("OnStart");
        super.onStart();
        if (context != null) {
            IntentFilter intentFilter = new IntentFilter(SonusService.INTENT_TO_UPDATE);
            context.registerReceiver(MICReceiver, intentFilter);
            startService(new Intent(this, SonusService.class));
        } else {
            throw new RuntimeException("Unable to start Sonus Demutator");
        }
    }

    @Override
    public void onStop() {
        System.out.println("OnStop");
        super.onStop();
        SonusService.setIsBackgroundUI(true);
    }

    @Override
    public void onDestroy() {
        System.out.println("OnDestroy");
        super.onDestroy();
        if (trigger.isChecked() == false) {
            if (context != null) {
                SonusService.setIsServiceRunning(false);
                stopService(new Intent(this, SonusService.class));
                if (MICReceiver != null) {
                    context.unregisterReceiver(MICReceiver);
                }
            }
        }
    }

    @Override
    public void onResume() {
        System.out.println("OnResume");
        super.onResume();
        SonusService.setIsBackgroundUI(false);
        trigger.setChecked(true);
    }
}
