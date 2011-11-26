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
        private static final int MAX_QUEUE = 3;
        private Thread generator;
        private Thread consumer;
        private short[] buffer;
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
		nativeSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
		bufSize = AudioTrack.getMinBufferSize(nativeSampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		if(bufSize % 2 != 0)
			bufSize++;
                System.out.println("...................... bufSize = " + bufSize);
		channelSize = bufSize / 2;
		//bufSize = nativeSampleRate / 6;

		track = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				nativeSampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				2 * bufSize,
				AudioTrack.MODE_STREAM
		);

                buffer = new short[44100/10];
                for (int i=0; i<buffer.length; ++i)
                    buffer[i] = (short)(25000 * java.lang.Math.sin(i*2*java.lang.Math.PI/100));

                generator = new Thread(new Runnable() {
                    public void run() {
                        while (! java.lang.Thread.interrupted()) {
                            if (queue.size() >= MAX_QUEUE) {
                                try{
                                    System.out.println(".............. Generator: Queue is full, waiting 50ms");
                                    Thread.sleep(50);
                                    continue;
                                } catch (Exception e) {}
                            }
                            short[][] kernels = MITData.get(azimuth, 0);
                            short[] rightBuffer = Convolutions.convolveAndScale(buffer, kernels[0]);
                            short[] leftBuffer = Convolutions.convolveAndScale(buffer, kernels[1]);
                            queue.add(Convolutions.zipper(leftBuffer, rightBuffer));
                        }
                    }
                });
                generator.start();

                consumer = new Thread(new Runnable() {
                    public void run() {
                        while (! java.lang.Thread.interrupted()) {
                            if (queue.size() == 0) {
                                try{
                                    System.out.println(".............. Consumer: Queue is empty, waiting 50ms");
                                    Thread.sleep(50);
                                    continue;
                                } catch (Exception e) {}
                            }
                            if (track != null) track.write(queue.remove(), 0, buffer.length*2);
                        }
                    }
                });
                consumer .start();

                track.play();
		super.onCreate();
	}

	@Override
	public void onDestroy(){
            track.stop();
            track = null;
            generator.interrupt();
            consumer.interrupt();
            sensorManager.unregisterListener(compassListener);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
