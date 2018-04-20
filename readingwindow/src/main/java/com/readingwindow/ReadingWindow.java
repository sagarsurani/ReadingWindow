package com.readingwindow;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.ImageView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by sagar on 12/04/18.
 */

@SuppressLint("AppCompatCustomView")
public class ReadingWindow extends ImageView {


    private static final int HANDLE_SIZE_IN_DP = 14;
    private static final int MIN_FRAME_SIZE_IN_DP = 50;
    private static final int FRAME_STROKE_WEIGHT_IN_DP = 1;
    private static final int GUIDE_STROKE_WEIGHT_IN_DP = 1;
    private static final float DEFAULT_INITIAL_FRAME_SCALE = 1f;
    private static final int DEFAULT_ANIMATION_DURATION_MILLIS = 100;
    private static final int DEBUG_TEXT_SIZE_IN_DP = 15;
    private static final int TRANSPARENT = 0x00000000;
    private static final int TRANSLUCENT_WHITE = 0xBBFFFFFF;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int TRANSLUCENT_BLACK = 0xBB000000;
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private float mScale = 1.0f;
    private float mAngle = 0.0f;
    private float mImgWidth = 1.0f;
    private float mImgHeight = 1.0f;
    private boolean mIsInitialized = false;
    private Matrix mMatrix = null;
    private Paint mPaintTranslucent;
    private Paint mPaintFrame;
    private Paint mPaintBitmap;
    private Paint mPaintDebug;
    private RectF mFrameRect;
    private RectF mImageRect;
    private PointF mCenter = new PointF();
    private float mLastX, mLastY;
    private boolean mIsAnimating = false;
    private int mExifRotation = 0;
    private boolean mIsDebug = false;
    private int mCompressQuality = 100;
    private int mInputImageWidth = 500;
    private int mInputImageHeight = 500;
    private AtomicBoolean mIsLoading = new AtomicBoolean(false);
    private ExecutorService mExecutor;
    private TouchArea mTouchArea = TouchArea.OUT_OF_BOUNDS;
    private MoveMode mMoveMode = MoveMode.FREE;
    private ShowMode mGuideShowMode = ShowMode.SHOW_ALWAYS;
    private ShowMode mHandleShowMode = ShowMode.SHOW_ALWAYS;
    private float mMinFrameSize;
    private int mHandleSize;
    private int mTouchPadding = 0;
    private boolean mShowGuide = true;
    private boolean mShowHandle = true;
    private boolean mismoveingEnabled = true;
    private boolean mIsEnabled = true;
    private PointF mCustomRatio = new PointF(1.0f, 1.0f);
    private float mFrameStrokeWeight = 2.0f;
    private float mGuideStrokeWeight = 2.0f;
    private int mBackgroundColor;
    private int mOverlayColor;
    private int mFrameColor;
    private int mHandleColor;
    private int mGuideColor;
    private float mInitialFrameScale; // 0.01 ~ 1.0, 0.75 is default value
    private boolean mIsAnimationEnabled = true;
    private int mAnimationDurationMillis = DEFAULT_ANIMATION_DURATION_MILLIS;
    private boolean mIsHandleShadowEnabled = true;
    public static float dpHeight;
    public static float dpWidth;

    public ReadingWindow(Context context) {
        this(context, null);
    }

