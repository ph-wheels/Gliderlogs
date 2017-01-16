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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.ezac.gliderlogs.contentprovider.FlightsContentProvider;
import com.ezac.gliderlogs.database.GliderLogTables;
import com.ezac.gliderlogs.misc.Common;

/*
 * FlightDetailActivity allows to enter a new flight item 
 * or to change an existing
 */
@SuppressLint("SimpleDateFormat")
public class FlightDetailActivity extends FragmentActivity {

	protected static String TAG = "FlightsDetail";
	private EditText mDateText;
	private Spinner mRegiSpin;
	private CheckBox mInstChck;
	private Spinner mPilotSpin;
	private Spinner mCoPilotSpin;
	private EditText mStartText;
	private EditText mLandText;
	private EditText mDuraText;
	private EditText mBodyText;

	private CheckBox mTypeNorm;
	private CheckBox mTypePass;
	private CheckBox mTypeDona;
	private CheckBox mTypeClub;
	private CheckBox mLaunchWinch;
	private CheckBox mLaunchTow;
	private CheckBox mLaunchMotor;

	private Uri flightUri;
	private int hour;
	private int minute;
	private int time_mde;
	private String result = null;
	private String mCallSign;
	private boolean flg_save = false;
	private boolean flg_two = false;
	
	// fake start id, meaning < 20000 are known, => 20000 are temporary
	private int ini_id = 20000;

	private List<String> GliderList = new ArrayList<String>();
	private List<String> GliderCall = new ArrayList<String>();
	private List<String> GliderSeatsList = new ArrayList<String>();
	private List<String> GliderPrivateList = new ArrayList<String>();
	private List<String> MemberList = new ArrayList<String>();
	//private List<String> MemberTrainList = new ArrayList<String>();
	private List<String> MemberInstrList = new ArrayList<String>();
	private List<String> MemberIndexList = new ArrayList<String>();

