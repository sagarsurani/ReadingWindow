package com.readingwindow;

import android.app.Activity;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import static android.view.MotionEvent.INVALID_POINTER_ID;

/**
 * Created by sagar on 12/04/18.
 */

public class ReadingWindow {

    private static View readingWindowView;
    static LinearLayout readingWindowViewContainer;
    static View blackView1, blackView2, blackView3, blackView4;
    private static float mLastTouchX, mLastTouchY;
    private static int mActivePointerId = INVALID_POINTER_ID;
    private static float mPosX, mPosY;
    private static Activity currentActivity;

    public static void show(Activity activity) {
        currentActivity = activity;
        ViewGroup parentView = (ViewGroup) ((ViewGroup) activity
                .findViewById(android.R.id.content)).getChildAt(0);

        readingWindowView = activity.getLayoutInflater().inflate(R.layout.view_reading_window, null);
        readingWindowViewContainer = readingWindowView.findViewById(R.id.readingWindowViewContainer);
        blackView1 = readingWindowView.findViewById(R.id.blackView1);
        blackView2 = readingWindowView.findViewById(R.id.blackView2);
        blackView3 = readingWindowView.findViewById(R.id.blackView3);
        blackView4 = readingWindowView.findViewById(R.id.blackView4);
        mPosX = readingWindowView.getX();
        mPosY = readingWindowView.getY();

        parentView.addView(readingWindowView);
        readingWindowView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        final int pointerIndex = MotionEventCompat.getActionIndex(event);
                        final float x = MotionEventCompat.getX(event, pointerIndex);
                        final float y = MotionEventCompat.getY(event, pointerIndex);

                        // Remember where we started (for dragging)
                        mLastTouchX = x;
                        mLastTouchY = y;
                        // Save the ID of this pointer (for dragging)
                        mActivePointerId = MotionEventCompat.getPointerId(event, 0);
                        break;
                    }

                    case MotionEvent.ACTION_MOVE: {
                        // Find the index of the active pointer and fetch its position
                        final int pointerIndex =
                                MotionEventCompat.findPointerIndex(event, mActivePointerId);

                        final float x = MotionEventCompat.getX(event, pointerIndex);
                        final float y = MotionEventCompat.getY(event, pointerIndex);

                        // Calculate the distance moved
                        final float dx = x - mLastTouchX;
                        final float dy = y - mLastTouchY;

                        mPosX += dx;
                        mPosY += dy;
                        reDrawBlackView(mPosX, mPosY);

                        // Remember this touch position for the next move event
                        mLastTouchX = x;
                        mLastTouchY = y;

                        break;
                    }

                    case MotionEvent.ACTION_UP: {
                        mActivePointerId = INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_CANCEL: {
                        mActivePointerId = INVALID_POINTER_ID;
                        break;
                    }

                    case MotionEvent.ACTION_POINTER_UP: {

                        final int pointerIndex = MotionEventCompat.getActionIndex(event);
                        final int pointerId = MotionEventCompat.getPointerId(event, pointerIndex);

                        if (pointerId == mActivePointerId) {
                            // This was our active pointer going up. Choose a new
                            // active pointer and adjust accordingly.
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            mLastTouchX = MotionEventCompat.getX(event, newPointerIndex);
                            mLastTouchY = MotionEventCompat.getY(event, newPointerIndex);
                            mActivePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                        }
                        break;
                    }
                }
                return true;
            }
        });
    }

    public static void hide() {
        if (readingWindowView != null) {
            if (readingWindowView.getVisibility() == View.VISIBLE) {
                readingWindowView.setVisibility(View.GONE);
            }
        }
    }

    public static boolean isVisible() {
        if (readingWindowView != null) {
            return readingWindowView.getVisibility() == View.VISIBLE;
        }
        return false;
    }

    private static void reDrawBlackView(float mPosX, float mPosY) {
//        readingWindowView.setY(mPosY);
        float x = mPosX;
        float y = mPosY;
        float blackViewWidth = readingWindowView.getWidth();
        float blackViewHeight = readingWindowView.getHeight();
        DisplayMetrics displayMetrics = new DisplayMetrics();
        currentActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenHeight = displayMetrics.heightPixels;
        int screenWidth = displayMetrics.widthPixels;

//        blackView1.setX(0);
//        blackView1.setY(0);
        blackView1.setLayoutParams(new LinearLayout.LayoutParams(screenWidth, (int) y));
//        blackView2.setX(0);
        blackView2.setY(y + blackViewHeight);
        blackView2.setLayoutParams(new LinearLayout.LayoutParams(screenWidth, (int) (screenHeight - (y + blackViewHeight))));
    }

}
