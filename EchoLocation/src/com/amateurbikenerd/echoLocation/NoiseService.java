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
    private java.util.Queue<short[]> bufferQueue = new java.util.concurrent.ConcurrentLinkedQueue<short[]>();
    private java.util.Queue<short[]> outputQueue = new java.util.concurrent.ConcurrentLinkedQueue<short[]>();
    private static final int MAX_QUEUE = 30;
    private short[] tone;
    private Thread generator;
    private Thread consumer;
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

        tone = new short[200*44];
        for (int i = 0; i < tone.length; ++i) {
            if (i < 200)
                if (i % 2 == 0)
                    tone[i] = (short)(25000 * java.lang.Math.sin(i*java.lang.Math.PI/100));
                else
                    tone[i] = tone[i-1];
            else
                tone[i] = tone[i%200];
        }

        while (bufferQueue.size() < MAX_QUEUE)
            bufferQueue.add(new short[tone.length]);

        generator = new Thread(new Runnable() {
            public void run() {
                while (! Thread.interrupted()) {
                    if (outputQueue.size() >= MAX_QUEUE) {
                        try{
                            System.out.println(".............. Generator: Output queue is full, waiting 50ms");
                            Thread.sleep(50);
                            continue;
                        } catch (Exception e) {}
                    }

                    short[][] kernels = MITData.get(azimuth);
                    synchronized(bufferQueue) {
                        short[] buf = bufferQueue.remove();
                        Convolutions.stereoConvolveInto(tone, buf, kernels);
                        outputQueue.add(buf);
                    }
                }
            }
        });
        generator.start();

        consumer = new Thread(new Runnable() {
            public void run() {
                while (! Thread.interrupted()) {
                    if (outputQueue.size() == 0) {
                        try{
                            System.out.println(".............. Consumer: Output queue is empty, waiting 2000ms");
                            Thread.sleep(2000);
                            continue;
                        } catch (Exception e) {}
                    }

                    synchronized (bufferQueue) {
                        short[] buf = outputQueue.remove();
                        if (track != null) track.write(buf, 0, buf.length - 1200);
                        bufferQueue.add(buf);
                    }
                }
            }
        });

        while (outputQueue.size() < 10) ; /* no-op; wait for for buffered output */
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
