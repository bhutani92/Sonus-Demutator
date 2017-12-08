package com.example.nicx.sonus1;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

public class SonusService extends Service {
    public SonusService() {

    }

    public static final String INTENT_TO_UPDATE = "updateValues";
    public final static String CURRENT_MIC_PROGRESS = "micProgress";
    public final static String CURRENT_MIC_DB_PROGRESS = "micDoubleProgress";
    public final static String CURRENT_VOLUME_PROGRESS = "volumeProgress";
    private static boolean updateCurrentVolume = true;
    private static double predictedVolume = 0.0;
    private static int VOLUME_LOW = 0;
    private static int VOLUME_HIGH = 15;
    private static final int UPDATE_VOLUME_DELAY = 300;
    private static boolean isServiceRunning = false;
    private final double MIC_FILTER = 0.6;
    private final int MIC_AMPL_HIGH = 32767;
    private final int MIC_AMPL_LOW = 0;
    private Intent progressIntent;

    AudioManager audioManager;
    MediaRecorder mediaRecorder;

    public static void setVolumeLow(int volume) {
        VOLUME_LOW = volume;
    }

    public static void setUpdateCurrentVolume(boolean update) {
        updateCurrentVolume = update;
    }

    public static void setIsServiceRunning(boolean running) {
        isServiceRunning = running;
    }

    public static boolean getIsServiceRunning() {
        return isServiceRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public void onCreate() {
        //Toast.makeText(this,"Sonus Demutator is running",Toast.LENGTH_LONG).show();

        progressIntent = new Intent(INTENT_TO_UPDATE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        VOLUME_LOW = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

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
    public int onStartCommand(Intent intent, int flags, int startId){
        //Toast.makeText(this,"Service Started",Toast.LENGTH_LONG).show();

        isServiceRunning = true;
        updateCurrentVolume = true;
        Thread volUpdate = new Thread(new Runnable() {
            @Override
            public void run() {
                while (updateCurrentVolume) {
                    Double reading = getAmplitudeEMA();
                    int micReading = (int)Math.round(reading);
                    int volume = (int)Math.round(Math.abs((reading/(MIC_AMPL_HIGH - MIC_AMPL_LOW))*(VOLUME_HIGH - VOLUME_LOW)));
                    if (volume < VOLUME_LOW) {
                        volume = VOLUME_LOW;
                    }

                    if (volume > VOLUME_HIGH) {
                        volume = VOLUME_HIGH;
                    }

                    if (progressIntent != null) {
                        progressIntent.putExtra(CURRENT_MIC_PROGRESS, micReading);
                        progressIntent.putExtra(CURRENT_MIC_DB_PROGRESS, reading);
                        progressIntent.putExtra(CURRENT_VOLUME_PROGRESS, volume);
                        try {
                            if (sonus.getAppContext() != null){
                                sonus.getAppContext().sendBroadcast(progressIntent);
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                        progressIntent = new Intent(INTENT_TO_UPDATE);
                    }

                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.AUDIOFOCUS_NONE);
                    try {
                        Thread.sleep(UPDATE_VOLUME_DELAY);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        volUpdate.start();
        return START_NOT_STICKY;
    }

    public void onDestroy(){
        //Toast.makeText(this,"Service Terminated",Toast.LENGTH_LONG).show();
        updateCurrentVolume = false;
        isServiceRunning = false;
    }
}
