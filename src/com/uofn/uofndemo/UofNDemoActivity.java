package com.uofn.uofndemo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.Symbol;
import com.esri.core.tasks.ags.geocode.Locator;


public class UofNDemoActivity extends Activity implements LocationListener  
{

	MapView mMapView ;
	GraphicsLayer locationLayer;
	GraphicsLayer locationReport;
	GraphicsLayer locationServerData;
	Locator locator;

	final static int WORLD_ID = 3;
	final static int UPDATE_ID = 2;
	final static int DESRIPTION_ID = 1;
	final static double SEARCH_RADIUS = 5;

	EditText editMessage;
	Button btnSend;
	ImageButton imgBtnMyPosition;
	ImageButton imgBtnGetMyData;

	boolean isBtnOn = false;

	boolean isMyDataOn = false;

	private LocationManager locManager;
	private Location myLocation = null;
	double latPoint = 0;
	double lngPoint = 0;

	WifiManager wifi;
	WifiInfo wi;

	String myMacAddress;
	String msg;

	String testMsg;
	private ProgressDialog mProgress;

	PictureMarkerSymbol drawable;
	PictureMarkerSymbol myPosFromServer;

	Callout callout;

	View content;

	ArrayList<UofNDataInfo> myCollectedData;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mProgress = new ProgressDialog(this);
		mProgress.setMessage("Communicate With Server....Please wait...");
		mProgress.setIndeterminate(true);
		mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgress.setCancelable(false);

		drawable = new PictureMarkerSymbol(this.getResources().getDrawable(R.drawable.icon_point));
		myPosFromServer = new PictureMarkerSymbol(this.getResources().getDrawable(R.drawable.icon_point_orange_data));


