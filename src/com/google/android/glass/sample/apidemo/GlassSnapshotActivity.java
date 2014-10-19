package com.google.android.glass.sample.apidemo;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.google.android.glass.view.WindowUtils;

public class GlassSnapshotActivity extends Activity implements
		SurfaceHolder.Callback {
	// This class implements a simple camera snapshot activity for Google Glass.
	// You would typically use this class when you want to take a one-off
	// snapshot
	// and immediately return to your calling activity.

	// Copyright (c) 2013, Darren Venn
	// All rights reserved.

	// Redistribution and use in source and binary forms, with or without
	// modification, are permitted provided that the following conditions are
	// met:

	// 1. Redistributions of source code must retain the above copyright notice,
	// this
	// list of conditions and the following disclaimer.
	// 2. Redistributions in binary form must reproduce the above copyright
	// notice,
	// this list of conditions and the following disclaimer in the documentation
	// and/or other materials provided with the distribution.

	// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	// "AS IS" AND
	// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
	// IMPLIED
	// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
	// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
	// LIABLE FOR
	// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
	// DAMAGES
	// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
	// SERVICES;
	// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
	// AND
	// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
	// TORT
	// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
	// THIS
	// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

	// The views and conclusions contained in the software and documentation are
	// those
	// of the authors and should not be interpreted as representing official
	// policies,
	// either expressed or implied, of the FreeBSD Project.

	private static final String TAG = "TAG";
	public static final int BUFFER_SIZE = 1024 * 8;
	// values passed in intent
	private String imageFileName = "";
	private int previewWidth = 640;
	private int previewHeight = 360;
	private int snapshotWidth = 2560;
	private int snapshotHeight = 1888;
	private int maximumWaitTimeForCamera = 2000;
	// a variable to store a reference to the Image View at the main.xml file.
	private ImageView iv_image;
	// a variable to store a reference to the Surface View at the main.xml file
	private SurfaceView sv;
	// a bitmap to display the captured image
	private Bitmap bmp;
	// Camera variables
	// a surface holder
	private SurfaceHolder sHolder;
	// a variable to control the camera
	private Camera mCamera;
	// the camera parameters
	private Parameters parameters;
	// toggle for interrupted activity
	private boolean gotInterrupted = false;
	private boolean cameraPreviouslyAcquired = false;
	private boolean mVoiceMenuEnabled = true;
	private volatile boolean mPicture = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.v(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);

		// Ensure screen stays on during demo.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_camera);

		// get the Image View at the main.xml file
		iv_image = (ImageView) findViewById(R.id.imageView);
		sv = (SurfaceView) findViewById(R.id.surfaceView);
		// Get a surface
		sHolder = sv.getHolder();
		sHolder.addCallback(this);
		// Bundle extras = getIntent().getExtras();
		// // save all the values found in the extras...
		// imageFileName = extras.getString("imageFileName");
		// previewWidth = extras.getInt("previewWidth");
		// previewHeight = extras.getInt("previewHeight");
		// ;
		// snapshotWidth = extras.getInt("snapshotWidth");
		// snapshotHeight = extras.getInt("snapshotHeight");
		// maximumWaitTimeForCamera = extras.getInt("maximumWaitTimeForCamera");
		// if (imageFileName.length() == 0 || previewWidth == 0
		// || previewHeight == 0 || snapshotWidth == 0
		// || snapshotHeight == 0 || maximumWaitTimeForCamera == 0) {
		// // abandon the activity if extras are not complete
		// Log.e(TAG, "Extras specified in the call are invalid");
		// Intent resultIntent = new Intent();
		// setResult(Activity.RESULT_CANCELED, resultIntent);
		// finish();
		// }
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.v(TAG, "onResume");
		if (gotInterrupted && cameraPreviouslyAcquired) {
			Log.v(TAG, "returned from interrupt by KeyDown");
			// this activity was running but was interrupted by a camera click
			// after camera was acquired
			// so try to get the camera again now
			if (!getCameraAndSetPreview(sHolder)) {
				Log.e(TAG, "Exception encountered creating surface, exiting");
				mCamera = null;
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED, resultIntent);
				finish();
			}
		}
	}

	@Override
	public boolean onCreatePanelMenu(int featureId, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			getMenuInflater().inflate(R.menu.voice_menu, menu);
			return true;
		}
		// Good practice to pass through, for options menu.
		return super.onCreatePanelMenu(featureId, menu);
	}

	@Override
	public boolean onPreparePanel(int featureId, View view, Menu menu) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			// Dynamically decides between enabling/disabling voice menu.
			return mVoiceMenuEnabled;
		}
		// Good practice to pass through, for options menu.
		return super.onPreparePanel(featureId, view, menu);
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			switch (item.getItemId()) {
			case R.id.menu_designer:
				mPicture = true;
				// // sets what code should be executed after the picture is
				// taken
				if (mCamera == null) {
					return true;
				}
				Camera.PictureCallback mCall = new Camera.PictureCallback() {
					public void onPictureTaken(byte[] data, Camera camera) {
						Log.v(TAG, "pictureTaken");
						Log.v(TAG, "data bytes=" + data.length);
						// decode the data obtained by the camera into a Bitmap
						Bitmap bmp = decodeSampledBitmapFromData(data, 640, 360);
						Log.v(TAG, "bmp width=" + bmp.getWidth() + " height="
								+ bmp.getHeight());
						FileOutputStream outStream = null;
						try {
							Calendar calendar = Calendar.getInstance();
							int year = calendar.get(Calendar.YEAR);
							int month = calendar.get(Calendar.MONTH);
							int day = calendar.get(Calendar.DAY_OF_MONTH);
							int hour = calendar.get(Calendar.HOUR_OF_DAY);
							int min = calendar.get(Calendar.MINUTE);
							int sec = calendar.get(Calendar.SECOND);
							imageFileName = String.valueOf(year) + "-"
									+ String.valueOf(month) + "-"
									+ String.valueOf(day) + "-"
									+ String.valueOf(hour) + "-"
									+ String.valueOf(year) + "-"
									+ String.valueOf(min) + "-"
									+ String.valueOf(sec) + ".jpg";
							FileOutputStream fos = new FileOutputStream(
									PictureActivity.GALLERY_PATH
											+ imageFileName);
							final BufferedOutputStream bos = new BufferedOutputStream(
									fos, BUFFER_SIZE);
							bmp.compress(CompressFormat.JPEG, 100, bos);
							bos.flush();
							bos.close();
							fos.close();
						} catch (FileNotFoundException e) {
							Log.v(TAG, e.getMessage());
						} catch (IOException e) {
							Log.v(TAG, e.getMessage());
						}
						Intent resultIntent = new Intent();
						// TODO Add extras or a data URI to this intent as
						// appropriate.
						resultIntent.putExtra(
								PictureActivity.ON_RESULT_FILE_NAME,
								imageFileName);
						resultIntent.putExtra(
								PictureActivity.ON_RESULT_FILE_PATH,
								PictureActivity.GALLERY_PATH + imageFileName);
						setResult(Activity.RESULT_OK, resultIntent);
						finish();
					}
				};

				mCamera.takePicture(null, null, mCall);

				break;
			case R.id.menu_back_pic:
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED, resultIntent);
				finish();
				break;
			default:
				return true; // No change.
			}
			return true;
		}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// if the user presses the camera key while this activity is happening
		// then release the
		// camera and return false, to allow the OS to handle it
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			Log.v(TAG, "onKeyDown");
			if (mCamera != null) {
				mCamera.stopPreview();
				// release the camera
				mCamera.release();
				// unbind the camera from this object
				mCamera = null;
			}
			gotInterrupted = true;
			return false;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.v(TAG, "surfaceChanged");
		// get camera parameters
		if (true)
			try {
				parameters = mCamera.getParameters();
				Log.v(TAG, "got parms");

				// set camera parameters
				parameters.setPreviewSize(previewWidth, previewHeight);
				parameters.setPictureSize(snapshotWidth, snapshotHeight);
				parameters.setPreviewFpsRange(30000, 30000);
				Log.v(TAG, "parms were set");
				mCamera.setParameters(parameters);

				mCamera.startPreview();
				Log.v(TAG, "set callback");
				// // wait
				// while (!mPicture) {
				//
				// }

				Log.v(TAG, "preview started");
				// // sets what code should be executed after the picture is
				// taken
				// Camera.PictureCallback mCall = new Camera.PictureCallback() {
				// public void onPictureTaken(byte[] data, Camera camera) {
				// Log.v(TAG, "pictureTaken");
				// Log.v(TAG, "data bytes=" + data.length);
				// // decode the data obtained by the camera into a Bitmap
				// Bitmap bmp = decodeSampledBitmapFromData(data, 640, 360);
				// Log.v(TAG, "bmp width=" + bmp.getWidth() + " height="
				// + bmp.getHeight());
				// FileOutputStream outStream = null;
				// try {
				// FileOutputStream fos = new FileOutputStream(
				// imageFileName);
				// final BufferedOutputStream bos = new BufferedOutputStream(
				// fos, BUFFER_SIZE);
				// bmp.compress(CompressFormat.JPEG, 100, bos);
				// bos.flush();
				// bos.close();
				// fos.close();
				// } catch (FileNotFoundException e) {
				// Log.v(TAG, e.getMessage());
				// } catch (IOException e) {
				// Log.v(TAG, e.getMessage());
				// }
				// Intent resultIntent = new Intent();
				// // TODO Add extras or a data URI to this intent as
				// // appropriate.
				// resultIntent.putExtra("testString", "here is my test");
				// setResult(Activity.RESULT_OK, resultIntent);
				// finish();
				// }
				// };
				//
				// mCamera.takePicture(null, null, mCall);
			} catch (Exception e) {
				try {
					mCamera.release();
					Log.e(TAG, "released the camera");
				} catch (Exception ee) {
					// do nothing
					Log.e(TAG, "error releasing camera");
					Log.e(TAG,
							"Exception encountered releasing camera, exiting:"
									+ ee.getLocalizedMessage());
				}
				Log.e(TAG,
						"Exception encountered, exiting:"
								+ e.getLocalizedMessage());
				mCamera = null;
				Intent resultIntent = new Intent();
				setResult(Activity.RESULT_CANCELED, resultIntent);
				finish();
			}
	}

	public static Bitmap decodeSampledBitmapFromData(byte[] data, int reqWidth,
			int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		boolean debug = false;
		if (debug) {
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);
			options.inSampleSize = 1; // saved image will be one half the width
										// and
										// height of the original (image
										// captured is
										// double the resolution of the screen
										// size)
			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;

			return BitmapFactory.decodeByteArray(data, 0, data.length, options);
		} else {
			return BitmapFactory.decodeByteArray(data, 0, data.length);
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.v(TAG, "surfaceCreated");
		// The Surface has been created, acquire the camera and tell it where
		// to draw the preview.
		if (!getCameraAndSetPreview(holder)) {
			Log.e(TAG, "Exception encountered creating surface, exiting");
			mCamera = null;
			Intent resultIntent = new Intent();
			setResult(Activity.RESULT_CANCELED, resultIntent);
			finish();
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surfaceDestroyed");
		if (mCamera != null) {
			mCamera.stopPreview();
			// release the camera
			mCamera.release();
			// unbind the camera from this object
			mCamera = null;
		}
	}

	@Override
	public void onPause() {
		Log.v(TAG, "onPause");
		super.onPause();
		if (mCamera != null) {
			mCamera.stopPreview();
			// release the camera
			mCamera.release();
			// unbind the camera from this object
			mCamera = null;
		}
	}

	@Override
	public void onDestroy() {
		Log.v(TAG, "onDestroy");
		super.onDestroy();

		if (mCamera != null) {
			mCamera.stopPreview();
			// release the camera
			mCamera.release();
			// unbind the camera from this object
			mCamera = null;
		}
	}

	private boolean getCameraAndSetPreview(SurfaceHolder holder) {
		// get the camera and set the preview surface
		if (getTheCamera(holder)) {
			try {
				mCamera.setPreviewDisplay(holder);
				Log.v(TAG, "surface holder for preview was set");
				cameraPreviouslyAcquired = true;
				return true; // the camera was acquired and the preview surface
								// set
			} catch (Exception e) {
				Log.e(TAG,
						"Exception encountered setting camera preview display:"
								+ e.getLocalizedMessage());
			}
		} else {
			Log.e(TAG, "Exception encountered getting camera, exiting");
			mCamera = null;
		}
		return false;
	}

	private boolean getTheCamera(SurfaceHolder holder) {
		Log.v(TAG, "getTheCamera");
		// keep trying to acquire the camera until "maximumWaitTimeForCamera"
		// seconds have passed
		boolean acquiredCam = false;
		int timePassed = 0;
		while (!acquiredCam && timePassed < maximumWaitTimeForCamera) {
			try {
				mCamera = Camera.open();
				Log.v(TAG, "acquired the camera");
				acquiredCam = true;
				return true;
			} catch (Exception e) {
				Log.e(TAG,
						"Exception encountered opening camera:"
								+ e.getLocalizedMessage());
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException ee) {
				Log.e(TAG,
						"Exception encountered sleeping:"
								+ ee.getLocalizedMessage());
			}
			timePassed += 200;
		}
		return false;
	}
}
