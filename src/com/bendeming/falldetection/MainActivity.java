package com.bendeming.falldetection;

import java.text.NumberFormat;
import java.util.Arrays;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

	private NumberFormat df;

	private GraphView accelXGraphView;
	private GraphView accelYGraphView;
	private GraphView accelZGraphView;

	private float[] accelerationXBuffer;
	private float[] accelerationYBuffer;
	private float[] accelerationZBuffer;

	private PhoneView phoneView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);

		this.phoneView = (PhoneView)this.findViewById(R.id.phoneView);

		this.accelXGraphView = (GraphView)this.findViewById(R.id.accelerometerXGraphView);
		this.accelYGraphView = (GraphView)this.findViewById(R.id.accelerometerYGraphView);
		this.accelZGraphView = (GraphView)this.findViewById(R.id.accelerometerZGraphView);

		this.df = NumberFormat.getNumberInstance();
		this.df.setMaximumFractionDigits(3);

		this.accelerationXBuffer = new float[10];
		for (int i = 0; i < this.accelerationXBuffer.length; i++)
			this.accelerationXBuffer[i] = Float.MAX_VALUE;

		this.accelerationYBuffer = new float[10];
		for (int i = 0; i < this.accelerationXBuffer.length; i++)
			this.accelerationYBuffer[i] = Float.MAX_VALUE;

		this.accelerationZBuffer = new float[10];
		for (int i = 0; i < this.accelerationXBuffer.length; i++)
			this.accelerationZBuffer[i] = Float.MAX_VALUE;

		if (!this.isServiceRunning()) {

			Intent serviceIntent = new Intent("data");
			serviceIntent.setAction("com.bendeming.falldetection.FallDetectionService");
			this.startService(serviceIntent);

		}

		BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

			@Override
			public synchronized void onReceive(Context context, Intent intent) {

				if (intent.getExtras().getFloatArray("accelerationData") != null) {

					float[] accelerationData = intent.getExtras().getFloatArray("accelerationData");

					// Update X graph. Really must abstract this out so it is cleaner looking!!

					int firstBlankIndex = -1;
					for (int i = 0; i < MainActivity.this.accelerationXBuffer.length; i++)
						if (MainActivity.this.accelerationXBuffer[i] == Float.MAX_VALUE)
							firstBlankIndex = i;

					if (firstBlankIndex == -1) {
						MainActivity.this.accelXGraphView.updateWithValues(Arrays.copyOf(MainActivity.this.accelerationXBuffer, MainActivity.this.accelerationXBuffer.length), intent.getExtras().getFloat("maximumValue"));

						for (int i = 0; i < MainActivity.this.accelerationXBuffer.length; i++)
							MainActivity.this.accelerationXBuffer[i] = Float.MAX_VALUE;

					}
					else
						MainActivity.this.accelerationXBuffer[firstBlankIndex] = accelerationData[0];

					// Update Y graph. Really must abstract this out so it is cleaner looking!!

					for (int i = 0; i < MainActivity.this.accelerationYBuffer.length; i++)
						if (MainActivity.this.accelerationYBuffer[i] == Float.MAX_VALUE)
							firstBlankIndex = i;

					if (firstBlankIndex == -1) {
						MainActivity.this.accelYGraphView.updateWithValues(Arrays.copyOf(MainActivity.this.accelerationYBuffer, MainActivity.this.accelerationYBuffer.length), intent.getExtras().getFloat("maximumValue"));

						for (int i = 0; i < MainActivity.this.accelerationYBuffer.length; i++)
							MainActivity.this.accelerationYBuffer[i] = Float.MAX_VALUE;

					}
					else
						MainActivity.this.accelerationYBuffer[firstBlankIndex] = accelerationData[1];

					// Update Z graph. Really must abstract this out so it is cleaner looking!!

					for (int i = 0; i < MainActivity.this.accelerationZBuffer.length; i++)
						if (MainActivity.this.accelerationZBuffer[i] == Float.MAX_VALUE)
							firstBlankIndex = i;

					if (firstBlankIndex == -1) {
						MainActivity.this.accelZGraphView.updateWithValues(Arrays.copyOf(MainActivity.this.accelerationZBuffer, MainActivity.this.accelerationZBuffer.length), intent.getExtras().getFloat("maximumValue"));

						for (int i = 0; i < MainActivity.this.accelerationZBuffer.length; i++)
							MainActivity.this.accelerationZBuffer[i] = Float.MAX_VALUE;

					}
					else
						MainActivity.this.accelerationZBuffer[firstBlankIndex] = accelerationData[2];


				}

				else if (intent.getExtras().getFloatArray("gyroscopeData") != null) {

					float[] gyroscopeData = intent.getExtras().getFloatArray("gyroscopeData");

					MainActivity.this.phoneView.rotate(MainActivity.this.radiansToDegrees(gyroscopeData[1]), MainActivity.this.radiansToDegrees(gyroscopeData[0]));

				}

			}
		};

		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
				new IntentFilter("data"));

	}

	private int radiansToDegrees(float radians) {

		return (int)(radians * (180 / Math.PI));

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		super.onOptionsItemSelected(item);

		if (item.getItemId() == R.id.menu_settings) {

			Intent intent = new Intent(this, SettingsActivity.class);
			this.startActivity(intent);

		}

		return false;

	}

	private boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (FallDetectionService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}



}
