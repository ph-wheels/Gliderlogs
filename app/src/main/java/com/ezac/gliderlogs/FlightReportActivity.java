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
import java.util.List;
import java.util.Locale;

import com.ezac.gliderlogs.contentprovider.FlightsContentProvider;
import com.ezac.gliderlogs.database.GliderLogTables;
import com.ezac.gliderlogs.misc.Common;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridView;

public class FlightReportActivity extends FragmentActivity {

	protected static String TAG = "FlightsReport";

	final Context edi_con = FlightReportActivity.this;

	private List<String> GliderList = new ArrayList<String>();
	private List<Integer> GliderListCount = new ArrayList<Integer>();
	private List<Integer> GliderListTime = new ArrayList<Integer>();

	ArrayList<String> stringArrayList = new ArrayList<String>();

	GridView gridView;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.report_list);

		gridView = (GridView) findViewById(R.id.gridView1);

		Uri uri = FlightsContentProvider.CONTENT_URI_FLIGHT;

		fillData(uri);

		String[] stringArray = stringArrayList
				.toArray(new String[stringArrayList.size()]);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, stringArray);

		gridView.setAdapter(adapter);

		setMode();
		
		Button close = (Button) findViewById(R.id.button_close);

		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				GliderList = null;
				GliderListCount = null;
				GliderListTime = null;
				stringArrayList = null;
				setResult(RESULT_OK);
				finish();
			}
		});

	}

	private void fillData(Uri uri) {

		String selection;
		Cursor cursor;
		/*
		 * alternative to aggregated query, it doesn't seem to be not
		 * supported by getContentResolver().query
		 */
		GliderList.clear();
		GliderListCount.clear();
		GliderListTime.clear();
		stringArrayList.clear();
		String[] projection = { GliderLogTables.F_ID,
				GliderLogTables.F_REGISTRATION, GliderLogTables.F_DURATION };
		selection = "(" + GliderLogTables.F_STARTED + " IS NOT '' AND "
				+ GliderLogTables.F_LANDED + " IS NOT '')";
		cursor = getContentResolver().query(uri, projection, selection, null,
				null);
		if ((cursor != null) && (cursor.getCount() > 0)) {
			cursor.moveToFirst();
			do {
				String res = cursor
						.getString(
								cursor.getColumnIndexOrThrow(GliderLogTables.F_REGISTRATION))
						.toUpperCase(Locale.US);
				if (GliderList.contains(res)) {
					int j = GliderList.indexOf(res);
					GliderListCount.set(j, GliderListCount.get(j) + 1);
					GliderListTime
							.set(j,
									GliderListTime.get(j)
											+ ToMinute(cursor.getString(cursor
													.getColumnIndexOrThrow(GliderLogTables.F_DURATION))));
				} else {
					GliderList.add(res);
					GliderListCount.add(1);
					GliderListTime
							.add(ToMinute(cursor.getString(cursor
									.getColumnIndexOrThrow(GliderLogTables.F_DURATION))));
				}
			} while (cursor.moveToNext());
			int GrandCnt = 0;
			int GrandTime = 0;
			for (int i = 0; i < GliderList.size(); i++) {
				stringArrayList.add(GliderList.get(i));
				stringArrayList.add(GliderListCount.get(i).toString());
				GrandCnt = GrandCnt + GliderListCount.get(i);
				int hour = GliderListTime.get(i) / 60;
				int minute = GliderListTime.get(i) - (hour * 60);
				stringArrayList.add("" + hour + ":" + Common.TwoDigits(minute));
				GrandTime = GrandTime + GliderListTime.get(i);
			}
			// added as to generate a grand total
			stringArrayList.add("Totaal");
			stringArrayList.add(""+ GrandCnt);
			int hour = GrandTime / 60;
			int minute = GrandTime - (hour * 60);
			stringArrayList.add("" + hour + ":" + Common.TwoDigits(minute));
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
