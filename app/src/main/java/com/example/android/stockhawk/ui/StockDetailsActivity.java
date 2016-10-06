package com.example.android.stockhawk.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.rest.Utils;
import com.example.android.stockhawk.service.HistoricDataParcelable;
import com.example.android.stockhawk.service.HistoricSingleDatas;
import com.example.android.stockhawk.service.HistoricalDatas;
import com.example.android.stockhawk.service.SingleDayData;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.util.ArrayList;
import java.util.Collections;

import butterknife.BindView;
import butterknife.ButterKnife;

public class StockDetailsActivity extends Activity implements HistoricSingleDatas.AsyncResponse, HistoricalDatas.AsyncListener {
	private static final String TAG = StockDetailsActivity.class.getSimpleName();

	@BindView(R.id.symbolname)
	TextView symbols;
	@BindView(R.id.spinner)
	Spinner periodspinner;
	@BindView(R.id.linechart)
	LineChart linechart;
	@BindView(R.id.todaysdate)
	TextView todaytext;
	@BindView(R.id.maxcloseval)
	TextView maxtext;
	@BindView(R.id.detailprogress)
	ProgressBar detprogress;
	@BindView(R.id.emptyView_acitivity)
	TextView emptytext;
	String symbolname;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stock_details);
		//getActionBar().setDisplayHomeAsUpEnabled(true);
		ButterKnife.bind(this);
		Intent intent = getIntent();
		String companynam = intent.getStringExtra("company");
		if (companynam.length() >= 22)
			symbols.setText(companynam.substring(0, 21));
		else
			symbols.setText(companynam);

		symbolname = intent.getStringExtra("symbols");
		String maxvalue = String.format(getString(R.string.format_closeval), intent.getStringExtra("bidvalue"));
		maxtext.setText(maxvalue);
		todaytext.setText(Utils.converttodaydate());
		//ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(this,R.array.timeperiod_array,android.R.layout.simple_spinner_item);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.timeperiod_array, R.layout.spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		periodspinner.setAdapter(adapter);
		periodspinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String choice = null;
				switch (position) {
					case 0:
						choice = "day";
						todaytext.setText(Utils.converttodaydate());
						todaytext.setText(Utils.converttodaydate());
						HistoricSingleDatas historicSingleDatas = new HistoricSingleDatas(symbolname, detprogress, choice, getApplicationContext(), StockDetailsActivity.this);
						historicSingleDatas.execute();
						break;
					case 1:
						choice = "week";
						todaytext.setText(Utils.converttodaydate());
						todaytext.setContentDescription(Utils.converttodaydate());
						HistoricSingleDatas historicSingleDatasweek = new HistoricSingleDatas(symbolname, detprogress, choice, getApplicationContext(), StockDetailsActivity.this);
						historicSingleDatasweek.execute();
						break;

					case 2:
						choice = "sixmonth";
						HistoricalDatas historicdatsmon = new HistoricalDatas(symbolname, detprogress, choice, getApplicationContext(), StockDetailsActivity.this);
						historicdatsmon.execute();

						break;
					case 3:
						choice = "1year";
						HistoricalDatas historicdasyear = new HistoricalDatas(symbolname, detprogress, choice, getApplicationContext(), StockDetailsActivity.this);
						historicdasyear.execute();
						break;
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});
	}

	@Override
	public void processFinish(ArrayList<SingleDayData> output) {
		ArrayList<Entry> entries = new ArrayList<>();
		ArrayList<String> xvalues = new ArrayList<>();
		ArrayList<Double> closevals = new ArrayList<>();
		if (output != null && output.size() > 0) {
			if (emptytext.getVisibility() == View.VISIBLE)
				emptytext.setVisibility(View.GONE);
			linechart.setVisibility(View.VISIBLE);
			for (int i = 0; i < output.size(); i = i + 7) {
				SingleDayData singleday = output.get(i);
				double yValue = singleday.getClosedValues();
				closevals.add(yValue);
				xvalues.add(Utils.converttotime(singleday.getTimeStamp()));
				entries.add(new Entry((float) yValue, i));
			}
			//	double min = Math.round(Collections.min(closevals)) - 0.1;
			//	double max = Math.round(Collections.max(closevals)) + 0.1;
			XAxis xAxis = linechart.getXAxis();
			xAxis.setLabelsToSkip(15);
			xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
			xAxis.setTextSize(14f);
			xAxis.setTextColor(Color.BLACK);
			YAxis yAxis = linechart.getAxisLeft();
			yAxis.setEnabled(true);
			yAxis.setLabelCount(10, true);
			yAxis.setTextColor(Color.BLACK);
			linechart.getAxisRight().setEnabled(false);
			linechart.getLegend().setTextSize(18f);
			linechart.getLegend().setWordWrapEnabled(true);
			linechart.setDrawGridBackground(true);
			//linechart.setGridBackgroundColor(Color.rgb(25, 118, 210));
			linechart.setGridBackgroundColor(Color.WHITE);
			linechart.setDescriptionColor(Color.BLACK);
			linechart.setDescription("Days stock graph");
			String name = getResources().getString(R.string.stock);
			LineDataSet dataSet = new LineDataSet(entries, name);
			dataSet.setColor(getResources().getColor(R.color.light_purple));
			dataSet.setFillColor(getResources().getColor(R.color.light_purple));
			dataSet.setCircleColor(getResources().getColor(R.color.light_purple));
			dataSet.setDrawValues(false);
			LineData lineData = new LineData(xvalues, dataSet);
			linechart.animateX(2500);
			linechart.setData(lineData);
			dataSet.setDrawCubic(true);
			dataSet.setDrawFilled(true);
			//Paint paint = linechart.getRenderer().getPaintRender();
			linechart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
				@Override
				public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
					double val = e.getVal();
					String xAxisValue = linechart.getData().getXVals().get(e.getXIndex());
					String valuesare = String.format(getString(R.string.format_values), val, xAxisValue);
					todaytext.setText(valuesare);
				}

				@Override
				public void onNothingSelected() {

				}
			});

		} else {
			linechart.setVisibility(View.INVISIBLE);
			failurefunc();
		}
	}

	@Override
	public void processdone(ArrayList<HistoricDataParcelable> olddatas) {
		String daterang = String.format(getString(R.string.datesrange), HistoricalDatas.startdate, HistoricalDatas.enddate);
		todaytext.setText(daterang);
		todaytext.setContentDescription(daterang);
		ArrayList<Entry> entries = new ArrayList<>();
		ArrayList<String> xvalues = new ArrayList<>();
		ArrayList<Double> closevals = new ArrayList<>();
		if (olddatas != null && olddatas.size() > 0) {
			if (emptytext.getVisibility() == View.VISIBLE)
				emptytext.setVisibility(View.GONE);
			linechart.setVisibility(View.VISIBLE);
			for (int i = 0; i < olddatas.size(); i = i + 3) {
				HistoricDataParcelable singleday = olddatas.get(i);
				double yValue = singleday.getClosevals();
				closevals.add(yValue);
				xvalues.add(Utils.converttodate(singleday.getDates()));
				entries.add(new Entry((float) yValue, i));
			}
			//	double min = Math.round(Collections.min(closevals))-0.1;
			//	double max = Math.round(Collections.max(closevals))+0.1;
			XAxis xAxis = linechart.getXAxis();
			xAxis.setLabelsToSkip(10);
			xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
			xAxis.setTextSize(14f);
			xAxis.setTextColor(Color.BLACK);
			YAxis yAxis = linechart.getAxisLeft();
			yAxis.setEnabled(true);
			yAxis.setLabelCount(10, true);
			yAxis.setTextColor(Color.BLACK);
			linechart.getAxisRight().setEnabled(false);
			linechart.getLegend().setTextSize(18f);
			linechart.getLegend().setWordWrapEnabled(true);
			linechart.setDrawGridBackground(true);
			linechart.setGridBackgroundColor(Color.WHITE);
			linechart.setDescriptionColor(Color.BLACK);
			linechart.setDescription("Comparision stock graph");
			String name = getResources().getString(R.string.stock);
			LineDataSet dataSet = new LineDataSet(entries, name);
			dataSet.setColor(getResources().getColor(R.color.light_purple));
			dataSet.setFillColor(getResources().getColor(R.color.light_purple));
			dataSet.setCircleColor(getResources().getColor(R.color.light_purple));
			dataSet.setDrawValues(false);
			LineData lineData = new LineData(xvalues, dataSet);
			linechart.animateX(2500);
			linechart.setData(lineData);
			dataSet.setDrawCubic(true);
			dataSet.setDrawFilled(true);
			linechart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
				@Override
				public void onValueSelected(Entry e, int dataSetIndex, Highlight h) {
					double val = e.getVal();
					String xAxisValue = linechart.getData().getXVals().get(e.getXIndex());
					String valuesare = String.format(getString(R.string.format_val), val, xAxisValue);
					todaytext.setText(valuesare);
					todaytext.setContentDescription(valuesare);
				}

				@Override
				public void onNothingSelected() {

				}
			});
		} else {
			linechart.setVisibility(View.INVISIBLE);
			failurefunc();
		}
	}

	private void failurefunc() {
		String message = "";
		int status = PreferenceManager.getDefaultSharedPreferences(this)
				.getInt(getString(R.string.historicalDataStatus), -1);
		boolean error = false;
		switch (status) {

			case MyStocksActivity.STATUS_ERROR_NO_NETWORK:
				message += getString(R.string.string_status_no_network);
				error = true;
				break;

			case MyStocksActivity.STATUS_ERROR_JSON:
				message += getString(R.string.string_error_json);
				error = true;
				break;

			case MyStocksActivity.STATUS_ERROR_PARSE:
				message += getString(R.string.string_error_server);
				error = true;
				break;

			case MyStocksActivity.STATUS_ERROR_SERVER:
				message += getString(R.string.string_server_down);
				error = true;
				break;

			case MyStocksActivity.STATUS_ERROR_UNKNOWN:
				message += getString(R.string.string_status_unknown);
				error = true;
				break;
			default:
				break;

		}
		if (error) {
			emptytext.setText(message);
			emptytext.setVisibility(View.VISIBLE);
		}
	}

}
