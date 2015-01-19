package al.shkurti.pullmenu.library.viewdelegates;

import android.view.View;
import android.widget.ScrollView;

public class ScrollYDelegate implements ViewDelegate {

    public static final Class[] SUPPORTED_VIEW_CLASSES =  { ScrollView.class };

    @Override
    public boolean isReadyForPull(View view, float x, float y) {
        return view.getScrollY() <= 0;
    }
}
