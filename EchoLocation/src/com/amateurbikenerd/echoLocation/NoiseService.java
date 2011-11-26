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
    private static final int MAX_QUEUE = 30;
    private Thread generator;
    private Thread consumer;
    private short[] buffer;
    private short[] csbuffer;
    private int nativeSampleRate;
    private int bufSize;
    private int channelSize;
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

        // buffer = new short[44100/10];
        // for (int i=0; i<buffer.length; ++i)
        //    buffer[i] = (short)(25000 * java.lang.Math.sin(i*2*java.lang.Math.PI/100));

        buffer = new short[200*441/3]; /* 1/3 of a second stereo; multiple of 200 for below */
        for (int i=0; i<buffer.length; ++i) {
            if (i < 200)
                if (i % 2 == 0)
                    buffer[i] = (short)(25000 * java.lang.Math.sin(i*java.lang.Math.PI/100));
                else
                    buffer[i] = buffer[i-1];
            else
                buffer[i] = buffer[i%200];
        }

        generator = new Thread(new Runnable() {
            public void run() {
                while (! Thread.interrupted()) {
                    if (queue.size() >= MAX_QUEUE) {
                        try{
                            System.out.println(".............. Generator: Queue is full, waiting 50ms");
                            Thread.sleep(50);
                            continue;
                        } catch (Exception e) {}
                    }
                    short[][] kernels = MITData.get(azimuth, 0);
                    queue.add(Convolutions.stereoConvolve(buffer, kernels));
                    //queue.add(buffer);
                }
            }
        });
        generator.start();

        consumer = new Thread(new Runnable() {
            public void run() {
                while (! Thread.interrupted()) {
                    if (queue.size() == 0) {
                        try{
                            System.out.println(".............. Consumer: Queue is empty, waiting 100ms");
                            Thread.sleep(100);
                            continue;
                        } catch (Exception e) {}
                    }
                    short[] buf = queue.remove();
                    if (track != null) track.write(buf, 0, buf.length);
                }
            }
        });
        consumer .start();

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
