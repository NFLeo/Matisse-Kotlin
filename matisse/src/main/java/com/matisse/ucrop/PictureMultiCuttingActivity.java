package com.matisse.ucrop;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.matisse.R;
import com.matisse.ucrop.callback.BitmapCropCallback;
import com.matisse.ucrop.immersion.CropImmersiveManage;
import com.matisse.ucrop.model.AspectRatio;
import com.matisse.ucrop.model.CutInfo;
import com.matisse.ucrop.util.FileUtils;
import com.matisse.ucrop.util.VersionUtils;
import com.matisse.ucrop.view.CropImageView;
import com.matisse.ucrop.view.GestureCropImageView;
import com.matisse.ucrop.view.OverlayView;
import com.matisse.ucrop.view.TransformImageView;
import com.matisse.ucrop.view.UCropView;

import java.io.File;
import java.io.FileInputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */

@SuppressWarnings("ConstantConditions")
public class PictureMultiCuttingActivity extends AppCompatActivity {

    public static final int DEFAULT_COMPRESS_QUALITY = 90;
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;

    public static final int NONE = 0;
    public static final int SCALE = 1;
    public static final int ROTATE = 2;
    public static final int ALL = 3;

    @IntDef({NONE, SCALE, ROTATE, ALL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface GestureTypes {

    }

    private static final String TAG = "UCropActivity";

    private static final int TABS_COUNT = 3;
    private static final int SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000;
    private static final int ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42;
    private RecyclerView mRecyclerView;
    private PicturePhotoGalleryAdapter adapter;
    private String mToolbarTitle;
    private ArrayList<CutInfo> list;
    // Enables dynamic coloring
    private int mToolbarColor;
    private int mStatusBarColor;
    private int mActiveWidgetColor;
    private int mToolbarWidgetColor;
    @ColorInt
    private int mRootViewBackgroundColor;
    @DrawableRes
    private int mToolbarCancelDrawable;
    @DrawableRes
    private int mToolbarCropDrawable;
    private int mLogoColor;

    private boolean mShowLoader = true;
    private boolean circleDimmedLayer;
    private UCropView mUCropView;
    private GestureCropImageView mGestureCropImageView;
    private OverlayView mOverlayView;
    private List<ViewGroup> mCropAspectRatioViews = new ArrayList<>();
    private TextView mTextViewRotateAngle, mTextViewScalePercent;
    private View mBlockingView;
    private RelativeLayout uCropMultiplePhotoBox;
    private Bitmap.CompressFormat mCompressFormat = DEFAULT_COMPRESS_FORMAT;
    private int mCompressQuality = DEFAULT_COMPRESS_QUALITY;
    private int[] mAllowedGestures = new int[]{SCALE, ROTATE, ALL};
    /**
     * 是否可拖动裁剪框
     */
    private boolean isDragFrame;

    /**
     * 图片是否可拖动或旋转
     */
    private boolean scaleEnabled, rotateEnabled, openWhiteStatusBar;

    private int cutIndex;

    /**
     * 是否使用沉浸式，子类复写该方法来确定是否采用沉浸式
     *
     * @return 是否沉浸式，默认true
     */
    @Override
    public boolean isImmersive() {
        return true;
    }


    /**
     * 具体沉浸的样式，可以根据需要自行修改状态栏和导航栏的颜色
     */
    public void immersive() {
        CropImmersiveManage.immersiveAboveAPI23(this
                , mStatusBarColor
                , mToolbarColor
                , openWhiteStatusBar);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();
        getIntentData(intent);
        if (isImmersive()) {
            immersive();
        }
        setContentView(R.layout.ucrop_picture_activity_multi_cutting);
        uCropMultiplePhotoBox = findViewById(R.id.ucrop_mulit_photobox);
        initLoadCutData();
        addPhotoRecyclerView();
        setupViews(intent);
        setInitialState();
        addBlockingView();
        setImageData(intent);

    }

    /**
     * 装载裁剪数据
     */
    private void initLoadCutData() {
        list = (ArrayList<CutInfo>) getIntent().getSerializableExtra(UCropMulti.Options.EXTRA_CUT_CROP);
        // Crop cut list
        if (list == null || list.size() == 0) {
            closeActivity();
            return;
        }
    }

    /**
     * 动态添加多图裁剪底部预览图片列表
     */
    private void addPhotoRecyclerView() {
        mRecyclerView = new RecyclerView(this);
        mRecyclerView.setId(R.id.id_recycler);
        mRecyclerView.setBackgroundColor(ContextCompat.getColor(this, R.color.ucrop_color_widget_background));
        RelativeLayout.LayoutParams lp =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dip2px(80));
        mRecyclerView.setLayoutParams(lp);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);
        resetCutDataStatus();
        list.get(cutIndex).setCut(true);
        adapter = new PicturePhotoGalleryAdapter(this, list);
        mRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener((position, view) -> {
            if (cutIndex == position) {
                return;
            }
            cutIndex = position;
            resetCutData();
        });

        uCropMultiplePhotoBox.addView(mRecyclerView);
        changeLayoutParams();
        FrameLayout uCropFrame = findViewById(R.id.ucrop_frame);
        ((RelativeLayout.LayoutParams) uCropFrame.getLayoutParams())
                .addRule(RelativeLayout.ABOVE, R.id.id_recycler);
    }

