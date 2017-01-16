package com.ezac.gliderlogs.database;

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

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class GliderLogTables {

	// Database table fields for flights
	public static final String TABLE_STARTS = "flights";
	public static final String F_ID = "_id";
	public static final String F_DATE = "datum";
	public static final String F_STARTED = "started";
	public static final String F_LANDED = "landed";
	public static final String F_DURATION = "duur";
	public static final String F_TYPE = "soort";
	public static final String F_LAUNCH = "methode";
	public static final String F_REGISTRATION = "registratie";
	public static final String F_CALLSIGN = "callsign";
	public static final String F_PILOT = "piloot";
	public static final String F_PILOT_ID = "piloot_id";
	public static final String F_COPILOT = "tweede";
	public static final String F_COPILOT_ID = "tweede_id";
	public static final String F_INSTRUCTION = "instructie";
	public static final String F_NOTES = "opmerking";
	public static final String F_SENT = "sent";
	public static final String F_ACK = "ack";
	public static final String F_DEL = "del";
	public static final String F_HID = "h_id";

	// Database creation SQL statement
	private static final String DATABASE_F_CREATE = "CREATE TABLE "
			+ TABLE_STARTS + "(" + F_ID
			+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + F_DATE + " TEXT, "
			+ F_STARTED + " TEXT, " + F_LANDED + " TEXT, " + F_DURATION
			+ " TEXT, " + F_TYPE + " TEXT, " + F_LAUNCH + " TEXT, "
			+ F_REGISTRATION + " TEXT, " + F_CALLSIGN + " TEXT, " + F_PILOT
			+ " TEXT, " + F_PILOT_ID + " TEXT, " + F_COPILOT + " TEXT, "
			+ F_COPILOT_ID + " TEXT, " + F_INSTRUCTION + " TEXT, " + F_NOTES
			+ " TEXT, " + F_SENT + " INTEGER DEFAULT 0, " + F_ACK
			+ " INTEGER DEFAULT 0, " + F_DEL + " INTEGER DEFAULT 0, " + F_HID
			+ " INTEGER DEFAULT 0 " + ");";

	// Database table fields for members
	public static final String TABLE_MEMBERS = "members";
	public static final String M_ID = "_id";
	public static final String M_2_NAME = "voorvoeg";
	public static final String M_3_NAME = "achternaam";
	public static final String M_1_NAME = "voornaam";
	public static final String M_ABBREV = "afkorting";
	public static final String M_ADRS = "adres";
	public static final String M_POST = "postcode";
	public static final String M_CITY = "plaats";
	public static final String M_CNTRY = "land";
	public static final String M_PHONE = "telefoon";
	public static final String M_MOBILE = "mobiel";
	public static final String M_CODE = "code";
	public static final String M_ACTIVE = "Actief";
	public static final String M_TRAIN = "Leerling";
	public static final String M_INSTRUCTION = "instructie";
	public static final String M_MAIL = "e_mail";
	public static final String M_TMP = "tmp";

	// Database creation SQL statement
	private static final String DATABASE_M_CREATE = "CREATE TABLE "
			+ TABLE_MEMBERS + "(" + M_ID + " INTEGER PRIMARY KEY, "
			+ M_2_NAME + " TEXT, " + M_3_NAME + " TEXT NOT NULL, "
			+ M_1_NAME + " TEXT NOT NULL, " + M_ABBREV + " TEXT, "
			+ M_ADRS + " TEXT, " + M_POST + " TEXT, "
			+ M_CITY + " TEXT, " + M_CNTRY + " TEXT, "
			+ M_PHONE + " TEXT, " + M_MOBILE + " TEXT, "
			+ M_CODE + " TEXT, " + M_ACTIVE + " INTEGER DEFAULT 0, "
			+ M_TRAIN + " INTEGER DEFAULT 0, "
			+ M_INSTRUCTION + " INTEGER DEFAULT 0, "
			+ M_MAIL + " TEXT, "
			+ M_TMP + " INTEGER DEFAULT 0 " + ");";

	// Database table fields for gliders
	public static final String TABLE_GLIDERS = "gliders";
	public static final String G_REGISTRATION = "registratie";
	public static final String G_CALLSIGN = "callsign";
	public static final String G_TYPE = "type";
	public static final String G_BUILD = "bouwjaar";
	public static final String G_SEATS = "inzittenden";
	public static final String G_OWNER = "eigenaar";
	public static final String G_PRIVATE = "prive";

	// Database creation SQL statement
	public static final String DATABASE_G_CREATE = "CREATE TABLE "
			+ TABLE_GLIDERS + "(" + G_REGISTRATION + " TEXT PRIMARY KEY, "
			+ G_CALLSIGN + " TEXT, " + G_TYPE + " TEXT, " + G_BUILD + " TEXT, "
			+ G_SEATS + " INTEGER DEFAULT 0, " + G_OWNER + " TEXT, "
			+ G_PRIVATE + " INTEGER DEFAULT 0 " + ");";

	// Database table fields for external passengers
	public static final String TABLE_EXTERN = "passengers";
	public static final String E_ID = "_id";
	public static final String E_DATE = "datum";
	public static final String E_TIME = "tijd";
	public static final String E_NAME = "naam";
	public static final String E_PHONE = "telefoon";
	public static final String E_REF_BY = "aanmaker";

	// Database creation SQL statement
	public static final String DATABASE_E_CREATE = "CREATE TABLE "
			+ TABLE_EXTERN + "(" + E_ID + " TEXT, " + E_DATE + " TEXT, "
			+ E_TIME + " TEXT, " + E_NAME + " TEXT, " + E_PHONE + " TEXT, "
			+ E_REF_BY + " TEXT " + ");";

	// Database table fields for external passengers
	public static final String TABLE_RESERV = "reservation";
	public static final String R_ID = "_id";
	public static final String R_DATE = "datum";
	public static final String R_PERIOD = "periode";
	public static final String R_TYPE = "soort";
	public static final String R_NAME_ID = "leden_id";
	public static final String R_PURPOSE = "doel";
	public static final String R_RESERVE = "reserve";
	public static final String R_STAMPED = "aangemaakt";
	public static final String R_1_NAME = "voornaam";
	public static final String R_2_NAME = "voorvoeg";
	public static final String R_3_NAME = "achternaam";

	// Database creation SQL statement
	public static final String DATABASE_R_CREATE = "CREATE TABLE "
			+ TABLE_RESERV + "(" + R_ID + " TEXT, " + R_DATE + " TEXT, "
			+ R_PERIOD + " TEXT, " + R_TYPE + " TEXT, " + R_NAME_ID + " TEXT, "
			+ R_PURPOSE + " TEXT, " + R_RESERVE + " TEXT, " + R_STAMPED
			+ " TEXT, " + R_1_NAME + " TEXT, " + R_2_NAME + " TEXT, "
			+ R_3_NAME + " TEXT " + ");";
	
	// Database table fields for external passengers
	public static final String TABLE_DUTIES = "rooster";
	public static final String D_DATE = "datum";
	public static final String D_PERIOD = "periode";
	public static final String D_P_NAME = "periode_naam";
	public static final String D_DUTY = "dienst";
	public static final String D_NAME = "naam";
	
	// Database creation SQL statement
	public static final String DATABASE_D_CREATE = "CREATE TABLE "
			+ TABLE_DUTIES + "(" + D_DATE + " TEXT, " + D_PERIOD + " TEXT, "
			+ D_P_NAME + " TEXT, " + D_DUTY + " TEXT, " + D_NAME + " TEXT " 
			+ ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(DATABASE_F_CREATE);
		database.execSQL(DATABASE_M_CREATE);
		database.execSQL(DATABASE_G_CREATE);
		database.execSQL(DATABASE_E_CREATE);
		database.execSQL(DATABASE_R_CREATE);
		database.execSQL(DATABASE_D_CREATE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(GliderLogTables.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_STARTS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_MEMBERS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_GLIDERS);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_EXTERN);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_RESERV);
		database.execSQL("DROP TABLE IF EXISTS " + TABLE_DUTIES);
		onCreate(database);
	}

}