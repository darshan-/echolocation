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

	private Timer timer;
	//private Queue<short[]> q;
	private short[][] dataBuffers;
        private short[] buffer;
	// useless comment
	private int nativeSampleRate;
	private int bufSize;
	private int channelSize;
	private AudioTrack track;
	private Random random;
	private int INTERVAL_MILLISECONDS;
	private static int numBuffers = 40;
	private static int elevation = 0;
	private SensorManager sensorManager;
	private Sensor sensor;
	int azimuth;
	private final SensorEventListener compassListener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {	
                azimuth = (int) event.values[0];
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
		sensorManager.registerListener(compassListener, sensor,
                SensorManager.SENSOR_DELAY_UI);
		timer = new Timer();
		nativeSampleRate = AudioTrack.getNativeOutputSampleRate(AudioManager.STREAM_MUSIC);
		bufSize = AudioTrack.getMinBufferSize(nativeSampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		if(bufSize % 2 != 0)
			bufSize++;
		channelSize = bufSize / 2;
		//bufSize = nativeSampleRate / 6;
		float[] preFFTData = new float[bufSize];
		for(int i = 0; i < preFFTData.length; i++)
			preFFTData[i] = 0;
		int fund_idx = preFFTData.length / 150;
		preFFTData[fund_idx] = 5;
		
		//FloatFFT_1D stereofft = new FloatFFT_1D(bufSize);
		//stereofft.realInverse(preFFTData, false);
		
		random = new Random();
		dataBuffers = new short[numBuffers][];
		for(int i = 0; i < numBuffers; i++){
			short[] data = new short[channelSize];
			for(int j = 0; j < data.length; j++)
				data[j] = (short)random.nextInt(Short.MAX_VALUE);
			dataBuffers[i] = data;
		}
		track = new AudioTrack(
				AudioManager.STREAM_MUSIC,
				nativeSampleRate,
				AudioFormat.CHANNEL_CONFIGURATION_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				2 * bufSize,
				AudioTrack.MODE_STREAM
		);

                /*
                byte[] data = new byte[0];
                try {
                    java.io.File file = new java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), "sparcle.wav");
                    java.io.FileInputStream fis = new java.io.FileInputStream(file);
                    data = new byte[fis.available()];
                    fis.read(data, 0, data.length);
                } catch (java.io.FileNotFoundException e) {
                    System.out.println(".....................FileNotFoundException");
                } catch (java.io.IOException e) {
                    System.out.println(".....................IOFoundException");
                }
                java.nio.ShortBuffer sb = java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                short[] inBuffer = new short[sb.limit()];
                sb.get(inBuffer);
                buffer = new short[inBuffer.length*8]; // *4 for 11025-> 44100; *2 for mono -> stereo

                for (int i=0; i<inBuffer.length; ++i)
                    for (int j=0; j<8; ++j)
                        buffer[i*8+j] = inBuffer[i];
                */

                buffer = new short[44100];
                for (int i=0; i<buffer.length; ++i)
                    buffer[i] = (short)(25000 * java.lang.Math.sin(i*2*java.lang.Math.PI/100));

                new Thread(new Runnable() {
                    public void run() {
                        while (true) {
                            short[][] kernels = MITData.get(azimuth, 0);
                            short[] rightBuffer = Convolutions.convolveAndScale(buffer, kernels[0]);
                            short[] leftBuffer = Convolutions.convolveAndScale(buffer, kernels[1]);
                            if (track != null) track.write(Convolutions.zipper(leftBuffer, rightBuffer), 0, buffer.length*2);
                        }
                    }
                }).start();

                track.play();
		super.onCreate();
	}

	@Override
	public void onDestroy(){
		timer.cancel();
		track.stop();
		track = null;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
