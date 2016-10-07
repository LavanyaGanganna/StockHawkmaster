package com.example.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.android.stockhawk.R;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

	public StockIntentService() {
		super(StockIntentService.class.getName());
	}

	public StockIntentService(String name) {
		super(name);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
		StockTaskService stockTaskService = new StockTaskService(this);
		Bundle args = new Bundle();
		if (intent.getStringExtra(getApplicationContext().getString(R.string.tagstr)).equals(getApplicationContext().getString(R.string.add))) {
			args.putString(getString(R.string.symbol), intent.getStringExtra(getString(R.string.symbol)));
		}
		// We can call OnRunTask from the intent service to force it to run immediately instead of
		// scheduling a task.
		stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(getApplicationContext().getString(R.string.tagstr)), args));

	}
}
