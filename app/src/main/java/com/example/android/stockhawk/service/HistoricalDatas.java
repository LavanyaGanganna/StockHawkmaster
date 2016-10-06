package com.example.android.stockhawk.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.rest.Utils;
import com.example.android.stockhawk.ui.MyStocksActivity;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.internal.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

/**
 * Created by lavanya on 9/29/16.
 */
public class HistoricalDatas extends AsyncTask<Void, Void, ArrayList<HistoricDataParcelable>> {
	public static final String TAG = HistoricalDatas.class.getSimpleName();
	final String BASE_URL = "http://chartapi.finance.yahoo.com/instrument/1.0/";
	String END_URL = "/chartdata;type=quote;range=1y/json";
	ArrayList<HistoricDataParcelable> historicdata = new ArrayList<>();
	String symbolname;
	Context mcontext;
	Response response;
	ProgressBar progressBar;
	OkHttpClient clients = new OkHttpClient();
	public static String startdate;
	public static String enddate;
	public AsyncListener asyncListener = null;
	String choice;


	public interface AsyncListener {
		public void processdone(ArrayList<HistoricDataParcelable> olddatas);
	}


	public HistoricalDatas(String symbolnames, ProgressBar progressBar, String choices, Context context, AsyncListener asyncListener) {
		this.symbolname = symbolnames;
		mcontext = context;
		this.asyncListener = asyncListener;
		this.choice = choices;
		this.progressBar = progressBar;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	protected ArrayList<HistoricDataParcelable> doInBackground(Void... params) {
		Request.Builder builder = new Request.Builder();
		if (choice.equals("sixmonth"))
			END_URL = "/chartdata;type=quote;range=6m/json";

		builder.url(BASE_URL + symbolname + END_URL);
		Request request = builder.build();
		try {
			response = clients.newCall(request).execute();
			String stockdata = response.body().string();
			//	Log.d(TAG,"the stock json data" + stockdata);
			try {
				//	Long start = System.currentTimeMillis();
				if (stockdata.equals(null)) {
					Log.d(TAG, "the stock json string is null");
					return null;
				}
				historicdata = parsingStockData(stockdata);

			} catch (JSONException e) {
				setHistoricalDataStatus(MyStocksActivity.STATUS_ERROR_JSON);
				e.printStackTrace();
			}
		} catch (IOException e) {
			setHistoricalDataStatus(MyStocksActivity.STATUS_ERROR_NO_NETWORK);
			e.printStackTrace();
		}
		return historicdata;
	}

	private ArrayList<HistoricDataParcelable> parsingStockData(String stockdata) throws JSONException {
		ArrayList<HistoricDataParcelable> historicDataParcelableArrayList = new ArrayList<>();
		if (stockdata.contains("(")) {
			String json = stockdata.substring(stockdata.indexOf("(") + 1, stockdata.lastIndexOf(")"));
			JSONObject mainobject = new JSONObject(json);
			JSONObject dateobject = mainobject.getJSONObject("Date");
			startdate = Utils.converttodates(dateobject.getLong("min")).toString();
			enddate = Utils.converttodates(dateobject.getLong("max")).toString();
			JSONArray seriesarray = mainobject.getJSONArray("series");
			for (int i = 0; i < seriesarray.length(); i++) {
				JSONObject singleobj = seriesarray.getJSONObject(i);
				long dates = singleobj.getLong("Date");
				double closeval = singleobj.getDouble("close");
				HistoricDataParcelable historicDataParcelable = new HistoricDataParcelable(dates, closeval);
				historicDataParcelableArrayList.add(historicDataParcelable);
			}
			return historicDataParcelableArrayList;
		} else {
			setHistoricalDataStatus(MyStocksActivity.STATUS_ERROR_JSON);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ArrayList<HistoricDataParcelable> historicDataParcelables) {
		super.onPostExecute(historicDataParcelables);
		progressBar.setVisibility(View.GONE);
		asyncListener.processdone(historicDataParcelables);
	}

	public void setHistoricalDataStatus(int status) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mcontext);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(mcontext.getString(R.string.historicalDataStatus), status);
		editor.apply();
	}

}