    /**
     * 切换裁剪图片
     */
    private void refreshPhotoRecyclerData() {
        resetCutDataStatus();
        list.get(cutIndex).setCut(true);
        adapter.notifyDataSetChanged();

        uCropMultiplePhotoBox.addView(mRecyclerView);
        changeLayoutParams();
        FrameLayout uCropFrame = findViewById(R.id.ucrop_frame);
        ((RelativeLayout.LayoutParams) uCropFrame.getLayoutParams())
                .addRule(RelativeLayout.ABOVE, R.id.id_recycler);
    }

    /**
     * 重置数据裁剪状态
     */
    private void resetCutDataStatus() {
        int size = list.size();
        for (int i = 0; i < size; i++) {
            CutInfo cutInfo = list.get(i);
            cutInfo.setCut(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.ucrop_menu_activity, menu);
        // Change crop & loader menu icons color to match the rest of the UI colors
        MenuItem menuItemLoader = menu.findItem(R.id.menu_loader);
        Drawable menuItemLoaderIcon = menuItemLoader.getIcon();
        if (menuItemLoaderIcon != null) {
            try {
                menuItemLoaderIcon.mutate();
                menuItemLoaderIcon.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
                menuItemLoader.setIcon(menuItemLoaderIcon);
            } catch (IllegalStateException e) {
                Log.i(TAG, String.format("%s - %s", e.getMessage(), getString(R.string.ucrop_mutate_exception_hint)));
            }
            ((Animatable) menuItemLoader.getIcon()).start();
        }

        MenuItem menuItemCrop = menu.findItem(R.id.menu_crop);
        Drawable menuItemCropIcon = ContextCompat.getDrawable(this, mToolbarCropDrawable);
        if (menuItemCropIcon != null) {
            menuItemCropIcon.mutate();
            menuItemCropIcon.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
            menuItemCrop.setIcon(menuItemCropIcon);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_crop).setVisible(!mShowLoader);
        menu.findItem(R.id.menu_loader).setVisible(mShowLoader);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_crop) {
            cropAndSaveImage();
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        exitAnimation();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGestureCropImageView != null) {
            mGestureCropImageView.cancelAllAnimations();
        }
    }

    /**
     * This method extracts all data from the incoming intent and setups views properly.
     */
    private void setImageData(@NonNull Intent intent) {
        Uri inputUri = intent.getParcelableExtra(UCropMulti.EXTRA_INPUT_URI);
        Uri outputUri = intent.getParcelableExtra(UCropMulti.EXTRA_OUTPUT_URI);
        processOptions(intent);

        if (inputUri != null && outputUri != null) {
            try {
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(inputUri, "r");
                FileInputStream inputStream = new FileInputStream(parcelFileDescriptor.getFileDescriptor());
                String suffix = FileUtils.extSuffix(inputStream);
                boolean isGif = FileUtils.isGifForSuffix(suffix);
                mGestureCropImageView.setRotateEnabled(isGif ? false : rotateEnabled);
                mGestureCropImageView.setScaleEnabled(isGif ? false : scaleEnabled);
                mGestureCropImageView.setImageUri(inputUri, outputUri);
            } catch (Exception e) {
                setResultError(e);
                closeActivity();
            }
        } else {
            setResultError(new NullPointerException(getString(R.string.ucrop_error_input_data_is_absent)));
            closeActivity();
        }
    }

