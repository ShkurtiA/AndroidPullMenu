package al.shkurti.pullmenu.library;

import java.util.ArrayList;

import al.shkurti.pullmenu.R;
import al.shkurti.pullmenu.library.listeners.HeaderViewListener;
import al.shkurti.pullmenu.library.viewdelegates.ViewDelegate;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * The main component of the library. You wrap the views you wish to be 'pullable' within this layout.
 * This layout is setup by using the {@link al.shkurti.pullmenu.library.ActionBarPullMenu} setup-wizard return by
 * @link ActionBarPullMenu#from(android.app.Activity)}.
 */
public class PullMenuLayout extends FrameLayout {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "PullMenuLayout";

    private PullMenuAttacher mPullToRefreshAttacher;

    public PullMenuLayout(Context context) {
        this(context, null);
    }

    public PullMenuLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PullMenuLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Manually set this Attacher's refreshing state. The header will be
     * displayed or hidden as requested.
     *
     * @param refreshing
     *            - Whether the attacher should be in a refreshing state,
     */
    public final void setRefreshing(boolean refreshing) {
        ensureAttacher();
        mPullToRefreshAttacher.setRefreshing(refreshing);
    }

    /**
     * @return true if this Attacher is currently in a refreshing state.
     */
    public final boolean isRefreshing() {
        ensureAttacher();
        return mPullToRefreshAttacher.isRefreshing();
    }

    /**
     * Call this when your refresh is complete and this view should reset itself
     * (header view will be hidden).
     *
     * This is the equivalent of calling <code>setRefreshing(false)</code>.
     */
    public final void setRefreshComplete() {
        ensureAttacher();
        mPullToRefreshAttacher.setRefreshComplete();
    }

    /**
     * Set a {@link al.shkurti.pullmenu.library.listeners.HeaderViewListener} which is called when the visibility
     * state of the Header View has changed.
     *
     * @param listener
     */
    public final void setHeaderViewListener(HeaderViewListener listener) {
        ensureAttacher();
        mPullToRefreshAttacher.setHeaderViewListener(listener);
    }

    /**
     * @return The Header View which is displayed when the user is pulling, or
     *         we are refreshing.
     */
    public final View getHeaderView() {
        ensureAttacher();
        return mPullToRefreshAttacher.getHeaderView();
    }

    /**
     * @return The HeaderTransformer currently used by this Attacher.
     */
    public HeaderTransformer getHeaderTransformer() {
        ensureAttacher();
        return mPullToRefreshAttacher.getHeaderTransformer();
    }


    @Override
    public final boolean onInterceptTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onInterceptTouchEvent. " + event.toString());
        }
        if (isEnabled() && mPullToRefreshAttacher != null && getChildCount() > 0) {
            return mPullToRefreshAttacher.onInterceptTouchEvent(event);
        }
        return false;
    }

    @Override
    public final boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onTouchEvent. " + event.toString());
        }
        if (isEnabled() && mPullToRefreshAttacher != null) {
            return mPullToRefreshAttacher.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onDetachedFromWindow() {
        // Destroy the PullMenuAttacher
        if (mPullToRefreshAttacher != null) {
            mPullToRefreshAttacher.destroy();
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (mPullToRefreshAttacher != null) {
            mPullToRefreshAttacher.onConfigurationChanged(newConfig);
        }
        super.onConfigurationChanged(newConfig);
    }

    void setPullMenuAttacher(PullMenuAttacher attacher) {
        if (mPullToRefreshAttacher != null) {
            mPullToRefreshAttacher.destroy();
        }
        mPullToRefreshAttacher = attacher;
    }

    void addAllChildrenAsPullable() {
        ensureAttacher();
        for (int i = 0, z = getChildCount(); i < z; i++) {
            addRefreshableView(getChildAt(i));
        }
    }

    void addChildrenAsPullable(int[] viewIds) {
        for (int i = 0, z = viewIds.length; i < z; i++) {
            View view = findViewById(viewIds[i]);
            if (view != null) {
                addRefreshableView(findViewById(viewIds[i]));
            }
        }
    }

    void addChildrenAsPullable(View[] views) {
        for (int i = 0, z = views.length; i < z; i++) {
            if (views[i] != null) {
                addRefreshableView(views[i]);
            }
        }
    }

    void addRefreshableView(View view) {
        if (mPullToRefreshAttacher != null) {
            mPullToRefreshAttacher.addRefreshableView(view, getViewDelegateFromLayoutParams(view));
        }
    }

    ViewDelegate getViewDelegateFromLayoutParams(View view) {
        if (view != null && view.getLayoutParams() instanceof LayoutParams) {
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            String clazzName = lp.getViewDelegateClassName();

            if (!TextUtils.isEmpty(clazzName)) {
                // Lets convert any relative class names (i.e. .XYZViewDelegate)
                final int firstDot = clazzName.indexOf('.');
                if (firstDot == -1) {
                    clazzName = getContext().getPackageName() + "." + clazzName;
                } else if (firstDot == 0) {
                    clazzName = getContext().getPackageName() + clazzName;
                }
                return InstanceCreationUtils.instantiateViewDelegate(getContext(), clazzName);
            }
        }
        return null;
    }

    protected PullMenuAttacher createPullToRefreshAttacher(Activity activity,
            Options options,int textColor,  int backgroundColor, int progresBarColor, ArrayList<String> mItems) {
        return new PullMenuAttacher(activity, options != null ? options : new Options(),
                textColor, backgroundColor, progresBarColor, mItems);
    }

    private void ensureAttacher() {
        if (mPullToRefreshAttacher == null) {
            throw new IllegalStateException("You need to setup the PullMenuLayout before using it");
        }
    }

    static class LayoutParams extends FrameLayout.LayoutParams {
        private final String mViewDelegateClassName;

        LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.PullMenuView);
            mViewDelegateClassName = a.getString(R.styleable.PullMenuView_pmViewDelegateClass);
            a.recycle();
        }

        String getViewDelegateClassName() {
            return mViewDelegateClassName;
        }
    }
}
