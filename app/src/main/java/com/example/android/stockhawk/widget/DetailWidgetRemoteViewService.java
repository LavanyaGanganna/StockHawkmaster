package com.example.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.example.android.stockhawk.R;
import com.example.android.stockhawk.data.QuoteColumns;
import com.example.android.stockhawk.data.QuoteProvider;
import com.example.android.stockhawk.ui.StockDetailsActivity;

/**
 * Created by lavanya on 10/3/16.
 */
public class DetailWidgetRemoteViewService extends RemoteViewsService {


	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new RemoteViewsFactory() {
			private Cursor data = null;

			@Override
			public void onCreate() {
				// Nothing to do
			}

			@Override
			public void onDataSetChanged() {
				if (data != null) {
					data.close();
				}
				// This method is called by the app hosting the widget (e.g., the launcher)
				// However, our ContentProvider is not exported so it doesn't have access to the
				// data. Therefore we need to clear (and finally restore) the calling identity so
				// that calls use our process and permission
				final long identityToken = Binder.clearCallingIdentity();
				data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
						new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
								QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.COMPANYNAME},
						QuoteColumns.ISCURRENT + " = ?",
						new String[]{"1"},
						null);
				Binder.restoreCallingIdentity(identityToken);
			}

			@Override
			public void onDestroy() {
				if (data != null) {
					data.close();
					data = null;
				}
			}

			@Override
			public int getCount() {
				return data == null ? 0 : data.getCount();
			}

			@Override
			public RemoteViews getViewAt(int position) {
				if (position == AdapterView.INVALID_POSITION ||
						data == null || !data.moveToPosition(position)) {
					return null;
				}
				RemoteViews views = new RemoteViews(getPackageName(),
						R.layout.widget_detail_list_item);

				String symbolnm = data.getString(data.getColumnIndexOrThrow(QuoteColumns.SYMBOL));
				String bidpri = data.getString(data.getColumnIndexOrThrow(QuoteColumns.BIDPRICE));
				String change = data.getString(data.getColumnIndexOrThrow(QuoteColumns.CHANGE));
				String company = data.getString(data.getColumnIndexOrThrow(QuoteColumns.COMPANYNAME));

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
					setRemoteContentDescription(views, symbolnm);
				}
				views.setTextViewText(R.id.stock_symbol_widget, symbolnm);
				views.setTextViewText(R.id.bid_price_widget, bidpri);
				views.setTextViewText(R.id.change_widget, change);
				Bundle bundle = new Bundle();
				bundle.putString("symbols", symbolnm);
				bundle.putString("bidvalue", bidpri);
				bundle.putString("changes", change);
				bundle.putString("company", company);
				final Intent fillInIntent = new Intent();
				fillInIntent.putExtras(bundle);
				views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);
				return views;
			}

			@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
			private void setRemoteContentDescription(RemoteViews views, String description) {
				views.setContentDescription(R.id.stock_symbol_widget, description);
			}

			@Override
			public RemoteViews getLoadingView() {
				return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
			}

			@Override
			public int getViewTypeCount() {
				return 1;
			}

			@Override
			public long getItemId(int position) {

				return this.data.getInt(0);
			}

			@Override
			public boolean hasStableIds() {
				return true;
			}
		};
	}
}


