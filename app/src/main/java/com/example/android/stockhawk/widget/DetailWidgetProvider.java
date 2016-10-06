package com.example.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.service.StockTaskService;
import com.example.android.stockhawk.ui.MyStocksActivity;
import com.example.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by lavanya on 10/3/16.
 */
public class DetailWidgetProvider extends AppWidgetProvider {
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int appWidgetId : appWidgetIds) {
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_detail);

			// Create an Intent to launch MainActivity
			Intent intent = new Intent(context, MyStocksActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
			views.setOnClickPendingIntent(R.id.widget, pendingIntent);
			// Set up the collection
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				setRemoteAdapter(context, views);
			} else {
				setRemoteAdapterV11(context, views);
			}
			boolean useDetailActivity = context.getResources()
					.getBoolean(R.bool.use_detail_activity);
			Intent clickIntentTemplate = useDetailActivity
					? new Intent(context, StockDetailsActivity.class)
					: new Intent(context, MyStocksActivity.class);
			PendingIntent clickPendingIntentTemplate = TaskStackBuilder.create(context)
					.addNextIntentWithParentStack(clickIntentTemplate)
					.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

			views.setPendingIntentTemplate(R.id.widget_list, clickPendingIntentTemplate);
			views.setEmptyView(R.id.widget_list, R.id.widget_empty);

			// Tell the AppWidgetManager to perform an update on the current app widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
		}
	}

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent) {
		super.onReceive(context, intent);
		if (StockTaskService.ACTION_DATA_UPDATED.equals(intent.getAction())) {
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
			int[] appWidgetIds = appWidgetManager.getAppWidgetIds(
					new ComponentName(context, getClass()));
			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list);
		}
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void setRemoteAdapter(Context context, @NonNull final RemoteViews views) {
		views.setRemoteAdapter(R.id.widget_list,
				new Intent(context, DetailWidgetRemoteViewService.class));
	}

	private void setRemoteAdapterV11(Context context, @NonNull final RemoteViews views) {
		views.setRemoteAdapter(0, R.id.widget_list,
				new Intent(context, DetailWidgetRemoteViewService.class));
	}
}