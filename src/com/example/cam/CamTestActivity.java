package com.example.cam;

/**
 * @author Jose Davis Nidhin
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.net.wifi.p2p.WifiP2pManager.PeerListListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class CamTestActivity extends Activity implements SensorEventListener {
	private static final String TAG = "CamTestActivity";
	Preview preview;
	Button buttonClick;
	Camera camera;
	Activity act;
	Context ctx;
	TextView text;
	float[] currentOrientation, picOrientation;
	DateFormat df = new DateFormat();

	private SensorManager mSensorManager;
	private Sensor mOrientation;
	private Timer _timer;
	private TimerTask _captuerAsynchronousTask;
	private WifiP2pManager mManager;
	private Channel mChannel;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;

		currentOrientation = picOrientation = new float[3];

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		text = (TextView) findViewById(R.id.txtStatus);
		preview = new Preview(this, (SurfaceView) findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((FrameLayout) findViewById(R.id.layout)).addView(preview);
		preview.setKeepScreenOn(true);

		mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
		mChannel = mManager.initialize(this, getMainLooper(), null);

		// preview.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View arg0) {
		// Log.d(TAG, "preview.setOnClickListener");
		// // camera.takePicture(shutterCallback, rawCallback,
		// // jpegCallback);
		// }
		// });

		Toast.makeText(ctx, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();

		buttonClick = (Button) findViewById(R.id.btnCapture);

		buttonClick.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				// preview.camera.takePicture(shutterCallback, rawCallback,
				// jpegCallback);
				Log.d(TAG, "setOnClickListener");

				// camera.stopPreview();
				// camera.takePicture(shutterCallback, rawCallback,
				// jpegCallback);

				// picOrientation = currentOrientation.clone();
				// camera.takePicture(null, null, jpegCallback);

				startTimer();
			}
		});

		buttonClick.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View arg0) {

				camera.autoFocus(new AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean arg0, Camera arg1) {
						// camera.takePicture(shutterCallback, rawCallback,
						// jpegCallback);
					}
				});

				return true;
			}
		});

		setupCameraCaptureAsyncTask();
	}

	public void setupCameraCaptureAsyncTask() {
		final Handler handler = new Handler();
		_timer = new Timer();
		_captuerAsynchronousTask = new TimerTask() {
			@Override
			public void run() {
				Log.d(TAG, "task start..");
				handler.post(new Runnable() {
					public void run() {
						Log.d(TAG, "run start..");
						try {
							picOrientation = currentOrientation.clone();
							camera.takePicture(null, null, jpegCallback);

							Log.d(TAG, "async finish");
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Log.e(TAG, e.getMessage());
						}
					}
				});
				Log.d(TAG, "task finish..");
			}
		};
	}

	private void startTimer() {
		_timer.schedule(_captuerAsynchronousTask, 0, 1000);
	}

	private void initiatePeerDiscovery() {
		mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {

			@Override
			public void onSuccess() {
				// Code for when the discovery initiation is successful goes
				// here.
				// No services have actually been discovered yet, so this method
				// can often be left blank. Code for peer discovery goes in the
				// onReceive method, detailed below.
			}

			@Override
			public void onFailure(int reasonCode) {
				// Code for when the discovery initiation fails goes here.
				// Alert the user that something went wrong.
			}
		});
	}

	private long lastPicMillis = 0;
	private byte[] lastPic;

	@Override
	protected void onResume() {
		super.onResume();
		int numCams = Camera.getNumberOfCameras();
		if (numCams > 0) {
			try {
				camera = Camera.open(0);

				Parameters params = camera.getParameters();
				logCameraParameters(params);

				params.setPictureSize(640, 480);
				camera.setParameters(params);

				logCameraParameters(params);

				camera.setPreviewCallback(new PreviewCallback() {

					@Override
					public void onPreviewFrame(byte[] data, Camera camera) {
						// TODO Auto-generated method stub
						Log.d(TAG, "onPreviewFrame " + data.length);

						long currentMillis = System.currentTimeMillis();
						;
						if (currentMillis - lastPicMillis > 1000) {
							lastPic = data.clone();
							new SaveImageTask().execute(lastPic);
							lastPicMillis = currentMillis;
						}
					}
				});

				camera.startPreview();
				preview.setCamera(camera);
			} catch (RuntimeException ex) {
				Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			}
		}

		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
	}

	private void logCameraParameters(Parameters params) {
		Size s = params.getPictureSize();
		Log.d(TAG, String.format("width: %s, height: %s", s.width, s.height));
	}

	@Override
	protected void onPause() {
		if (camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		super.onPause();

		mSensorManager.unregisterListener(this);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// Do something here if sensor accuracy changes.
		// You must implement this callback in your code.
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float azimuth_angle = event.values[0];
		float pitch_angle = event.values[1];
		float roll_angle = event.values[2];

		currentOrientation = event.values.clone();

		text.setText(String.format("rotation %s,%s,%s", azimuth_angle, pitch_angle, roll_angle));
	}

	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);

	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "onPictureTaken - raw");
		}
	};

	PictureCallback postviewCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "taking picture - postview");

			// new SaveImageTask().execute(data);
			// resetCam();
			//
			// Log.d(TAG, "onPictureTaken - postview");
		}
	};

	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new SaveImageTask().execute(data);
			resetCam();
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};

	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... data) {
			Log.d(TAG, "saving image..");

			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File(sdCard.getAbsolutePath() + "/camtest");
				dir.mkdirs();

				String date = (String) df.format("yyyyMMddhhmmss", new java.util.Date());

				String fileName = String.format("%s_%s_%s_%s.jpg", date, picOrientation[0], picOrientation[1],
						picOrientation[2]);
				File outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

				refreshGallery(outFile);

				Log.d(TAG, "image saved");

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

	}

	public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context arg0, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
				// Determine if Wifi P2P mode is enabled or not, alert
				// the Activity.
				int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
				if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
					// activity.setIsWifiP2pEnabled(true);
				} else {
					// activity.setIsWifiP2pEnabled(false);
				}
			} else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

				// The peer list has changed! We should probably do something
				// about
				// that.

				// Request available peers from the wifi p2p manager. This is an
				// asynchronous call and the calling activity is notified with a
				// callback on PeerListListener.onPeersAvailable()
				if (mManager != null) {
					mManager.requestPeers(mChannel, peerListListener);
				}
				Log.d(CamTestActivity.TAG, "P2P peers changed");

			} else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

				// Connection state changed! We should probably do something
				// about
				// that.

			} else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
				// DeviceListFragment fragment = (DeviceListFragment)
				// activity.getFragmentManager()
				// .findFragmentById(R.id.frag_list);
				// fragment.updateThisDevice((WifiP2pDevice)
				// intent.getParcelableExtra(
				// WifiP2pManager.EXTRA_WIFI_P2P_DEVICE));

			}
		}

		private List peers = new ArrayList();

		private PeerListListener peerListListener = new PeerListListener() {
			@Override
			public void onPeersAvailable(WifiP2pDeviceList peerList) {

				// Out with the old, in with the new.
				peers.clear();
				peers.addAll(peerList.getDeviceList());

				// If an AdapterView is backed by this data, notify it
				// of the change. For instance, if you have a ListView of
				// available
				// peers, trigger an update.
				// ((WiFiPeerListAdapter)
				// getListAdapter()).notifyDataSetChanged();
				// if (peers.size() == 0) {
				// Log.d(WiFiDirectActivity.TAG, "No devices found");
				// return;
				// }
			}
		};

	}

}
