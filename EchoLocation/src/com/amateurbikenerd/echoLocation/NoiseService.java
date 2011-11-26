package com.amateurbikenerd.echoLocation;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.IBinder;

import com.amateurbikenerd.echoLocation.math.Convolutions;
import com.amateurbikenerd.echoLocation.math.MITData;

public class NoiseService extends Service {
    private java.util.Queue<short[]> queue = new java.util.concurrent.ConcurrentLinkedQueue<short[]>();
    private Thread generator;
    private Thread consumer;
    private int maxWriteAhead = 44100; /* 1 second */
    private short[] toneBuffer;
    private short[] convBuffer;
    private int toneOffset = 0;
    private int convOffset = 0;
    private int lastOffset = 0;
    private AudioTrack track;
    private static int elevation = 0;
    private SensorManager sensorManager;
    private Sensor sensor;
    int azimuth;
    private final SensorEventListener compassListener = new SensorEventListener() {
        public void onSensorChanged(SensorEvent event) {	
            azimuth = 360 - (int) event.values[0];
        }

        @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
        }
    };

    @Override public void onCreate(){
        azimuth = 0;
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(compassListener, sensor, SensorManager.SENSOR_DELAY_UI);

        track = new AudioTrack(AudioManager.STREAM_MUSIC,
                               44100,
                               AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                               AudioFormat.ENCODING_PCM_16BIT,
                               44100*2*5, /* 5 seconds stereo */
                               AudioTrack.MODE_STREAM
                               );

        toneBuffer = new short[200*441 *10];
        convBuffer = new short[200*441 *10];
        for (int i=0; i<toneBuffer.length; ++i) {
            if (i < 200)
                if (i % 2 == 0)
                    toneBuffer[i] = (short)(25000 * java.lang.Math.sin(i*java.lang.Math.PI/100));
                else
                    toneBuffer[i] = toneBuffer[i-1];
            else
                toneBuffer[i] = toneBuffer[i%200];
        }

        generator = new Thread(new Runnable() {
            public void run() {
                while (! Thread.interrupted()) {
                    if (convOffset - lastOffset > maxWriteAhead) {
                        try{
                            System.out.println(".............. Generator: Queue is full, waiting 50ms");
                            Thread.sleep(50);
                            continue;
                        } catch (Exception e) {}
                    }
                    short[][] kernels = MITData.get(azimuth, 0);
                    synchronized(this) {
                        if (convOffset + 44100 >= convBuffer.length) {
                            toneOffset = 0;
                            convOffset = 0;
                            lastOffset = 0;
                        }

                        int inc = Convolutions.stereoConvolveInto(toneBuffer, toneOffset, convBuffer, convOffset, kernels);
                        toneOffset += inc;
                        convOffset += inc;
                    }
                }
            }
        });
        generator.start();

        consumer = new Thread(new Runnable() {
            public void run() {
                while (! Thread.interrupted()) {
                    if (convOffset == lastOffset) {
                        try{
                            System.out.println(".............. Consumer: Queue is empty, waiting 50ms");
                            Thread.sleep(50);
                            continue;
                        } catch (Exception e) {}
                    }
                    synchronized (this) {
                        if (convOffset == lastOffset) {continue;}
                        if (track != null) track.write(convBuffer, lastOffset, convOffset-lastOffset);
                        lastOffset = convOffset;
                    }
                }
            }
        });
        while (convOffset < 44100/2) ; /* Wait for 500ms buffer */
        consumer.start();

        track.play();
        super.onCreate();
    }

    @Override
	public void onDestroy(){
        generator.interrupt();
        consumer.interrupt();
        track.stop();
        track = null;
        sensorManager.unregisterListener(compassListener);
    }

    @Override
        public IBinder onBind(Intent arg0) {
        // TODO Auto-generated method stub
        return null;
    }
}
