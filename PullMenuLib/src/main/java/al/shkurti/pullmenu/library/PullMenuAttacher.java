
package al.shkurti.pullmenu.library;

import java.util.ArrayList;
import java.util.WeakHashMap;

import al.shkurti.pullmenu.R;
import al.shkurti.pullmenu.library.listeners.HeaderViewListener;
import al.shkurti.pullmenu.library.listeners.OnRefreshListener;
import al.shkurti.pullmenu.library.slidingtabstrip.MenuSlidingTabStrip;
import al.shkurti.pullmenu.library.viewdelegates.ViewDelegate;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;

public class PullMenuAttacher {

    private static final boolean DEBUG = false;
    private static final String LOG_TAG = "PullMenuAttacher";

    /* Member Variables */

    private EnvironmentDelegate mEnvironmentDelegate;
    private HeaderTransformer mHeaderTransformer;

    private OnRefreshListener mOnRefreshListener;

    private Activity mActivity;
    private View mHeaderView;
    private HeaderViewListener mHeaderViewListener;

    private final int mTouchSlop;
    private final float mRefreshScrollDistance;

    private float mInitialMotionY, mLastMotionY, mPullBeginY;
    private float mInitialMotionX;
    private boolean mIsBeingDragged, mIsRefreshing, mHandlingTouchEventFromDown;
    private View mViewBeingDragged;

    private final WeakHashMap<View, ViewDelegate> mRefreshableViews;

    private final boolean mRefreshOnUp;
    private final int mRefreshMinimizeDelay;
    private final boolean mRefreshMinimize;
    private boolean mIsDestroyed = false;

    private final int[] mViewLocationResult = new int[2];
    private final Rect mRect = new Rect();

    private final AddHeaderViewRunnable mAddHeaderViewRunnable;
    
    MenuSlidingTabStrip mMenuSlidingTabStrip;
    boolean pullStarted,pullEnded;

    protected PullMenuAttacher(Activity activity, Options options,int color, ArrayList<String> mItems) {
        if (activity == null) {
            throw new IllegalArgumentException("activity cannot be null");
        }
        if (options == null) {
            Log.i(LOG_TAG, "Given null options so using default options.");
            options = new Options();
        }

        mActivity = activity;
        mRefreshableViews = new WeakHashMap<View, ViewDelegate>();

        // Copy necessary values from options
        mRefreshScrollDistance = options.refreshScrollDistance;
        mRefreshOnUp = options.refreshOnUp;
        mRefreshMinimizeDelay = options.refreshMinimizeDelay;
        mRefreshMinimize = options.refreshMinimize;

        // EnvironmentDelegate
        mEnvironmentDelegate = options.environmentDelegate != null
                ? options.environmentDelegate
                : createDefaultEnvironmentDelegate();

        // Header Transformer
        mHeaderTransformer = options.headerTransformer != null
                ? options.headerTransformer
                : createDefaultHeaderTransformer();

        // Get touch slop for use later
        mTouchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();

        // Get Window Decor View
        final ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();

        // Create Header view and then add to Decor View
        mHeaderView = LayoutInflater.from(mEnvironmentDelegate.getContextForInflater(activity)).inflate(
                options.headerLayout, decorView, false);
        if (mHeaderView == null) {
            throw new IllegalArgumentException("Must supply valid layout id for header.");
        }

        //costumize menu tab strip
        mMenuSlidingTabStrip = (MenuSlidingTabStrip) mHeaderView.findViewById(R.id.menuIndicator);

        mMenuSlidingTabStrip.setTextColorResource(color);

        // set the array for menu items
        mMenuSlidingTabStrip.setArray(mItems);
        
        // Make Header View invisible so it still gets a layout pass
        mHeaderView.setVisibility(View.INVISIBLE);

        // Notify transformer
        mHeaderTransformer.onViewCreated(activity, mHeaderView);

        // Now HeaderView to Activity
        mAddHeaderViewRunnable = new AddHeaderViewRunnable();
        mAddHeaderViewRunnable.start();
    }

