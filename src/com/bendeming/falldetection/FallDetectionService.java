package com.bendeming.falldetection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Toast;

public class FallDetectionService extends Service implements SensorEventListener {

	private Sensor accelerometer;
	private Sensor gyroscope;

	private ArrayList<double[]> lastAccelValuesInInterval;
	private ArrayList<double[]> lastGyroValuesInInterval;

	private final ArrayList<double[]> accelValuesInInterval = new ArrayList<double[]>();
	private final ArrayList<double[]> gyroValuesInInterval = new ArrayList<double[]>();

	private final int axisOfDirectionOfMovement = 2;

	private long lastUpdateTime;

	private WakeLock wakeLock;

	private boolean sensorsAreRunning = false;

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

		SensorManager manager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);

		this.accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		this.playSound();

		this.lastUpdateTime = System.currentTimeMillis();

		this.startSensors();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Toast.makeText(this, "Fall Service Destroyed", Toast.LENGTH_LONG).show();

		this.wakeLock.release();

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

		if (System.currentTimeMillis() - this.lastUpdateTime >= 1000) {

			long time = System.currentTimeMillis();

			this.lastAccelValuesInInterval = new ArrayList<double[]>(this.accelValuesInInterval);
			this.lastGyroValuesInInterval = new ArrayList<double[]>(this.gyroValuesInInterval);

			this.lastUpdateTime = time;

			if (this.performFallAnalysis()) {

				this.playSound();

			}

			else {



			}

			this.accelValuesInInterval.clear();
			this.gyroValuesInInterval.clear();

		}

		else {

			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

				double[] filteredValues = new double[3];
				this.filter(event.values, filteredValues);

				this.accelValuesInInterval.add(filteredValues);

			}

			else {

				double[] output = new double[event.values.length];
				for (int i = 0; i < event.values.length; i++)
				{
					output[i] = event.values[i];
				}

				this.gyroValuesInInterval.add(output);

			}

		}

	}

	private void playSound() {

		MediaPlayer player = new MediaPlayer();
		try {
			player.setDataSource(this, Settings.System.DEFAULT_NOTIFICATION_URI);
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

	}

	private boolean performFallAnalysis() {

		if (this.lastGyroValuesInInterval == null)
			return false;

		boolean accel = this.performAccelerationAnalysis();
		boolean ang = this.performAngularVelocityAnalysis();
		boolean angVar = this.performAngularVariationAnalysis();

		System.out.println("" + accel + " " + ang + " " + angVar);

		return accel && ang && angVar;

	}

	private boolean performAccelerationAnalysis() {

		double peakHorizontal = Double.MIN_VALUE;
		double peakDirectionMovement = Double.MIN_VALUE;

		for (double[] accelValues : this.accelValuesInInterval) {

			System.out.println(Arrays.toString(accelValues));

			if (Math.abs(accelValues[this.axisOfDirectionOfMovement]) > peakDirectionMovement)
				peakDirectionMovement = Math.abs(accelValues[this.axisOfDirectionOfMovement]);

			if (Math.abs(accelValues[0]) > peakHorizontal)
				peakHorizontal = Math.abs(accelValues[0]);

		}

		double value = -0.139 + 0.0195 * peakHorizontal + 0.0163 * peakDirectionMovement;

		System.out.println("Accel analysis value " + value);

		return value >= 0.05;

	}

	private boolean performAngularVelocityAnalysis() {

		double peakAxisValue = Double.MIN_VALUE;

		for (double[] accelValues : this.gyroValuesInInterval) {

			for (double value : accelValues) {

				if (Math.abs(value) > peakAxisValue)
					peakAxisValue = Math.abs(value);

			}

		}

		System.out.println("Peak: " + peakAxisValue);

		return peakAxisValue > 2.4543;

	}

	private boolean performAngularVariationAnalysis() {

		int num = this.lastGyroValuesInInterval.size() + this.gyroValuesInInterval.size();
		double[] x = new double[num];

		for (int i = 0; i < x.length; i++) {

			x[i] = i;

		}

		double[] y = new double[num];

		int counter = 0;

		while(counter < this.lastGyroValuesInInterval.size()) {

			double[] gyroValues = this.lastGyroValuesInInterval.get(counter);
			y[counter] = Math.abs(gyroValues[0]);
			counter++;

		}

		while (counter < this.gyroValuesInInterval.size()) {

			double[] gyroValues = this.gyroValuesInInterval.get(counter);
			y[counter] = Math.abs(gyroValues[0]);
			counter++;

		}

		double value = TrapezoidalIntegrator.integrate(x, y);

		System.out.println("" + value);

		return value > 0.872664626;

	}

	private void fallDetected() {

		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

		if (pref.getBoolean("pref_key_notifications_enabled", false)) {

			SmsManager manager = SmsManager.getDefault();

			if (pref.getBoolean("pref_key_notifications_enabled", false))
				manager.sendTextMessage(pref.getString("pref_key_phone_number", ""), null, pref.getString("pref_key_custom_sms_message", ""), null, null);

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

	private static final float alpha = 0.8f;
	private static final double[] gravity = new double[3];

	private void filter(float[] input, double[] output ) {

		gravity[0] = alpha * gravity[0] + (1 - alpha) * input[0];
		gravity[1] = alpha * gravity[1] + (1 - alpha) * input[1];
		gravity[2] = alpha * gravity[2] + (1 - alpha) * input[2];

		output[0] = input[0] - gravity[0];
		output[1] = input[1] - gravity[1];
		output[2] = input[2] - gravity[2];

	}

}
