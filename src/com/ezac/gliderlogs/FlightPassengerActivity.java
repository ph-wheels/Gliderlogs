package com.ezac.gliderlogs;

/*
 *  Copyright (c) <2015> <Pro-Serv, P van der Wielen, EZAC>
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated 
 *	documentation files (the "Software"), to deal in the Software without restriction, including without limitation 
 *	the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, 
 *	and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *	
 *	Commercial usage of  (the "Software") is not prohibited
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 *	WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 *	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
 *	ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.ArrayList;
import java.util.Calendar;

import com.ezac.gliderlogs.contentprovider.FlightsContentProvider;
import com.ezac.gliderlogs.database.GliderLogTables;
import com.ezac.gliderlogs.misc.Common;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;

public class FlightPassengerActivity extends FragmentActivity {

	protected static String TAG = "FlightsPassenger";

	private int year;
	private int month;
	private int day;
	
	final Context edi_con = FlightPassengerActivity.this;
	
	ArrayList<String> stringArrayList = new ArrayList<String>();

	GridView gridView;
	
	// This handles the message send from DatePickerDialogFragment for setting
	Handler dHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			Bundle b = m.getData();
			year = b.getInt("set_year");
			month = b.getInt("set_month");
			day = b.getInt("set_day");
			getArryData(Common.FourDigits(year) + "-" + Common.TwoDigits(month) + "-" + Common.TwoDigits(day));
		}
	};

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.passenger_list);

		setMode();
		
		Button close = (Button) findViewById(R.id.button_close);
		Button date = (Button) findViewById(R.id.button_date);
		
		date.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// On button click show datepicker dialog
				Bundle b = new Bundle();
				final Calendar c = Calendar.getInstance();
				year = c.get(Calendar.YEAR);
				month = c.get(Calendar.MONTH) + 1;
				day = c.get(Calendar.DAY_OF_MONTH);
				b.putInt("set_year", year);
				b.putInt("set_month", month);
				b.putInt("set_day", day);
				// Instantiating TimePickerDialogFragment & pass it' arguments
				DatePickerDialogFragment datePicker = new DatePickerDialogFragment(
						dHandler);
				datePicker.setArguments(b);
				// Getting fragment manger for this activity & start transaction
				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(datePicker, "date_picker");
				/** Opening the TimePicker fragment */
				ft.commit();
			}
		});
		
		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				stringArrayList = null;
				setResult(RESULT_OK);
				finish();
			}
		});
		// load list based on current date
		final Calendar c = Calendar.getInstance();
		year = c.get(Calendar.YEAR);
		month = c.get(Calendar.MONTH) + 1;
		day = c.get(Calendar.DAY_OF_MONTH);
		getArryData(Common.FourDigits(year) + "-" + Common.TwoDigits(month) + "-" + Common.TwoDigits(day));
	}

	private void getArryData(String date) {
		
		gridView = (GridView) findViewById(R.id.gridView1);

		Uri uri = FlightsContentProvider.CONTENT_URI_EXTERN;

		fillData(uri, date);

		String[] stringArray = stringArrayList
				.toArray(new String[stringArrayList.size()]);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, stringArray);

		gridView.setAdapter(adapter);
	}
	
	private void fillData(Uri uri, String date) {

		String selection;
		Cursor cursor;
		stringArrayList.clear();
		String[] projection = { GliderLogTables.E_ID,
				GliderLogTables.E_DATE, GliderLogTables.E_TIME,
				GliderLogTables.E_NAME, GliderLogTables.E_PHONE };
		selection = GliderLogTables.E_DATE + " LIKE '" + date + "'";
		String sort = "" + GliderLogTables.E_TIME + " ASC";
		cursor = getContentResolver().query(uri, projection, selection, null,
				sort);
		//Log.d(TAG,"select " + selection + " cnt: " + cursor.getCount());
		if ((cursor != null) && (cursor.getCount() > 0)) {
			cursor.moveToFirst();
			do {
				stringArrayList.add(cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.E_ID)));
				String mydate = cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.E_DATE));
				String[] d = mydate.split("-");
				stringArrayList.add(d[2]+"-"+d[1]+"-"+d[0]);
				String mytime = cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.E_TIME));
				String[] t = mytime.split(":");
				stringArrayList.add(t[0]+":"+t[1]);
				stringArrayList.add(cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.E_NAME)));
				stringArrayList.add(cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.E_PHONE)));
				stringArrayList.add("");
			} while (cursor.moveToNext());
		}
		cursor.close();
	}
	
	public int ToMinute(String Duration) {
		String s[] = Duration.toString().split(":");
		return (Integer.parseInt(s[0]) * 60) + Integer.parseInt(s[1]);
	}

	public void setMode() {
		// hide soft keyboard on app launch
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
}
