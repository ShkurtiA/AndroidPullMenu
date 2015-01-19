package al.shkurti.pullmenu.library.viewdelegates;

import android.view.View;
import android.webkit.WebView;


public class WebViewDelegate implements ViewDelegate {

    public static final Class[] SUPPORTED_VIEW_CLASSES =  { WebView.class };

    @Override
    public boolean isReadyForPull(View view, float x, float y) {
        return view.getScrollY() <= 0;
    }
}
