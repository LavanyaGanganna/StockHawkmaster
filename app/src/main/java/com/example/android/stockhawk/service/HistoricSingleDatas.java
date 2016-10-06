package com.example.android.stockhawk.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.rest.Utils;
import com.example.android.stockhawk.ui.MyStocksActivity;
import com.example.android.stockhawk.ui.StockDetailsActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * Created by lavanya on 9/29/16.
 */
public class HistoricSingleDatas extends AsyncTask<Void, Void, ArrayList<SingleDayData>> {
	private static final String TAG = HistoricalDatas.class.getSimpleName();
	String symbolname;
	Response response;
	private ProgressBar progressBar;
	ArrayList<SingleDayData> singledaydatas = new ArrayList<SingleDayData>();
	OkHttpClient clients = new OkHttpClient();
	final String BASE_URL = "http://chartapi.finance.yahoo.com/instrument/1.0/";
	String END_URL = "/chartdata;type=quote;range=1d/json";
	ArrayList<SingleDayData> singledaydata;
	Context mcontext;
	String choices;
	public AsyncResponse delegate = null;


	public interface AsyncResponse {
		void processFinish(ArrayList<SingleDayData> output);
	}


	public HistoricSingleDatas(String symbolname, ProgressBar progressBar, String choices, Context context, AsyncResponse asyncs) {
		this.symbolname = symbolname;
		mcontext = context;
		delegate = asyncs;
		this.choices = choices;
		this.progressBar = progressBar;


	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		progressBar.setVisibility(View.VISIBLE);
	}

	@Override
	protected ArrayList<SingleDayData> doInBackground(Void... params) {
		Request.Builder builder = new Request.Builder();
		if (choices.equals("week")) {
			END_URL = "/chartdata;type=quote;range=7d/json";
		}

		builder.url(BASE_URL + symbolname + END_URL);
		//	Log.d(TAG,"the url is" + BASE_URL+symbolname+END_URL);
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
				singledaydata = parsingStockData(stockdata);

			} catch (JSONException e) {
				setHistoricalDataStatus(MyStocksActivity.STATUS_ERROR_JSON);
				e.printStackTrace();
			}
		} catch (IOException e) {
			setHistoricalDataStatus(MyStocksActivity.STATUS_ERROR_NO_NETWORK);
			e.printStackTrace();
		}
		return singledaydata;
	}

	private ArrayList<SingleDayData> parsingStockData(String stockdata) throws JSONException {
		if (stockdata.contains("(")) {
			String json = stockdata.substring(stockdata.indexOf("(") + 1, stockdata.lastIndexOf(")"));
			JSONObject mainObject = new JSONObject(json);
			JSONArray seriesarray = mainObject.getJSONArray("series");
			for (int i = 0; i < seriesarray.length(); i++) {
				JSONObject jsonobj = seriesarray.getJSONObject(i);
				long timestamp = jsonobj.getLong("Timestamp");
				Double closeval = jsonobj.getDouble("close");
				SingleDayData singledaydata = new SingleDayData(timestamp, closeval);
				singledaydatas.add(singledaydata);
			}
			return singledaydatas;
		} else {
			setHistoricalDataStatus(MyStocksActivity.STATUS_ERROR_JSON);
			return null;
		}
	}

	@Override
	protected void onPostExecute(ArrayList<SingleDayData> singleDayDatas) {
		super.onPostExecute(singleDayDatas);
		progressBar.setVisibility(View.GONE);
		delegate.processFinish(singleDayDatas);

	}

	public void setHistoricalDataStatus(int status) {
		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mcontext);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt(mcontext.getString(R.string.historicalDataStatus), status);
		editor.apply();
	}

}