    public ReadingWindow(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public static int getScreenWidthInPixels(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        return width;
    }

    public static int getScreenHeightInPixels(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(dm);
        int height = dm.heightPixels;
        return height;
    }

    public ReadingWindow(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        dpHeight = getScreenHeightInPixels(context);
        dpWidth = getScreenWidthInPixels(context);
        mExecutor = Executors.newSingleThreadExecutor();
        float density = getDensity();
        mHandleSize = (int) (density * HANDLE_SIZE_IN_DP);
        mMinFrameSize = density * MIN_FRAME_SIZE_IN_DP;
        mFrameStrokeWeight = density * FRAME_STROKE_WEIGHT_IN_DP;
        mGuideStrokeWeight = density * GUIDE_STROKE_WEIGHT_IN_DP;
        mPaintFrame = new Paint();
        mPaintTranslucent = new Paint();
        mPaintBitmap = new Paint();
        mPaintBitmap.setFilterBitmap(true);
        mPaintDebug = new Paint();
        mPaintDebug.setAntiAlias(true);
        mPaintDebug.setStyle(Paint.Style.STROKE);
        mPaintDebug.setColor(WHITE);
        mPaintDebug.setTextSize((float) DEBUG_TEXT_SIZE_IN_DP * density);
        mMatrix = new Matrix();
        mScale = 1.0f;
        mBackgroundColor = TRANSPARENT;
        mFrameColor = WHITE;
        mOverlayColor = TRANSLUCENT_BLACK;
        mHandleColor = WHITE;
        mGuideColor = TRANSLUCENT_WHITE;
        handleStyleable(context, attrs, defStyle, density);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.mode = this.mMoveMode;
        ss.backgroundColor = this.mBackgroundColor;
        ss.overlayColor = this.mOverlayColor;
        ss.frameColor = this.mFrameColor;
        ss.guideShowMode = this.mGuideShowMode;
        ss.handleShowMode = this.mHandleShowMode;
        ss.showGuide = this.mShowGuide;
        ss.showHandle = this.mShowHandle;
        ss.handleSize = this.mHandleSize;
        ss.touchPadding = this.mTouchPadding;
        ss.minFrameSize = this.mMinFrameSize;
        ss.customRatioX = this.mCustomRatio.x;
        ss.customRatioY = this.mCustomRatio.y;
        ss.frameStrokeWeight = this.mFrameStrokeWeight;
        ss.guideStrokeWeight = this.mGuideStrokeWeight;
        ss.ismoveingEnabled = this.mismoveingEnabled;
        ss.handleColor = this.mHandleColor;
        ss.guideColor = this.mGuideColor;
        ss.initialFrameScale = this.mInitialFrameScale;
        ss.angle = this.mAngle;
        ss.isAnimationEnabled = this.mIsAnimationEnabled;
        ss.animationDuration = this.mAnimationDurationMillis;
        ss.exifRotation = this.mExifRotation;
        ss.compressQuality = this.mCompressQuality;
        ss.isDebug = this.mIsDebug;
        ss.isHandleShadowEnabled = this.mIsHandleShadowEnabled;
        ss.inputImageWidth = this.mInputImageWidth;
        ss.inputImageHeight = this.mInputImageHeight;
        return ss;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        this.mMoveMode = ss.mode;
        this.mBackgroundColor = ss.backgroundColor;
        this.mOverlayColor = ss.overlayColor;
        this.mFrameColor = ss.frameColor;
        this.mGuideShowMode = ss.guideShowMode;
        this.mHandleShowMode = ss.handleShowMode;
        this.mShowGuide = ss.showGuide;
        this.mShowHandle = ss.showHandle;
        this.mHandleSize = ss.handleSize;
        this.mTouchPadding = ss.touchPadding;
        this.mMinFrameSize = ss.minFrameSize;
        this.mCustomRatio = new PointF(ss.customRatioX, ss.customRatioY);
        this.mFrameStrokeWeight = ss.frameStrokeWeight;
        this.mGuideStrokeWeight = ss.guideStrokeWeight;
        this.mismoveingEnabled = ss.ismoveingEnabled;
        this.mHandleColor = ss.handleColor;
        this.mGuideColor = ss.guideColor;
        this.mInitialFrameScale = ss.initialFrameScale;
        this.mAngle = ss.angle;
        this.mIsAnimationEnabled = ss.isAnimationEnabled;
        this.mAnimationDurationMillis = ss.animationDuration;
        this.mExifRotation = ss.exifRotation;
        this.mCompressQuality = ss.compressQuality;
        this.mIsDebug = ss.isDebug;
        this.mIsHandleShadowEnabled = ss.isHandleShadowEnabled;
        this.mInputImageWidth = ss.inputImageWidth;
        this.mInputImageHeight = ss.inputImageHeight;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(viewWidth, viewHeight);
        mViewWidth = viewWidth - getPaddingLeft() - getPaddingRight();
        mViewHeight = viewHeight - getPaddingTop() - getPaddingBottom();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        setupLayout(mViewWidth, mViewHeight);
    }
    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);
        if (mIsInitialized) {
            setMatrix();
            drawOverlay(canvas);
            drawCropFrame(canvas);
        }
    }
    @Override
    protected void onDetachedFromWindow() {
        mExecutor.shutdown();
        super.onDetachedFromWindow();
    }
    private void handleStyleable(Context context, AttributeSet attrs, int defStyle, float mDensity) {
        TypedArray ta =
                context.obtainStyledAttributes(attrs, R.styleable.reading_ReadingWindowView, defStyle, 0);
        Drawable drawable;
        mMoveMode = MoveMode.FREE;
        try {
            drawable = ta.getDrawable(R.styleable.reading_ReadingWindowView_reading_img_src);
            if (drawable != null) setImageDrawable(drawable);
            for (MoveMode mode : MoveMode.values()) {
                if (ta.getInt(R.styleable.reading_ReadingWindowView_reading_move_mode, 3) == mode.getId()) {
                    mMoveMode = mode;
                    break;
                }
            }
            mBackgroundColor =
                    ta.getColor(R.styleable.reading_ReadingWindowView_reading_background_color, TRANSPARENT);
            mOverlayColor =
                    ta.getColor(R.styleable.reading_ReadingWindowView_reading_overlay_color, TRANSLUCENT_BLACK);
            mFrameColor = ta.getColor(R.styleable.reading_ReadingWindowView_reading_frame_color, WHITE);
            mHandleColor = ta.getColor(R.styleable.reading_ReadingWindowView_reading_handle_color, WHITE);
            mGuideColor = ta.getColor(R.styleable.reading_ReadingWindowView_reading_guide_color, TRANSLUCENT_WHITE);
            for (ShowMode mode : ShowMode.values()) {
                if (ta.getInt(R.styleable.reading_ReadingWindowView_reading_guide_show_mode, 1) == mode.getId()) {
                    mGuideShowMode = mode;
                    break;
                }
            }
            for (ShowMode mode : ShowMode.values()) {
                if (ta.getInt(R.styleable.reading_ReadingWindowView_reading_handle_show_mode, 1) == mode.getId()) {
                    mHandleShowMode = mode;
                    break;
                }
            }
            setGuideShowMode(mGuideShowMode);
            setHandleShowMode(mHandleShowMode);
            mHandleSize = ta.getDimensionPixelSize(R.styleable.reading_ReadingWindowView_reading_handle_size,
                    (int) (HANDLE_SIZE_IN_DP * mDensity));
            mTouchPadding = ta.getDimensionPixelSize(R.styleable.reading_ReadingWindowView_reading_touch_padding, 0);
            mMinFrameSize = ta.getDimensionPixelSize(R.styleable.reading_ReadingWindowView_reading_min_frame_size,
                    (int) (MIN_FRAME_SIZE_IN_DP * mDensity));
            mFrameStrokeWeight =
                    ta.getDimensionPixelSize(R.styleable.reading_ReadingWindowView_reading_frame_stroke_weight,
                            (int) (FRAME_STROKE_WEIGHT_IN_DP * mDensity));
            mGuideStrokeWeight =
                    ta.getDimensionPixelSize(R.styleable.reading_ReadingWindowView_reading_guide_stroke_weight,
                            (int) (GUIDE_STROKE_WEIGHT_IN_DP * mDensity));
            mismoveingEnabled = ta.getBoolean(R.styleable.reading_ReadingWindowView_reading_move_enabled, true);
            mInitialFrameScale = constrain(
                    ta.getFloat(R.styleable.reading_ReadingWindowView_reading_initial_frame_scale,
                            DEFAULT_INITIAL_FRAME_SCALE), 0.01f, 1.0f, DEFAULT_INITIAL_FRAME_SCALE);
            mIsAnimationEnabled =
                    ta.getBoolean(R.styleable.reading_ReadingWindowView_reading_animation_enabled, true);
            mAnimationDurationMillis = ta.getInt(R.styleable.reading_ReadingWindowView_reading_animation_duration,
                    DEFAULT_ANIMATION_DURATION_MILLIS);
            mIsHandleShadowEnabled =
                    ta.getBoolean(R.styleable.reading_ReadingWindowView_reading_handle_shadow_enabled, true);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ta.recycle();
        }
    }
    private void drawCropFrame(Canvas canvas) {
        drawFrame(canvas);

    }
    private void drawOverlay(Canvas canvas) {
        mPaintTranslucent.setAntiAlias(true);
        mPaintTranslucent.setColor(mOverlayColor);
        mPaintTranslucent.setStyle(Paint.Style.FILL);
        Path path = new Path();
        RectF overlayRect =
                new RectF((float) Math.floor(mImageRect.left), (float) Math.floor(mImageRect.top),
                        (float) Math.ceil(mImageRect.right), (float) Math.ceil(mImageRect.bottom));
        if (!mIsAnimating && (mMoveMode == MoveMode.CIRCLE || mMoveMode == MoveMode.CIRCLE_SQUARE)) {
            path.addRect(overlayRect, Path.Direction.CW);
            PointF circleCenter = new PointF((mFrameRect.left + mFrameRect.right) / 2,
                    (mFrameRect.top + mFrameRect.bottom) / 2);
            float circleRadius = (mFrameRect.right - mFrameRect.left) / 2;
            path.addCircle(circleCenter.x, circleCenter.y, circleRadius, Path.Direction.CCW);
            canvas.drawPath(path, mPaintTranslucent);
        } else {
            path.addRect(overlayRect, Path.Direction.CW);
            path.addRect(mFrameRect, Path.Direction.CCW);
            canvas.drawPath(path, mPaintTranslucent);
        }
    }
    private void drawFrame(Canvas canvas) {
        mPaintFrame.setAntiAlias(true);
        mPaintFrame.setFilterBitmap(true);
        mPaintFrame.setStyle(Paint.Style.STROKE);
        mPaintFrame.setColor(mFrameColor);
        mPaintFrame.setStrokeWidth(mFrameStrokeWeight);
        canvas.drawRect(mFrameRect, mPaintFrame);
    }
    private void setMatrix() {
        mMatrix.reset();
        mMatrix.setTranslate(mCenter.x - mImgWidth * 0.5f, mCenter.y - mImgHeight * 0.5f);
        mMatrix.postScale(mScale, mScale, mCenter.x, mCenter.y);
        mMatrix.postRotate(mAngle, mCenter.x, mCenter.y);
    }
    private void setupLayout(int viewW, int viewH) {
        if (viewW == 0 || viewH == 0) return;
        setCenter(new PointF(getPaddingLeft() + viewW * 0.5f, getPaddingTop() + viewH * 0.5f));
        setScale(calcScale(viewW, viewH, mAngle));
        setMatrix();
        mImageRect = calcImageRect(new RectF(-50, 0, dpWidth + 100, dpHeight), mMatrix);
        mFrameRect = calcFrameRect(mImageRect);


        mIsInitialized = true;
        invalidate();
    }
    private float calcScale(int viewW, int viewH, float angle) {
        mImgWidth = dpWidth;
        mImgHeight = dpHeight;

        if (mImgWidth <= 0) mImgWidth = viewW;
        if (mImgHeight <= 0) mImgHeight = viewH;
        float viewRatio = (float) viewW / (float) viewH;
        float imgRatio = getRotatedWidth(angle) / getRotatedHeight(angle);
        float scale = 1.0f;
        if (imgRatio >= viewRatio) {
            scale = viewW / getRotatedWidth(angle);
        } else if (imgRatio < viewRatio) {
            scale = viewH / getRotatedHeight(angle);
        }
        return scale;
    }
    private RectF calcImageRect(RectF rect, Matrix matrix) {
        RectF applied = new RectF();
        matrix.mapRect(applied, rect);
        return applied;
    }



    public void setshow()
    {
        setVisibility(VISIBLE);
        invalidate();

    }

    public void sethide()
    {
        setVisibility(GONE);
        invalidate();

    }


    public Boolean isvisible()
    {
     if(getVisibility()==VISIBLE)
     {
         return true;
     }
        return false;
    }



    private RectF calcFrameRect(RectF imageRect) {
        float frameW = getRatioX(imageRect.width());
        float frameH = getRatioY(imageRect.height());
        float imgRatio = imageRect.width() / imageRect.height();
        float frameRatio = frameW / frameH;
        float l = imageRect.left, t = imageRect.top, r = imageRect.right, b = imageRect.bottom;
        if (frameRatio >= imgRatio) {
            l = imageRect.left;
            r = imageRect.right;
            float hy = (imageRect.top + imageRect.bottom) * 0.5f;
            float hh = (imageRect.width() / frameRatio) * 0.5f;
            t = hy - hh;
            b = hy + hh;
        } else if (frameRatio < imgRatio) {
            t = imageRect.top;
            b = imageRect.bottom;
            float hx = (imageRect.left + imageRect.right) * 0.5f;
            float hw = imageRect.height() * frameRatio * 0.5f;
            l = hx - hw;
            r = hx + hw;
        }
        float w = r - l;
        float h = b - t;
        float cx = l + w / 2;
        float cy = t + h / 2;
        float sw = w * mInitialFrameScale;
        float sh = h * mInitialFrameScale;
        return new RectF(cx - sw / 5, cy - sh / 5, cx + sw / 5, cy + sh / 5);
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onDown(event);
                if (mTouchArea == TouchArea.OUT_OF_BOUNDS) {
                    return false;
                }


                return true;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                if (mTouchArea != TouchArea.OUT_OF_BOUNDS) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                onCancel();
                return true;
            case MotionEvent.ACTION_UP:
                getParent().requestDisallowInterceptTouchEvent(false);
                onUp(event);
                return true;
        }
        return false;
    }
    private void onDown(MotionEvent e) {
        mLastX = e.getX();
        mLastY = e.getY();
        checkTouchArea(e.getX(), e.getY());
    }
    private void onMove(MotionEvent e) {
        float diffX = e.getX() - mLastX;
        float diffY = e.getY() - mLastY;
        switch (mTouchArea) {
            case CENTER:
                moveFrame(diffX, diffY);
                break;
            case LEFT_TOP:
                moveHandleLT(diffX, diffY);
                break;
            case RIGHT_TOP:
                moveHandleRT(diffX, diffY);
                break;
            case LEFT_BOTTOM:
                moveHandleLB(diffX, diffY);
                break;
            case RIGHT_BOTTOM:
                moveHandleRB(diffX, diffY);
                break;
            case OUT_OF_BOUNDS:

                return;

        }
        invalidate();
        mLastX = e.getX();
        mLastY = e.getY();
    }
    private void onUp(MotionEvent e) {
        if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = false;
        if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = false;
        mTouchArea = TouchArea.OUT_OF_BOUNDS;
        invalidate();
    }
    private void onCancel() {
        mTouchArea = TouchArea.OUT_OF_BOUNDS;
        invalidate();
    }
    private void checkTouchArea(float x, float y) {
        if (isInsideCornerLeftTop(x, y)) {
            mTouchArea = TouchArea.LEFT_TOP;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideCornerRightTop(x, y)) {
            mTouchArea = TouchArea.RIGHT_TOP;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideCornerLeftBottom(x, y)) {
            mTouchArea = TouchArea.LEFT_BOTTOM;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideCornerRightBottom(x, y)) {
            mTouchArea = TouchArea.RIGHT_BOTTOM;
            if (mHandleShowMode == ShowMode.SHOW_ON_TOUCH) mShowHandle = true;
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            return;
        }
        if (isInsideFrame(x, y)) {
            if (mGuideShowMode == ShowMode.SHOW_ON_TOUCH) mShowGuide = true;
            mTouchArea = TouchArea.CENTER;
            return;
        }
        mTouchArea = TouchArea.OUT_OF_BOUNDS;
    }
    private boolean isInsideFrame(float x, float y) {
        if (mFrameRect.left <= x && mFrameRect.right >= x) {
            if (mFrameRect.top <= y && mFrameRect.bottom >= y) {
                mTouchArea = TouchArea.CENTER;
                return true;
            }
        }
        return false;
    }
    private boolean isInsideCornerLeftTop(float x, float y) {
        float dx = x - mFrameRect.left;
        float dy = y - mFrameRect.top;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }
    private boolean isInsideCornerRightTop(float x, float y) {
        float dx = x - mFrameRect.right;
        float dy = y - mFrameRect.top;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }
    private boolean isInsideCornerLeftBottom(float x, float y) {
        float dx = x - mFrameRect.left;
        float dy = y - mFrameRect.bottom;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }
    private boolean isInsideCornerRightBottom(float x, float y) {
        float dx = x - mFrameRect.right;
        float dy = y - mFrameRect.bottom;
        float d = dx * dx + dy * dy;
        return sq(mHandleSize + mTouchPadding) >= d;
    }
    private void moveFrame(float x, float y) {
        mFrameRect.left += x;
        mFrameRect.right += x;
        mFrameRect.top += y;
        mFrameRect.bottom += y;
        checkMoveBounds();
    }
    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleLT(float diffX, float diffY) {
        if (mMoveMode == MoveMode.FREE) {
            mFrameRect.left += diffX;
            mFrameRect.top += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.top -= offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.left += dx;
            mFrameRect.top += dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.left -= offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.left -= offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.left)) {
                ox = mImageRect.left - mFrameRect.left;
                mFrameRect.left += ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.top += oy;
            }
            if (!isInsideVertical(mFrameRect.top)) {
                oy = mImageRect.top - mFrameRect.top;
                mFrameRect.top += oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.left += ox;
            }
        }
    }
    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleRT(float diffX, float diffY) {
        if (mMoveMode == MoveMode.FREE) {
            mFrameRect.right += diffX;
            mFrameRect.top += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.right += offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.top -= offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.right += dx;
            mFrameRect.top -= dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.right += offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.top -= offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.top -= offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.right += offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.right)) {
                ox = mFrameRect.right - mImageRect.right;
                mFrameRect.right -= ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.top += oy;
            }
            if (!isInsideVertical(mFrameRect.top)) {
                oy = mImageRect.top - mFrameRect.top;
                mFrameRect.top += oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.right -= ox;
            }
        }
    }
    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleLB(float diffX, float diffY) {
        if (mMoveMode == MoveMode.FREE) {
            mFrameRect.left += diffX;
            mFrameRect.bottom += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.left -= offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.bottom += offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.left += dx;
            mFrameRect.bottom -= dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.left -= offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.bottom += offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.bottom += offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.left -= offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.left)) {
                ox = mImageRect.left - mFrameRect.left;
                mFrameRect.left += ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.bottom -= oy;
            }
            if (!isInsideVertical(mFrameRect.bottom)) {
                oy = mFrameRect.bottom - mImageRect.bottom;
                mFrameRect.bottom -= oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.left += ox;
            }
        }
    }
    @SuppressWarnings("UnnecessaryLocalVariable")
    private void moveHandleRB(float diffX, float diffY) {
        if (mMoveMode == MoveMode.FREE) {
            mFrameRect.right += diffX;
            mFrameRect.bottom += diffY;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.right += offsetX;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.bottom += offsetY;
            }
            checkScaleBounds();
        } else {
            float dx = diffX;
            float dy = diffX * getRatioY() / getRatioX();
            mFrameRect.right += dx;
            mFrameRect.bottom += dy;
            if (isWidthTooSmall()) {
                float offsetX = mMinFrameSize - getFrameW();
                mFrameRect.right += offsetX;
                float offsetY = offsetX * getRatioY() / getRatioX();
                mFrameRect.bottom += offsetY;
            }
            if (isHeightTooSmall()) {
                float offsetY = mMinFrameSize - getFrameH();
                mFrameRect.bottom += offsetY;
                float offsetX = offsetY * getRatioX() / getRatioY();
                mFrameRect.right += offsetX;
            }
            float ox, oy;
            if (!isInsideHorizontal(mFrameRect.right)) {
                ox = mFrameRect.right - mImageRect.right;
                mFrameRect.right -= ox;
                oy = ox * getRatioY() / getRatioX();
                mFrameRect.bottom -= oy;
            }
            if (!isInsideVertical(mFrameRect.bottom)) {
                oy = mFrameRect.bottom - mImageRect.bottom;
                mFrameRect.bottom -= oy;
                ox = oy * getRatioX() / getRatioY();
                mFrameRect.right -= ox;
            }
        }
    }
    private void checkScaleBounds() {
        float lDiff = mFrameRect.left - mImageRect.left;
        float rDiff = mFrameRect.right - mImageRect.right;
        float tDiff = mFrameRect.top - mImageRect.top;
        float bDiff = mFrameRect.bottom - mImageRect.bottom;

        if (lDiff < 0) {
            mFrameRect.left -= lDiff;
        }
        if (rDiff > 0) {
            mFrameRect.right -= rDiff;
        }
        if (tDiff < 0) {
            mFrameRect.top -= tDiff;
        }
        if (bDiff > 0) {
            mFrameRect.bottom -= bDiff;
        }
    }
    private void checkMoveBounds() {
        float diff = mFrameRect.left - mImageRect.left;
        if (diff < 0) {
            mFrameRect.left -= diff;
            mFrameRect.right -= diff;
        }
        diff = mFrameRect.right - mImageRect.right;
        if (diff > 0) {
            mFrameRect.left -= diff;
            mFrameRect.right -= diff;
        }
        diff = mFrameRect.top - mImageRect.top;
        if (diff < 0) {
            mFrameRect.top -= diff;
            mFrameRect.bottom -= diff;
        }
        diff = mFrameRect.bottom - mImageRect.bottom;
        if (diff > 0) {
            mFrameRect.top -= diff;
            mFrameRect.bottom -= diff;
        }
    }
    private boolean isInsideHorizontal(float x) {
        return mImageRect.left <= x && mImageRect.right >= x;
    }
    private boolean isInsideVertical(float y) {
        return mImageRect.top <= y && mImageRect.bottom >= y;
    }
    private boolean isWidthTooSmall() {
        return getFrameW() < mMinFrameSize;
    }
    private boolean isHeightTooSmall() {
        return getFrameH() < mMinFrameSize;
    }
    private void recalculateFrameRect(int durationMillis) {
        if (mImageRect == null) return;

        final RectF currentRect = new RectF(mFrameRect);
        final RectF newRect = calcFrameRect(mImageRect);
        final float diffL = newRect.left - currentRect.left;
        final float diffT = newRect.top - currentRect.top;
        final float diffR = newRect.right - currentRect.right;
        final float diffB = newRect.bottom - currentRect.bottom;

        mFrameRect = calcFrameRect(mImageRect);
        invalidate();

    }
    private float getRatioX(float w) {
        switch (mMoveMode) {
            case FIT_IMAGE:
                return mImageRect.width();
            case FREE:
                return w;
            case RATIO_4_3:
                return 4;
            case RATIO_3_4:
                return 3;
            case RATIO_16_9:
                return 16;
            case RATIO_9_16:
                return 9;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.x;
            default:
                return w;
        }
    }
    private float getRatioY(float h) {
        switch (mMoveMode) {
            case FIT_IMAGE:
                return mImageRect.height();
            case FREE:
                return h;
            case RATIO_4_3:
                return 3;
            case RATIO_3_4:
                return 4;
            case RATIO_16_9:
                return 9;
            case RATIO_9_16:
                return 16;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.y;
            default:
                return h;
        }
    }
    private float getRatioX() {
        switch (mMoveMode) {
            case FIT_IMAGE:
                return mImageRect.width();
            case RATIO_4_3:
                return 4;
            case RATIO_3_4:
                return 3;
            case RATIO_16_9:
                return 16;
            case RATIO_9_16:
                return 9;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.x;
            default:
                return 1;
        }
    }
    private float getRatioY() {
        switch (mMoveMode) {
            case FIT_IMAGE:
                return mImageRect.height();
            case RATIO_4_3:
                return 3;
            case RATIO_3_4:
                return 4;
            case RATIO_16_9:
                return 9;
            case RATIO_9_16:
                return 16;
            case SQUARE:
            case CIRCLE:
            case CIRCLE_SQUARE:
                return 1;
            case CUSTOM:
                return mCustomRatio.y;
            default:
                return 1;
        }
    }
    private float getDensity() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((WindowManager) getContext().getSystemService(WINDOW_SERVICE)).getDefaultDisplay()
                .getMetrics(displayMetrics);
        return displayMetrics.density;
    }
    private float sq(float value) {
        return value * value;
    }
    private float constrain(float val, float min, float max, float defaultVal) {
        if (val < min || val > max) return defaultVal;
        return val;
    }
    private float getRotatedWidth(float angle) {
        return getRotatedWidth(angle, mImgWidth, mImgHeight);
    }
    private float getRotatedWidth(float angle, float width, float height) {
        return angle % 180 == 0 ? width : height;
    }
    private float getRotatedHeight(float angle) {
        return getRotatedHeight(angle, mImgWidth, mImgHeight);
    }
    private float getRotatedHeight(float angle, float width, float height) {
        return angle % 180 == 0 ? height : width;
    }
    @Override
    public void setImageBitmap(Bitmap bitmap) {
        super.setImageBitmap(bitmap);
    }
    @Override
    public void setImageResource(int resId) {
        mIsInitialized = false;
        resetImageInfo();
        super.setImageResource(resId);
        updateLayout();
    }
    @Override
    public void setImageDrawable(Drawable drawable) {
        mIsInitialized = false;
        resetImageInfo();
        setImageDrawableInternal(drawable);
    }
    private void setImageDrawableInternal(Drawable drawable) {
        super.setImageDrawable(drawable);
        updateLayout();
    }
    @Override
    public void setImageURI(Uri uri) {
        mIsInitialized = false;
        super.setImageURI(uri);
        updateLayout();
    }
    private void updateLayout() {
        Drawable d = getDrawable();
        if (d != null) {
            setupLayout(mViewWidth, mViewHeight);
        }
    }
    private void resetImageInfo() {
        if (mIsLoading.get()) return;

        mInputImageWidth = 500;
        mInputImageHeight = 500;

        mAngle = mExifRotation;
    }
    public void rotateImage(RotateDegrees degrees, int durationMillis) {


        final float currentAngle = mAngle;
        final float newAngle = (mAngle + degrees.getValue());
        final float angleDiff = newAngle - currentAngle;
        final float currentScale = mScale;
        final float newScale = calcScale(mViewWidth, mViewHeight, newAngle);

        mAngle = newAngle % 360;
        mScale = newScale;
        setupLayout(mViewWidth, mViewHeight);

    }
    public RectF getActualCropRect() {
        float offsetX = (mImageRect.left / mScale);
        float offsetY = (mImageRect.top / mScale);
        float l = (mFrameRect.left / mScale) - offsetX;
        float t = (mFrameRect.top / mScale) - offsetY;
        float r = (mFrameRect.right / mScale) - offsetX;
        float b = (mFrameRect.bottom / mScale) - offsetY;
        l = Math.max(0, l);
        t = Math.max(0, t);
        r = Math.min(mImageRect.right / mScale, r);
        b = Math.min(mImageRect.bottom / mScale, b);
        return new RectF(200, 200, 200, 200);
    }
    private RectF applyInitialFrameRect(RectF initialFrameRect) {
        RectF frameRect = new RectF();
        frameRect.set(initialFrameRect.left * mScale, initialFrameRect.top * mScale,
                initialFrameRect.right * mScale, initialFrameRect.bottom * mScale);
        frameRect.offset(mImageRect.left, mImageRect.top);
        float l = Math.max(mImageRect.left, frameRect.left);
        float t = Math.max(mImageRect.top, frameRect.top);
        float r = Math.min(mImageRect.right, frameRect.right);
        float b = Math.min(mImageRect.bottom, frameRect.bottom);
        frameRect.set(100, 100, 100, 100);
        return frameRect;
    }
    public void setCustomRatio(int ratioX, int ratioY, int durationMillis) {
        if (ratioX == 0 || ratioY == 0) return;
        mMoveMode = MoveMode.CUSTOM;
        mCustomRatio = new PointF(ratioX, ratioY);
        recalculateFrameRect(durationMillis);
    }
    public void setCustomRatio(int ratioX, int ratioY) {
        setCustomRatio(ratioX, ratioY, mAnimationDurationMillis);
    }
    public void setOverlayColor(int overlayColor) {
        this.mOverlayColor = overlayColor;
        invalidate();
    }
    public void setFrameColor(int frameColor) {
        this.mFrameColor = frameColor;
        invalidate();
    }
    public void setHandleColor(int handleColor) {
        this.mHandleColor = handleColor;
        invalidate();
    }
    public void setGuideColor(int guideColor) {
        this.mGuideColor = guideColor;
        invalidate();
    }
    public void setBackgroundColor(int bgColor) {
        this.mBackgroundColor = bgColor;
        invalidate();
    }
    public void setMinFrameSizeInDp(int minDp) {
        mMinFrameSize = minDp * getDensity();
    }
    public void setMinFrameSizeInPx(int minPx) {
        mMinFrameSize = minPx;
    }
    public void setHandleSizeInDp(int handleDp) {
        mHandleSize = (int) (handleDp * getDensity());
    }
    public void setTouchPaddingInDp(int paddingDp) {
        mTouchPadding = (int) (paddingDp * getDensity());
    }

    public void setGuideShowMode(ShowMode mode) {
        mGuideShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowGuide = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowGuide = false;
                break;
        }
        invalidate();
    }

    public void setHandleShowMode(ShowMode mode) {
        mHandleShowMode = mode;
        switch (mode) {
            case SHOW_ALWAYS:
                mShowHandle = true;
                break;
            case NOT_SHOW:
            case SHOW_ON_TOUCH:
                mShowHandle = false;
                break;
        }
        invalidate();
    }

    public void setFrameStrokeWeightInDp(int weightDp) {
        mFrameStrokeWeight = weightDp * getDensity();
        invalidate();
    }

    public void setGuideStrokeWeightInDp(int weightDp) {
        mGuideStrokeWeight = weightDp * getDensity();
        invalidate();
    }

    public void setCropEnabled(boolean enabled) {
        mismoveingEnabled = enabled;
        invalidate();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    public void setInitialFrameScale(float initialScale) {
        mInitialFrameScale = constrain(initialScale, 0.01f, 1.0f, DEFAULT_INITIAL_FRAME_SCALE);
    }

    public void setAnimationEnabled(boolean enabled) {
        mIsAnimationEnabled = enabled;
    }
    public void setAnimationDuration(int durationMillis) {
        mAnimationDurationMillis = durationMillis;
    }
    private void setScale(float mScale) {
        this.mScale = mScale;
    }
    private void setCenter(PointF mCenter) {
        this.mCenter = mCenter;
    }
    private float getFrameW() {
        return (mFrameRect.right - mFrameRect.left);
    }
    private float getFrameH() {
        return (mFrameRect.bottom - mFrameRect.top);
    }
    private enum TouchArea {
        OUT_OF_BOUNDS, CENTER, LEFT_TOP, RIGHT_TOP, LEFT_BOTTOM, RIGHT_BOTTOM
    }
    public enum MoveMode {
        FIT_IMAGE(0), RATIO_4_3(1), RATIO_3_4(2), SQUARE(3), RATIO_16_9(4), RATIO_9_16(5), FREE(
                6), CUSTOM(7), CIRCLE(8), CIRCLE_SQUARE(9);
        private final int ID;

        MoveMode(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }
    public enum ShowMode {
        SHOW_ALWAYS(1), SHOW_ON_TOUCH(2), NOT_SHOW(3);
        private final int ID;

        ShowMode(final int id) {
            this.ID = id;
        }

        public int getId() {
            return ID;
        }
    }
    public enum RotateDegrees {
        ROTATE_90D(90), ROTATE_180D(180), ROTATE_270D(270), ROTATE_M90D(-90), ROTATE_M180D(
                -180), ROTATE_M270D(-270);

        private final int VALUE;

        RotateDegrees(final int value) {
            this.VALUE = value;
        }

        public int getValue() {
            return VALUE;
        }
    }
    public static class SavedState extends BaseSavedState {
        MoveMode mode;
        int backgroundColor;
        int overlayColor;
        int frameColor;
        ShowMode guideShowMode;
        ShowMode handleShowMode;
        boolean showGuide;
        boolean showHandle;
        int handleSize;
        int touchPadding;
        float minFrameSize;
        float customRatioX;
        float customRatioY;
        float frameStrokeWeight;
        float guideStrokeWeight;
        boolean ismoveingEnabled;
        int handleColor;
        int guideColor;
        float initialFrameScale;
        float angle;
        boolean isAnimationEnabled;
        int animationDuration;
        int exifRotation;

        int compressQuality;
        boolean isDebug;

        boolean isHandleShadowEnabled;
        int inputImageWidth;
        int inputImageHeight;


        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            mode = (MoveMode) in.readSerializable();
            backgroundColor = in.readInt();
            overlayColor = in.readInt();
            frameColor = in.readInt();
            guideShowMode = (ShowMode) in.readSerializable();
            handleShowMode = (ShowMode) in.readSerializable();
            showGuide = (in.readInt() != 0);
            showHandle = (in.readInt() != 0);
            handleSize = in.readInt();
            touchPadding = in.readInt();
            minFrameSize = in.readFloat();
            customRatioX = in.readFloat();
            customRatioY = in.readFloat();
            frameStrokeWeight = in.readFloat();
            guideStrokeWeight = in.readFloat();
            ismoveingEnabled = (in.readInt() != 0);
            handleColor = in.readInt();
            guideColor = in.readInt();
            initialFrameScale = in.readFloat();
            angle = in.readFloat();
            isAnimationEnabled = (in.readInt() != 0);
            animationDuration = in.readInt();
            exifRotation = in.readInt();
            compressQuality = in.readInt();
            isDebug = (in.readInt() != 0);

            isHandleShadowEnabled = (in.readInt() != 0);
            inputImageWidth = in.readInt();
            inputImageHeight = in.readInt();

        }

        @Override
        public void writeToParcel(Parcel out, int flag) {
            super.writeToParcel(out, flag);
            out.writeSerializable(mode);
            out.writeInt(backgroundColor);
            out.writeInt(overlayColor);
            out.writeInt(frameColor);
            out.writeSerializable(guideShowMode);
            out.writeSerializable(handleShowMode);
            out.writeInt(showGuide ? 1 : 0);
            out.writeInt(showHandle ? 1 : 0);
            out.writeInt(handleSize);
            out.writeInt(touchPadding);
            out.writeFloat(minFrameSize);
            out.writeFloat(customRatioX);
            out.writeFloat(customRatioY);
            out.writeFloat(frameStrokeWeight);
            out.writeFloat(guideStrokeWeight);
            out.writeInt(ismoveingEnabled ? 1 : 0);
            out.writeInt(handleColor);
            out.writeInt(guideColor);
            out.writeFloat(initialFrameScale);
            out.writeFloat(angle);
            out.writeInt(isAnimationEnabled ? 1 : 0);
            out.writeInt(animationDuration);
            out.writeInt(exifRotation);
            out.writeInt(compressQuality);
            out.writeInt(isDebug ? 1 : 0);
            out.writeInt(isHandleShadowEnabled ? 1 : 0);
            out.writeInt(inputImageWidth);
            out.writeInt(inputImageHeight);

        }

        public static final Creator CREATOR = new Creator() {
            public SavedState createFromParcel(final Parcel inParcel) {
                return new SavedState(inParcel);
            }

            public SavedState[] newArray(final int inSize) {
                return new SavedState[inSize];
            }
        };
    }
}