	final Context edi_con = FlightDetailActivity.this;
	// handles the message send from TimePickerDialogFragment for setting time
	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			// Creating a bundle object to pass currently set Time to the fragment
			Bundle b = m.getData();
			hour = b.getInt("set_hour");
			minute = b.getInt("set_minute");
			// update selected (either start of landing) time button & it's field
			if (time_mde == 10) {
				mStartText.setText(new StringBuilder().append(Common.TwoDigits(hour))
						.append(":").append(Common.TwoDigits(minute)));
			}
			if (time_mde == 20) {
				mLandText.setText(new StringBuilder().append(Common.TwoDigits(hour))
						.append(":").append(Common.TwoDigits(minute)));
			}
		}
	};

	public void onCB_G1_Clicked(View view) {
		// Is the view now checked?
		boolean chk_g1 = ((CheckBox) view).isChecked();
		// Check which checkbox was clicked
		switch (view.getId()) {
		case R.id.flight_type_norm:
			if (chk_g1) {
				mTypePass.setChecked(false);
				mTypeDona.setChecked(false);
				mTypeClub.setChecked(false);
			}
			break;
		case R.id.flight_type_pass:
			if (chk_g1) {
				mTypeNorm.setChecked(false);
				mTypeDona.setChecked(false);
				mTypeClub.setChecked(false);
			}
			break;
		case R.id.flight_type_dona:
			if (chk_g1) {
				mTypeNorm.setChecked(false);
				mTypePass.setChecked(false);
				mTypeClub.setChecked(false);
			}
			break;
		case R.id.flight_type_club:
			if (chk_g1) {
				mTypeNorm.setChecked(false);
				mTypeDona.setChecked(false);
				mTypePass.setChecked(false);
			}
			break;
		}
		if ((!mTypeNorm.isChecked()) && (!mTypePass.isChecked())
				&& (!mTypeDona.isChecked()) && (!mTypeClub.isChecked())) {
			mTypeNorm.setChecked(true);
		} else {
			setInstruction(4);
		}
	}

	public void onCB_G2_Clicked(View view) {
		// Is the view now checked?
		boolean chk_g2 = ((CheckBox) view).isChecked();
		// Check which checkbox was clicked
		switch (view.getId()) {
		case R.id.flight_launch_winch:
			if (chk_g2) {
				mLaunchTow.setChecked(false);
				mLaunchMotor.setChecked(false);
			}
			break;
		case R.id.flight_launch_tow:
			if (chk_g2) {
				mLaunchWinch.setChecked(false);
				mLaunchMotor.setChecked(false);
			}
			break;
		case R.id.flight_launch_motor:
			if (chk_g2) {
				mLaunchWinch.setChecked(false);
				mLaunchTow.setChecked(false);
			}
			break;
		}
		if ((!mLaunchWinch.isChecked()) && (!mLaunchTow.isChecked())
				&& (!mLaunchMotor.isChecked())) {
			mLaunchWinch.setChecked(true);
		}
	}

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.start_edit);
		// get references to our objects
		mDateText = (EditText) findViewById(R.id.flight_date);
		mRegiSpin = (Spinner) findViewById(R.id.flight_registration);
		mPilotSpin = (Spinner) findViewById(R.id.flight_pilot);
		mInstChck = (CheckBox) findViewById(R.id.flight_instruction);
		mCoPilotSpin = (Spinner) findViewById(R.id.flight_copilot);
		mStartText = (EditText) findViewById(R.id.flight_start);
		mLandText = (EditText) findViewById(R.id.flight_landing);
		mDuraText = (EditText) findViewById(R.id.flight_duration);
		// disable input, these are output only
		mDateText.setClickable(false);
		mDateText.setFocusable(false);
		mStartText.setClickable(false);
		mStartText.setFocusable(false);
		mLandText.setClickable(false);
		mLandText.setFocusable(false);
		mDuraText.setClickable(false);
		mDuraText.setFocusable(false);
		mBodyText = (EditText) findViewById(R.id.flight_edit_notes);
		mDateText.setText(FlightOverviewActivity.ToDay);
		mTypeNorm = (CheckBox) findViewById(R.id.flight_type_norm);
		mTypePass = (CheckBox) findViewById(R.id.flight_type_pass);
		mTypeDona = (CheckBox) findViewById(R.id.flight_type_dona);
		mTypeClub = (CheckBox) findViewById(R.id.flight_type_club);

		mLaunchWinch = (CheckBox) findViewById(R.id.flight_launch_winch);
		mLaunchTow = (CheckBox) findViewById(R.id.flight_launch_tow);
		mLaunchMotor = (CheckBox) findViewById(R.id.flight_launch_motor);

		Button confirmButton = (Button) findViewById(R.id.flight_edit_button);
		Button exitButton = (Button) findViewById(R.id.flight_quit_button);
		Button againButton = (Button) findViewById(R.id.flight_again_button);
		Button timeSButton = (Button) findViewById(R.id.btnChangeSTime);
		Button timeLButton = (Button) findViewById(R.id.btnChangeLTime);
		Button clearSButton = (Button) findViewById(R.id.btnClearSTime);
		Button clearLButton = (Button) findViewById(R.id.btnClearLTime);
		Button gliderButton = (Button) findViewById(R.id.btn_ext_1);
		Button pilotButton = (Button) findViewById(R.id.btn_ext_2);
		Bundle extras = getIntent().getExtras();

		// get data from DB tables and load our glider/member list
		addItemSpinner1();
		addItemSpinner2();
		// only now check if these are still empty
		if (GliderList.isEmpty() || MemberList.isEmpty()) {
			makeToast("Opties -> Voer eerst de actie 'Dag opstarten' uit, mogelijk was er een netwerk probleem !.");
			setResult(RESULT_CANCELED); 
			finish();
		}
		// check from the saved Instance
		flightUri = (bundle == null) ? null : (Uri) bundle
				.getParcelable(FlightsContentProvider.CONTENT_ITEM_TYPE);
		// Or passed from the other activity
		if (extras != null) {
			flightUri = extras
					.getParcelable(FlightsContentProvider.CONTENT_ITEM_TYPE);
			fillData(flightUri);
		}
		// bewaar ingevoerde informatie
		confirmButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mPilotSpin.getSelectedItem().equals(mCoPilotSpin.getSelectedItem())) { 
					makeToast("'Gezagvoerder' en 'Co-Piloot' kunnen niet het zelfde zijn !!");
				} else {
					if (TextUtils.isEmpty((String) mRegiSpin.getSelectedItem())) {
						makeToast("Verplichte velden invullen aub");
					} else {
						setResult(RESULT_OK);
						finish();
					}
				}
			}
		});
		// dupliceer de geselecteerde vlucht
		againButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				new AlertDialog.Builder(FlightDetailActivity.this)
				.setTitle("Bevestig deze opdracht")
				.setMessage("Wilt u deze vlucht dupliceren ? ")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						// put some caheck in place as t avoid bogus record to be created
						if ((!mDateText.getText().toString().equals("")) && 
								(!mStartText.getText().toString().equals("")) && 
								(!mLandText.getText().toString().equals("")) && 
								(!mRegiSpin.getSelectedItem().equals("")) && 
								(!mPilotSpin.getSelectedItem().equals("")))	{
							ContentValues values = new ContentValues();
							values.put(GliderLogTables.F_DATE, mDateText.getText().toString());
							values.put(GliderLogTables.F_REGISTRATION, (String) mRegiSpin.getSelectedItem());
							String ftype = "";
							if (mTypeNorm.isChecked()) {
								ftype = "";
							}
							if (mTypePass.isChecked()) {
								ftype = "PASS";
							}
							if (mTypeDona.isChecked()) {
								ftype = "DONA";
							}
							if (mTypeClub.isChecked()) {
								ftype = "CLUB";
							}
							values.put(GliderLogTables.F_TYPE, ftype);
							values.put(GliderLogTables.F_INSTRUCTION, (mInstChck.isChecked()) ? "J" : "N");
							String fPilo = (String) mPilotSpin.getSelectedItem();
							values.put(GliderLogTables.F_PILOT, fPilo);
							String fPilo_id = "";
							for (int i = 0; i < mPilotSpin.getCount(); i++) {
								String s = (String) mPilotSpin.getItemAtPosition(i);
								if (s.equalsIgnoreCase(fPilo)) {
									fPilo_id = MemberIndexList.get(i);
								}
							}
							values.put(GliderLogTables.F_PILOT_ID, fPilo_id);
							String fCoPi = (String) mCoPilotSpin.getSelectedItem();
							values.put(GliderLogTables.F_COPILOT, fCoPi);
							String fCoPi_id = "";
							for (int i = 0; i < mCoPilotSpin.getCount(); i++) {
								String s = (String) mCoPilotSpin.getItemAtPosition(i);
								if (s.equalsIgnoreCase(fCoPi)) {
									fCoPi_id = MemberIndexList.get(i);
								}
							}
							values.put(GliderLogTables.F_COPILOT_ID, fCoPi_id);
							values.put(GliderLogTables.F_STARTED, "");
							values.put(GliderLogTables.F_LANDED, "");
							values.put(GliderLogTables.F_DURATION, "");
							String fLaun = "";
							if (mLaunchWinch.isChecked()) {
								fLaun = "L";
							}
							if (mLaunchTow.isChecked()) {
								fLaun = "S";
							}
							if (mLaunchMotor.isChecked()) {
								fLaun = "M";
							}
							values.put(GliderLogTables.F_LAUNCH, fLaun);
							values.put(GliderLogTables.F_SENT, "0");
							values.put(GliderLogTables.F_ACK, "0");
							values.put(GliderLogTables.F_NOTES,"");
							// New flight
							flightUri = getContentResolver().insert(
										FlightsContentProvider.CONTENT_URI_FLIGHT, values);
						}
					}
				})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int whichButton) {
								// Do nothing.
							}
				})
				.show();
			}
		});
		
		exitButton.setOnClickListener(new View.OnClickListener() { 
			@Override
			public void onClick(View view) { 
				flg_save = true;
				setResult(RESULT_CANCELED); 
				finish();
			}
		});

		timeSButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				time_mde = 10;
				// Creating a bundle object to pass currently set time to the fragment
				Bundle b = new Bundle();
				if (!mStartText.getText().toString().isEmpty()) {
					String[] split = mStartText.getText().toString().split(":");
					hour = Integer.parseInt(split[0]);
					minute = Integer.parseInt(split[1]);
				} else {
					final Calendar c = Calendar.getInstance();
					hour = c.get(Calendar.HOUR_OF_DAY);
					minute = c.get(Calendar.MINUTE);
				}
				b.putInt("set_hour", hour);
				b.putInt("set_minute", minute);
				// Instantiating TimePickerDialogFragment & pass it' arguments
				TimePickerDialogFragment timePicker = new TimePickerDialogFragment(
						mHandler);
				timePicker.setArguments(b);
				// Getting fragment manger for this activity & start transaction
				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(timePicker, "time_picker");
				/** Opening the TimePicker fragment */
				ft.commit();
			}
		});

		timeLButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				time_mde = 20;
				// Creating a bundle object to pass currently set time to the fragment
				Bundle b = new Bundle();
				if (!mLandText.getText().toString().isEmpty()) {
					String[] split = mLandText.getText().toString().split(":");
					hour = Integer.parseInt(split[0]);
					minute = Integer.parseInt(split[1]);
				} else {
					final Calendar c = Calendar.getInstance();
					hour = c.get(Calendar.HOUR_OF_DAY);
					minute = c.get(Calendar.MINUTE);
				}
				b.putInt("set_hour", hour);
				b.putInt("set_minute", minute);
				// Instantiating TimePickerDialogFragment & pass it' arguments
				TimePickerDialogFragment timePicker = new TimePickerDialogFragment(
						mHandler);
				timePicker.setArguments(b);
				// Getting fragment manger for this activity & start transaction
				FragmentManager fm = getSupportFragmentManager();
				FragmentTransaction ft = fm.beginTransaction();
				ft.add(timePicker, "time_picker");
				/** Opening the TimePicker fragment */
				ft.commit();
			}
		});

		clearSButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String S_T = mStartText.getText().toString();
				if (S_T.isEmpty()) {
					final Calendar c = Calendar.getInstance();
					//second = c.get(Calendar.SECOND);
					//mStartText.setText(Common.TwoDigits(c.get(Calendar.HOUR_OF_DAY)) + ":" + Common.TwoDigits(c.get(Calendar.MINUTE)));
					mStartText.setText(new StringBuilder().append(Common.TwoDigits(c.get(Calendar.HOUR_OF_DAY)))
							.append(":").append(Common.TwoDigits(c.get(Calendar.MINUTE))));
				} else {
					mStartText.setText("");
				}
			}
		});

		clearLButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				String L_T = mLandText.getText().toString();
				if (L_T.isEmpty()) {
					final Calendar c = Calendar.getInstance();
					//second = c.get(Calendar.SECOND);
					//mLandText.setText(Common.TwoDigits(c.get(Calendar.HOUR_OF_DAY)) + ":" + Common.TwoDigits(c.get(Calendar.MINUTE)));
					mLandText.setText(new StringBuilder().append(Common.TwoDigits(c.get(Calendar.HOUR_OF_DAY)))
							.append(":").append(Common.TwoDigits(c.get(Calendar.MINUTE))));
				} else {
					mLandText.setText("");
				}
			}
		});
		
		mRegiSpin.setOnItemSelectedListener(new Custom0_OnItemSelectedListener());
		mPilotSpin.setOnItemSelectedListener(new Custom1_OnItemSelectedListener());
		
		gliderButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get add_glider.xml view
				LayoutInflater li = LayoutInflater.from(edi_con);
				View promptsView = li.inflate(R.layout.add_glider, null);
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(edi_con);
				// set add_glider.xml to alertdialog builder
				alertDialogBuilder.setView(promptsView);
				final EditText userInput1 = (EditText) promptsView
						.findViewById(R.id.editTextInput1);
				final EditText userInput2 = (EditText) promptsView
						.findViewById(R.id.editTextInput2);
				final CheckBox userInput3 = (CheckBox) promptsView
						.findViewById(R.id.editCheckInput3);
				final CheckBox userInput4 = (CheckBox) promptsView
						.findViewById(R.id.editCheckInput4);
				// set dialog message
				alertDialogBuilder
						.setCancelable(false)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									@SuppressLint("DefaultLocale")
									public void onClick(DialogInterface dialog,
											int id) {
										// user input, convert to UC, add to glider list & spinner
										result = userInput1.getText().toString()
												.toUpperCase();
										if (GliderList.contains(result)) {
											makeToast("Invoer extra, deze kist bestaat reeds !");
											result = null;
											dialog.cancel();
										} else {
											GliderList.add(result);
											GliderCall.add(userInput2.getText().toString().toUpperCase());
											GliderSeatsList.add(userInput3.isChecked() ? "2" : "1");
											GliderPrivateList.add(userInput4.isChecked() ? "1" : "0");
											ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
													FlightDetailActivity.this,
													android.R.layout.simple_spinner_item, GliderList);
											dataAdapter
													.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
											mRegiSpin.setAdapter(dataAdapter);
											// add to DB table
											try {
												ContentValues values = new ContentValues();
												values.put(
														GliderLogTables.G_REGISTRATION,
														result);
												getContentResolver()
														.insert(FlightsContentProvider.CONTENT_URI_GLIDER,
																values);
												values = null;
											} catch (Exception e) {
												Log.e("Exception", "Error: "
														+ e.toString());
											}
										}
									}
								})
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										result = null;
										dialog.cancel();
									}
								});
				// create alert dialog & show it
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
			}
		});

		pilotButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// get prompts.xml view
				LayoutInflater li = LayoutInflater.from(edi_con);
				View promptsView = li.inflate(R.layout.add_member, null);
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
						edi_con);
				// set prompts.xml to alertdialog builder
				alertDialogBuilder.setView(promptsView);
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.editTextDialogUserInput);
				// set dialog message
				alertDialogBuilder
						.setCancelable(false)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@Override
									@SuppressLint("DefaultLocale")
									public void onClick(DialogInterface dialog,
											int id) {
										// get user input check for at least 2
										// parts
										result = userInput.getText().toString();
										if (MemberList.contains(result)) {
											makeToast("Invoer extra, deze naam bestaat reeds !");
											result = null;
											dialog.cancel();
										} else {
											String[] name = result.split(" ");
											if (name.length < 2) {
												makeToast("Invoer extra, formaat => Voornaam (tussenvoegsel(s)) Achternaam is vereist !");
												result = null;
												dialog.cancel();
											}
											// add to member list & spinners
											MemberList.add(result);
											MemberIndexList.add("" + ini_id);
											MemberInstrList.add("0");
											ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
													FlightDetailActivity.this,
													android.R.layout.simple_spinner_item, MemberList);
											dataAdapter
													.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
											mPilotSpin.setAdapter(dataAdapter);
											mCoPilotSpin.setAdapter(dataAdapter);
											// add parts to DB table fields
											AddNewMember(name);
										}
									}
								})
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int id) {
										result = null;
										dialog.cancel();
									}
								});
				// create alert dialog & show it
				AlertDialog alertDialog = alertDialogBuilder.create();
				alertDialog.show();
			}
		});

		setMode();
	}

	public void AddNewMember(String[] nam) {

		try {
			ContentValues values = new ContentValues();
			switch (nam.length) {
			case 2:
				values.put(GliderLogTables.M_1_NAME, nam[0]);
				values.put(GliderLogTables.M_2_NAME, "");
				values.put(GliderLogTables.M_3_NAME, nam[1]);
				break;
			default:
				values.put(GliderLogTables.M_1_NAME, nam[0]);
				String mid_nam = "";
				for (int i = 1; i < nam.length - 1; i++) {
					mid_nam = mid_nam + " " + nam[i];
				}
				values.put(GliderLogTables.M_2_NAME, mid_nam.trim());
				values.put(GliderLogTables.M_3_NAME, nam[nam.length - 1]);
				break;
			}
			values.put(GliderLogTables.M_ID, ini_id++);
			getContentResolver().insert(
					FlightsContentProvider.CONTENT_URI_MEMBER, values);
			values = null;
		} catch (Exception e) {
			Log.e("Exception", "Error: " + e.toString());
		}
	}

	public void addItemSpinner1() {
		Uri uri = FlightsContentProvider.CONTENT_URI_GLIDER;
		String[] projection = { GliderLogTables.G_REGISTRATION, GliderLogTables.G_CALLSIGN,
				GliderLogTables.G_SEATS, GliderLogTables.G_PRIVATE };
		Cursor cursor = getContentResolver().query(uri, projection, null, null,
				"Registratie ASC");
		if (cursor != null) {
			cursor.moveToFirst();
			// insert dummy item with no data as to avoid pre-selection
			GliderList.add("");
			GliderCall.add("");
			GliderSeatsList.add("");
			GliderPrivateList.add("");
			for (int i = 0; i < cursor.getCount(); i++) {
				GliderList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.G_REGISTRATION)));
				GliderCall.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.G_CALLSIGN)));
				GliderSeatsList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.G_SEATS)));
				GliderPrivateList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.G_PRIVATE)));
				cursor.moveToNext();
			}
			cursor.close();
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
					FlightDetailActivity.this,
					android.R.layout.simple_spinner_item, GliderList);
			dataAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mRegiSpin.setAdapter(dataAdapter);
		}
	}

	public void addItemSpinner2() {
		Uri uri = FlightsContentProvider.CONTENT_URI_MEMBER;
		String[] projection = { GliderLogTables.M_ID, GliderLogTables.M_2_NAME,
				GliderLogTables.M_3_NAME, GliderLogTables.M_1_NAME,
				GliderLogTables.M_INSTRUCTION };
		Cursor cursor = getContentResolver().query(uri, projection, null, null,
				"Voornaam ASC");
		if (cursor != null) {
			cursor.moveToFirst();
			// insert dummy item with no data as to avoid pre-selection
			MemberList.add("");
			//MemberTrainList.add("");
			MemberIndexList.add("");
			MemberInstrList.add("");
			for (int i = 0; i < cursor.getCount(); i++) {
				String tmp = cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_1_NAME))
						+ " "
						+ cursor.getString(cursor
								.getColumnIndexOrThrow(GliderLogTables.M_2_NAME))
						+ " "
						+ cursor.getString(cursor
								.getColumnIndexOrThrow(GliderLogTables.M_3_NAME));
				MemberList.add(tmp.replaceAll("\\s+", " "));
				MemberIndexList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_ID)));
		//		MemberTrainList.add(cursor.getString(cursor
		//				.getColumnIndexOrThrow(GliderLogTables.M_TRAIN)));
				MemberInstrList.add(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_INSTRUCTION)));
				// some logic add as to deal with new members with dummy id
				if (Integer.parseInt(cursor.getString(cursor
						.getColumnIndexOrThrow(GliderLogTables.M_ID))) >= ini_id) {
					ini_id = Integer.parseInt(cursor.getString(cursor
							.getColumnIndexOrThrow(GliderLogTables.M_ID))) + 1;
					Log.d(TAG, "key " + ini_id);
				}
				cursor.moveToNext();
			}
			cursor.close();
			ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
					FlightDetailActivity.this,
					android.R.layout.simple_spinner_item, MemberList);
			dataAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mPilotSpin.setAdapter(dataAdapter);
			mCoPilotSpin.setAdapter(dataAdapter);
		}
	}

	public void setInstruction(int src) {
		// if not 'NORM' than kill instruction flag
		Log.d(TAG, "fired by " + src + " !");
		if (!mTypeNorm.isChecked()) {
			mInstChck.setChecked(false);
			return;
		}
		// if this isn't a two seater then kill the instruction flag
		String fregi = (String) mRegiSpin.getSelectedItem();
		for (int i = 0; i < mRegiSpin.getCount(); i++) {
			String s = (String) mRegiSpin.getItemAtPosition(i);
			if (s.equalsIgnoreCase(fregi)) {
				if (!GliderSeatsList.get(i).equals("2")) {
					mInstChck.setChecked(false);
					return;
				}
			}
		}
		// if no co-pilot used then kill the instruction flag
		if (mCoPilotSpin.getSelectedItem().equals("")) {
			mInstChck.setChecked(false);
			return;
		}
		// if pilot is an instructor then set the instruction flag
		String fPilo = (String) mPilotSpin.getSelectedItem();
		for (int i = 0; i < mPilotSpin.getCount(); i++) {
			String s = (String) mPilotSpin.getItemAtPosition(i);
			if (s.equalsIgnoreCase(fPilo)) {
				if (MemberInstrList.get(i).equals("1")) {
					mInstChck.setChecked(true);
				}
			}
		}
	}

	public void setMode() {
		// hide soft keyboard on app launch
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	private void fillData(Uri uri) {
		String[] projection = { 
				GliderLogTables.F_DATE, GliderLogTables.F_REGISTRATION, 
				GliderLogTables.F_CALLSIGN, GliderLogTables.F_TYPE,
				GliderLogTables.F_INSTRUCTION, GliderLogTables.F_PILOT,
				GliderLogTables.F_COPILOT, GliderLogTables.F_STARTED,
				GliderLogTables.F_LANDED, GliderLogTables.F_DURATION,
				GliderLogTables.F_LAUNCH, GliderLogTables.F_NOTES };
		Cursor cursor = getContentResolver().query(uri, projection, null, null,
				null);
		if (cursor != null) {
			cursor.moveToFirst();
			mDateText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_DATE)));
			String sp_regi = cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_REGISTRATION));
			mCallSign = cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_CALLSIGN));
			for (int i = 0; i < mRegiSpin.getCount(); i++) {
				String s = (String) mRegiSpin.getItemAtPosition(i);
				if (s.equalsIgnoreCase(sp_regi)) {
					mRegiSpin.setSelection(i);
				}
			}

			// get record value for type and set controls
			String sp_type = cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_TYPE));
			mTypeNorm.setChecked(false);
			mTypePass.setChecked(false);
			mTypeDona.setChecked(false);
			mTypeClub.setChecked(false);
			if (sp_type.equals("")) {
				mTypeNorm.setChecked(true);
			}
			if (sp_type.equals("PASS")) {
				mTypePass.setChecked(true);
			}
			if (sp_type.equals("DONA")) {
				mTypeDona.setChecked(true);
			}
			if (sp_type.equals("CLUB")) {
				mTypeClub.setChecked(true);
			}

			// get record value for pilot and set spinner
			String sp_pilo = cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_PILOT));
			for (int i = 0; i < mPilotSpin.getCount(); i++) {
				String s = (String) mPilotSpin.getItemAtPosition(i);
				if (s.equalsIgnoreCase(sp_pilo)) {
					mPilotSpin.setSelection(i);
				}
			}
			// get record value for copilot and set spinner
			String sp_copi = cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_COPILOT));
			for (int i = 0; i < mCoPilotSpin.getCount(); i++) {
				String s = (String) mCoPilotSpin.getItemAtPosition(i);
				if (s.equalsIgnoreCase(sp_copi)) {
					mCoPilotSpin.setSelection(i);
				}
			}
			if (cursor
					.getString(
							cursor.getColumnIndexOrThrow(GliderLogTables.F_INSTRUCTION))
					.equals("N")) {
				mInstChck.setChecked(false);
			}
			if (cursor
					.getString(
							cursor.getColumnIndexOrThrow(GliderLogTables.F_INSTRUCTION))
					.equals("J")) {
				mInstChck.setChecked(true);
			}
			mStartText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_STARTED)));
			mLandText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_LANDED)));
			mDuraText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_DURATION)));
			String sp_laun = cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_LAUNCH));
			mLaunchWinch.setChecked(false);
			mLaunchTow.setChecked(false);
			mLaunchMotor.setChecked(false);
			if (sp_laun.equals("L")) {
				mLaunchWinch.setChecked(true);
			}
			if (sp_laun.equals("S")) {
				mLaunchTow.setChecked(true);
			}
			if (sp_laun.equals("M")) {
				mLaunchMotor.setChecked(true);
			}
			mBodyText.setText(cursor.getString(cursor
					.getColumnIndexOrThrow(GliderLogTables.F_NOTES)));
			// always close the cursor
			cursor.close();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		saveState();
		outState.putParcelable(FlightsContentProvider.CONTENT_ITEM_TYPE,
				flightUri);
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveState();
	}

	private void saveState() {
		if (flg_save) {
			flg_save = false;
			return;
		}
		String fdate = mDateText.getText().toString();
		String fregi = (String) mRegiSpin.getSelectedItem();

		String ftype = "";
		if (mTypeNorm.isChecked()) {
			ftype = "";
		}
		if (mTypePass.isChecked()) {
			ftype = "PASS";
		}
		if (mTypeDona.isChecked()) {
			ftype = "DONA";
		}
		if (mTypeClub.isChecked()) {
			ftype = "CLUB";
		}

		String finst = (mInstChck.isChecked()) ? "J" : "N";
		String fPilo = (String) mPilotSpin.getSelectedItem();
		String fPilo_id = "";
		for (int i = 0; i < mPilotSpin.getCount(); i++) {
			String s = (String) mPilotSpin.getItemAtPosition(i);
			if (s.equalsIgnoreCase(fPilo)) {
				fPilo_id = MemberIndexList.get(i);
			}
		}
		String fCoPi = (String) mCoPilotSpin.getSelectedItem();
		String fCoPi_id = "";
		for (int i = 0; i < mCoPilotSpin.getCount(); i++) {
			String s = (String) mCoPilotSpin.getItemAtPosition(i);
			if (s.equalsIgnoreCase(fCoPi)) {
				fCoPi_id = MemberIndexList.get(i);
			}
		}
		String fstrt = mStartText.getText().toString();
		String fland = mLandText.getText().toString();
		String fdura = "";
		// ok now we need to calculate our flight time
		if (!(fstrt.isEmpty() || fland.isEmpty())) {
			SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm");
			try {
				Date d1 = format.parse(fdate + " " + fstrt);
				Date d2 = format.parse(fdate + " " + fland);
				long diff = d2.getTime() - d1.getTime();
				int diffSeconds = (int) diff / 1000 % 60;
				int diffMinutes = (int) diff / (60 * 1000) % 60;
				if (diffSeconds >= 30) {
					diffMinutes++;
				}
				int diffHours = (int) diff / (60 * 60 * 1000) % 24;
				if (diffMinutes >= 60) {
					diffHours++;
					diffMinutes = diffMinutes - 60;
				}
				if ((diffHours < 0) || (diffMinutes < 00)) {
					makeToast("Start tijd later dan Landings tijd, juiste waardes invullen");
					return;
				}
				fdura = Common.TwoDigits(diffHours) + ":" + Common.TwoDigits(diffMinutes);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		String fLaun = "";
		if (mLaunchWinch.isChecked()) {
			fLaun = "L";
		}
		if (mLaunchTow.isChecked()) {
			fLaun = "S";
		}
		if (mLaunchMotor.isChecked()) {
			fLaun = "M";
		}
		String description = mBodyText.getText().toString();
		// only save if either summary or description is available
		if (description.length() == 0 && fregi.length() == 0) {
			return;
		}

		ContentValues values = new ContentValues();
		values.put(GliderLogTables.F_DATE, fdate);
		values.put(GliderLogTables.F_REGISTRATION, fregi);
		values.put(GliderLogTables.F_CALLSIGN, mCallSign);
		values.put(GliderLogTables.F_TYPE, ftype);
		values.put(GliderLogTables.F_INSTRUCTION, finst);
		values.put(GliderLogTables.F_PILOT, fPilo);
		values.put(GliderLogTables.F_PILOT_ID, fPilo_id);
		values.put(GliderLogTables.F_COPILOT, fCoPi);
		values.put(GliderLogTables.F_COPILOT_ID, fCoPi_id);
		values.put(GliderLogTables.F_STARTED, fstrt);
		values.put(GliderLogTables.F_LANDED, fland);
		values.put(GliderLogTables.F_DURATION, fdura);
		values.put(GliderLogTables.F_LAUNCH, fLaun);
		values.put(GliderLogTables.F_SENT, "0");
		values.put(GliderLogTables.F_ACK, "0");
		values.put(GliderLogTables.F_NOTES, description);

		if (flightUri == null) {
			// New flight
			flightUri = getContentResolver().insert(
					FlightsContentProvider.CONTENT_URI_FLIGHT, values);
		} else {
			// Update flight
			getContentResolver().update(flightUri, values, null, null);
		}
	}

	private void makeToast(String msg) {
		Toast.makeText(FlightDetailActivity.this, msg, Toast.LENGTH_LONG)
				.show();
	}

	public class Custom0_OnItemSelectedListener implements OnItemSelectedListener {
		 
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
			if (GliderSeatsList.get(pos).equals("1")) {
				mInstChck.setChecked(false);
				flg_two = false;
				mCoPilotSpin.setSelection(0);
				mCoPilotSpin.setClickable(false);
				mCoPilotSpin.setFocusable(false);
			} else {
				if (GliderPrivateList.get(pos).equals("0")) {
					mInstChck.setChecked(true);
					flg_two = true;
				} else {
					mInstChck.setChecked(false);
					flg_two = false;
				}
				mCoPilotSpin.setClickable(true);
				mCoPilotSpin.setFocusable(true);
			}
			mCallSign = GliderCall.get(pos);
		}
		 
		@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}
	}
	
	public class Custom1_OnItemSelectedListener implements OnItemSelectedListener {
		 
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
			if (MemberInstrList.get(pos).equals("0")) {
				if (flg_two) 
					mInstChck.setChecked(false);
			} else {
				if (flg_two) 
					mInstChck.setChecked(true);
			}
		}
		 
		@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
		}
	}
}