    /**
     * Add a view which will be used to initiate refresh requests.
     *
     * @param view View which will be used to initiate refresh requests.
     */
    void addRefreshableView(View view, ViewDelegate viewDelegate) {
        if (isDestroyed()) return;

        // Check to see if view is null
        if (view == null) {
            Log.i(LOG_TAG, "Refreshable View is null.");
            return;
        }

        // ViewDelegate
        if (viewDelegate == null) {
            viewDelegate = InstanceCreationUtils.getBuiltInViewDelegate(view);
        }

        // View to detect refreshes for
        mRefreshableViews.put(view, viewDelegate);
    }

    void useViewDelegate(Class<?> viewClass, ViewDelegate delegate) {
        for (View view : mRefreshableViews.keySet()) {
            if (viewClass.isInstance(view)) {
                mRefreshableViews.put(view, delegate);
            }
        }
    }

    /**
     * Clear all views which were previously used to initiate refresh requests.
     */
    void clearRefreshableViews() {
        mRefreshableViews.clear();
    }

    /**
     * This method should be called by your Activity's or Fragment's
     * onConfigurationChanged method.
     *
     * @param newConfig The new configuration
     */
    public void onConfigurationChanged(Configuration newConfig) {
        mHeaderTransformer.onConfigurationChanged(mActivity, newConfig);
    }

    /**
     * Manually set this Attacher's refreshing state. The header will be
     * displayed or hidden as requested.
     *
     * @param refreshing
     *            - Whether the attacher should be in a refreshing state,
     */
    final void setRefreshing(boolean refreshing) {
        setRefreshingInt(null, refreshing, false);
    }

    /**
     * @return true if this Attacher is currently in a refreshing state.
     */
    final boolean isRefreshing() {
        return mIsRefreshing;
    }

    /**
     * Call this when your refresh is complete and this view should reset itself
     * (header view will be hidden).
     *
     * This is the equivalent of calling <code>setRefreshing(false)</code>.
     */
    final void setRefreshComplete() {
        setRefreshingInt(null, false, false);
    }

    /**
     * Set the Listener to be called when a refresh is initiated.
     */
    void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    void destroy() {
        if (mIsDestroyed) return; // We've already been destroyed

        // Remove the Header View from the Activity
        removeHeaderViewFromActivity(mHeaderView);

        // Lets clear out all of our internal state
        clearRefreshableViews();

        mActivity = null;
        mHeaderView = null;
        mHeaderViewListener = null;
        mEnvironmentDelegate = null;
        mHeaderTransformer = null;

        mIsDestroyed = true;
    }

    /**
     * Set a {@link al.shkurti.pullmenu.library.listeners.HeaderViewListener} which is called when the visibility
     * state of the Header View has changed.
     */
    final void setHeaderViewListener(HeaderViewListener listener) {
        mHeaderViewListener = listener;
    }

    /**
     * @return The Header View which is displayed when the user is pulling, or
     *         we are refreshing.
     */
    final View getHeaderView() {
        return mHeaderView;
    }

    /**
     * @return The HeaderTransformer currently used by this Attacher.
     */
    HeaderTransformer getHeaderTransformer() {
        return mHeaderTransformer;
    }

