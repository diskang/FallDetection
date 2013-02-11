package com.bendeming.falldetection;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

public class FallDetectionService extends Service implements SensorEventListener {

	private Sensor accelerometer;
	private Sensor gyroscope;

	private float[] accelerationData;
	private float[] gyroscopeData;

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		SensorManager manager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);

		this.accelerometer = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		this.gyroscope = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

		manager.registerListener(this, this.accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		manager.registerListener(this, this.gyroscope, SensorManager.SENSOR_DELAY_NORMAL);

	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		Toast.makeText(this, "Fall Service Destroyed", Toast.LENGTH_LONG).show();

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

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

			this.accelerationData = event.values;

			Intent broadcastIntent = new Intent("data");
			broadcastIntent.putExtra("accelerationData", this.accelerationData);
			broadcastIntent.putExtra("maximumValue", this.accelerometer.getMaximumRange());
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

		}

		else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {

			this.gyroscopeData = event.values;

			Intent broadcastIntent = new Intent("data");
			broadcastIntent.putExtra("gyroscopeData", this.gyroscopeData);
			broadcastIntent.putExtra("maximumValue", this.gyroscope.getMaximumRange());
			LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

		}

	}

}
