package com.ezac.gliderlogs.misc;

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

//import oauth.signpost.OAuthConsumer;
//import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
//import oauth.signpost.exception.OAuthCommunicationException;
//import oauth.signpost.exception.OAuthExpectationFailedException;
//import oauth.signpost.exception.OAuthMessageSignerException;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
//import android.content.Context;
import android.os.Handler;
import android.util.Log;

/*
 * this class handles basic read / write to http server
 */
public class NetworkUtility {

	protected static String TAG = "NetworkUtil";

	// private static final String SIGNATURE_METHOD = "HMAC-SHA1";

	public static String GetFromServer(String uri, Handler handler, String sess_param) {
		// final Context context,
		// return 0 = ok, 1 = error, 2 = exception + :: message content
		String[] sess_msg = sess_param.split(";");
		StringBuilder builder = new StringBuilder();
		try {
	//		OAuthConsumer consumer = new CommonsHttpOAuthConsumer(key, secret);
			// Set up HTTP get
			HttpGet httpGet = new HttpGet(uri);
			httpGet.setHeader("X-CSRF-Token", sess_msg[2]);
			httpGet.addHeader("Cookie", sess_msg[0]+"="+sess_msg[1]);
	//		consumer.sign(httpGet);
			HttpClient client = new DefaultHttpClient();
			
			HttpResponse httpResponse = client.execute(httpGet);
			// did we get proper 200 response ?
			Log.d(TAG, "Status " + httpResponse.getStatusLine());
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				HttpEntity entity = httpResponse.getEntity();
				InputStream content = entity.getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(content));
				String line;
				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}
				return "0::" + builder.toString();
			} else {
				Log.e(TAG,
						"Recieve failed, status: "
								+ httpResponse.getStatusLine());
				return "1::" + httpResponse.getStatusLine();
			}
			 // gotten a response, now process it 
	//		  } catch (OAuthCommunicationException e) { 
	//			  e.printStackTrace();
	//			  return "2::" + e; 
	//			  } catch (OAuthExpectationFailedException e) { 
	//				  e.printStackTrace(); 
	//				  return "2::" + e; 
	//		  } catch (OAuthMessageSignerException e) { 
	//			  e.printStackTrace(); 
	//			  return "2::" + e;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "2::" + e;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return "2::" + e;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return "2::" + e;
		} catch (IOException e) {
			e.printStackTrace();
			return "2::" + e;
		}
	}

	public static String SentToServer(String uri, Handler handler, int method,
			String url, String sess_param) {
		// return 0 = ok, 1 = error, 2 = exception + :: message content
		// final Context context,
		String[] sess_msg = sess_param.split(";");
		String Response = new String();
		String postURL = new String();
		// construct full url
		postURL = uri + url;
		// Create a new HttpClient and Post Header
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse = null;
		try {
	//	 OAuthConsumer consumer = new CommonsHttpOAuthConsumer(key, secret);
			switch (method) {
			case 0:
				break;
			case 1:
				HttpPost httpPost = new HttpPost(postURL);
	//			 consumer.sign(httpPost);
				httpPost.setHeader("X-CSRF-Token", sess_msg[2]);
				httpPost.addHeader("Cookie", sess_msg[0]+"="+sess_msg[1]);
				httpResponse = httpclient.execute(httpPost);
				break;
			case 2:
				HttpPut httpPut = new HttpPut(postURL);
	//			 consumer.sign(httpPut);
				httpPut.setHeader("X-CSRF-Token", sess_msg[2]);
				httpPut.addHeader("Cookie", sess_msg[0]+"="+sess_msg[1]);
				httpResponse = httpclient.execute(httpPut);
				break;
			case 3:
				HttpDelete httpDel = new HttpDelete(postURL);
	//			 consumer.sign(httpDel);
				httpDel.setHeader("X-CSRF-Token", sess_msg[2]);
				httpDel.addHeader("Cookie", sess_msg[0]+"="+sess_msg[1]);
				httpResponse = httpclient.execute(httpDel);
				break;
			}
			Log.d(TAG, "NETStat - " + httpResponse.getStatusLine() + "  "
					+ postURL);
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// comm to server was ok
				HttpEntity responseEntity = httpResponse.getEntity();
				if (responseEntity != null) {
					Response = EntityUtils.toString(responseEntity);
					// strip off those surrounding none digit char's
					return "0::" + Response.replaceAll("\\D", "");
				}
			} else {
				Log.e(TAG,
						"Sent failed, status: " + httpResponse.getStatusLine());
				return "1::" + httpResponse.getStatusLine();
			} 

	//		  } catch (OAuthCommunicationException e) { 
	//			  e.printStackTrace();
	//		  return "2::" + e; 
	//		  } catch (OAuthExpectationFailedException e) {
	//		  e.printStackTrace(); 
	//		  return "2::" + e; 
	//		  } catch (OAuthMessageSignerException e) { 
	//			  e.printStackTrace(); 
	//			  return "2::" + e;
				  
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return "2::" + e;
		} catch (IOException e) {
			e.printStackTrace();
			return "2::" + e;
		}
		return "2::unexpected";
	}

	public static String URLReachable(String uri, Context context) {
		// return 0 = ok, 1 = error, 2 = exception + :: message content
		ConnectivityManager cm = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo != null && netInfo.isConnected()) {
			try {
				URL url = new URL("http://" + uri); // Change to
													// "http://google.com" for
													// www test.
				HttpURLConnection urlc = (HttpURLConnection) url
						.openConnection();
				urlc.setConnectTimeout(10 * 1000); // 10 s.
				urlc.connect();
				if (urlc.getResponseCode() == 200) { // 200 = "OK" code (http
														// connection is fine).
					// Log.d("Connection", "Success !");
					return "0::OK";
				} else {
					Log.d(TAG, "1::" + urlc.getResponseCode());
					return "1::" + urlc.getResponseCode();
				}
			} catch (MalformedURLException e) {
				Log.d(TAG, "2::" + e);
				return "2::" + e;
			} catch (IOException e) {
				Log.d(TAG, "2::" + e);
				return "2::" + e;
			}
		}
		return "2::No Network found";
	}
}
