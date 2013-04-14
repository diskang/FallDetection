package com.bendeming.falldetection;

import java.text.NumberFormat;

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
import android.view.View;
import android.widget.Button;

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

		BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

			@Override
			public synchronized void onReceive(Context context, Intent intent) {



			}
		};

		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
				new IntentFilter("fallDetected"));

		if (!this.isServiceRunning()) {

			Intent intent = new Intent(this, FallDetectionService.class);
			this.startService(intent);

		}

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

	public void buttonPressed(View target) {

		if (this.isServiceRunning()) {

			Intent intent = new Intent(this, FallDetectionService.class);
			this.stopService(intent);

			Button button = (Button)target;
			button.setText("Start Data Collection");

		}

		else {

			Intent intent = new Intent(this, FallDetectionService.class);
			this.startService(intent);

			Button button = (Button)target;
			button.setText("End Data Collection");

		}

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
