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

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.widget.TimePicker;

public class TimePickerDialogFragment extends DialogFragment {
	Handler mHandler;
	int mHour;
	int mMinute;

	public TimePickerDialogFragment(Handler h) {
		// Getting the reference to the message handler instantiated in MainActivity class
		mHandler = h;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		// Creating a bundle object to pass currently set time to the fragment
		Bundle b = getArguments();
		mHour = b.getInt("set_hour");
		mMinute = b.getInt("set_minute");

		TimePickerDialog.OnTimeSetListener listener = new TimePickerDialog.OnTimeSetListener() {
			@Override
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				mHour = hourOfDay;
				mMinute = minute;
				 // Creating a bundle object to pass currently set time to the fragment
				Bundle b = new Bundle();
				// Adding currently set time to bundle object
				b.putInt("set_hour", mHour);
				b.putInt("set_minute", mMinute);
				// Adding Current time in a string to bundle object
				b.putString("set_time", "Set Time : " + Integer.toString(mHour)
						+ ":" + Integer.toString(mMinute));
				// Creating an instance of Message
				Message m = new Message();
				// Setting bundle object on the message object m
				m.setData(b);
				 // Message m is sending using the message handler instantiated in MainActivity class
				mHandler.sendMessage(m);
			}
		};
		// Opening the TimePickerDialog window
		return new TimePickerDialog(getActivity(), listener, mHour, mMinute, true);
	}
}