    final boolean onInterceptTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onInterceptTouchEvent: " + event.toString());
        }

        // If we're not enabled or currently refreshing don't handle any touch
        // events
        if (isRefreshing()) {
            return false;
        }

        final float x = event.getX(), y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
            	//Log.e("MotionEvent.ACTION_MOVE ", " ok ");
                // We're not currently being dragged so check to see if the user has
                // scrolled enough
                if (!mIsBeingDragged && mInitialMotionY > 0f) {
                    final float yDiff = y - mInitialMotionY;
                    final float xDiff = x - mInitialMotionX;

                    if (Math.abs(yDiff) > Math.abs(xDiff) && yDiff > mTouchSlop) {
                        mIsBeingDragged = true;
                        onPullStarted(y);
                    } else if (yDiff < -mTouchSlop) {
                        resetTouch();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                // If we're already refreshing, ignore
                if (canRefresh(true)) {
                    for (View view : mRefreshableViews.keySet()) {
                        if (isViewBeingDragged(view, event)) {
                            mInitialMotionX = x;
                            mInitialMotionY = y;
                            mViewBeingDragged = view;
                        }
                    }
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                resetTouch();
                break;
            }
        }

        if (DEBUG) Log.d(LOG_TAG, "onInterceptTouchEvent. Returning " + mIsBeingDragged);

        return mIsBeingDragged;
    }

    final boolean isViewBeingDragged(View view, MotionEvent event) {
        if (view.isShown() && mRefreshableViews.containsKey(view)) {
            // First we need to set the rect to the view's screen co-ordinates
            view.getLocationOnScreen(mViewLocationResult);
            final int viewLeft = mViewLocationResult[0], viewTop = mViewLocationResult[1];
            mRect.set(viewLeft, viewTop, viewLeft + view.getWidth(), viewTop + view.getHeight());

            if (DEBUG) Log.d(LOG_TAG, "isViewBeingDragged. View Rect: " + mRect.toString());

            final int rawX = (int) event.getRawX(), rawY = (int) event.getRawY();
            if (mRect.contains(rawX, rawY)) {
                // The Touch Event is within the View's display Rect
                ViewDelegate delegate = mRefreshableViews.get(view);
                if (delegate != null) {
                    // Now call the delegate, converting the X/Y into the View's co-ordinate system
                    return delegate.isReadyForPull(view, rawX - mRect.left, rawY - mRect.top);
                }
            }
        }
        return false;
    }

    final boolean onTouchEvent(MotionEvent event) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onTouchEvent: " + event.toString());
        }

        // Record whether our handling is started from ACTION_DOWN
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mHandlingTouchEventFromDown = true;
        }

        // If we're being called from ACTION_DOWN then we must call through to
        // onInterceptTouchEvent until it sets mIsBeingDragged
        if (mHandlingTouchEventFromDown && !mIsBeingDragged) {
            onInterceptTouchEvent(event);
            return true;
        }

        if (mViewBeingDragged == null) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE: {
                // If we're already refreshing ignore it
                if (isRefreshing()) {
                    return false;
                }

                final float y = event.getY();

                if (mIsBeingDragged && y != mLastMotionY) {

                	/**
                	 * Check if teh user scrolls up more than he is allowed, 
                	 * this depends from the start point, error case:  when he scrolls 
                	 * above the starting point the its detected as he is scrolling down,
                	 * this part of code fixes that
                	 * */
                	if(mInitialMotionY>y){
                		break;
                	}
                    onPull(mViewBeingDragged, y);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                checkScrollForRefresh(mViewBeingDragged);
                if (mIsBeingDragged) {
                	if(triggerMenuAction()){

                    }else{
                        onPullEnded();
                    }
                }
                resetTouch();
                break;
            }
        }

        return true;
    }

    void minimizeHeader() {
        if (isDestroyed()) return;

        mHeaderTransformer.onRefreshMinimized();

        if (mHeaderViewListener != null) {
            mHeaderViewListener.onStateChanged(mHeaderView, HeaderViewListener.STATE_MINIMIZED);
        }
    }

    void resetTouch() {
        mIsBeingDragged = false;
        mHandlingTouchEventFromDown = false;
        mInitialMotionY = mLastMotionY = mPullBeginY = -1f;
    }

    void onPullStarted(float y) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onPullStarted");
        }
        showHeaderView();
        mPullBeginY = y;
    }

    void onPull(View view, float y) {
        if (DEBUG) {
            Log.d(LOG_TAG, "onPull");
        }

        final float pxScrollForRefresh = getScrollNeededForRefresh(view);
        final float scrollLength = y - mPullBeginY;

        if (scrollLength < pxScrollForRefresh) {
            mHeaderTransformer.onPulled(scrollLength / pxScrollForRefresh);
        } else {
            if (mRefreshOnUp) {
                mHeaderTransformer.onReleaseToRefresh();
            } else {
                setRefreshingInt(view, true, true);
            }
        }
    }

    void onPullEnded() {
        if (DEBUG) {
            Log.d(LOG_TAG, "onPullEnded");
        }
        if (!mIsRefreshing) {
            reset(true);
        }
    }

    void showHeaderView() {
        updateHeaderViewPosition(mHeaderView);
        if (mHeaderTransformer.showHeaderView()) {
            if (mHeaderViewListener != null) {
                mHeaderViewListener.onStateChanged(mHeaderView,
                        HeaderViewListener.STATE_VISIBLE);
            }
        }
    }

    void hideHeaderView() {
        if (mHeaderTransformer.hideHeaderView()) {
            if (mHeaderViewListener != null) {
                mHeaderViewListener.onStateChanged(mHeaderView,
                        HeaderViewListener.STATE_HIDDEN);
            }
        }
    }
    
    private boolean checkIfMenuActionAvailable(){
    	if(!mIsRefreshing && mIsBeingDragged && 
    			DefaultHeaderTransformer.scrollPercantage > DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE && 
    			DefaultHeaderTransformer.scrollPercantage < 100){
    		return true;
    	}
    	return false;
    }
    
    /**
     * */
    private boolean triggerMenuAction(){
    	if(checkIfMenuActionAvailable()){
    		if (mOnRefreshListener != null) {
                if (isDestroyed() || mIsRefreshing) return false;

                resetTouch();

                if (canRefresh(true)) {
                     // Update isRefreshing state
                     mIsRefreshing = true;

                    mOnRefreshListener.onRefreshStarted(null, mMenuSlidingTabStrip.getSelectedPosition(),
                            mMenuSlidingTabStrip.getSelectedField());
                    mMenuSlidingTabStrip.reorderArray();

                    // Call Transformer
                    mHeaderTransformer.onRefreshStarted();

                    // Show Header View
                    showHeaderView();

                    // Post a runnable to minimize the refresh header
                    if (mRefreshMinimize) {
                        if (mRefreshMinimizeDelay > 0) {
                            getHeaderView().postDelayed(mRefreshMinimizeRunnable, mRefreshMinimizeDelay);
                        } else {
                            getHeaderView().post(mRefreshMinimizeRunnable);
                        }
                    }
                    return true;
                } else {
                    reset(true);
                    return false;
                }



    		}else return false;

    	}else return false;
    }

    protected final Activity getAttachedActivity() {
        return mActivity;
    }

    protected EnvironmentDelegate createDefaultEnvironmentDelegate() {
        return new EnvironmentDelegate() {
            @Override
            public Context getContextForInflater(Activity activity) {
                Context context = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    ActionBar ab = activity.getActionBar();
                    if (ab != null) {
                        context = ab.getThemedContext();
                    }
                }
                if (context == null) {
                    context = activity;
                }
                return context;
            }
        };
    }

    protected HeaderTransformer createDefaultHeaderTransformer() {
        return new DefaultHeaderTransformer();
    }

    private boolean checkScrollForRefresh(View view) {
        if (mIsBeingDragged && mRefreshOnUp && view != null) {
            if (mLastMotionY - mPullBeginY >= getScrollNeededForRefresh(view)) {
                setRefreshingInt(view, true, true);
                return true;
            }
        }
        return false;
    }

    private void setRefreshingInt(View view, boolean refreshing, boolean fromTouch) {
        if (isDestroyed()) return;

        if (DEBUG) Log.d(LOG_TAG, "setRefreshingInt: " + refreshing);
        // Check to see if we need to do anything
        if (mIsRefreshing == refreshing) {
            return;
        }

        resetTouch();

        if (refreshing && canRefresh(fromTouch)) {
            startRefresh(view, fromTouch);
        } else {
            reset(fromTouch);
        }
    }

    /**
     * @param fromTouch Whether this is being invoked from a touch event
     * @return true if we're currently in a state where a refresh can be
     *         started.
     */
    private boolean canRefresh(boolean fromTouch) {
        return !mIsRefreshing && (!fromTouch || mOnRefreshListener != null);
    }

    private float getScrollNeededForRefresh(View view) {
        return view.getHeight() * mRefreshScrollDistance;
    }

    private void reset(boolean fromTouch) {
        // Update isRefreshing state
        mIsRefreshing = false;

        // Remove any minimize callbacks
        if (mRefreshMinimize) {
            getHeaderView().removeCallbacks(mRefreshMinimizeRunnable);
        }

        // Hide Header View
        hideHeaderView();
    }

    private void startRefresh(View view, boolean fromTouch) {
        // Update isRefreshing state
        mIsRefreshing = true;

        // Call OnRefreshListener if this call has originated from a touch event
        if (fromTouch) {
            if (mOnRefreshListener != null) {
                mOnRefreshListener.onRefreshStarted(view,-1,null);
            }
        }

        // Call Transformer
        mHeaderTransformer.onRefreshStarted();

        // Show Header View
        showHeaderView();

        // Post a runnable to minimize the refresh header
        if (mRefreshMinimize) {
            if (mRefreshMinimizeDelay > 0) {
                getHeaderView().postDelayed(mRefreshMinimizeRunnable, mRefreshMinimizeDelay);
            } else {
                getHeaderView().post(mRefreshMinimizeRunnable);
            }
        }
    }

    private boolean isDestroyed() {
        if (mIsDestroyed) {
            Log.i(LOG_TAG, "PullMenuAttacher is destroyed.");
        }
        return mIsDestroyed;
    }

    protected void addHeaderViewToActivity(View headerView) {
        // Get the Display Rect of the Decor View
        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(mRect);

        // Honour the requested layout params
        int width = WindowManager.LayoutParams.MATCH_PARENT;
        int height = WindowManager.LayoutParams.WRAP_CONTENT;
        ViewGroup.LayoutParams requestedLp = headerView.getLayoutParams();
        if (requestedLp != null) {
            width = requestedLp.width;
            height = requestedLp.height;
        }

        // Create LayoutParams for adding the View as a panel
        WindowManager.LayoutParams wlp = new WindowManager.LayoutParams(width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        wlp.x = 0;
        wlp.y = mRect.top;
        wlp.gravity = Gravity.TOP;

        headerView.setTag(wlp);
        mActivity.getWindowManager().addView(headerView, wlp);
    }

    protected void updateHeaderViewPosition(View headerView) {
        // Refresh the Display Rect of the Decor View
        mActivity.getWindow().getDecorView().getWindowVisibleDisplayFrame(mRect);

        WindowManager.LayoutParams wlp = null;
        if (headerView.getLayoutParams() instanceof WindowManager.LayoutParams) {
            wlp = (WindowManager.LayoutParams) headerView.getLayoutParams();
        } else if (headerView.getTag() instanceof  WindowManager.LayoutParams) {
            wlp = (WindowManager.LayoutParams) headerView.getTag();
        }

        if (wlp != null && wlp.y != mRect.top) {
            wlp.y = mRect.top;
            mActivity.getWindowManager().updateViewLayout(headerView, wlp);
        }
    }

    protected void removeHeaderViewFromActivity(View headerView) {
        mAddHeaderViewRunnable.finish();

        if (headerView.getWindowToken() != null) {
            mActivity.getWindowManager().removeViewImmediate(headerView);
        }
    }

    private final Runnable mRefreshMinimizeRunnable = new Runnable() {
        @Override
        public void run() {
            minimizeHeader();
        }
    };

    private class AddHeaderViewRunnable implements Runnable {
        @Override
        public void run() {
            if (isDestroyed()) return;

            if (getDecorView().getWindowToken() != null) {
                // The Decor View has a Window Token, so we can add the HeaderView!
                addHeaderViewToActivity(mHeaderView);
            } else {
                // The Decor View doesn't have a Window Token yet, post ourselves again...
                start();
            }
        }

        public void start() {
            getDecorView().post(this);
        }

        public void finish() {
            getDecorView().removeCallbacks(this);
        }

        private View getDecorView() {
            return getAttachedActivity().getWindow().getDecorView();
        }
    }
}
