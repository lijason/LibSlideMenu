/*
 * A sliding menu for Android, very much like the Google+ and Facebook apps have.
 * 
 * Copyright (C) 2012 CoboltForge
 * 
 * Based upon the great work done by stackoverflow user Scirocco (http://stackoverflow.com/a/11367825/361413), thanks a lot!
 * The XML parsing code comes from https://github.com/darvds/RibbonMenu, thanks!
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
 */

package com.coboltforge.slidemenu;

import java.lang.reflect.Method;


import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;

public abstract class SlideView extends LinearLayout {
	
	// keys for saving/restoring instance state
	private final static String KEY_MENUSHOWN = "menuWasShown";
	private final static String KEY_STATUSBARHEIGHT = "statusBarHeight";
	private final static String KEY_SUPERSTATE = "superState";

	
	private static boolean menuShown = false;
	protected int statusHeight;
	private static LinearLayout content;
	private static FrameLayout parent;
	protected static int menuSize;
	private View menu;
	protected Activity act;
	private TranslateAnimation slideRightAnim;
	private TranslateAnimation slideMenuLeftAnim;
	private TranslateAnimation slideContentLeftAnim;
	
	/**
	 * Constructor used by the inflation apparatus.
	 * To be able to use the SlideMenu, call the {@link #init init()} method.
	 * @param context
	 */
	public SlideView(Context context) {
		super(context);
	}
	
	/**
	 * Constructor used by the inflation apparatus.
	 * To be able to use the SlideMenu, call the {@link #init init()} method.
	 * @param attrs
	 */
	public SlideView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	
	/** 
	 * @param act The calling activity.
	 * @param slideDuration Slide in/out duration in milliseconds.
	 */
	public void init(Activity act, int slideDuration) {
		this.act = act;

		// set size
		menuSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 250, act.getResources().getDisplayMetrics());
		
		// create animations accordingly
		slideRightAnim = new TranslateAnimation(-menuSize, 0, 0, 0);
		slideRightAnim.setDuration(slideDuration);
		slideMenuLeftAnim = new TranslateAnimation(0, -menuSize, 0, 0);
		slideMenuLeftAnim.setDuration(slideDuration);
		slideContentLeftAnim = new TranslateAnimation(menuSize, 0, 0, 0);
		slideContentLeftAnim.setDuration(slideDuration);
	}

	public void toggle() {
		if (menuShown) {
			hide();
		} else {
			show();
		}
	}

	/**
	 * Slide the menu in.
	 */
	public void show() {
		/*
		 *  We have to adopt to status bar height in most cases,
		 *  but not if there is a support actionbar!
		 */
		try {
			Method getSupportActionBar = act.getClass().getMethod("getSupportActionBar", (Class[])null);
			Object sab = getSupportActionBar.invoke(act, (Object[])null);
			sab.toString(); // check for null

			if (android.os.Build.VERSION.SDK_INT >= 11) {
				// over api level 11? add the margin
				applyStatusbarOffset();
			}
		}
		catch(Exception es) {
			// there is no support action bar!
			applyStatusbarOffset();
		}

		/*
		 * phew, finally!
		 */
		this.show(true);
	}

	
	
	private void show(boolean animate) {
		
		// modify content layout params
		content = ((LinearLayout) act.findViewById(android.R.id.content).getParent());
		FrameLayout.LayoutParams parm = new FrameLayout.LayoutParams(-1, -1, 3);
		parm.setMargins(menuSize, 0, -menuSize, 0);
		content.setLayoutParams(parm);
		
		// animation for smooth slide-out
		if(animate)
			content.startAnimation(slideRightAnim);
		
		// add the slide menu to parent
		parent = (FrameLayout) content.getParent();
		LayoutInflater inflater = (LayoutInflater) act.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		menu = inflateSlideview(inflater);

		parent.addView(menu);
//		FrameLayout overlay = new FrameLayout(act);
//		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
//		overlay.setLayoutParams(lp);
//		overlay.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				SlideView.this.hide();
//			}
//		});
//		parent.addView(overlay);
		
		// slide menu in
		if(animate)
			menu.startAnimation(slideRightAnim);
		
		
		enableDisableViewGroup((LinearLayout) parent.findViewById(android.R.id.content).getParent(), false);

		menuShown = true;
	}
	
	
	
	/**
	 * Slide the menu out.
	 */
	public void hide() {
		menu.startAnimation(slideMenuLeftAnim);
		parent.removeView(menu);

		content.startAnimation(slideContentLeftAnim);

		FrameLayout.LayoutParams parm = (FrameLayout.LayoutParams) content.getLayoutParams();
		parm.setMargins(0, 0, 0, 0);
		content.setLayoutParams(parm);
		enableDisableViewGroup((LinearLayout) parent.findViewById(android.R.id.content).getParent(), true);

		menuShown = false;
	}

	
	private void applyStatusbarOffset() {
		Rect r = new Rect();
		Window window = act.getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(r);
		statusHeight = r.top;
	}
	
	
	//originally: http://stackoverflow.com/questions/5418510/disable-the-touch-events-for-all-the-views
	//modified for the needs here
	private void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
		int childCount = viewGroup.getChildCount();
		for (int i = 0; i < childCount; i++) {
			View view = viewGroup.getChildAt(i);
			if(view.isFocusable())
				view.setEnabled(enabled);
			if (view instanceof ViewGroup) {
				enableDisableViewGroup((ViewGroup) view, enabled);
			} else if (view instanceof ListView) {
				if(view.isFocusable())
					view.setEnabled(enabled);
				ListView listView = (ListView) view;
				int listChildCount = listView.getChildCount();
				for (int j = 0; j < listChildCount; j++) {
					if(view.isFocusable())
						listView.getChildAt(j).setEnabled(false);
				}
			}
		}
	}
	
		
	@Override 
	protected void onRestoreInstanceState(Parcelable state)	{
		try{
			
			if (state instanceof Bundle) {
				Bundle bundle = (Bundle) state;
				
				statusHeight = bundle.getInt(KEY_STATUSBARHEIGHT);

				if(bundle.getBoolean(KEY_MENUSHOWN))
					show(false); // show without animation
				
				super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPERSTATE));
				
				return;
			}
			
			super.onRestoreInstanceState(state);
			
		}
		catch(NullPointerException e) { 
			// in case the menu was not declared via XML but added from code
		}
	}
	
	

	@Override 
	protected Parcelable onSaveInstanceState()	{
		Bundle bundle = new Bundle();
		bundle.putParcelable(KEY_SUPERSTATE, super.onSaveInstanceState());
		bundle.putBoolean(KEY_MENUSHOWN, menuShown);
		bundle.putInt(KEY_STATUSBARHEIGHT, statusHeight);

		return bundle;
	}
	

	public abstract View inflateSlideview(LayoutInflater inflater);

	
}