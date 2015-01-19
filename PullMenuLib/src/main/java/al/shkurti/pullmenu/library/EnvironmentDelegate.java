package al.shkurti.pullmenu.library;

import android.app.Activity;
import android.content.Context;

/**
 * This is used to provide platform and environment specific functionality for the Attacher.
 */
public interface EnvironmentDelegate {

    /**
     * @return Context which should be used for inflating the header layout
     */
    public Context getContextForInflater(Activity activity);

}
