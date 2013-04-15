package com.bendeming.falldetection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class FallDetectionService extends Service implements SensorEventListener {

	private MediaPlayer mediaPlayer;

	private Sensor accelerometer;
	private Sensor gyroscope;

	final float[] accelValues = new float[3];
	final float[] gyroValues = new float[3];

	float[] accelerometerAverages = new float[] { Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE };
	float[] gyroscopesAverages = new float[] { Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE };

	private final float[] lastAccelerometerAverages = new float[3];
	private final float[] lastGyroscopeAverages = new float[3];

	private long lastAccelerationDeltaUpdateTime;
	private long lastGyroscopeDeltaUpdateTime;

	private WakeLock wakeLock;

	private BufferedWriter writer;

	private boolean sensorsAreRunning = false;
	private boolean shouldLog = false;

	private svm_model model;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
		this.wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getName());
		this.wakeLock.acquire();

		File outputFile = new File(Environment.getExternalStorageDirectory(), "falllog.txt");
		System.out.println(outputFile);

		try {
			this.writer = new BufferedWriter(new FileWriter(outputFile));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		svm_parameter param = new svm_parameter();
		param.svm_type = svm_parameter.ONE_CLASS;
		param.cache_size = 40.0;
		param.coef0 = 0.0;
		param.C = 1.0;
		param.gamma = 0.0;
		param.degree = 3;
		param.eps = 0.0010;
		param.kernel_type = svm_parameter.RBF;
		param.nu = 0.5;
		param.shrinking = 1;

		svm_problem problem = new svm_problem();

		try {

			int fileLines = getLines(this.getAssets().openFd("poop.svm").getFileDescriptor());

			problem.x = new svm_node[fileLines][6];
			problem.l = fileLines;

			BufferedReader reader = new BufferedReader(new FileReader(this.getAssets().openFd("poop.svm").getFileDescriptor()));

			String line = null;
			int lineNum = 0;

			double[][] instances = new double[fileLines][6];

			while ((line = reader.readLine()) != null) {

				String[] splits = line.split("\\s+");

				for (int i = 0; i < splits.length; i++)
					instances[lineNum][i] = Double.parseDouble(splits[i]);

			}

			for (int i = 0; i < instances.length; i++) {

				for (int j = 0; i < 6; i++) {

					problem.x[i][j] = new svm_node();
					problem.x[i][j].index = j;
					problem.x[i][j].value = instances[i][j];

				}

				problem.y[i] = 0;

			}

			this.model = svm.svm_train(problem, param);

		}

		catch (Exception e) {


		}

		SensorManager manager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);

		this.accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		int result = ((AudioManager)this.getSystemService(AUDIO_SERVICE)).requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {

			@Override
			public void onAudioFocusChange(int focusChange) {

				System.out.println(focusChange == AudioManager.AUDIOFOCUS_GAIN ? "Focus gained" : "Focus lost");

			}

		}, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		((AudioManager)this.getSystemService(AUDIO_SERVICE)).registerMediaButtonEventReceiver(new ComponentName(this, MediaButtonIntentReciever.class));

		LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {

				if (FallDetectionService.this.shouldLog)
					FallDetectionService.this.shouldLog = false;
				else {

					MediaPlayer player = new MediaPlayer();
					try {
						player.setDataSource(FallDetectionService.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
					try {
						player.prepare();
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					player.start();

					FallDetectionService.this.shouldLog = true;

				}

			}

		}, new IntentFilter("headphoneEvent"));

		this.startSensors();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Toast.makeText(this, "Fall Service Destroyed", Toast.LENGTH_LONG).show();

		this.wakeLock.release();

		try {
			this.writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.stopSensors();

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Toast.makeText(this, "Fall Service Started", Toast.LENGTH_LONG).show();
		return START_STICKY;

	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	@Override
	public void onSensorChanged(SensorEvent event) {

		if (System.currentTimeMillis() - this.lastAccelerationDeltaUpdateTime >= 250) {

			//float percentChange = ((this.accelerometerAverage - this.lastAccelerometerAverage) / this.lastAccelerometerAverage) * 100;

			/*if (this.lastAccelerometerAverage != 0.0f && this.accelerometerAverage != 0.0f && percentChange >= 10) {

				Intent intent = new Intent("fallDetected");
				LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

				if (!this.mediaPlayer.isPlaying())
					this.mediaPlayer.start();

				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				SmsManager manager = SmsManager.getDefault();

				if (pref.getBoolean("pref_key_notifications_enabled", false))
					manager.sendTextMessage(pref.getString("pref_key_phone_number", ""), null, pref.getString("pref_key_custom_sms_message", ""), null, null);

			}*/

			try {

				if (this.shouldLog) {

					this.writer.write("" + this.lastAccelerometerAverages[0] + "," + this.lastAccelerometerAverages[1] + "," + this.lastAccelerometerAverages[2] + "," + this.lastGyroscopeAverages[0] + "," + this.lastGyroscopeAverages[1] + "," + this.lastGyroscopeAverages[2] + ",\n");
					this.writer.flush();

				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			// Remove acceleration!!
			for (int i = 0; i < this.lastAccelerometerAverages.length; i++)
				this.lastAccelerometerAverages[i] = this.accelerometerAverages[i];

			for (int i = 0; i < this.lastAccelerometerAverages.length; i++)
				this.lastAccelerometerAverages[i] = this.accelerometerAverages[i];

			for (int i = 0; i < this.lastGyroscopeAverages.length; i++)
				this.lastGyroscopeAverages[i] = this.gyroscopesAverages[i];

			long time = System.currentTimeMillis();

			this.lastAccelerationDeltaUpdateTime = time;
			this.lastGyroscopeDeltaUpdateTime = time;

			svm_node[] nodes = new svm_node[6];

			for (int i = 0; i < 3; i++) {

				svm_node node = new svm_node();
				node.index = 0;
				node.value = this.lastAccelerometerAverages[i];
				nodes[i] = node;

			}

			for (int i = 0; i < 3; i++) {

				svm_node node = new svm_node();
				node.index = 0;
				node.value = this.lastGyroscopeAverages[i];
				nodes[i] = node;

			}

			double value = svm.svm_predict(this.model, nodes);

			System.out.println("Value " + value);

		}

		else {

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

				this.lowPass(event.values, this.accelValues);

				for (int i = 0; i < this.accelerometerAverages.length; i++)
					this.accelerometerAverages[i] = (float)((this.accelerometerAverages[i] * 0.1) + (0.9 * this.accelValues[i]));

			}

			else {

				this.lowPass(event.values, this.gyroValues);

				for (int i = 0; i < this.gyroscopesAverages.length; i++)
					this.gyroscopesAverages[i] = (float)((this.gyroscopesAverages[i] * 0.1) + (0.9 * this.gyroValues[i]));

			}

		}

	}

	private void startSensors() {

		System.out.println("Start");

		SensorManager manager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);

		manager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		manager.registerListener(this, this.gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

		this.sensorsAreRunning = true;

	}

	private void stopSensors() {

		System.out.println("Stop");

		SensorManager manager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);

		manager.unregisterListener(this);

		this.sensorsAreRunning = false;

	}

	static final float ALPHA = 0.15f;

	/**
	 * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
	 * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
	 */
	protected float[] lowPass( float[] input, float[] output ) {
		if ( output == null ) return input;

		for ( int i=0; i<input.length; i++ ) {
			output[i] = output[i] + ALPHA * (input[i] - output[i]);
		}
		return output;
	}

	public static int getLines(FileDescriptor ds) throws IOException {
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(new FileReader(ds));
			while ((reader.readLine()) != null);
			return reader.getLineNumber();
		} catch (Exception ex) {
			return -1;
		} finally {
			if(reader != null)
				reader.close();
		}
	}

}
