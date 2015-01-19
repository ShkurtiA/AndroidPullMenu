/*
 * Copyright (C) 2013 Andreas Stuetz <andreas.stuetz@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Taken from this repository: https://github.com/jpardogo/PagerSlidingTabStrip
 * Armando Shkurti
 */

package al.shkurti.pullmenu.library.slidingtabstrip;

import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import al.shkurti.pullmenu.R;
import al.shkurti.pullmenu.library.DefaultHeaderTransformer;


public class MenuSlidingTabStrip extends HorizontalScrollView {

    private static final float OPAQUE = 1.0f;
    private static final float HALF_TRANSP = 0.5f;

    public interface CustomTabProvider {
        public View getCustomTabView(ViewGroup parent, int position);
    }

    public interface OnTabReselectedListener {
        public void onTabReselected(int position);
    }

    // @formatter:off
    private static final int[] ATTRS = new int[]{
            android.R.attr.textSize,
            android.R.attr.textColor,
            android.R.attr.paddingLeft,
            android.R.attr.paddingRight,
            android.R.attr.textColorPrimary,
    };
    // @formatter:on

    //private final PagerAdapterObserver adapterObserver = new PagerAdapterObserver();

    //These indexes must be related with the ATTR array above
    private static final int TEXT_SIZE_INDEX = 0;
    private static final int TEXT_COLOR_INDEX = 1;
    private static final int PADDING_LEFT_INDEX = 2;
    private static final int PADDING_RIGHT_INDEX = 3;
    private static final int TEXT_COLOR_PRIMARY = 4;

    private LinearLayout.LayoutParams defaultTabLayoutParams;
    private LinearLayout.LayoutParams expandedTabLayoutParams;

    //private final PageListener pageListener = new PageListener();
    //private OnTabReselectedListener tabReselectedListener = null;
    public OnPageChangeListener delegatePageListener;

    private LinearLayout tabsContainer;
    //private ViewPager pager;

    private int tabCount;

    private int currentPosition = 0;
    private float currentPositionOffset = 0f;

    private Paint rectPaint;
    private Paint dividerPaint;

    private int indicatorColor;
    private int indicatorHeight = 2;

    private int underlineHeight = 0;
    private int underlineColor;

    private int dividerWidth = 0;
    private int dividerPadding = 0;
    private int dividerColor;

    private int tabPadding = 12;
    private int tabTextSize = 14;
    private ColorStateList tabTextColor = null;
    private float tabTextAlpha = HALF_TRANSP;
    private float tabTextSelectedAlpha = OPAQUE;

    private int padding = 0;

    private boolean shouldExpand = false;
    private boolean textAllCaps = true;
    private boolean isPaddingMiddle = false;

    private Typeface tabTypeface = null;
    private int tabTypefaceStyle = Typeface.BOLD;
    private int tabTypefaceSelectedStyle = Typeface.BOLD;

    private int scrollOffset;
    private int lastScrollX = 0;

    private int tabBackgroundResId = R.drawable.background_tab;

    private Locale locale;
    
    private ArrayList<String> mMenuArray;

    public MenuSlidingTabStrip(Context context) {
        this(context, null);
    }

