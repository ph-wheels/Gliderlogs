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

import com.ezac.gliderlogs.contentprovider.FlightsContentProvider;
import com.ezac.gliderlogs.database.GliderLogTables;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;


public class FlightMemberActivity extends FragmentActivity {

	protected static String TAG = "FlightsPassenger";

	final Context edi_con = FlightMemberActivity.this;
	
	ArrayList<String> stringArrayList = new ArrayList<String>();

	GridView gridView;
	
	// This handles the message send from DatePickerDialogFragment for setting
	Handler dHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			//Bundle b = m.getData();
			getArryData("");
		}
	};

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.mem_info_list);

		setMode();
		
		Button close = (Button) findViewById(R.id.button_close);
		
		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				stringArrayList = null;
				setResult(RESULT_OK);
				finish();
			}
		});

		getArryData("");
	}

	private void getArryData(String date) {
		
		gridView = (GridView) findViewById(R.id.gridView1);

		Uri uri = FlightsContentProvider.CONTENT_URI_MEMBER;

		fillData(uri, date);

		String[] stringArray = stringArrayList
				.toArray(new String[stringArrayList.size()]);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, stringArray);

		gridView.setAdapter(adapter);
	}
	
	private void fillData(Uri uri, String date) {

		Cursor cursor;
		stringArrayList.clear();
		String[] projection = { GliderLogTables.M_CITY,
				GliderLogTables.M_2_NAME, GliderLogTables.M_3_NAME,
				GliderLogTables.M_1_NAME, GliderLogTables.M_ADRS, 
				GliderLogTables.M_PHONE, GliderLogTables.M_MOBILE,
				GliderLogTables.M_CODE, GliderLogTables.M_INSTRUCTION,
				GliderLogTables.M_TRAIN};
		//selection = GliderLogTables.M_ACTIVE + " LIKE '*'";
		String sort = "" + GliderLogTables.M_1_NAME + " ASC";
		cursor = getContentResolver().query(uri, projection, null, null,
				sort);
		if ((cursor != null) && (cursor.getCount() > 0)) {
			cursor.moveToFirst();
			do {
				String tmp = cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_1_NAME))
						+ " "
						+ cursor.getString(cursor
								.getColumnIndexOrThrow(GliderLogTables.M_2_NAME))
						+ " "
						+ cursor.getString(cursor
								.getColumnIndexOrThrow(GliderLogTables.M_3_NAME));
				stringArrayList.add(tmp.replaceAll("\\s+", " "));
				stringArrayList.add(cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.M_ADRS)));
				stringArrayList.add(cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.M_CITY)));
				stringArrayList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_PHONE)).equals("null") ? "-" : cursor.getString(cursor
								.getColumnIndexOrThrow(GliderLogTables.M_PHONE)));
				stringArrayList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_MOBILE)).equals("null") ? "-" : cursor.getString(cursor
								.getColumnIndexOrThrow(GliderLogTables.M_MOBILE)));
				tmp = " " + cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.M_CODE)) ;
				tmp = tmp + " - " + (cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.M_INSTRUCTION)).equals("1") ? "J" : "N");
				tmp = tmp + " - " + (cursor.getString(cursor.getColumnIndexOrThrow(GliderLogTables.M_TRAIN)).equals("1") ? "J" : "N");
				stringArrayList.add(tmp);
				stringArrayList.add("");
			} while (cursor.moveToNext());
		}
		cursor.close();
	}
	
	public void setMode() {
		// hide soft keyboard on app launch
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}
	
}