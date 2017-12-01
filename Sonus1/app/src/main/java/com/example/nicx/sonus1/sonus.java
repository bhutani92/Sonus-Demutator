package com.example.nicx.sonus1;

import android.content.Context;
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

import java.io.IOException;

public class sonus extends AppCompatActivity {

    private static final int UPDATE_VOLUME_DELAY = 300;
    private static final int MIC_DELAY = 300;
    private static final int PERMISSION_CODE = 200;
    private static final double MIC_FILTER = 0.6;
    private static final int MIC_AMPL_HIGH = 32767;
    private static final int MIC_AMPL_LOW = 0;
    private static int VOLUME_LOW=0;
    private static int VOLUME_HIGH = 15;
    private static double predictedVolume = 0.0;
    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String [] permissions = {"android.permission.RECORD_AUDIO", "android.permission.WRITE_EXTERNAL_STORAGE"};
    boolean updateCurrentVolume;
    boolean getMicReading;
    Switch trigger;
    SeekBar volSeekBar;
    SeekBar micSeekBar;
    ProgressBar currentVolume;
    ProgressBar currentMic;
    Context context;
    AudioManager audioManager;
    MediaRecorder mediaRecorder;
    TextView decibelReading;
    TextView tvTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sonus);

        trigger = (Switch)findViewById(R.id.enable_switch);

        volSeekBar = (SeekBar)findViewById(R.id.volume_seek);

        micSeekBar = (SeekBar)findViewById(R.id.mic_seek);

        currentVolume = (ProgressBar)findViewById(R.id.current_volume_progress);

        currentMic = (ProgressBar)findViewById(R.id.current_mic_progress);

        context = getApplicationContext();

        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

        //decibelReading = (TextView) findViewById(R.id.dbReading);

        int requestCode = PERMISSION_CODE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(permissions, requestCode);
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile("/dev/null");

        try {
            mediaRecorder.prepare();
        } catch(IOException e) {
            e.printStackTrace();
        }

        mediaRecorder.start();

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
                    onStop();
                } else {
                    onStart();
                }
            }
        });

        tvTime = (TextView) findViewById(R.id.mic_disp);
        //getMicReading = true;
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(MIC_DELAY);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                double x = getAmplitudeEMA();
                                int y = (int) x;
                                tvTime.setText(Double.toString((getAmplitudeEMA())) + " db");
                                currentMic.setProgress(y);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();


        volSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser == true) {
                    VOLUME_LOW = progress;
                    currentVolume.setProgress(progress);
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

        } else {
            throw new RuntimeException("Unable to start Sonus Demutator");
        }

        updateCurrentVolume = true;
        Thread volUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                while (updateCurrentVolume) {
                    //currentVolume.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                    //decibelReading.setText(Integer.toString(currentVolume.getProgress()));
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume.getProgress(), AudioManager.AUDIOFOCUS_NONE);
                    try {
                        Thread.sleep(UPDATE_VOLUME_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        volUpdate.start();

        getMicReading = true;
        tvTime = (TextView) findViewById(R.id.mic_disp);
        Thread recorder = new Thread(new Runnable() {
            @Override
            public void run() {while (getMicReading) {
                Double reading = getAmplitudeEMA();
                int volume = (int)Math.round(Math.abs(VOLUME_LOW + (reading/(MIC_AMPL_HIGH-MIC_AMPL_LOW))*(VOLUME_HIGH-VOLUME_LOW)));
                if(volume>VOLUME_LOW){
                    currentVolume.setProgress(volume);
                }
                try {
                    Thread.sleep(MIC_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            }
        });
        recorder.start();
    }

    public double getAmplitude() {
        if (mediaRecorder != null)
            return  (mediaRecorder.getMaxAmplitude());
        else
            return 0;

    }
    public double getAmplitudeEMA() {
        double amplitude =  getAmplitude();
        predictedVolume = MIC_FILTER * amplitude + (1.0 - MIC_FILTER) * predictedVolume;
        return predictedVolume;
    }

    @Override
    public void onStop() {
        super.onStop();
        updateCurrentVolume = false;
        getMicReading = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (trigger.isChecked() == false) {
            updateCurrentVolume = false;
            getMicReading = false;
        } else {
            updateCurrentVolume = true;
            getMicReading = true;
        }
    }
}
