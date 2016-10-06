package com.example.android.stockhawk.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by lavanya on 9/29/16.
 */
public class HistoricDataParcelable implements Parcelable {
	long dates;
	Double closevals;

	public HistoricDataParcelable(long dates, double closevals) {
		this.dates = dates;
		this.closevals = closevals;
	}

	public long getDates() {
		return dates;
	}

	public void setDates(long dates) {
		this.dates = dates;
	}

	public Double getClosevals() {
		return closevals;
	}

	public void setClosevals(Double closevals) {
		this.closevals = closevals;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(dates);
		dest.writeDouble(closevals);
	}

	protected HistoricDataParcelable(Parcel in) {
		dates = in.readLong();
		closevals = in.readDouble();
	}

	public static final Creator<HistoricDataParcelable> CREATOR = new Creator<HistoricDataParcelable>() {
		@Override
		public HistoricDataParcelable createFromParcel(Parcel in) {
			return new HistoricDataParcelable(in);
		}

		@Override
		public HistoricDataParcelable[] newArray(int size) {
			return new HistoricDataParcelable[size];
		}
	};
}
