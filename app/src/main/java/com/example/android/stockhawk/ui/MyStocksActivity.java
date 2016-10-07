package com.example.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.elmargomez.typer.Font;
import com.elmargomez.typer.Typer;
import com.example.android.stockhawk.R;
import com.example.android.stockhawk.data.QuoteColumns;
import com.example.android.stockhawk.data.QuoteProvider;
import com.example.android.stockhawk.rest.QuoteCursorAdapter;
import com.example.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.example.android.stockhawk.rest.Utils;
import com.example.android.stockhawk.service.StockIntentService;
import com.example.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;


import com.example.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = MyStocksActivity.class.getSimpleName();

	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	/**
	 * Used to store the last screen title. For use in {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	private Intent mServiceIntent;
	private ItemTouchHelper mItemTouchHelper;
	private static final int CURSOR_LOADER_ID = 0;
	private QuoteCursorAdapter mCursorAdapter;
	private Context mContext;
	private Cursor mCursor;
	boolean isConnected;
	String symbolname, companyname;
	@BindView(R.id.progressbar)
	ProgressBar progressBar;
	Toolbar mToolbar;
	@BindView(R.id.emptyView_acitivity_my_stocks)
	TextView emptytext;
	public static final int STATUS_ERROR_JSON = 1;
	public static final int STATUS_ERROR_SERVER = 2;
	public static final int STATUS_ERROR_PARSE = 3;
	public static final int STATUS_ERROR_NO_NETWORK = 4;
	public static final int STATUS_ERROR_UNKNOWN = 5;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		ConnectivityManager cm =
				(ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		isConnected = activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();
		setContentView(R.layout.activity_my_stocks);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(mToolbar);
		ButterKnife.bind(this);

		// The intent service is for executing immediate pulls from the Yahoo API
		// GCMTaskService can only schedule tasks, they cannot execute immediately
		mServiceIntent = new Intent(this, StockIntentService.class);
		if (savedInstanceState == null) {
			// Run the initialize task service so that some stocks appear upon an empty database
			//	mServiceIntent.putExtra("tag", "init");
			mServiceIntent.putExtra(getString(R.string.tagstr), getString(R.string.init));
			if (isConnected) {
				startService(mServiceIntent);
			} else {
				networkToast();
			}
		}

		RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

		mCursorAdapter = new QuoteCursorAdapter(this, null);

		recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
				new RecyclerViewItemClickListener.OnItemClickListener() {
					@Override
					public void onItemClick(View v, int position) {
						//TODO:
						// do something on item click
						mCursor.moveToPosition(position);
						Log.d(TAG, "the position is" + position);
						companyname = mCursor.getString(mCursor.getColumnIndexOrThrow(QuoteColumns.COMPANYNAME));
						symbolname = mCursor.getString(mCursor.getColumnIndexOrThrow(QuoteColumns.SYMBOL));
						Log.d(TAG, "the symbol name is" + symbolname + companyname);
						Intent intent = new Intent(MyStocksActivity.this, StockDetailsActivity.class);
						intent.putExtra(getString(R.string.widget_symbols), symbolname);
						intent.putExtra(getString(R.string.widget_company), companyname);
						intent.putExtra(getString(R.string.widget_bid), mCursor.getString(mCursor.getColumnIndexOrThrow(QuoteColumns.BIDPRICE)));
						startActivity(intent);
					}
				}));
		recyclerView.setAdapter(mCursorAdapter);
		emptyViewBehavior();
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		//  fab.attachToRecyclerView(recyclerView);
		if (fab != null) {
			fab.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (isConnected) {
						new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
								.content(R.string.content_test)
								.inputType(InputType.TYPE_CLASS_TEXT)
								.input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
									@Override
									public void onInput(MaterialDialog dialog, CharSequence input) {
										// On FAB click, receive user input. Make sure the stock doesn't already exist
										// in the DB and proceed accordingly
										String inputstr = input.toString().toUpperCase();
										Log.d(TAG, "the input is" + inputstr);
										Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
												new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
												new String[]{inputstr}, null);
										if (c.getCount() != 0) {
											Toast toast =
													Toast.makeText(MyStocksActivity.this, "This stock is already saved!",
															Toast.LENGTH_LONG);
											toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
											toast.show();
											c.close();
											return;
										} else {
											// Add the stock to DB
											//	mServiceIntent.putExtra("tag", "add");
											mServiceIntent.putExtra(getString(R.string.tagstr), getString(R.string.add));
											mServiceIntent.putExtra(getString(R.string.symbol), input.toString().toUpperCase());
											startService(mServiceIntent);
										}
									}
								})
								.show();
					} else {
						networkToast();
					}

				}
			});
		}
		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
		mItemTouchHelper = new ItemTouchHelper(callback);
		mItemTouchHelper.attachToRecyclerView(recyclerView);

		mTitle = getTitle();
		if (isConnected) {
			long period = 3600L;
			long flex = 10L;
			String periodicTag = getString(R.string.periodic);

			// create a periodic task to pull stocks once every hour after the app has been opened. This
			// is so Widget data stays up to date.
			PeriodicTask periodicTask = new PeriodicTask.Builder()
					.setService(StockTaskService.class)
					.setPeriod(period)
					.setFlex(flex)
					.setTag(periodicTag)
					.setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
					.setRequiresCharging(false)
					.build();
			// Schedule task with tag "periodic." This ensure that only the stocks present in the DB
			// are updated.
			GcmNetworkManager.getInstance(this).schedule(periodicTask);

		}
	}

	private void emptyViewBehavior() {
		if (mCursorAdapter.getItemCount() <= 0) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
			int stockStatus = sp.getInt(getString(R.string.stockStatus), -1);
			String message = getString(R.string.data_not_available);
			boolean error = false;
			switch (stockStatus) {

				case STATUS_ERROR_NO_NETWORK:
					message += getString(R.string.string_status_no_network);
					error = true;
					break;

				case STATUS_ERROR_JSON:
					message += getString(R.string.string_error_json);
					error = true;
					break;

				case STATUS_ERROR_PARSE:
					message += getString(R.string.string_error_server);
					error = true;
					break;

				case STATUS_ERROR_SERVER:
					message += getString(R.string.string_server_down);
					error = true;
					break;

				case STATUS_ERROR_UNKNOWN:
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
		} else {
			emptytext.setVisibility(View.GONE);
		}

	}


	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);

	}

	public void networkToast() {
		Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
		Toast.makeText(mContext, getString(R.string.network_toast2), Toast.LENGTH_SHORT).show();
	}

	public void restoreActionBar() {
		mToolbar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.my_stocks, menu);
		restoreActionBar();
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.widget_info_detail.
		int id = item.getItemId();
		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}
		if (id == R.id.action_change_units) {
			// this is for changing stock changes from percent value to dollar value
			Utils.showPercent = !Utils.showPercent;
			this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		// This narrows the return to only the stocks that are most current.
		return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
				new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
						QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.COMPANYNAME},
				QuoteColumns.ISCURRENT + " = ?",
				new String[]{"1"},
				null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		if ((data == null) || (data.getCount() == 0)) {
			mCursorAdapter.swapCursor(data);
			mCursor = data;
			progressBar.setVisibility(View.VISIBLE);
		} else {
			progressBar.setVisibility(View.GONE);
			emptytext.setVisibility(View.GONE);
			mCursorAdapter.swapCursor(data);
			mCursor = data;
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mCursorAdapter.swapCursor(null);
	}

}