		// Get The Device Phone number
		TelephonyManager mgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE); 
		String myNumber = mgr.getLine1Number(); 

		WifiManager wifi = (WifiManager) getSystemService(WIFI_SERVICE);

		//Number for the device
		String imei = mgr.getDeviceId();
		WifiInfo info = wifi.getConnectionInfo();

		myMacAddress = info.getMacAddress();
		editMessage = (EditText)findViewById(R.id.edit_main_message);
		btnSend = (Button)findViewById(R.id.btn_main_send);

		imgBtnMyPosition = (ImageButton)findViewById(R.id.imgBtn_main_current_position);
		imgBtnGetMyData = (ImageButton)findViewById(R.id.imgBtn_main_get_my_data);


		mMapView = (MapView)findViewById(R.id.map);
		// Add dynamic layer to MapView
		mMapView.addLayer(new ArcGISTiledMapServiceLayer("" +
				"http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer"));

		callout = mMapView.getCallout();

		myCollectedData =  new ArrayList<UofNDataInfo>();
		content = createContent();

		locationLayer = new GraphicsLayer();
		locationReport = new GraphicsLayer();
		locationServerData = new GraphicsLayer();

		mMapView.addLayer(locationLayer);

		mMapView.addLayer(locationReport);
		mMapView.addLayer(locationServerData);

		locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		boolean isGPS = locManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

		if(!isGPS)
		{
			startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), 0);
		}

		// Ask GPS to update location information every 1 sec. move 5km
		locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
		// Ask Network to update location information every 1 sec. move 5km
		locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5, this);

		btnSend.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				msg = editMessage.getText().toString();

				if(myLocation != null)
				{
					locationReport.removeAll();

					GetLocations();

					Symbol symbol = drawable;

					Point wgspoint = new Point(lngPoint, latPoint);
					Point mapPoint = (Point) GeometryEngine
							.project(wgspoint,
									SpatialReference.create(4326),
									mMapView.getSpatialReference());

					locationReport.addGraphic(new Graphic(mapPoint, symbol, null, null));

					mMapView.zoomToResolution(mapPoint, 2);

					//need time to working on after server thing is done
					sendData2Server();

					editMessage.setText("");
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Please Wait for your GPS Warming up~~!!!", Toast.LENGTH_LONG).show();
				}

			}
		});


		imgBtnMyPosition.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(myLocation != null)
				{
					GetLocations();

					if(!isBtnOn)
					{
						locationLayer.removeAll();

						imgBtnMyPosition.setBackgroundResource(R.drawable.current_position_on);

						SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(
								Color.BLUE, 20, SimpleMarkerSymbol.STYLE.CIRCLE);

						Point wgspoint = new Point(lngPoint, latPoint);
						Point mapPoint = (Point) GeometryEngine
								.project(wgspoint,
										SpatialReference.create(4326),
										mMapView.getSpatialReference());

						locationLayer.addGraphic(new Graphic(mapPoint, resultSymbol, null, null));

						mMapView.zoomToResolution(mapPoint, 2);

						isBtnOn = true;
					}
					else
					{
						locationLayer.removeAll();

						imgBtnMyPosition.setBackgroundResource(R.drawable.current_position_off);
						isBtnOn = false;
					}
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Please Wait for your GPS Warming up~~!!!", Toast.LENGTH_LONG).show();
				}

			}
		});


		imgBtnGetMyData.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if(!isMyDataOn)
				{
					imgBtnGetMyData.setBackgroundResource(R.drawable.icon_message_on);
					getDataFromSever();
					isMyDataOn = true;
				}
				else
				{
					locationReport.removeAll();
					imgBtnGetMyData.setBackgroundResource(R.drawable.icon_message_off);

					isMyDataOn = false;
				}
			}
		});

		mMapView.setOnSingleTapListener(new OnSingleTapListener() {

			private static final long serialVersionUID = 1L;

			public void onSingleTap(float x, float y) {

				callout.hide();

				// Handles the tapping on Graphic

				int[] graphicIDs = locationReport.getGraphicIDs(x, y, 25);
				if (graphicIDs != null && graphicIDs.length > 0) {
					Graphic gr = locationReport.getGraphic(graphicIDs[0]);
					updateContent((String) gr.getAttributeValue("Description"),
							(String) gr.getAttributeValue("Updated"),(String) gr.getAttributeValue("World Id"));
					Point location = (Point) gr.getGeometry();
					callout.setOffset(0, -15);
					callout.show(location, content);
				}

			}
		});
	}


	public void GetLocations() {
		if (myLocation != null) {
			latPoint = myLocation.getLatitude();
			lngPoint = myLocation.getLongitude();

		}
	}

	/**
	 * Creates a LinearLayout which contains tile and rating.
	 * 
	 * @return content view to be added to the callout.
	 */
	public View createContent() {
		// create linear layout for the entire view
		LinearLayout layout = new LinearLayout(this);
		layout.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT));
		layout.setOrientation(LinearLayout.VERTICAL);


		// create TextView for the title
		TextView descView = new TextView(this);
		descView.setId(DESRIPTION_ID);

		descView.setTextColor(Color.GRAY);
		layout.addView(descView);

		TextView updateView = new TextView(this);
		updateView.setId(UPDATE_ID);

		updateView.setTextColor(Color.GRAY);
		layout.addView(updateView);

		TextView worldView = new TextView(this);
		worldView.setId(WORLD_ID);

		worldView.setTextColor(Color.GRAY);
		layout.addView(worldView);

		return layout;
	}


	public void updateContent(String desc, String time, String world) {
		if (content == null)
			return;

		TextView txt = (TextView) content.findViewById(DESRIPTION_ID);
		txt.setText(desc);

		TextView txt1 = (TextView)content.findViewById(UPDATE_ID);
		txt1.setText(time);

		TextView txt2 = (TextView)content.findViewById(WORLD_ID);
		txt2.setText(world);

	}

	void getDataFromSever()
	{
		mProgress.show();
		Thread thread = new Thread(new Runnable()
		{

			@Override
			public void run() {
				String path = "http://50.56.178.159/g/";

				BufferedReader br;

				try
				{
					HttpClient client = new DefaultHttpClient();
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair("my_address",myMacAddress));

					String url = path + "?" + URLEncodedUtils.format(params, HTTP.UTF_8);
					HttpGet get = new HttpGet(url);

					HttpResponse responsePOST = client.execute(get);
					HttpEntity resEntity = responsePOST.getEntity();

					if(resEntity != null)
					{
						br = new BufferedReader(new InputStreamReader(resEntity.getContent()));
						String str = null;
						StringBuffer sb = new StringBuffer();

						while((str = br.readLine()) != null)
						{
							sb.append(str).append("\n");
						}
						br.close();



						testMsg = sb.toString();

						handler.sendEmptyMessage(1);
						JSONArray json = new JSONArray(sb.toString());
						JSONArray myDataList;

						locationReport.removeAll();

						// Get the number of search results in this set
						int resultCount = json.length();
						Point lastPoint = null;

						// Loop over each result and print the title, summary, and URL
						for (int i = 0; i < resultCount; i++) {
							try {
								JSONObject resultObject = json.getJSONObject(i);
								JSONObject fields = resultObject.getJSONObject("fields");
								Point p = new Point(fields.getDouble("lon"),fields.getDouble("lat"));
								String desc = fields.getString("desc");
								String world_id = fields.getString("world_id");
								String updated_time = fields.getString("updated");

								Symbol symbol = myPosFromServer;

								Point point = (Point) GeometryEngine.project(p,
										SpatialReference.create(4326),
										mMapView.getSpatialReference());

								HashMap<String, Object> attrMap = new HashMap<String, Object>();
								attrMap.put("Description", desc);
								attrMap.put("World Id", world_id);
								attrMap.put("Updated", updated_time);
								locationReport.addGraphic(new Graphic(point, symbol,
										attrMap, null));

								if(i==resultCount-1)
								{
									lastPoint = point;
								}

							} catch (JSONException r) {

							}
						}

						mMapView.zoomToResolution(lastPoint, 2);
						handler.sendEmptyMessage(1);


					}
				}
				catch(Exception e)
				{
				}
			}
		});
		thread.start();	
	}

	void sendData2Server()
	{

		mProgress.show();

		Thread thread = new Thread(new Runnable()
		{

			@Override
			public void run() {


				String path = "http://50.56.178.159/m/";

				BufferedReader br;


				try
				{
					HttpClient client = new DefaultHttpClient();
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					params.add(new BasicNameValuePair("my_lat",Double.toString(latPoint)));
					params.add(new BasicNameValuePair("my_lon",Double.toString(lngPoint)));
					if(!msg.equals(""))
					{
						params.add(new BasicNameValuePair("description",msg));	
					}
					else
					{
						params.add(new BasicNameValuePair("description","I'm here!"));
					}
					params.add(new BasicNameValuePair("my_address",myMacAddress));

					String url = path + "?" + URLEncodedUtils.format(params, HTTP.UTF_8);
					HttpGet get = new HttpGet(url);

					HttpResponse responsePOST = client.execute(get);
					HttpEntity resEntity = responsePOST.getEntity();

					handler.sendEmptyMessage(0);
				}
				catch(Exception e)
				{

				}
			}
		});
		thread.start();
	}

	final Handler handler = new Handler()
	{
		public void handleMessage(Message msg)
		{

			mProgress.dismiss();

			String message = null;

			switch(msg.what)
			{
			case 0:
				break;
			case 1:
				break;
			case 2:
				Toast.makeText(getApplicationContext(), "Communication Trouble Please Try later....", Toast.LENGTH_LONG).show();
				break;
			default:

				break;
			}
		}
	};

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		myLocation = location;
	}


	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}


	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}


	@Override 
	protected void onDestroy() { 
		super.onDestroy();
	}
	@Override
	protected void onPause() {
		super.onPause();
		mMapView.pause();
	}
	@Override 	protected void onResume() {
		super.onResume(); 
		mMapView.unpause();
	}

}