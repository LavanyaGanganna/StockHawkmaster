package com.example.android.stockhawk.service;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * Created by lavanya on 9/29/16.
 */
public class SingleDayData implements Parcelable {
	long TimeStamp;
	Double ClosedValues;

	public SingleDayData(long timeStamp, Double closedvalues) {
		TimeStamp = timeStamp;
		ClosedValues = closedvalues;
	}

	public SingleDayData() {
	}

	public long getTimeStamp() {
		return TimeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		TimeStamp = timeStamp;
	}

	public Double getClosedValues() {
		return ClosedValues;
	}

	public void setClosedValues(Double closedValues) {
		ClosedValues = closedValues;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	protected SingleDayData(Parcel in) {
		TimeStamp = in.readLong();
		ClosedValues = in.readDouble();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(TimeStamp);
		dest.writeDouble(ClosedValues);
	}

	public static final Creator<SingleDayData> CREATOR = new Creator<SingleDayData>() {
		@Override
		public SingleDayData createFromParcel(Parcel in) {
			return new SingleDayData(in);
		}

		@Override
		public SingleDayData[] newArray(int size) {
			return new SingleDayData[size];
		}
	};
}