    public MenuSlidingTabStrip(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MenuSlidingTabStrip(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setFillViewport(true);
        setWillNotDraw(false);
        tabsContainer = new LinearLayout(context);
        tabsContainer.setOrientation(LinearLayout.HORIZONTAL);
        tabsContainer.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        addView(tabsContainer);

        DisplayMetrics dm = getResources().getDisplayMetrics();
        scrollOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, scrollOffset, dm);
        indicatorHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indicatorHeight, dm);
        underlineHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, underlineHeight, dm);
        dividerPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerPadding, dm);
        tabPadding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, tabPadding, dm);
        dividerWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dividerWidth, dm);
        tabTextSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, tabTextSize, dm);

        // get system attrs (android:textSize and android:textColor)
        TypedArray a = context.obtainStyledAttributes(attrs, ATTRS);
        tabTextSize = a.getDimensionPixelSize(TEXT_SIZE_INDEX, tabTextSize);
        ColorStateList colorStateList = a.getColorStateList(TEXT_COLOR_INDEX);
        int textPrimaryColor = a.getColor(TEXT_COLOR_PRIMARY, android.R.color.white);
        if (colorStateList != null) {
            tabTextColor = colorStateList;
        } else {
            tabTextColor = getColorStateList(textPrimaryColor);
        }

        underlineColor = textPrimaryColor;
        dividerColor = textPrimaryColor;
        indicatorColor = textPrimaryColor;
        int paddingLeft = a.getDimensionPixelSize(PADDING_LEFT_INDEX, padding);
        int paddingRight = a.getDimensionPixelSize(PADDING_RIGHT_INDEX, padding);
        a.recycle();

        //In case we have the padding they must be equal so we take the biggest
        if (paddingRight < paddingLeft) {
            padding = paddingLeft;
        }

        if (paddingLeft < paddingRight) {
            padding = paddingRight;
        }

        // get custom attrs
        a = context.obtainStyledAttributes(attrs, R.styleable.MenuSlidingTabStrip);
        indicatorColor = a.getColor(R.styleable.MenuSlidingTabStrip_mstsIndicatorColor, indicatorColor);
        underlineColor = a.getColor(R.styleable.MenuSlidingTabStrip_mstsUnderlineColor, underlineColor);
        dividerColor = a.getColor(R.styleable.MenuSlidingTabStrip_mstsDividerColor, dividerColor);
        dividerWidth = a.getDimensionPixelSize(R.styleable.MenuSlidingTabStrip_mstsDividerWidth, dividerWidth);
        indicatorHeight = a.getDimensionPixelSize(R.styleable.MenuSlidingTabStrip_mstsIndicatorHeight, indicatorHeight);
        underlineHeight = a.getDimensionPixelSize(R.styleable.MenuSlidingTabStrip_mstsUnderlineHeight, underlineHeight);
        dividerPadding = a.getDimensionPixelSize(R.styleable.MenuSlidingTabStrip_mstsDividerPadding, dividerPadding);
        tabPadding = a.getDimensionPixelSize(R.styleable.MenuSlidingTabStrip_mstsTabPaddingLeftRight, tabPadding);
        tabBackgroundResId = a.getResourceId(R.styleable.MenuSlidingTabStrip_mstsTabBackground, tabBackgroundResId);
        shouldExpand = a.getBoolean(R.styleable.MenuSlidingTabStrip_mstsShouldExpand, shouldExpand);
        scrollOffset = a.getDimensionPixelSize(R.styleable.MenuSlidingTabStrip_mstsScrollOffset, scrollOffset);
        textAllCaps = a.getBoolean(R.styleable.MenuSlidingTabStrip_mstsTextAllCaps, textAllCaps);
        isPaddingMiddle = a.getBoolean(R.styleable.MenuSlidingTabStrip_mstsPaddingMiddle, isPaddingMiddle);
        tabTypefaceStyle = a.getInt(R.styleable.MenuSlidingTabStrip_mstsTextStyle, Typeface.BOLD);
        tabTypefaceSelectedStyle = a.getInt(R.styleable.MenuSlidingTabStrip_mstsTextSelectedStyle, Typeface.BOLD);
        tabTextAlpha = a.getFloat(R.styleable.MenuSlidingTabStrip_mstsTextAlpha, HALF_TRANSP);
        tabTextSelectedAlpha = a.getFloat(R.styleable.MenuSlidingTabStrip_mstsTextSelectedAlpha, OPAQUE);
        a.recycle();

        setMarginBottomTabContainer();

        rectPaint = new Paint();
        rectPaint.setAntiAlias(true);
        rectPaint.setStyle(Style.FILL);


        dividerPaint = new Paint();
        dividerPaint.setAntiAlias(true);
        dividerPaint.setStrokeWidth(dividerWidth);

        defaultTabLayoutParams = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        expandedTabLayoutParams = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f);

        if (locale == null) {
            locale = getResources().getConfiguration().locale;
        }
    }

    private void setMarginBottomTabContainer() {
        MarginLayoutParams mlp = (MarginLayoutParams) tabsContainer.getLayoutParams();
        int bottomMargin = indicatorHeight >= underlineHeight ? indicatorHeight : underlineHeight;
        mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, bottomMargin);
        tabsContainer.setLayoutParams(mlp);
    }

  	
  	public void setArray(ArrayList<String> mList){
  		this.mMenuArray = mList;
  		notifyDataSetChanged();// notifies view for the changes
  	}

  	/**
  	 * This method is called from 
  	 * DefaultHeaderTransformer class from onPulled(percentagePulled) method
  	 * @param position takes value from 0 to 100
  	 * */
    public void changeMenuIndicatorPosition(int position){
    	
		if (DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE < position && position < 100) {
			switch (tabCount) {
			case 2:
				setScrollForTwo(position);
				break;
			case 3:
				setScrollForThree(position);
				break;
			case 4:
				setScrollForFour(position);
				break;
			case 5:
				setScrollForFive(position);
				break;
			case 6:
				setScrollForSix(position);
				break;

			default:
				break;
			}
		}
    	
    }
    
    /**
     * Scroll available when there are six items in pull menu
     * 
     * @param position 
     */
    private void setScrollForSix(int position) {
    	if(DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE<position && position<19){
    		setScrollTo(0);
    	}else if (19<=position && position < 34){
    		setScrollTo(1);
    	}else if (34<=position && position < 50){
    		setScrollTo(2);
    	}else if (50<=position && position < 66){
    		setScrollTo(3);
    	}else if (66<=position && position < 82){
    		setScrollTo(4);
    	}else if (82<=position && position < 100){
    		setScrollTo(5);
    	}
	}

	/**
     * Scroll available when there are five items in pull menu
     * 
     * @param position 
     */
    private void setScrollForFive(int position) {
    	if(DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE<position && position<22){
    		setScrollTo(0);
    	}else if (22<=position && position < 42){
    		setScrollTo(1);
    	}else if (42<=position && position < 62){
    		setScrollTo(2);
    	}else if (62<=position && position < 82){
    		setScrollTo(3);
    	}else if (82<=position && position < 100){
    		setScrollTo(4);
    	}
		
	}

	/**
     * Scroll available when there are four items in pull menu
     * 
     * @param position 
     */
    private void setScrollForFour(int position) {
    	if(DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE<position && position<27){
    		setScrollTo(0);
    	}else if (27<=position && position < 52){
    		setScrollTo(1);
    	}else if (52<=position && position < 77){
    		setScrollTo(2);
    	}else if (77<=position && position < 100){
    		setScrollTo(3);
    	}
		
	}

	/**
     * Scroll available when there are three items in pull menu
     * 
     * @param position 
     */
    private void setScrollForThree(int position) {
    	if(DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE<position && position<35){
    		setScrollTo(0);
    	}else if (35<=position && position < 68){
    		setScrollTo(1);
    	}else if (68<=position && position < 100){
    		setScrollTo(2);
    	}
	}

	/**
     * Scroll available when there are two items in pull menu
     * 
     * @param position 
     */
  	private void setScrollForTwo(int position) {
		
  		if(DefaultHeaderTransformer.MENU_INDICATOR_MIN_VALUE<position && position<50){
    		setScrollTo(0);
    	}else if (50<=position && position < 100){
    		setScrollTo(1);
    	}
	}

	public void setScrollTo(int position){
  		currentPosition = position;
        currentPositionOffset = 0;
        int offset = tabCount > 0 ? (int) (0 * tabsContainer.getChildAt(position).getWidth()) : 0;
        scrollToChild(position, offset);
        invalidate();
  		
        //Full alpha for current item
        View currentTab = tabsContainer.getChildAt(currentPosition);
        selected(currentTab);
        /*//Half transparent for prev item
        if (currentPosition - 1 >= 0) {
            View prevTab = tabsContainer.getChildAt(currentPosition - 1);
            notSelected(prevTab);
        }
        //Half transparent for next item
        if (currentPosition + 1 <= mMenuArray.size() - 1) {
            View nextTab = tabsContainer.getChildAt(currentPosition + 1);
            notSelected(nextTab);
        }*/
        
        for (int i = 0; i < tabCount; i++) {
        	if(!(i==currentPosition)){
        		View tab = tabsContainer.getChildAt(i);
                notSelected(tab);
        	}
        }
  	
  	}

    public void notifyDataSetChanged() {
        tabsContainer.removeAllViews();
        tabCount = mMenuArray.size();//pager.getAdapter().getCount();
        View tabView;
        for (int i = 0; i < tabCount; i++) {

           /* if (pager.getAdapter() instanceof CustomTabProvider) {
                tabView = ((CustomTabProvider) pager.getAdapter()).getCustomTabView(this, i);
            } else {*/
                tabView = LayoutInflater.from(getContext()).inflate(R.layout.tab, this, false);
            //}

            CharSequence title = mMenuArray.get(i);//pager.getAdapter().getPageTitle(i);

            addTab(i, title, tabView);
        }

        updateTabStyles();
        getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @SuppressWarnings("deprecation")
            @SuppressLint("NewApi")
            @Override
            public void onGlobalLayout() {

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
 
              //TODO we should receive the position of view that we are
              // and  the actions that it has
                currentPosition =  0;//pager.getCurrentItem();
                currentPositionOffset = 0f;
                scrollToChild(currentPosition, 0);
                updateSelection(currentPosition);
            }
        });
    }

    private void addTab(final int position, CharSequence title, View tabView) {
        TextView textView = (TextView) tabView.findViewById(R.id.tab_title);
        if (textView != null) {
            if (title != null) textView.setText(title);
            float alpha = currentPosition/*pager.getCurrentItem()*/ == position ? tabTextSelectedAlpha : tabTextAlpha;
            ViewCompat.setAlpha(textView, alpha);
        }

        tabsContainer.addView(tabView, position, shouldExpand ? expandedTabLayoutParams : defaultTabLayoutParams);
    }

    private void updateTabStyles() {
        for (int i = 0; i < tabCount; i++) {
            View v = tabsContainer.getChildAt(i);
            v.setBackgroundResource(tabBackgroundResId);
            v.setPadding(tabPadding, v.getPaddingTop(), tabPadding, v.getPaddingBottom());
            TextView tab_title = (TextView) v.findViewById(R.id.tab_title);

            if (tab_title != null) {
                tab_title.setTextSize(TypedValue.COMPLEX_UNIT_PX, tabTextSize);
                tab_title.setTypeface(tabTypeface, currentPosition == i ? tabTypefaceSelectedStyle : tabTypefaceStyle);
                if (tabTextColor != null) {
                    tab_title.setTextColor(tabTextColor);
                }
                // setAllCaps() is only available from API 14, so the upper case is made manually if we are on a
                // pre-ICS-build
                if (textAllCaps) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        tab_title.setAllCaps(true);
                    } else {
                        tab_title.setText(tab_title.getText().toString().toUpperCase(locale));
                    }
                }
            }
        }
    }

    private void scrollToChild(int position, int offset) {
        if (tabCount == 0) {
            return;
        }

        int newScrollX = tabsContainer.getChildAt(position).getLeft() + offset;
        if (position > 0 || offset > 0) {

            //Half screen offset.
            //- Either tabs start at the middle of the view scrolling straight away
            //- Or tabs start at the begging (no padding) scrolling when indicator gets
            //  to the middle of the view width
            newScrollX -= scrollOffset;
            Pair<Float, Float> lines = getIndicatorCoordinates();
            newScrollX += ((lines.second - lines.first) / 2);
        }

        if (newScrollX != lastScrollX) {
            lastScrollX = newScrollX;
            smoothScrollTo(newScrollX, 0);
        }
    }

    private Pair<Float, Float> getIndicatorCoordinates() {
        // default: line below current tab
        View currentTab = tabsContainer.getChildAt(currentPosition);
        float lineLeft = currentTab.getLeft();
        float lineRight = currentTab.getRight();

        // if there is an offset, start interpolating left and right coordinates between current and next tab
        if (currentPositionOffset > 0f && currentPosition < tabCount - 1) {

            View nextTab = tabsContainer.getChildAt(currentPosition + 1);
            final float nextTabLeft = nextTab.getLeft();
            final float nextTabRight = nextTab.getRight();

            lineLeft = (currentPositionOffset * nextTabLeft + (1f - currentPositionOffset) * lineLeft);
            lineRight = (currentPositionOffset * nextTabRight + (1f - currentPositionOffset) * lineRight);
        }
        return new Pair<Float, Float>(lineLeft, lineRight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (isPaddingMiddle || padding > 0) {
            //Make sure tabContainer is bigger than the HorizontalScrollView to be able to scroll
            tabsContainer.setMinimumWidth(getWidth());
            //Clipping padding to false to see the tabs while we pass them swiping
            setClipToPadding(false);
        }

        if (tabsContainer.getChildCount() > 0) {
            tabsContainer
                    .getChildAt(0)
                    .getViewTreeObserver()
                    .addOnGlobalLayoutListener(firstTabGlobalLayoutListener);
        }
        super.onLayout(changed, l, t, r, b);
    }

    private OnGlobalLayoutListener firstTabGlobalLayoutListener = new OnGlobalLayoutListener() {

        @SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		@Override
        public void onGlobalLayout() {
            View view = tabsContainer.getChildAt(0);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }

            if (isPaddingMiddle) {
                int mHalfWidthFirstTab = view.getWidth() / 2;
                padding = getWidth() / 2 - mHalfWidthFirstTab;
            }
            setPadding(padding, getPaddingTop(), padding, getPaddingBottom());
            if (scrollOffset == 0) scrollOffset = getWidth() / 2 - padding;
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode() || tabCount == 0) {
            return;
        }

        final int height = getHeight();
        // draw indicator line
        rectPaint.setColor(indicatorColor);
        Pair<Float, Float> lines = getIndicatorCoordinates();
        canvas.drawRect(lines.first + padding, height - indicatorHeight, lines.second + padding, height, rectPaint);
        // draw underline
        rectPaint.setColor(underlineColor);
        canvas.drawRect(padding, height - underlineHeight, tabsContainer.getWidth() + padding, height, rectPaint);
        // draw divider
        if (dividerWidth != 0) {
            dividerPaint.setStrokeWidth(dividerWidth);
            dividerPaint.setColor(dividerColor);
            for (int i = 0; i < tabCount - 1; i++) {
                View tab = tabsContainer.getChildAt(i);
                canvas.drawLine(tab.getRight(), dividerPadding, tab.getRight(), height - dividerPadding, dividerPaint);
            }
        }
    }

    /*public void setOnTabReselectedListener(OnTabReselectedListener tabReselectedListener) {
        this.tabReselectedListener = tabReselectedListener;
    }*/

    public void setOnPageChangeListener(OnPageChangeListener listener) {
        this.delegatePageListener = listener;
    }


    private void updateSelection(int position) {
        for (int i = 0; i < tabCount; ++i) {
            View tv = tabsContainer.getChildAt(i);
            tv.setSelected(i == position);
        }
    }

    private void notSelected(View tab) {
        TextView title = (TextView) tab.findViewById(R.id.tab_title);
        if (title != null) {
            title.setTypeface(tabTypeface, tabTypefaceStyle);
            ViewCompat.setAlpha(title, tabTextAlpha);
        }
    }

    private void selected(View tab) {
        TextView title = (TextView) tab.findViewById(R.id.tab_title);
        if (title != null) {
            title.setTypeface(tabTypeface, tabTypefaceSelectedStyle);
            ViewCompat.setAlpha(title, tabTextSelectedAlpha);
        }
    }


    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        currentPosition = savedState.currentPosition;
        if (currentPosition != 0 && tabsContainer.getChildCount() > 0) {
            notSelected(tabsContainer.getChildAt(0));
            selected(tabsContainer.getChildAt(currentPosition));
        }
        requestLayout();
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.currentPosition = currentPosition;
        return savedState;
    }

    static class SavedState extends BaseSavedState {
        int currentPosition;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentPosition = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(currentPosition);
        }

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public int getIndicatorColor() {
        return this.indicatorColor;
    }

    public int getIndicatorHeight() {
        return indicatorHeight;
    }

    public int getUnderlineColor() {
        return underlineColor;
    }

    public int getDividerColor() {
        return dividerColor;
    }

    public int getDividerWidth() {
        return dividerWidth;
    }

    public int getUnderlineHeight() {
        return underlineHeight;
    }

    public int getDividerPadding() {
        return dividerPadding;
    }

    public int getScrollOffset() {
        return scrollOffset;
    }

    public boolean getShouldExpand() {
        return shouldExpand;
    }

    public int getTextSize() {
        return tabTextSize;
    }

    public boolean isTextAllCaps() {
        return textAllCaps;
    }

    public ColorStateList getTextColor() {
        return tabTextColor;
    }

    public int getTabBackground() {
        return tabBackgroundResId;
    }

    public int getTabPaddingLeftRight() {
        return tabPadding;
    }

    public void setIndicatorColor(int indicatorColor) {
        this.indicatorColor = indicatorColor;
        invalidate();
    }

    public void setIndicatorColorResource(int resId) {
        this.indicatorColor = getResources().getColor(resId);
        invalidate();
    }

    public void setIndicatorHeight(int indicatorLineHeightPx) {
        this.indicatorHeight = indicatorLineHeightPx;
        invalidate();
    }

    public void setUnderlineColor(int underlineColor) {
        this.underlineColor = underlineColor;
        invalidate();
    }

    public void setUnderlineColorResource(int resId) {
        this.underlineColor = getResources().getColor(resId);
        invalidate();
    }

    public void setDividerColor(int dividerColor) {
        this.dividerColor = dividerColor;
        invalidate();
    }

    public void setDividerColorResource(int resId) {
        this.dividerColor = getResources().getColor(resId);
        invalidate();
    }

    public void setDividerWidth(int dividerWidthPx) {
        this.dividerWidth = dividerWidthPx;
        invalidate();
    }

    public void setUnderlineHeight(int underlineHeightPx) {
        this.underlineHeight = underlineHeightPx;
        invalidate();
    }

    public void setDividerPadding(int dividerPaddingPx) {
        this.dividerPadding = dividerPaddingPx;
        invalidate();
    }

    public void setScrollOffset(int scrollOffsetPx) {
        this.scrollOffset = scrollOffsetPx;
        invalidate();
    }

    /*public void setShouldExpand(boolean shouldExpand) {
        this.shouldExpand = shouldExpand;
        if (pager != null) {
            requestLayout();
        }
    }*/

    public void setAllCaps(boolean textAllCaps) {
        this.textAllCaps = textAllCaps;
    }

    public void setTextSize(int textSizePx) {
        this.tabTextSize = textSizePx;
        updateTabStyles();
    }

    public void setTextColor(int textColor) {
        setTextColor(getColorStateList(textColor));
    }

    private ColorStateList getColorStateList(int textColor) {
        return new ColorStateList(new int[][]{new int[]{}}, new int[]{textColor});
    }

    public void setTextColor(ColorStateList colorStateList) {
        this.tabTextColor = colorStateList;
        updateTabStyles();
    }

    public void setTextColorResource(int resId) {
        setTextColor(getResources().getColor(resId));
    }

    public void setTextColorStateListResource(int resId) {
        setTextColor(getResources().getColorStateList(resId));
    }

    public void setTypeface(Typeface typeface, int style) {
        this.tabTypeface = typeface;
        this.tabTypefaceSelectedStyle = style;
        updateTabStyles();
    }

    public void setTabBackground(int resId) {
        this.tabBackgroundResId = resId;
    }

    public void setTabPaddingLeftRight(int paddingPx) {
        this.tabPadding = paddingPx;
        updateTabStyles();
    }
    
    public int getSelectedPosition(){
    	return currentPosition;
    }
    
    public String getSelectedField(){
    	return mMenuArray.get(currentPosition);
    }
    
    public void reorderArray(){
    	String temp = mMenuArray.get(currentPosition);
    	mMenuArray.remove(currentPosition);
    	mMenuArray.add(0,temp);
    	
    	final Handler handler = new Handler(); 
    	handler.postDelayed(new Runnable() {
    	  @Override 
    	  public void run() { 
    		  notifyDataSetChanged();
    	  } 
    	}, 200);
    	
    }
}