    /**
     * This method extracts {@link UCrop.Options #optionsBundle} from incoming intent
     * and setups Activity, {@link OverlayView} and {@link CropImageView} properly.
     */
    @SuppressWarnings("deprecation")
    private void processOptions(@NonNull Intent intent) {
        // Bitmap compression options
        String compressionFormatName = intent.getStringExtra(UCropMulti.Options.EXTRA_COMPRESSION_FORMAT_NAME);
        Bitmap.CompressFormat compressFormat = null;
        if (!TextUtils.isEmpty(compressionFormatName)) {
            compressFormat = Bitmap.CompressFormat.valueOf(compressionFormatName);
        }
        mCompressFormat = (compressFormat == null) ? DEFAULT_COMPRESS_FORMAT : compressFormat;

        mCompressQuality = intent.getIntExtra(UCrop.Options.EXTRA_COMPRESSION_QUALITY, PictureMultiCuttingActivity.DEFAULT_COMPRESS_QUALITY);

        // Gestures options
        int[] allowedGestures = intent.getIntArrayExtra(UCropMulti.Options.EXTRA_ALLOWED_GESTURES);
        if (allowedGestures != null && allowedGestures.length == TABS_COUNT) {
            mAllowedGestures = allowedGestures;
        }

        // Crop image view options
        mGestureCropImageView.setMaxBitmapSize(intent.getIntExtra(UCropMulti.Options.EXTRA_MAX_BITMAP_SIZE, CropImageView.DEFAULT_MAX_BITMAP_SIZE));
        mGestureCropImageView.setMaxScaleMultiplier(intent.getFloatExtra(UCropMulti.Options.EXTRA_MAX_SCALE_MULTIPLIER, CropImageView.DEFAULT_MAX_SCALE_MULTIPLIER));
        mGestureCropImageView.setImageToWrapCropBoundsAnimDuration(intent.getIntExtra(UCropMulti.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION, CropImageView.DEFAULT_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION));

        // Overlay view options
        mOverlayView.setDragFrame(isDragFrame);
        mOverlayView.setFreestyleCropEnabled(intent.getBooleanExtra(UCropMulti.Options.EXTRA_FREE_STYLE_CROP, false));
        circleDimmedLayer = intent.getBooleanExtra(UCropMulti.Options.EXTRA_CIRCLE_DIMMED_LAYER, OverlayView.DEFAULT_CIRCLE_DIMMED_LAYER);
        mOverlayView.setDimmedColor(intent.getIntExtra(UCropMulti.Options.EXTRA_DIMMED_LAYER_COLOR, getResources().getColor(R.color.ucrop_color_default_dimmed)));
        mOverlayView.setCircleDimmedLayer(circleDimmedLayer);

        mOverlayView.setShowCropFrame(intent.getBooleanExtra(UCropMulti.Options.EXTRA_SHOW_CROP_FRAME, OverlayView.DEFAULT_SHOW_CROP_FRAME));
        mOverlayView.setCropFrameColor(intent.getIntExtra(UCropMulti.Options.EXTRA_CROP_FRAME_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_frame)));
        mOverlayView.setCropFrameStrokeWidth(intent.getIntExtra(UCropMulti.Options.EXTRA_CROP_FRAME_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_frame_stoke_width)));

        mOverlayView.setShowCropGrid(intent.getBooleanExtra(UCropMulti.Options.EXTRA_SHOW_CROP_GRID, OverlayView.DEFAULT_SHOW_CROP_GRID));
        mOverlayView.setCropGridRowCount(intent.getIntExtra(UCropMulti.Options.EXTRA_CROP_GRID_ROW_COUNT, OverlayView.DEFAULT_CROP_GRID_ROW_COUNT));
        mOverlayView.setCropGridColumnCount(intent.getIntExtra(UCropMulti.Options.EXTRA_CROP_GRID_COLUMN_COUNT, OverlayView.DEFAULT_CROP_GRID_COLUMN_COUNT));
        mOverlayView.setCropGridColor(intent.getIntExtra(UCropMulti.Options.EXTRA_CROP_GRID_COLOR, getResources().getColor(R.color.ucrop_color_default_crop_grid)));
        mOverlayView.setCropGridStrokeWidth(intent.getIntExtra(UCropMulti.Options.EXTRA_CROP_GRID_STROKE_WIDTH, getResources().getDimensionPixelSize(R.dimen.ucrop_default_crop_grid_stoke_width)));

        // Aspect ratio options
        float aspectRatioX = intent.getFloatExtra(UCropMulti.EXTRA_ASPECT_RATIO_X, 0);
        float aspectRatioY = intent.getFloatExtra(UCropMulti.EXTRA_ASPECT_RATIO_Y, 0);

        int aspectRationSelectedByDefault = intent.getIntExtra(UCropMulti.Options.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, 0);
        ArrayList<AspectRatio> aspectRatioList = intent.getParcelableArrayListExtra(UCropMulti.Options.EXTRA_ASPECT_RATIO_OPTIONS);

        if (aspectRatioX > 0 && aspectRatioY > 0) {
            mGestureCropImageView.setTargetAspectRatio(aspectRatioX / aspectRatioY);
        } else if (aspectRatioList != null && aspectRationSelectedByDefault < aspectRatioList.size()) {
            mGestureCropImageView.setTargetAspectRatio(aspectRatioList.get(aspectRationSelectedByDefault).getAspectRatioX() /
                    aspectRatioList.get(aspectRationSelectedByDefault).getAspectRatioY());
        } else {
            mGestureCropImageView.setTargetAspectRatio(CropImageView.SOURCE_IMAGE_ASPECT_RATIO);
        }

        // Result bitmap max size options
        int maxSizeX = intent.getIntExtra(UCropMulti.EXTRA_MAX_SIZE_X, 0);
        int maxSizeY = intent.getIntExtra(UCropMulti.EXTRA_MAX_SIZE_Y, 0);

        if (maxSizeX > 0 && maxSizeY > 0) {
            mGestureCropImageView.setMaxResultImageSizeX(maxSizeX);
            mGestureCropImageView.setMaxResultImageSizeY(maxSizeY);
        }
    }

    private void getIntentData(@NonNull Intent intent) {
        openWhiteStatusBar = intent.getBooleanExtra(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_OPEN_WHITE_STATUSBAR, false);
        mStatusBarColor = intent.getIntExtra(UCropMulti.Options.EXTRA_STATUS_BAR_COLOR, ContextCompat.getColor(this, R.color.ucrop_color_statusbar));
        mToolbarColor = intent.getIntExtra(UCropMulti.Options.EXTRA_TOOL_BAR_COLOR, ContextCompat.getColor(this, R.color.ucrop_color_toolbar));
        if (mToolbarColor == 0) {
            mToolbarColor = ContextCompat.getColor(this, R.color.ucrop_color_toolbar);
        }
        if (mStatusBarColor == 0) {
            mStatusBarColor = ContextCompat.getColor(this, R.color.ucrop_color_statusbar);
        }
    }

    private void setupViews(@NonNull Intent intent) {
        scaleEnabled = intent.getBooleanExtra(UCropMulti.Options.EXTRA_SCALE, false);
        rotateEnabled = intent.getBooleanExtra(UCropMulti.Options.EXTRA_ROTATE, false);
        // 是否可拖动裁剪框
        isDragFrame = intent.getBooleanExtra(UCrop.Options.EXTRA_DRAG_CROP_FRAME, true);
        mActiveWidgetColor = intent.getIntExtra(UCropMulti.Options.EXTRA_UCROP_COLOR_WIDGET_ACTIVE, ContextCompat.getColor(this, R.color.ucrop_color_widget_active));
        mToolbarWidgetColor = intent.getIntExtra(UCropMulti.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR, ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget));
        if (mToolbarWidgetColor == 0) {
            mToolbarWidgetColor = ContextCompat.getColor(this, R.color.ucrop_color_toolbar_widget);
        }
        mToolbarCancelDrawable = intent.getIntExtra(UCropMulti.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE, R.drawable.ucrop_ic_cross);
        mToolbarCropDrawable = intent.getIntExtra(UCropMulti.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE, R.drawable.ucrop_ic_done);
        mToolbarTitle = intent.getStringExtra(UCropMulti.Options.EXTRA_UCROP_TITLE_TEXT_TOOLBAR);
        mToolbarTitle = mToolbarTitle != null ? mToolbarTitle : getResources().getString(R.string.ucrop_label_edit_photo);
        mLogoColor = intent.getIntExtra(UCropMulti.Options.EXTRA_UCROP_LOGO_COLOR, ContextCompat.getColor(this, R.color.ucrop_color_default_logo));
        mRootViewBackgroundColor = intent.getIntExtra(UCropMulti.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR, ContextCompat.getColor(this, R.color.ucrop_color_crop_background));
        setNavBarColor();
        setupAppBar();
        initiateRootViews();

        changeLayoutParams();
    }

    /**
     * set NavBar Color
     */
    private void setNavBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int navBarColor = getIntent().getIntExtra(UCropMulti.EXTRA_NAV_BAR_COLOR, 0);
            if (navBarColor != 0) {
                getWindow().setNavigationBarColor(navBarColor);
            }
        }
    }

    /**
     * Configures and styles both status bar and toolbar.
     */
    private void setupAppBar() {
        setStatusBarColor(mStatusBarColor);

        final Toolbar toolbar = findViewById(R.id.toolbar);

        // Set all of the Toolbar coloring
        toolbar.setBackgroundColor(mToolbarColor);
        toolbar.setTitleTextColor(mToolbarWidgetColor);

        final TextView toolbarTitle = toolbar.findViewById(R.id.toolbar_title);
        toolbarTitle.setTextColor(mToolbarWidgetColor);
        toolbarTitle.setText(mToolbarTitle);

        // Color buttons inside the Toolbar
        Drawable stateButtonDrawable = ContextCompat.getDrawable(this, mToolbarCancelDrawable).mutate();
        stateButtonDrawable.setColorFilter(mToolbarWidgetColor, PorterDuff.Mode.SRC_ATOP);
        toolbar.setNavigationIcon(stateButtonDrawable);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    private void initiateRootViews() {
        mUCropView = findViewById(R.id.ucrop);
        mGestureCropImageView = mUCropView.getCropImageView();
        mOverlayView = mUCropView.getOverlayView();
        mGestureCropImageView.setTransformImageListener(mImageListener);

//        ((ImageView) findViewById(R.id.image_view_logo)).setColorFilter(mLogoColor, PorterDuff.Mode.SRC_ATOP);
//
//        findViewById(R.id.ucrop_frame).setBackgroundColor(mRootViewBackgroundColor);
    }

    private TransformImageView.TransformImageListener mImageListener = new TransformImageView.TransformImageListener() {
        @Override
        public void onRotate(float currentAngle) {
            setAngleText(currentAngle);
        }

        @Override
        public void onScale(float currentScale) {
            setScaleText(currentScale);
        }

        @Override
        public void onLoadComplete() {
            mUCropView.animate().alpha(1).setDuration(300).setInterpolator(new AccelerateInterpolator());
            mBlockingView.setClickable(false);
            mShowLoader = false;
            supportInvalidateOptionsMenu();
        }

        @Override
        public void onLoadFailure(@NonNull Exception e) {
            setResultError(e);
            closeActivity();
        }

    };

    /**
     * Sets status-bar color for L devices.
     *
     * @param color - status-bar color
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setStatusBarColor(@ColorInt int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(color);
            }
        }
    }

    private void setAngleText(float angle) {
        if (mTextViewRotateAngle != null) {
            mTextViewRotateAngle.setText(String.format(Locale.getDefault(), "%.1f°", angle));
        }
    }

    private void setScaleText(float scale) {
        if (mTextViewScalePercent != null) {
            mTextViewScalePercent.setText(String.format(Locale.getDefault(), "%d%%", (int) (scale * 100)));
        }
    }

    private void resetRotation() {
        mGestureCropImageView.postRotate(-mGestureCropImageView.getCurrentAngle());
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private void rotateByAngle(int angle) {
        mGestureCropImageView.postRotate(angle);
        mGestureCropImageView.setImageToWrapCropBounds();
    }

    private void setInitialState() {
        setAllowedGestures(0);
    }

    private void setAllowedGestures(int tab) {
        //mGestureCropImageView.setScaleEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == SCALE);
        //mGestureCropImageView.setRotateEnabled(mAllowedGestures[tab] == ALL || mAllowedGestures[tab] == ROTATE);
    }

    /**
     * Adds view that covers everything below the Toolbar.
     * When it's clickable - user won't be able to click/touch anything below the Toolbar.
     * Need to block user input while loading and cropping an image.
     */
    private void addBlockingView() {
        if (mBlockingView == null) {
            mBlockingView = new View(this);
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            lp.addRule(RelativeLayout.BELOW, R.id.toolbar);
            mBlockingView.setLayoutParams(lp);
            mBlockingView.setClickable(true);
        }
        uCropMultiplePhotoBox.addView(mBlockingView);
    }

    protected void cropAndSaveImage() {
        mBlockingView.setClickable(true);
        mShowLoader = true;
        supportInvalidateOptionsMenu();
        mGestureCropImageView.cropAndSaveImage(mCompressFormat, mCompressQuality, false, new BitmapCropCallback() {

            @Override
            public void onBitmapCropped(@NonNull Uri resultUri, int offsetX, int offsetY, int imageWidth, int imageHeight) {
                setResultUri(resultUri, mGestureCropImageView.getTargetAspectRatio(), offsetX, offsetY, imageWidth, imageHeight);
            }

            @Override
            public void onCropFailure(@NonNull Throwable t) {
                setResultError(t);
                closeActivity();
            }
        });
    }

    protected void setResultUri(Uri uri, float resultAspectRatio, int offsetX, int offsetY, int imageWidth, int imageHeight) {
        try {
//            CutInfo info = list.get(cutIndex);
//            info.setCutPath(uri.getPath());
//            info.setCut(true);
//            info.setResultAspectRatio(resultAspectRatio);
//            info.setOffsetX(offsetX);
//            info.setOffsetY(offsetY);
//            info.setImageWidth(imageWidth);
//            info.setImageHeight(imageHeight);
            cutIndex++;
            if (cutIndex >= list.size()) {
                setResult(RESULT_OK, new Intent()
                        .putExtra(UCropMulti.EXTRA_OUTPUT_URI_LIST, list)
                );
                closeActivity();
            } else {
                resetCutData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 重置裁剪参数
     */
    protected void resetCutData() {
        uCropMultiplePhotoBox.removeView(mRecyclerView);
        setContentView(R.layout.ucrop_picture_activity_multi_cutting);
        uCropMultiplePhotoBox = findViewById(R.id.ucrop_mulit_photobox);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        boolean isAndroidQ = VersionUtils.isAndroidQ();
        String path = list.get(cutIndex).getPath();
        boolean isHttp = FileUtils.isHttp(path);
        String imgType = getLastImgType(isAndroidQ ? FileUtils.getPath(this, Uri.parse(path)) : path);
        Uri uri = isHttp || isAndroidQ ? Uri.parse(path) : Uri.fromFile(new File(path));
        extras.putParcelable(UCropMulti.EXTRA_INPUT_URI, uri);

        File file = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) ?
                getExternalFilesDir(Environment.DIRECTORY_PICTURES) : getCacheDir();
        extras.putParcelable(UCropMulti.EXTRA_OUTPUT_URI,
                Uri.fromFile(new File(file, FileUtils.getCreateFileName("IMG_") + imgType)));
        intent.putExtras(extras);
        refreshPhotoRecyclerData();
        setupViews(intent);
        setImageData(intent);
        // 预览图 一页5个,裁剪到第6个的时候滚动到最新位置，不然预览图片看不到
        if (cutIndex >= 5) {
            mRecyclerView.scrollToPosition(cutIndex);
        }
        changeLayoutParams();
    }

    private void changeLayoutParams() {
        if (mRecyclerView.getLayoutParams() == null) {
            return;
        }
        ((RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams())
                .addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        ((RelativeLayout.LayoutParams) mRecyclerView.getLayoutParams())
                .addRule(RelativeLayout.ABOVE, 0);
    }

    /**
     * 获取图片后缀
     *
     * @param path
     * @return
     */
    public static String getLastImgType(String path) {
        try {
            int index = path.lastIndexOf(".");
            if (index > 0) {
                String imageType = path.substring(index);
                switch (imageType) {
                    case ".png":
                    case ".PNG":
                    case ".jpg":
                    case ".jpeg":
                    case ".JPEG":
                    case ".WEBP":
                    case ".bmp":
                    case ".BMP":
                    case ".webp":
                    case ".gif":
                    case ".GIF":
                        return imageType;
                    default:
                        return ".png";
                }
            } else {
                return ".png";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ".png";
        }
    }

    protected void setResultError(Throwable throwable) {
        setResult(UCropMulti.RESULT_ERROR, new Intent().putExtra(UCropMulti.EXTRA_ERROR, throwable));
    }

    /**
     * exit activity
     */
    protected void closeActivity() {
        finish();
        exitAnimation();
    }

    protected void exitAnimation() {
        int exitAnimation = getIntent().getIntExtra(UCropMulti.EXTRA_WINDOW_EXIT_ANIMATION, 0);
        overridePendingTransition(R.anim.ucrop_anim_fade_in, exitAnimation != 0 ? exitAnimation : R.anim.ucrop_close);
    }

    public int dip2px(float dpValue) {
        return (int) (0.5f + dpValue * getResources().getDisplayMetrics().density);
    }
}
