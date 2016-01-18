package com.ezac.gliderlogs;

import android.app.Dialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;

public class DatePickerDialogFragment extends DialogFragment {
	Handler dHandler;
	int mYear;
	int mMonth;
	int mDay;

	public DatePickerDialogFragment(Handler h) {
		// Getting the reference to the message handler instantiated in MainActivity class
		dHandler = h;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Creating a bundle object to pass currently set time to the fragment
		Bundle b = getArguments();

		// Getting the year, month, day from bundle
		mYear = b.getInt("set_year");
		mMonth = b.getInt("set_month") - 1;
		mDay = b.getInt("set_day");

		DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {

			@Override
			public void onDateSet(DatePicker view, int YearofDate,
					int MonthofDate, int DayofDate) {

				mYear = YearofDate;
				mMonth = MonthofDate;
				mDay = DayofDate;

				// Creating a bundle object to pass currently set time to the fragment
				Bundle b = new Bundle();

				// Adding currently set year to bundle object
				b.putInt("set_year", mYear);
				b.putInt("set_month", mMonth + 1);
				b.putInt("set_day", mDay);
				// Adding Current time in a string to bundle object
				b.putString("set_date", "Set Date : " + Integer.toString(mDay)
						+ " - " + Integer.toString(mMonth + 1) + " - "
						+ Integer.toString(mYear));
				// Creating an instance of Message
				Message m = new Message();
				// Setting bundle object on the message object m
				m.setData(b);
				// Message m is sending using the message handler instantiated in MainActivity class
				dHandler.sendMessage(m);
			}
		};
		// Opening the TimePickerDialog window
		return new DatePickerDialog(getActivity(), listener, mYear, mMonth,
				mDay);
	}
}
