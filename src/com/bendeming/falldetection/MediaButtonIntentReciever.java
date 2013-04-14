package com.bendeming.falldetection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.view.KeyEvent;

public class MediaButtonIntentReciever extends BroadcastReceiver  {

	@Override
	public void onReceive(Context context, Intent intent) {

		KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

		if (event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK && event.getAction() == KeyEvent.ACTION_UP) {

			Intent mediaIntent = new Intent("headphoneEvent");
			LocalBroadcastManager.getInstance(context).sendBroadcast(mediaIntent);

		}

		this.abortBroadcast();
	}


}
