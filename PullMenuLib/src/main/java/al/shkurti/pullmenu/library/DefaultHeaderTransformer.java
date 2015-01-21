
package al.shkurti.pullmenu.library;

import al.shkurti.pullmenu.R;
import al.shkurti.pullmenu.library.sdk.Compat;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.RelativeLayout;
import android.widget.Toast;

import al.shkurti.pullmenu.library.slidingtabstrip.MenuSlidingTabStrip;
import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

/**
 * Default Header Transformer.
 */
public class DefaultHeaderTransformer extends HeaderTransformer {

    public static final int PROGRESS_BAR_STYLE_INSIDE = 0;
    public static final int PROGRESS_BAR_STYLE_OUTSIDE = 1;
    public static final int MENU_INDICATOR_MIN_VALUE = 6;

    private View mHeaderView;
    private ViewGroup mContentLayout;
    private MenuSlidingTabStrip mSlidingTabStrip;
    private SmoothProgressBar mHeaderProgressBar;

    private int mProgressDrawableColor;

    private long mAnimationDuration;
    private int mProgressBarStyle;
    private int mProgressBarHeight = RelativeLayout.LayoutParams.WRAP_CONTENT;
    
    public static int scrollPercantage=0;

    private final Interpolator mInterpolator = new AccelerateInterpolator();

    protected DefaultHeaderTransformer() {
        final int min = getMinimumApiLevel();
        if (Build.VERSION.SDK_INT < min) {
            throw new IllegalStateException("This HeaderTransformer is designed to run on SDK "
                    + min
                    + "+. If using ActionBarSherlock or ActionBarCompat you should use the appropriate provided extra.");
        }
    }

    @Override
    public void onViewCreated(Activity activity, View headerView) {
        mHeaderView = headerView;

        // Get ProgressBar and MenuSlidingTabStrip
        mHeaderProgressBar = (SmoothProgressBar) headerView.findViewById(R.id.pm_progress);
        mSlidingTabStrip = (MenuSlidingTabStrip)headerView.findViewById(R.id.menuIndicator);
        mContentLayout = (ViewGroup) headerView.findViewById(R.id.pm_content);

        mAnimationDuration = activity.getResources().getInteger(android.R.integer.config_shortAnimTime);

        mProgressDrawableColor = activity.getResources().getColor(R.color.default_progress_bar_color);

        // Setup the View styles
        setupViewsFromStyles(activity, headerView);

        applyProgressBarStyle();

        // Apply any custom ProgressBar colors and corner radius
        applyProgressBarSettings();

        // FIXME: I do not like this call here
        onReset();
    }

    @Override
    public void onConfigurationChanged(Activity activity, Configuration newConfig) {
        setupViewsFromStyles(activity, getHeaderView());
    }

    @Override
    public void onReset() {
        // Reset Progress Bar
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setVisibility(View.VISIBLE);
            mHeaderProgressBar.setProgress(0);
            mHeaderProgressBar.setIndeterminate(false);
        }

        // Reset menu sliding tab strip
        if(mSlidingTabStrip!=null){
        	mSlidingTabStrip.setVisibility(View.VISIBLE);
            mSlidingTabStrip.setScrollTo(0);//we reset
            mSlidingTabStrip.changeMenuIndicatorPosition(0);//
        }

