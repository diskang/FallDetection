package com.bendeming.falldetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {

		Intent serviceIntent = new Intent();
		serviceIntent.setAction("com.bendeming.falldetection.FallDetectionService");
		context.startService(serviceIntent);

	}

}
