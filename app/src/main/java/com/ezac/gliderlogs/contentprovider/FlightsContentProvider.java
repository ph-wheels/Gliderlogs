package com.ezac.gliderlogs.contentprovider;

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

import java.util.Arrays;
import java.util.HashSet;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.ezac.gliderlogs.database.GliderDatabaseHelper;
import com.ezac.gliderlogs.database.GliderLogTables;

public class FlightsContentProvider extends ContentProvider {

	// database
	private GliderDatabaseHelper database;

	// used for the UriMacher
	private static final int FLIGHTS = 10;
	private static final int FLIGHT_ID = 20;
	private static final int GLIDERS = 30;
	private static final int MEMBERS = 50;
	private static final int EXTERN = 70;
	private static final int DUTIES = 80;
	private static final int RESERV = 90;

	private static final String AUTHORITY = "com.ezac.gliderlogs.contentprovider";

	private static final String PATH_FLIGHT = "flights";
	private static final String PATH_GLIDER = "gliders";
	private static final String PATH_MEMBER = "members";
	private static final String PATH_EXTERN = "passengers";
	private static final String PATH_DUTIES = "rooster";
	private static final String PATH_RESERV = "reservation";

	public static final Uri CONTENT_URI_FLIGHT = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_FLIGHT);
	public static final Uri CONTENT_URI_GLIDER = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_GLIDER);
	public static final Uri CONTENT_URI_MEMBER = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_MEMBER);
	public static final Uri CONTENT_URI_EXTERN = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_EXTERN);
	public static final Uri CONTENT_URI_DUTIES = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_DUTIES);
	public static final Uri CONTENT_URI_RESERV = Uri.parse("content://"
			+ AUTHORITY + "/" + PATH_RESERV);

	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
			+ "/flights";

	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
			+ "/flight";

	private static final UriMatcher sURIMatcher = new UriMatcher(
			UriMatcher.NO_MATCH);
	static {
		sURIMatcher.addURI(AUTHORITY, PATH_FLIGHT, FLIGHTS);
		sURIMatcher.addURI(AUTHORITY, PATH_FLIGHT + "/#", FLIGHT_ID);
		sURIMatcher.addURI(AUTHORITY, PATH_GLIDER, GLIDERS);
		sURIMatcher.addURI(AUTHORITY, PATH_MEMBER, MEMBERS);
		sURIMatcher.addURI(AUTHORITY, PATH_EXTERN, EXTERN);
		sURIMatcher.addURI(AUTHORITY, PATH_DUTIES, DUTIES);
		sURIMatcher.addURI(AUTHORITY, PATH_RESERV, RESERV);
	}

	@Override
	public boolean onCreate() {
		database = new GliderDatabaseHelper(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		//Log.d ("---","sorted by " + " 1-" + projection[0]+";"+projection[0] + " 2-" + selection + " 3-" + sortOrder);
		// Using SQLiteQueryBuilder instead of query() method
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// check if the caller has requested a column which does not exists
		checkColumns(projection);

		// Set the table
		queryBuilder.setTables(GliderLogTables.TABLE_STARTS);

		int uriType = sURIMatcher.match(uri);
		switch (uriType) {
		case FLIGHTS:
			break;
		case FLIGHT_ID:
			// adding the ID to the original query
			queryBuilder.appendWhere(GliderLogTables.F_ID + "="
					+ uri.getLastPathSegment());
			break;
		case GLIDERS:
			queryBuilder.setTables(GliderLogTables.TABLE_GLIDERS);
			break;
		case MEMBERS:
			queryBuilder.setTables(GliderLogTables.TABLE_MEMBERS);
			break;
		case EXTERN:
			queryBuilder.setTables(GliderLogTables.TABLE_EXTERN);
			break;
		case DUTIES:
			queryBuilder.setTables(GliderLogTables.TABLE_DUTIES);
			break;
		case RESERV:
			queryBuilder.setTables(GliderLogTables.TABLE_RESERV);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		SQLiteDatabase db = database.getWritableDatabase();
		Cursor cursor = queryBuilder.query(db, projection, selection,
				selectionArgs, null, null, sortOrder);
		// make sure that potential listeners are getting notified
		cursor.setNotificationUri(getContext().getContentResolver(), uri);
		return cursor;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		long id = 0;
		switch (uriType) {
		case FLIGHTS:
			id = sqlDB.insert(GliderLogTables.TABLE_STARTS, null, values);
			break;
		case GLIDERS:
			id = sqlDB.insert(GliderLogTables.TABLE_GLIDERS, null, values);
			break;
		case MEMBERS:
			id = sqlDB.insert(GliderLogTables.TABLE_MEMBERS, null, values);
			break;
		case EXTERN:
			id = sqlDB.insert(GliderLogTables.TABLE_EXTERN, null, values);
			break;
		case DUTIES:
			id = sqlDB.insert(GliderLogTables.TABLE_DUTIES, null, values);
			break;
		case RESERV:
			id = sqlDB.insert(GliderLogTables.TABLE_RESERV, null, values);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return Uri.parse(PATH_FLIGHT + "/" + id);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsDeleted = 0;
		switch (uriType) {
		case FLIGHTS:
			rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_STARTS, selection,
					selectionArgs);
			break;
		case FLIGHT_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_STARTS,
						GliderLogTables.F_ID + "=" + id, null);
			} else {
				rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_STARTS,
						GliderLogTables.F_ID + "=" + id + " and " + selection,
						selectionArgs);
			}
			break;
		case GLIDERS:
			rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_GLIDERS,
					selection, selectionArgs);
			break;
		case MEMBERS:
			rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_MEMBERS,
					selection, selectionArgs);
		case EXTERN:
			rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_EXTERN, selection,
					selectionArgs);
			break;
		case DUTIES:
			rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_DUTIES, selection,
					selectionArgs);
			break;
		case RESERV:
			rowsDeleted = sqlDB.delete(GliderLogTables.TABLE_RESERV, selection,
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsDeleted;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {

		int uriType = sURIMatcher.match(uri);
		SQLiteDatabase sqlDB = database.getWritableDatabase();
		int rowsUpdated = 0;
		switch (uriType) {
		case FLIGHTS:
			rowsUpdated = sqlDB.update(GliderLogTables.TABLE_STARTS, values,
					selection, selectionArgs);
			break;
		case FLIGHT_ID:
			String id = uri.getLastPathSegment();
			if (TextUtils.isEmpty(selection)) {
				rowsUpdated = sqlDB.update(GliderLogTables.TABLE_STARTS,
						values, GliderLogTables.F_ID + "=" + id, null);
			} else {
				rowsUpdated = sqlDB.update(GliderLogTables.TABLE_STARTS,
						values, GliderLogTables.F_ID + "=" + id + " and "
								+ selection, selectionArgs);
			}
			break;
		case GLIDERS:
			break;
		case MEMBERS:
			break;
		case EXTERN:
			break;
		case DUTIES:
			break;
		case RESERV:
			break;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return rowsUpdated;
	}

	private void checkColumns(String[] projection) {
		String[] available = { GliderLogTables.F_ID, GliderLogTables.F_DATE,
				GliderLogTables.F_STARTED, GliderLogTables.F_LANDED,
				GliderLogTables.F_DURATION, GliderLogTables.F_TYPE,
				GliderLogTables.F_LAUNCH, GliderLogTables.F_REGISTRATION,
				GliderLogTables.F_CALLSIGN, GliderLogTables.F_PILOT,
				GliderLogTables.F_COPILOT, GliderLogTables.F_PILOT_ID,
				GliderLogTables.F_COPILOT_ID, GliderLogTables.F_INSTRUCTION,
				GliderLogTables.F_NOTES, GliderLogTables.F_SENT,
				GliderLogTables.F_ACK, GliderLogTables.F_DEL,
				GliderLogTables.F_HID, GliderLogTables.G_REGISTRATION,
				GliderLogTables.G_SEATS, GliderLogTables.G_CALLSIGN,
				GliderLogTables.G_TYPE, GliderLogTables.G_BUILD,
				GliderLogTables.G_OWNER, GliderLogTables.G_PRIVATE,
				GliderLogTables.M_1_NAME, GliderLogTables.M_2_NAME,
				GliderLogTables.M_3_NAME, GliderLogTables.M_ID,
				GliderLogTables.M_ACTIVE, GliderLogTables.M_TRAIN,
				GliderLogTables.M_INSTRUCTION, GliderLogTables.M_ABBREV,
				GliderLogTables.M_PHONE, GliderLogTables.M_MOBILE,
				GliderLogTables.M_ADRS, GliderLogTables.M_POST,
				GliderLogTables.M_CITY, GliderLogTables.M_CNTRY,
				GliderLogTables.M_CODE, GliderLogTables.M_ACTIVE,
				GliderLogTables.E_ID, GliderLogTables.E_DATE,
				GliderLogTables.E_TIME, GliderLogTables.E_NAME,
				GliderLogTables.E_PHONE, GliderLogTables.R_ID,
				GliderLogTables.R_PERIOD, GliderLogTables.R_TYPE,
				GliderLogTables.R_NAME_ID, GliderLogTables.R_PURPOSE,
				GliderLogTables.R_RESERVE, GliderLogTables.R_STAMPED,
				GliderLogTables.R_1_NAME, GliderLogTables.R_2_NAME,
				GliderLogTables.R_3_NAME, GliderLogTables.D_DATE,
				GliderLogTables.D_PERIOD, GliderLogTables.D_P_NAME,
				GliderLogTables.D_DUTY, GliderLogTables.D_NAME,
				};
		if (projection != null) {
			HashSet<String> requestedColumns = new HashSet<String>(
					Arrays.asList(projection));
			HashSet<String> availableColumns = new HashSet<String>(
					Arrays.asList(available));
			// check if all columns which are requested are available
			if (!availableColumns.containsAll(requestedColumns)) {
				throw new IllegalArgumentException(
						"Unknown columns in projection");
			}
		}
	}

}