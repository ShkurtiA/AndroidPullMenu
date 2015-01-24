package al.shkurti.pullmenu.library;

import android.app.Activity;
import android.content.res.Configuration;
import android.view.View;

/**
 * HeaderTransformers are what controls and update the Header View to reflect the current state
 * of the pull-menu interaction. They are responsible for showing and hiding the header
 * view, as well as update the state.
 */
public abstract class HeaderTransformer {

    /**
     * Called whether the header view has been inflated from the resources
     * defined in {@link al.shkurti.pullmenu.library.Options#headerLayout}.
     *
     * @param activity The {@link android.app.Activity} that the header view is attached to.
     * @param headerView The inflated header view.
     */
    public void onViewCreated(Activity activity, View headerView, int progresBarColor) {}

    /**
     * Called when the header should be reset. You should update any child
     * views to reflect this.
     * <p/>
     * You should <strong>not</strong> change the visibility of the header
     * view.
     */
    public void onReset() {}

    /**
     * Called the user has pulled on the scrollable view.
     *
     * @param percentagePulled value between 0.0f and 1.0f depending on how far the
     *                         user has pulled.
     */
    public void onPulled(float percentagePulled) {}

    /**
     * Called when a refresh has begun. Theoretically this call is similar
     * to that provided from {@link al.shkurti.pullmenu.library.listeners.OnRefreshListener} but is more suitable
     * for header view updates.
     */
    public void onRefreshStarted() {}

    /**
     * Called when a refresh can be initiated when the user ends the touch
     * event. This is only called when {@link al.shkurti.pullmenu.library.Options#refreshOnUp} is set to
     * true.
     */
    public void onReleaseToRefresh() {}

    /**
     * Called when the current refresh has taken longer than the time
     * specified in {@link al.shkurti.pullmenu.library.Options#refreshMinimizeDelay}.
     */
    public void onRefreshMinimized() {}

    /**
     * Called when the Header View should be made visible, usually with an animation.
     *
     * @return true if the visibility has changed.
     */
    public abstract boolean showHeaderView();

    /**
     * Called when the Header View should be made invisible, usually with an animation.
     *
     * @return true if the visibility has changed.
     */
    public abstract boolean hideHeaderView();

    /**
     * Called when the Activity's configuration has changed.
     *
     * @param activity The {@link android.app.Activity} that the header view is attached to.
     * @param newConfig New configuration.
     *
     * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration)
     */
    public void onConfigurationChanged(Activity activity, Configuration newConfig) {}
    
    /**
     * Called when the pull is released and is selected a tab in menu
     * */
    public void onMenuSelected(){
    	
    }
}
