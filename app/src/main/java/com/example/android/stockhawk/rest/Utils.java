package com.example.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.data.QuoteColumns;
import com.example.android.stockhawk.data.QuoteProvider;
import com.example.android.stockhawk.service.HistoricSingleDatas;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

	private static final String TAG = Utils.class.getSimpleName();
	private static String LOG_TAG = Utils.class.getSimpleName();

	public static boolean showPercent = true;

	public static ArrayList quoteJsonToContentVals(String JSON) {
		ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
		JSONObject jsonObject = null;
		JSONArray resultsArray = null;
		try {
			jsonObject = new JSONObject(JSON);
			if (jsonObject != null && jsonObject.length() != 0) {
				jsonObject = jsonObject.getJSONObject("query");
				int count = Integer.parseInt(jsonObject.getString("count"));
				if (count == 1) {
					jsonObject = jsonObject.getJSONObject("results")
							.getJSONObject("quote");
					batchOperations.add(buildBatchOperation(jsonObject));
				} else {
					resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");

					if (resultsArray != null && resultsArray.length() != 0) {
						for (int i = 0; i < resultsArray.length(); i++) {
							jsonObject = resultsArray.getJSONObject(i);
							batchOperations.add(buildBatchOperation(jsonObject));
						}
					}
				}
			}
		} catch (JSONException e) {
			Log.e(LOG_TAG, "String to JSON failed: " + e);
		}
		return batchOperations;
	}

	public static String truncateBidPrice(String bidPrice) {
		bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
		return bidPrice;
	}

	public static String truncateChange(String change, boolean isPercentChange) {
		String weight = change.substring(0, 1);
		String ampersand = "";
		if (isPercentChange) {
			ampersand = change.substring(change.length() - 1, change.length());
			change = change.substring(0, change.length() - 1);
		}
		change = change.substring(1, change.length());
		double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
		change = String.format("%.2f", round);
		StringBuffer changeBuffer = new StringBuffer(change);
		changeBuffer.insert(0, weight);
		changeBuffer.append(ampersand);
		change = changeBuffer.toString();
		return change;
	}

	public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
		ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
				QuoteProvider.Quotes.CONTENT_URI);
		try {
			String change = jsonObject.getString("Change");
			String bid = jsonObject.getString("Bid");
			if (change.equals("null") || bid.equals("null"))
				return null;
			else {
				builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
				builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
				builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
						jsonObject.getString("ChangeinPercent"), true));
				builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
				builder.withValue(QuoteColumns.ISCURRENT, 1);
				if (change.charAt(0) == '-') {
					builder.withValue(QuoteColumns.ISUP, 0);
				} else {
					builder.withValue(QuoteColumns.ISUP, 1);
				}
				builder.withValue(QuoteColumns.COMPANYNAME, jsonObject.getString("Name"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return builder.build();
	}

	public static String converttotime(long timestamp) {
		try {
			Calendar calendar = Calendar.getInstance();
			TimeZone tz = TimeZone.getDefault();
			calendar.setTimeInMillis(timestamp * 1000);
			calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
			SimpleDateFormat sdf = new SimpleDateFormat("KK:mm");
			Date currenTimeZone = (Date) calendar.getTime();
			return sdf.format(currenTimeZone);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String converttodaydate() {
		long yourmilliseconds = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy");
		Date resultdate = new Date(yourmilliseconds);
		String result = sdf.format(resultdate);
		return result;
	}

	public static String converttodate(long dates) {
		String datess = Long.toString(dates);
		StringBuilder formatteddate = new StringBuilder();
		formatteddate.append(datess.substring(4, 6))
				.append("/")
				.append(datess.substring(6));
		return formatteddate.toString();

	}

	public static String converttodates(long dates) {
		String datess = Long.toString(dates);
		StringBuilder formatteddate = new StringBuilder();
		formatteddate.append(datess.substring(4, 6))
				.append("/")
				.append(datess.substring(6))
				.append("/")
				.append(datess.substring(0, 4));
		return formatteddate.toString();

	}


}