        // Reset the Content Layout
        if (mContentLayout != null) {
            mContentLayout.setVisibility(View.VISIBLE);
            Compat.setAlpha(mContentLayout, 1f);
        }
    }

    @Override
    public void onPulled(float percentagePulled) {
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setVisibility(View.VISIBLE);
            final float progress = mInterpolator.getInterpolation(percentagePulled);
            
            mHeaderProgressBar.setProgress(Math.round(mHeaderProgressBar.getMax() * progress));
            scrollPercantage = Math.round(mHeaderProgressBar.getMax() * progress);//mHeaderProgressBar.getMax() = 100
            mSlidingTabStrip.changeMenuIndicatorPosition(scrollPercantage);
            
        }
    }
    


    @Override
    public void onRefreshStarted() {

        // TODO the menu sliding tab strip should become invisible and the refresh state should go on
        // progress bar is set in refreshable state
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setVisibility(View.VISIBLE);
            mHeaderProgressBar.setIndeterminate(true);
        }
    }

    @Override
    public void onReleaseToRefresh() {
        if (mHeaderProgressBar != null) {
            mHeaderProgressBar.setProgress(mHeaderProgressBar.getMax());
        }
    }

    @Override
    public void onRefreshMinimized() {
        // Here we fade out most of the header, leaving just the progress bar
        if (mContentLayout != null) {// this happens only when the boolean in Option class refreshMinimize is true
            // here is made invisible the menu sliding tab strip
            ObjectAnimator.ofFloat(mContentLayout, "alpha", 1f, 0f).start();
        }
    }
    
    @Override
    public void onMenuSelected(){
    	if(mSlidingTabStrip!=null){// a pull menu item is selected
    		/*int m = mSlidingTabStrip.getSelectedPosition();
    		Toast.makeText(mSlidingTabStrip.getContext(), m + "", Toast.LENGTH_SHORT).show();*/
    	}
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    @Override
    public boolean showHeaderView() {
        final boolean changeVis = mHeaderView.getVisibility() != View.VISIBLE;

        if (changeVis) {
            mHeaderView.setVisibility(View.VISIBLE);
            AnimatorSet animSet = new AnimatorSet();
            ObjectAnimator transAnim = ObjectAnimator.ofFloat(mContentLayout, "translationY",
                    -mContentLayout.getHeight(), 0f);
            ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mHeaderView, "alpha", 0f, 1f);
            animSet.playTogether(transAnim, alphaAnim);
            animSet.setDuration(mAnimationDuration);
            animSet.start();
        }

        return changeVis;
    }

    @Override
    public boolean hideHeaderView() {
        final boolean changeVis = mHeaderView.getVisibility() != View.GONE;

        if (changeVis) {
            Animator animator;
            if (mContentLayout.getAlpha() >= 0.5f) {
                // If the content layout is showing, translate and fade out
                animator = new AnimatorSet();
                ObjectAnimator transAnim = ObjectAnimator.ofFloat(mContentLayout, "translationY",
                        0f, -mContentLayout.getHeight());
                ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(mHeaderView, "alpha", 1f, 0f);
                ((AnimatorSet) animator).playTogether(transAnim, alphaAnim);
            } else {
                // If the content layout isn't showing (minimized), just fade out
                animator = ObjectAnimator.ofFloat(mHeaderView, "alpha", 1f, 0f);
            }
            animator.setDuration(mAnimationDuration);
            animator.addListener(new HideAnimationCallback());
            animator.start();
        }

        return changeVis;
    }

    /**
     * Set color to apply to the progress bar.
     * <p/>
     * The best way to apply a color is to load the color from resources: {@code
     * setProgressBarColor(getResources().getColor(R.color.your_color_name))}.
     *
     * @param color The color to use.
     */
    public void setProgressBarColor(int color) {
        if (color != mProgressDrawableColor) {
            mProgressDrawableColor = color;
            mHeaderProgressBar.setSmoothProgressDrawableColor(color);
            applyProgressBarSettings();
        }
    }

    /**
     * Set the progress bar style. {@code style} must be one of {@link #PROGRESS_BAR_STYLE_OUTSIDE}
     * or {@link #PROGRESS_BAR_STYLE_INSIDE}.
     */
    public void setProgressBarStyle(int style) {
        if (mProgressBarStyle != style) {
            mProgressBarStyle = style;
            applyProgressBarStyle();
        }
    }

    /**
     * Set the progress bar height.
     */
    public void setProgressBarHeight(int height) {
        if (mProgressBarHeight != height) {
            mProgressBarHeight = height;
            applyProgressBarStyle();
        }
    }


    private void setupViewsFromStyles(Activity activity, View headerView) {
        final TypedArray styleAttrs = obtainStyledAttrsFromThemeAttr(activity,
                R.attr.pmHeaderStyle, R.styleable.PullMenuHeader);

        // Retrieve the Action Bar size from the app theme or the Action Bar's style
        if (mContentLayout != null) {
            final int height = styleAttrs.getDimensionPixelSize(
                    R.styleable.PullMenuHeader_pmHeaderHeight, getActionBarSize(activity));
            mContentLayout.getLayoutParams().height = height;
            mContentLayout.requestLayout();
        }

        // Retrieve the Action Bar background from the app theme or the Action Bar's style (see #93)
        Drawable bg = styleAttrs.hasValue(R.styleable.PullMenuHeader_pmHeaderBackground)
                ? styleAttrs.getDrawable(R.styleable.PullMenuHeader_pmHeaderBackground)
                : getActionBarBackground(activity);
        if (bg != null) {
            //mHeaderTextView.setBackgroundDrawable(bg);

            // If we have an opaque background we can remove the background from the content layout
            if (mContentLayout != null && bg.getOpacity() == PixelFormat.OPAQUE) {
                mContentLayout.setBackgroundResource(0);
            }
        }

        // Retrieve the Progress Bar Color the style
        if (styleAttrs.hasValue(R.styleable.PullMenuHeader_pmProgressBarColor)) {
            mProgressDrawableColor = styleAttrs.getColor(
                    R.styleable.PullMenuHeader_pmProgressBarColor, mProgressDrawableColor);
        }

        mProgressBarStyle = styleAttrs.getInt(
                R.styleable.PullMenuHeader_pmProgressBarStyle, PROGRESS_BAR_STYLE_OUTSIDE);

        if (styleAttrs.hasValue(R.styleable.PullMenuHeader_pmProgressBarHeight)) {
            mProgressBarHeight = styleAttrs.getDimensionPixelSize(
                    R.styleable.PullMenuHeader_pmProgressBarHeight, mProgressBarHeight);
        }

        //SmoothProgressBar Style
        if (styleAttrs.hasValue(R.styleable.PullMenuHeader_pmSmoothProgressBarStyle)) {
            int spbStyleRes = styleAttrs.getResourceId(R.styleable.PullMenuHeader_pmSmoothProgressBarStyle, 0);
            if (spbStyleRes != 0)
                mHeaderProgressBar.applyStyle(spbStyleRes);

        }

        styleAttrs.recycle();
    }

    private void applyProgressBarStyle() {
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, mProgressBarHeight);

        switch (mProgressBarStyle) {
            case PROGRESS_BAR_STYLE_INSIDE:
                lp.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.pm_content);
                break;
            case PROGRESS_BAR_STYLE_OUTSIDE:
                lp.addRule(RelativeLayout.BELOW, R.id.pm_content);
                break;
        }

        mHeaderProgressBar.setLayoutParams(lp);
    }

    private void applyProgressBarSettings() {
        if (mHeaderProgressBar != null) {
            ShapeDrawable shape = new ShapeDrawable();
            shape.setShape(new RectShape());
            shape.getPaint().setColor(mProgressDrawableColor);
            ClipDrawable clipDrawable = new ClipDrawable(shape, Gravity.CENTER, ClipDrawable.HORIZONTAL);

            mHeaderProgressBar.setProgressDrawable(clipDrawable);
        }
    }

    protected Drawable getActionBarBackground(Context context) {
        int[] android_styleable_ActionBar = {android.R.attr.background};

        // Now get the action bar style values...
        TypedArray abStyle = obtainStyledAttrsFromThemeAttr(context, android.R.attr.actionBarStyle,
                android_styleable_ActionBar);
        try {
            // background is the first attr in the array above so it's index is 0.
            return abStyle.getDrawable(0);
        } finally {
            abStyle.recycle();
        }
    }

    protected int getActionBarSize(Context context) {
        int[] attrs = {android.R.attr.actionBarSize};
        TypedArray values = context.getTheme().obtainStyledAttributes(attrs);
        try {
            return values.getDimensionPixelSize(0, 0);
        } finally {
            values.recycle();
        }
    }

    protected int getActionBarTitleStyle(Context context) {
        int[] android_styleable_ActionBar = {android.R.attr.titleTextStyle};

        // Now get the action bar style values...
        TypedArray abStyle = obtainStyledAttrsFromThemeAttr(context, android.R.attr.actionBarStyle,
                android_styleable_ActionBar);
        try {
            // titleTextStyle is the first attr in the array above so it's index is 0.
            return abStyle.getResourceId(0, 0);
        } finally {
            abStyle.recycle();
        }
    }

    protected int getMinimumApiLevel() {
        return Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    class HideAnimationCallback extends AnimatorListenerAdapter {
        @Override
        public void onAnimationEnd(Animator animation) {
            View headerView = getHeaderView();
            if (headerView != null) {
                headerView.setVisibility(View.GONE);
            }
            onReset();
        }
    }

    protected static TypedArray obtainStyledAttrsFromThemeAttr(Context context, int themeAttr,
                                                               int[] styleAttrs) {
        // Need to get resource id of style pointed to from the theme attr
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(themeAttr, outValue, true);
        final int styleResId = outValue.resourceId;

        // Now return the values (from styleAttrs) from the style
        return context.obtainStyledAttributes(styleResId, styleAttrs);
    }
}
