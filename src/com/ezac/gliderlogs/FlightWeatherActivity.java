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

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoup.Jsoup;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;

import com.ezac.gliderlogs.FlightOverviewActivity;

public class FlightWeatherActivity extends FragmentActivity {

	protected static String TAG = "WeatherReport";

	// private LinearLayout layout;
	final Context context = FlightWeatherActivity.this;

	private EditText nameText;
	private EditText windText;
	private EditText wdpkText;
	private EditText rainText;
	private EditText presText;
	private EditText humiText;
	private EditText wdgrText;
	private EditText wdirText;
	private EditText tempText;
	private EditText wolkText;
	private EditText sunuText;
	private EditText sundText;
	private EditText mtr1Text;
	private EditText mtr2Text;
	private EditText knmiText;
	private String appLND;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			appLND = extras.getString("Ref");
		}
		setContentView(R.layout.weather_list);
		// hide soft keyboard
		setMode();
		// disable these fields, these are output only
		nameText = (EditText) findViewById(R.id.editText61);
		nameText.setClickable(false);
		nameText.setFocusable(false);
		rainText = (EditText) findViewById(R.id.editText61a);
		rainText.setClickable(false);
		rainText.setFocusable(false);
		windText = (EditText) findViewById(R.id.editText62);
		windText.setClickable(false);
		windText.setFocusable(false);
		wdpkText = (EditText) findViewById(R.id.editText62a);
		wdpkText.setClickable(false);
		wdpkText.setFocusable(false);
		presText = (EditText) findViewById(R.id.editText63);
		presText.setClickable(false);
		presText.setFocusable(false);
		humiText = (EditText) findViewById(R.id.editText64);
		humiText.setClickable(false);
		humiText.setFocusable(false);
		wdgrText = (EditText) findViewById(R.id.editText65);
		wdgrText.setClickable(false);
		wdgrText.setFocusable(false);
		wdirText = (EditText) findViewById(R.id.editText66);
		wdirText.setClickable(false);
		wdirText.setFocusable(false);
		tempText = (EditText) findViewById(R.id.editText67);
		tempText.setClickable(false);
		tempText.setFocusable(false);
		wolkText = (EditText) findViewById(R.id.editText67a);
		wolkText.setClickable(false);
		wolkText.setFocusable(false);
		sunuText = (EditText) findViewById(R.id.editText68);
		sunuText.setClickable(false);
		sunuText.setFocusable(false);
		sundText = (EditText) findViewById(R.id.editText69);
		sundText.setClickable(false);
		sundText.setFocusable(false);
		mtr1Text = (EditText) findViewById(R.id.editText70);
		mtr1Text.setClickable(false);
		mtr1Text.setFocusable(false);
		mtr2Text = (EditText) findViewById(R.id.editText71);
		mtr2Text.setClickable(false);
		mtr2Text.setFocusable(false);
		knmiText = (EditText) findViewById(R.id.editText81);
		knmiText.setClickable(false);
		knmiText.setFocusable(false);

		Button close = (Button) findViewById(R.id.button_close);
		WebView browser = (WebView) findViewById(R.id.webview_1);

		browser.getSettings().setLoadsImagesAutomatically(true);
		// browser.getSettings().setJavaScriptEnabled(true);
		browser.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		browser.loadUrl("http://www.buienradar.nl/images.aspx?jaar=-3&soort=sp-loop");

		close.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setResult(RESULT_OK);
				finish();
			}
		});

		new NET1_Task().execute("http://xml.buienradar.nl", "1;"
				+ FlightOverviewActivity.appMST);
		String s[] = FlightOverviewActivity.appMTR.split(";");
		new NET1_Task().execute(
				"http://aviationweather.gov/adds/dataserver_current/httpparam?"
					+ "dataSource=metars&requestType=retrieve&format=xml&stationString=EHFS&"
					+ "hoursBeforeNow=3&mostRecent=true", "2;" + s[0]);
		new NET1_Task().execute(
				"http://aviationweather.gov/adds/dataserver_current/httpparam?"
					+ "dataSource=metars&requestType=retrieve&format=xml&stationString=EHWO&"
					+ "hoursBeforeNow=3&mostRecent=true", "3;" + s[1]);
		new NET2_Task().execute(
				"http://www.knmi.nl/waarschuwingen_en_verwachtingen/luchtvaart/"
					+ "weerbulletin_kleine_luchtvaart.html", "4;0");
	}
	
	public String SelToAngle(String Sel) {
		String[] mTestArray;
		mTestArray = getResources().getStringArray(R.array.heading_arrays);
		for (int i = 0; i < mTestArray.length; i++) {
			if (mTestArray[i].equals(Sel)) {
				return Double.toString(i * 22.5);
			}
		}
		return "00.0";
	}

	public void setMode() {
		// hide soft keyboard on app launch
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
	}

	class NET1_Task extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected String doInBackground(String... params) {

			String Ret_Sts = "";

			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);

			try {

				URL url = new URL(params[0]);
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder();
				Document doc = db.parse(new InputSource(url.openStream()));
				doc.getDocumentElement().normalize();
				String p[] = params[1].split(";");
				// Log.d("xml","param -" + params[1] + "___" + p[0] + "===" + p[1]);
				if (p[0].equals("1")) {
					// get all relevant nodes for this 'station'
					NodeList nodeList = doc.getElementsByTagName("weerstation");
					for (int i = 0; i < nodeList.getLength(); i++) {

						Node node = nodeList.item(i);

						Element fstElmnt = (Element) node;
						NodeList codeList = fstElmnt
								.getElementsByTagName("stationcode");
						Element codeElement = (Element) codeList.item(0);
						codeList = codeElement.getChildNodes();
						// Log.d("xml","-"+((Node) codeList.item(0)).getNodeValue()+"-");
						if (codeList.item(0).getNodeValue().equals(
								p[1])) {
							Ret_Sts = "1;";
							NodeList nameList = fstElmnt
									.getElementsByTagName("stationnaam");
							Element nameElement = (Element) nameList.item(0);
							nameList = nameElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ nameList.item(0).getNodeValue()
											.replace("Meetstation ", "") + ";";
							NodeList windList = fstElmnt
									.getElementsByTagName("windsnelheidMS");
							Element windElement = (Element) windList.item(0);
							windList = windElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ windList.item(0).getNodeValue()
									+ ";";
							NodeList presList = fstElmnt
									.getElementsByTagName("luchtdruk");
							Element presElement = (Element) presList.item(0);
							presList = presElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ presList.item(0).getNodeValue()
									+ ";";
							NodeList humiList = fstElmnt
									.getElementsByTagName("luchtvochtigheid");
							Element humiElement = (Element) humiList.item(0);
							humiList = humiElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ humiList.item(0).getNodeValue()
									+ ";";
							NodeList wdgrList = fstElmnt
									.getElementsByTagName("windrichtingGR");
							Element wdgrElement = (Element) wdgrList.item(0);
							wdgrList = wdgrElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ wdgrList.item(0).getNodeValue()
									+ ";";
							NodeList wdirList = fstElmnt
									.getElementsByTagName("windrichting");
							Element wdirElement = (Element) wdirList.item(0);
							wdirList = wdirElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ wdirList.item(0).getNodeValue()
									+ ";";
							NodeList tempList = fstElmnt
									.getElementsByTagName("temperatuurGC");
							Element tempElement = (Element) tempList.item(0);
							tempList = tempElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ tempList.item(0).getNodeValue()
									+ ";";
							//
							NodeList wdpkList = fstElmnt
									.getElementsByTagName("windstotenMS");
							Element wdpkElement = (Element) wdpkList.item(0);
							wdpkList = wdpkElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ wdpkList.item(0).getNodeValue()
									+ ";";
							//
							NodeList rainList = fstElmnt
									.getElementsByTagName("regenMMPU");
							Element rainElement = (Element) rainList.item(0);
							rainList = rainElement.getChildNodes();
							Ret_Sts = Ret_Sts
									+ rainList.item(0).getNodeValue()
									+ ";";
							//
						}
					}

					nodeList = doc.getElementsByTagName("buienradar");
					Node node = nodeList.item(0);
					Element fstElmnt = (Element) node;
					// sun up
					NodeList snupList = fstElmnt
							.getElementsByTagName("zonopkomst");
					Element snupElement = (Element) snupList.item(0);
					snupList = snupElement.getChildNodes();
					String[] sup = snupList.item(0).getNodeValue()
							.split(" ");
					Ret_Sts = Ret_Sts + sup[1] + ";";
					// sun down
					NodeList sndnList = fstElmnt
							.getElementsByTagName("zononder");
					Element sndnElement = (Element) sndnList.item(0);
					sndnList = sndnElement.getChildNodes();
					String[] sdn = sndnList.item(0).getNodeValue()
							.split(" ");
					Ret_Sts = Ret_Sts + sdn[1];
				}
				if (p[0].equals("2")) {
					// get all relevant nodes for this 'metar'
					NodeList nodeList = doc.getElementsByTagName("METAR");
					for (int i = 0; i < nodeList.getLength(); i++) {

						Node node = nodeList.item(i);

						Element fstElmnt = (Element) node;
						NodeList rawtList = fstElmnt
								.getElementsByTagName("raw_text");
						Element rawtElement = (Element) rawtList.item(0);
						rawtList = rawtElement.getChildNodes();
						Ret_Sts = "2;"
								+ rawtList.item(0).getNodeValue();
					}
				}
				if (p[0].equals("3")) {
					// get all relevant nodes for this 'metar'
					NodeList nodeList = doc.getElementsByTagName("METAR");
					for (int i = 0; i < nodeList.getLength(); i++) {

						Node node = nodeList.item(i);

						Element fstElmnt = (Element) node;
						NodeList rawtList = fstElmnt
								.getElementsByTagName("raw_text");
						Element rawtElement = (Element) rawtList.item(0);
						rawtList = rawtElement.getChildNodes();
						Ret_Sts = "3;"
								+ rawtList.item(0).getNodeValue();
					}
				}

			} catch (Exception e) {
				System.out.println("XML Pasing Exception = " + e);
			}
			return Ret_Sts;
		}

		@Override
		protected void onPostExecute(String result) {
			String s[] = result.toString().split(";");
			// for all results step load their data info layout fields
			if (s[0].equals("1")) {
				nameText.setText(s[1]);
				Log.d(TAG,"Veld orientatie = " + SelToAngle(appLND));
				String crx_base = CrossBase(s[2], s[5], SelToAngle(appLND));
				windText.setText(s[2] + " (Cross = " + crx_base +" m/s)");
				/* for test purposes. 
				String wnd = "20";
				Log.d("tst","=== start ===");
				Log.d("tst", wnd + " m/s -   0 grd" + " (Cross = "  + CrossBase(wnd,"180", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  10 grd" + " (Cross = "  + CrossBase(wnd,"190", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  20 grd" + " (Cross = "  + CrossBase(wnd,"200", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  30 grd" + " (Cross = "  + CrossBase(wnd,"210", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  40 grd" + " (Cross = "  + CrossBase(wnd,"220", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  50 grd" + " (Cross = "  + CrossBase(wnd,"230", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  60 grd" + " (Cross = "  + CrossBase(wnd,"240", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  70 grd" + " (Cross = "  + CrossBase(wnd,"250", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  80 grd" + " (Cross = "  + CrossBase(wnd,"260", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s -  90 grd" + " (Cross = "  + CrossBase(wnd,"270", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 100 grd" + " (Cross = "  + CrossBase(wnd,"280", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 110 grd" + " (Cross = "  + CrossBase(wnd,"290", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 120 grd" + " (Cross = "  + CrossBase(wnd,"300", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 130 grd" + " (Cross = "  + CrossBase(wnd,"310", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 140 grd" + " (Cross = "  + CrossBase(wnd,"320", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 150 grd" + " (Cross = "  + CrossBase(wnd,"330", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 160 grd" + " (Cross = "  + CrossBase(wnd,"340", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 170 grd" + " (Cross = "  + CrossBase(wnd,"350", SelToAngle(appLND)) + " m/s");
				Log.d("tst", wnd + " m/s - 180 grd" + " (Cross = "  + CrossBase(wnd,"360", SelToAngle(appLND)) + " m/s");
				Log.d("tst","=== einde ===");
				*/
				if ((int) Double.parseDouble(crx_base) < 3) {
					windText.setTextColor(Color.GREEN);
				} else if ((int) Double.parseDouble(crx_base) > 5) {
					windText.setTextColor(Color.RED);
				} else {
					windText.setTextColor(Color.YELLOW);
				}
				presText.setText(s[3]);
				humiText.setText(s[4]);
				wdgrText.setText(s[5]);
				wdirText.setText(s[6]);
				tempText.setText(s[7]);
				int cur_base = CloudBase(s[4], s[7]);
				wolkText.setText(Integer.toString(cur_base)
						+ " (Berekend uit RH, T)");
				if (cur_base < 300) {
					wolkText.setTextColor(Color.RED);
				} else if (cur_base > 500) {
					wolkText.setTextColor(Color.GREEN);
				}
				crx_base = CrossBase(s[8],s[5], SelToAngle(appLND));
				wdpkText.setText(s[8] + " (Cross = " + crx_base +" m/s)");
				if ((int) Double.parseDouble(crx_base) < 3) {
					wdpkText.setTextColor(Color.GREEN);
				} else if ((int) Double.parseDouble(crx_base) > 5) {
					wdpkText.setTextColor(Color.RED);
				} else {
					wdpkText.setTextColor(Color.YELLOW);
				}
				rainText.setText(s[9]);
				sunuText.setText(s[10]);
				sundText.setText(s[11]);
			}
			if (s[0].equals("2")) {
				mtr1Text.setText(s[1]);
			}
			if (s[0].equals("3")) {
				mtr2Text.setText(s[1]);
			}
			if (s[0].equals("4")) {
				mtr2Text.setText(s[1]);
			}
			// Log.d("res", "result " + result);
		}
		
		protected String CrossBase(String wind, String wdir, String fhdg) {
			DecimalFormat dF = new DecimalFormat("0.0");
			String base = "";
			try {
				Number w_val = dF.parse(wind);
				Number r_val = dF.parse(wdir);
				Number f_val = dF.parse(fhdg);
				Number b_val = 0;
				if (f_val.intValue() < 90) {
					b_val = f_val;
				} else {
					b_val = f_val.doubleValue() - 90.0;
				}
				// todo -> rotate field orientation -;)
				if (r_val.intValue() >= 0 && r_val.intValue() < 90) {
					base = new BigDecimal(Math.abs(Math.sin(DegToRad((90.0 - b_val.doubleValue()) - r_val.doubleValue()))) * w_val.doubleValue())
						.round(new MathContext(2, RoundingMode.HALF_UP)).toString();
				} 
				if (r_val.intValue() >= 90 && r_val.intValue() < 180) {
					base = new BigDecimal(Math.abs(Math.sin(DegToRad(r_val.doubleValue() - (90.0 - b_val.doubleValue())))) * w_val.doubleValue())
						.round(new MathContext(2, RoundingMode.HALF_UP)).toString();
				}
				if (r_val.intValue() >= 180 && r_val.intValue() < 270) {
					base = new BigDecimal(Math.abs(Math.sin(DegToRad((270.0 - b_val.doubleValue()) - r_val.doubleValue()))) * w_val.doubleValue())
						.round(new MathContext(2, RoundingMode.HALF_UP)).toString();
				}
				if (r_val.intValue() >= 270 && r_val.intValue() <= 360) {
					base = new BigDecimal(Math.abs(Math.sin(DegToRad(r_val.doubleValue() - (270.0 - b_val.doubleValue())))) * w_val.doubleValue())
						.round(new MathContext(2, RoundingMode.HALF_UP)).toString();
				}
			
			} catch (Exception e) {
				Log.d(TAG, "value conversion error for wind " + wind + " wdir "
						+ wdir);
			}
			return base;
		}
		
		public double DegToRad(double degrees) {
			return degrees * (Math.PI / 180);
		}

		protected int CloudBase(String humi, String temp) {
			// use formula to calculate cloud base from reported Temp & RH:
			// =243.04*(LN(RH/100)+((17.625*T)/(243.04+T)))/(17.625-LN(RH/100)-((17.625*T)/(243.04+T)))
			DecimalFormat dF = new DecimalFormat("0.000");
			int base = 0;
			try {
				Number h_val = dF.parse(humi);
				Number t_val = dF.parse(temp);
				Double td_val = 243.04
						* (Math.log(h_val.doubleValue() / 100) + ((17.625 * t_val
								.doubleValue()) / (243.04 + t_val.doubleValue())))
						/ (17.625 - Math.log(h_val.doubleValue() / 100) - ((17.625 * t_val
								.doubleValue()) / (243.04 + t_val.doubleValue())));
				base = (int) Math.round((t_val.doubleValue() - td_val) * 120);
			} catch (Exception e) {
				Log.d(TAG, "value conversion error humi " + humi + " temp "
						+ temp);
			}
			return base;
		}

	}

	class NET2_Task extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected String doInBackground(String... params) {

			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
					.permitAll().build();
			StrictMode.setThreadPolicy(policy);
			
			org.jsoup.select.Elements elements = null;
			try {

				org.jsoup.nodes.Document html_doc = Jsoup.connect(params[0])
						.get();
				elements = html_doc.select("pre");
				
			} catch (Exception e) {
				Log.d("err", "error " + e);
			}
			Log.d(TAG, "docu " + elements.html().toString() + " length " + elements.html().toString().length());
			return elements.html().toString(); 
		}

		@Override
		protected void onPostExecute(String result) {
			// System.out.println(result);
			knmiText.setText(result);
		}
	}

}
