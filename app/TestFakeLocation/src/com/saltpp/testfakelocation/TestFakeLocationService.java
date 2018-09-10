package com.saltpp.testfakelocation;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class TestFakeLocationService extends Service {

	
	private final String SOCKET_NAME = "salt.modify.location";
	private LocalSocket mLocalSocket;
	private double mdLat  = 0.0;
	private double mdLong = 0.0;
	private boolean mbFoundServerSocket;

	private int mnWindowX;
	private int mnWindowY;
	private final String KEY_X = "mnWindowX";
	private final String KEY_Y = "mnWindowY";
	private void readSharedPreference() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TestFakeLocationService.this);
		mnWindowX = prefs.getInt(KEY_X, 0);
		mnWindowY = prefs.getInt(KEY_Y, 0);
	}
	private void saveSharedPreference() {
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TestFakeLocationService.this);
		Editor editor = prefs.edit();
		editor.putInt(KEY_X, mnWindowX);
		editor.putInt(KEY_Y, mnWindowY);
		editor.commit();
	}
	
	private void sendLocation() {
		Log.e("Salt", "sendLocation() lat=" + mdLat + ", long=" + mdLong);
		try {
			mLocalSocket = new LocalSocket();
			LocalSocketAddress addr = new LocalSocketAddress(SOCKET_NAME);
	
			mLocalSocket.connect(addr);

			// lat
			ByteBuffer buffer = ByteBuffer.allocate(8);		// double = 64bits
			buffer.putDouble(mdLat);
			mLocalSocket.getOutputStream().write(buffer.array());
			
			// long
			buffer.clear();
			buffer.putDouble(mdLong);
			mLocalSocket.getOutputStream().write(buffer.array());
			
			mLocalSocket.close();
			
			mbFoundServerSocket = true;
			
		} catch (IOException e) {
			// e.printStackTrace();
			Log.e("Salt", "sendLocation() no server");
			mbFoundServerSocket = false;
		}

	}
	
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		// return super.onStartCommand(intent, flags, startId);
    	super.onStartCommand(intent, flags, startId);
    	return Service.START_STICKY;
    }

	private float mfDensity;
	private PadView mPadView;
	WindowManager mWindowManagaer;
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		readSharedPreference();
		
		startForeground(0, new Notification());
		
		mfDensity = getResources().getDisplayMetrics().density;
		
		mPadView = new PadView(this);
		
		// from https://github.com/k0shk0sh/AndroidFloatingImage/blob/master/src/com/styleme/floating/example/views/FloatingImage.java
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.WRAP_CONTENT,
				WindowManager.LayoutParams.WRAP_CONTENT,
				PixelFormat.TRANSLUCENT); // assigning height/width to the imageView
		params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
				| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM; // assigning some flags to the layout
		
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = mnWindowX;
        params.y = mnWindowY;
        params.width  = (int) (80 * mfDensity);
        params.height = (int) (80 * mfDensity);
        params.setTitle("PadView");
        
        mWindowManagaer = (WindowManager)getSystemService(WINDOW_SERVICE);
            
        mWindowManagaer.addView(mPadView, params);

	}
	
	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();

		stopForeground(true);
		
        if (mPadView != null) {
	        ((WindowManager)getSystemService(WINDOW_SERVICE)).removeView(mPadView);
	        mPadView = null;
        }
	}
	
    private class PadView extends View {


		public PadView(Context context) {
			super(context);

			mPaint = new Paint();
			mPaint.setColor(0x4000ff00);	// 0x40ff0000);

			mPaintDragging = new Paint();
			mPaintDragging.setColor(0xa000ff00);	// 0xa0ff0000);
			
			mPaintError = new Paint();
			mPaintError.setColor(0x40ff0000);

			mPaintFont = new Paint();
			mPaintFont.setColor(0xC0FFFFFF);
			mPaintFont.setTextSize(10);
			mDecimalFormat = new DecimalFormat("#.########");
			
			mPaintCurrentLocation = new Paint();
			mPaintCurrentLocation.setColor(0xa0ffff00);
			
		}
		
		private Paint mPaint;
		private Paint mPaintDragging;
		private Paint mPaintError;
		
		private Paint mPaintFont;
		private DecimalFormat mDecimalFormat;

		private Paint mPaintCurrentLocation;

		int mnWidth;
		int mnHeight;
		int mnCenterX;
		int mnCenterY;
		int mnPointerRadius;
		int mnPointerX;
		int mnPointerY;

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			
			final Paint fmPaint         = mPaint;
			final Paint fmPaintDragging = mPaintDragging;
			final Paint fmPaintError    = mPaintError;
			
			if (mnTouchMode == TOUCH_MODE_DRAG_AFTER_DOUBLE_TOUCH) {
				canvas.drawCircle(mnCenterX, mnCenterY, mnCenterX, fmPaintDragging);
				canvas.drawCircle(mnPointerX, mnPointerY, mnPointerRadius, fmPaintDragging);
			}
			else {
				canvas.drawCircle(mnCenterX, mnCenterY, mnCenterX, fmPaint);
				if (mbFoundServerSocket) {
					canvas.drawCircle(mnPointerX, mnPointerY, mnPointerRadius, fmPaint);
				}
				else {
					canvas.drawCircle(mnPointerX, mnPointerY, mnPointerRadius, fmPaintError);
				}
			}
			
			// draw long and lat
			final Paint fmPaintFont = mPaintFont;
			canvas.drawText(mDecimalFormat.format(mdLong), 0, 10, fmPaintFont);
			canvas.drawText(mDecimalFormat.format(mdLat),  0, 20, fmPaintFont);
			
			// draw current location by line
			canvas.drawLine(mnCenterX,
							mnCenterY,
							mnCenterX + (int) (mdLong * 100000 / 2),
							mnCenterY - (int) (mdLat  * 100000 / 2),
							mPaintCurrentLocation);
		}
		
		
		@Override
		protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
			super.onLayout(changed, left, top, right, bottom);
			
			mnWidth  = right - left;
			mnHeight = bottom - top;
			
			if (mnCenterX == 0 && mnCenterY == 0) {
				// it seems 1st time launch
				mnCenterX = mnWidth  / 2;
				mnCenterY = mnHeight / 2;
			}
			
			mnPointerX = mnCenterX;
			mnPointerY = mnCenterY;
			mnPointerRadius = (int) (mnWidth / 2.5f);
		}
		
		private static final int TOUCH_MODE_NONE = 0;
		private static final int TOUCH_MODE_DRAG = 1;
		private static final int TOUCH_MODE_DRAG_AFTER_DOUBLE_TOUCH = 2;
		private volatile int mnTouchMode = TOUCH_MODE_NONE;
		private float mfStartX;
		private float mfStartY;
		private int mnStartViewX;
		private int mnStartViewY;
		WindowManager.LayoutParams mLayoutParamsDragging;
		private volatile boolean mbCheckingDoubleTouch;
		private SenderThread mSenderThread;
		private int mnDiffX;
		private int mnDiffY;
		
		class SenderThread extends Thread {
			private boolean mbAbort;
			@Override
			public void run() {
				super.run();
				
				while (!mbAbort) {
					if (mnDiffX != 0 || mnDiffY != 0) {
						final double FACTOR_MOVE = 0.000001f;	// 11.1cm
						final double FACTOR_RAND = 0.000001f;
						if (mnDiffX > 0) { mdLong += FACTOR_MOVE * ((int) (0.01 * mnDiffX * mnDiffX + 1)); }
						else             { mdLong -= FACTOR_MOVE * ((int) (0.01 * mnDiffX * mnDiffX + 1)); }
						if (mnDiffY < 0) { mdLat  += FACTOR_MOVE * ((int) (0.01 * mnDiffY * mnDiffY + 1)); }
						else             { mdLat  -= FACTOR_MOVE * ((int) (0.01 * mnDiffY * mnDiffY + 1)); }
												
						Log.e("Salt", "Long, Lat        = " + mDecimalFormat.format(mdLong) + ", " + mDecimalFormat.format(mdLat));
						mdLong += FACTOR_RAND * ((int)(Math.random() * 5) - 2);
						mdLat  += FACTOR_RAND * ((int)(Math.random() * 5) - 2);
						Log.e("Salt", "Long, Lat + rand = " + mDecimalFormat.format(mdLong) + ", " + mDecimalFormat.format(mdLat));
						
						
						sendLocation();
					}
					
					postInvalidate();
					
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			public void abort() { mbAbort = true; }
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			// return super.onTouchEvent(event);
			
			final int nAction = event.getAction();
			switch (nAction & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				if (event.getPointerCount() == 1) {
					if (mnTouchMode == TOUCH_MODE_NONE) {
						Log.e("Salt", "ACTION_POINTER_DOWN TOUCH_MODE_NONE " + event.getRawX() + ", " + event.getRawY());
						if (mbCheckingDoubleTouch) {
							// doubale tap
							Log.e("Salt", "ACTION_POINTER_DOWN mbCheckingDoubleTouch == true " + event.getRawX() + ", " + event.getRawY());
							mnTouchMode = TOUCH_MODE_DRAG_AFTER_DOUBLE_TOUCH;
							mfStartX = event.getRawX();
							mfStartY = event.getRawY();
							
							mLayoutParamsDragging = (WindowManager.LayoutParams) getLayoutParams();
							mnStartViewX = mLayoutParamsDragging.x;
							mnStartViewY = mLayoutParamsDragging.y;
						}
						else {
							// single tap
							Log.e("Salt", "ACTION_POINTER_DOWN mbCheckingDoubleTouch == false " + event.getRawX() + ", " + event.getRawY());
							mnTouchMode = TOUCH_MODE_DRAG;
							mfStartX = event.getRawX();
							mfStartY = event.getRawY();
							
							mSenderThread = new SenderThread();
							
							mSenderThread.start();

						}
					}
				}
				break;

			case MotionEvent.ACTION_MOVE:
				if (mnTouchMode == TOUCH_MODE_DRAG) {
					mnPointerX = mnCenterX + (int) (event.getRawX() - mfStartX);
					mnPointerY = mnCenterY + (int) (event.getRawY() - mfStartY);
					if (mnPointerX < 0)       { mnPointerX = 0; }
					if (mnPointerX > mnWidth) { mnPointerX = mnWidth; }
					if (mnPointerY < 0)       { mnPointerY = 0; }
					if (mnPointerY > mnHeight) { mnPointerY = mnHeight; }
					
					invalidate();

					mnDiffX = mnPointerX - mnCenterX;
					mnDiffY = mnPointerY - mnCenterY;
					
				}
				else if (mnTouchMode == TOUCH_MODE_DRAG_AFTER_DOUBLE_TOUCH) {
					mLayoutParamsDragging.x = mnWindowX = mnStartViewX + (int) (event.getRawX() - mfStartX);
					mLayoutParamsDragging.y = mnWindowY = mnStartViewY + (int) (event.getRawY() - mfStartY);
					mWindowManagaer.updateViewLayout(this, mLayoutParamsDragging);
					invalidate();
					saveSharedPreference();
				}
				break;
				
			case MotionEvent.ACTION_UP:
				if (mnTouchMode == TOUCH_MODE_DRAG) {
					Log.e("Salt", "ACTION_UP TOUCH_MODE_DRAG");
					mnTouchMode = TOUCH_MODE_NONE;

					mnDiffX = 0;
					mnDiffY = 0;
					
					mnPointerX = mnCenterX;
					mnPointerY = mnCenterY;
					invalidate();

					if (mSenderThread != null) {
						mSenderThread.abort();
						mSenderThread = null;
					}

					if (!mbCheckingDoubleTouch) {
						(new Thread(new Runnable() {
							@Override
							public void run() {
								mbCheckingDoubleTouch = true;
								Log.e("Salt", "mbCheckingDoubleTouch = true");
								try {
									Thread.sleep(150);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								mbCheckingDoubleTouch = false;
								Log.e("Salt", "mbCheckingDoubleTouch = false");
							}
						})).start();
					}
				}
				else if (mnTouchMode == TOUCH_MODE_DRAG_AFTER_DOUBLE_TOUCH) {
					Log.e("Salt", "ACTION_UP TOUCH_MODE_DRAG_AFTER_DOUBLE_TOUCH");
					mnTouchMode = TOUCH_MODE_NONE;
					invalidate();
				}
				break;
			}
			return true;
		}
    }
}
