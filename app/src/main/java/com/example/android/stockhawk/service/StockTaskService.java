package com.example.android.stockhawk.service;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Looper;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.ui.MyStocksActivity;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.example.android.stockhawk.data.QuoteColumns;
import com.example.android.stockhawk.data.QuoteProvider;
import com.example.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.os.Handler;

import java.util.logging.LogRecord;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
	private static final String TAG = StockTaskService.class.getSimpleName();
	private String LOG_TAG = StockTaskService.class.getSimpleName();
	public static final String ACTION_DATA_UPDATED =
			"com.example.android.stockhawk.ACTION_DATA_UPDATED";


	private OkHttpClient client = new OkHttpClient();
	private Context mContext;
	private StringBuilder mStoredSymbols = new StringBuilder();
	private boolean isUpdate;

	public StockTaskService() {

	}

	public StockTaskService(Context context) {
		mContext = context;
	}

	String fetchData(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		Response response = client.newCall(request).execute();
		return response.body().string();
	}

	@Override
	public int onRunTask(TaskParams params) {

		Cursor initQueryCursor;
		if (mContext == null) {
			mContext = this;
		}
		StringBuilder urlStringBuilder = new StringBuilder();
		try {
			// Base URL for the Yahoo query
			urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
			urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
					+ "in (", "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_SERVER);
			e.printStackTrace();
		}
		if (params.getTag().equals(mContext.getString(R.string.init)) || params.getTag().equals(mContext.getString(R.string.periodic))) {
			isUpdate = true;
			initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
					new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
					null, null);
			if (initQueryCursor.getCount() == 0 || initQueryCursor == null) {
				// Init task. Populates DB with quotes for the symbols seen below
				try {
					urlStringBuilder.append(
							URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_SERVER);
					e.printStackTrace();
				}
			} else if (initQueryCursor != null) {
				DatabaseUtils.dumpCursor(initQueryCursor);
				initQueryCursor.moveToFirst();
				for (int i = 0; i < initQueryCursor.getCount(); i++) {
					mStoredSymbols.append("\"" +
							initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol")) + "\",");
					initQueryCursor.moveToNext();
				}
				mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
				try {
					urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_SERVER);
					e.printStackTrace();
				}
			}
		} else if (params.getTag().equals(mContext.getString(R.string.add))) {
			isUpdate = false;
			// get symbol from params.getExtra and build query
			//	String stockInput = params.getExtras().getString("symbol");
			String stockInput = params.getExtras().getString(mContext.getString(R.string.symbol));
			try {
				urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_SERVER);
				e.printStackTrace();
			}
		}
		// finalize the URL for the API query.
		urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
				+ "org%2Falltableswithkeys&callback=");

		String urlString;
		String getResponse;
		int result = GcmNetworkManager.RESULT_FAILURE;

		if (urlStringBuilder != null) {
			urlString = urlStringBuilder.toString();
			Log.d(TAG, "the url is" + urlString);
			try {

				getResponse = fetchData(urlString);
				Log.d(TAG, "the json" + getResponse);
				result = GcmNetworkManager.RESULT_SUCCESS;
				try {
					ContentValues contentValues = new ContentValues();
					// update ISCURRENT to 0 (false) so new data is current
					if (isUpdate) {
						contentValues.put(QuoteColumns.ISCURRENT, 0);
						mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
								null, null);
						updatewidgets();

					}

					ArrayList<ContentProviderOperation> contentarrlist = Utils.quoteJsonToContentVals(getResponse);
					boolean foundnull = true;
					for (ContentProviderOperation contentProviderOperation : contentarrlist) {
						if (contentProviderOperation != null)
							foundnull = false;
					}
					//   mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
					//         Utils.quoteJsonToContentVals(getResponse));

					if (!foundnull) {
						mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, contentarrlist);
						if (!isUpdate) {
							new Thread(new Runnable() {
								@Override
								public void run() {
									new Handler(Looper.getMainLooper()).post(
											new Runnable() {
												public void run() {
													// yourContext is Activity or Application context
													Toast.makeText(mContext, "Added to Database", Toast.LENGTH_SHORT).show();
												}
											}
									);
								}
							}).start();
						}
					}
					if (foundnull && !isUpdate) {
						//		Handler h = new Handler(getMainLooper());
						new Thread(new Runnable() {
							@Override
							public void run() {
								new Handler(Looper.getMainLooper()).post(
										new Runnable() {
											public void run() {
												// yourContext is Activity or Application context
												Toast.makeText(mContext, "wrong stock symbol input", Toast.LENGTH_SHORT).show();
											}
										}
								);
							}
						}).start();
					}
					if (foundnull)
						setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_JSON);

				} catch (RemoteException | OperationApplicationException e) {
					Log.e(LOG_TAG, "Error applying batch insert", e);
					setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_JSON);
				}
			} catch (IOException e) {
				setStockStatus(mContext, MyStocksActivity.STATUS_ERROR_NO_NETWORK);
				e.printStackTrace();
			}
		}

		return result;
	}

	private void setStockStatus(Context mContext, int status) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(mContext.getString(R.string.stockStatus), status);
		editor.apply();
	}

	private void updatewidgets() {
		//sending broadcast for appwidget which is wrapper around the broadcast receiver
		Intent dataupdateintent = new Intent(ACTION_DATA_UPDATED).setPackage(mContext.getPackageName());
		mContext.sendBroadcast(dataupdateintent);
	}


}
