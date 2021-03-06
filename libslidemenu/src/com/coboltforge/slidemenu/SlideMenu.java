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

import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParser;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class SlideMenu extends SlideView {


	public static class SlideMenuItem {
		public int id;
		public Drawable icon;
		public String label;
	}

	// a simple adapter
	private static class SlideMenuAdapter extends ArrayAdapter<SlideMenuItem> {
		Activity act;
		SlideMenuItem[] items;
		Typeface itemFont;

		class MenuItemHolder {
			public TextView label;
			public ImageView icon;
		}

		public SlideMenuAdapter(Activity act, SlideMenuItem[] items, Typeface itemFont) {
			super(act, R.id.menu_label, items);
			this.act = act;
			this.items = items;
			this.itemFont = itemFont;
		}
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View rowView = convertView;
			if (rowView == null) {
				LayoutInflater inflater = act.getLayoutInflater();
				rowView = inflater.inflate(R.layout.slidemenu_listitem, null);
				MenuItemHolder viewHolder = new MenuItemHolder();
				viewHolder.label = (TextView) rowView.findViewById(R.id.menu_label);
				if(itemFont != null)
					viewHolder.label.setTypeface(itemFont);
				viewHolder.icon = (ImageView) rowView.findViewById(R.id.menu_icon);
				rowView.setTag(viewHolder);
			}

			MenuItemHolder holder = (MenuItemHolder) rowView.getTag();
			String s = items[position].label;
			holder.label.setText(s);
			holder.icon.setImageDrawable(items[position].icon);

			return rowView;
		}
	}

	private Drawable headerImage;
	private Typeface font;

	private ArrayList<SlideMenuItem> menuItemList;
	private SlideMenuInterface.OnSlideMenuItemClickListener callback;

	/**
	 * Constructor used by the inflation apparatus.
	 * To be able to use the SlideMenu, call the {@link #init init()} method.
	 * @param context
	 */
	public SlideMenu(Context context) {
		super(context);
	}

	/**
	 * Constructor used by the inflation apparatus.
	 * To be able to use the SlideMenu, call the {@link #init init()} method.
	 * @param attrs
	 */
	public SlideMenu(Context context, AttributeSet attrs) {
		super(context, attrs);
	}


	/**
	 * Constructs a SlideMenu with the given menu XML.
	 * @param act The calling activity.
	 * @param menuResource Menu resource identifier.
	 * @param cb Callback to be invoked on menu item click.
	 * @param slideDuration Slide in/out duration in milliseconds.
	 */
	public SlideMenu(Activity act, int menuResource, SlideMenuInterface.OnSlideMenuItemClickListener cb, int slideDuration) {
		super(act);
		init(act, menuResource, cb, slideDuration);
	}

	/**
	 * Constructs an empty SlideMenu.
	 * @param act The calling activity.
	 * @param cb Callback to be invoked on menu item click.
	 * @param slideDuration Slide in/out duration in milliseconds.
	 */
	public SlideMenu(Activity act, SlideMenuInterface.OnSlideMenuItemClickListener cb, int slideDuration) {
		this(act, 0, cb, slideDuration);
	}

	/**
	 * If inflated from XML, initializes the SlideMenu.
	 * @param act The calling activity.
	 * @param menuResource Menu resource identifier, can be 0 for an empty SlideMenu.
	 * @param cb Callback to be invoked on menu item click.
	 * @param slideDuration Slide in/out duration in milliseconds.
	 */
	public void init(Activity act, int menuResource, SlideMenuInterface.OnSlideMenuItemClickListener cb, int slideDuration) {
        super.init(act, slideDuration);

		this.callback = cb;
		// and get our menu
		parseXml(menuResource);

	}

	/**
	 * Sets an optional image to be displayed on top of the menu.
	 * @param d
	 */
	public void setHeaderImage(Drawable d) {
		headerImage = d;
	}

	/**
	 * Optionally sets the font for the menu items.
	 * @param f A font.
	 */
	public void setFont(Typeface f) {
		font = f;
	}


	/**
	 * Dynamically adds a menu item.
	 * @param item
	 */
	public void addMenuItem(SlideMenuItem item) {
		menuItemList.add(item);
	}


	/**
	 * Empties the SlideMenu.
	 */
	public void clearMenuItems() {
		menuItemList.clear();
	}

    @Override
    public View inflateSlideview(LayoutInflater inflater) {

        View v = inflater.inflate(R.layout.slidemenu, null);

        FrameLayout.LayoutParams lays = new FrameLayout.LayoutParams(-1, -1, 3);
        lays.setMargins(0, statusHeight, 0, 0);
        v.setLayoutParams(lays);

        // set header
        try {
            ImageView header = (ImageView) act.findViewById(R.id.menu_header);
            header.setImageDrawable(headerImage);
        }
        catch(Exception e) {
            // not found
        }

        // connect the menu's listview
        ListView list = (ListView) v.findViewById(R.id.menu_listview);
        SlideMenuItem[] items = menuItemList.toArray(new SlideMenuItem[menuItemList.size()]);
        SlideMenuAdapter adap = new SlideMenuAdapter(act, items, font);
        list.setAdapter(adap);
        list.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if(callback != null)
                    callback.onSlideMenuItemClick(menuItemList.get(position).id);

                hide();
            }
        });


        return v;
    }


	// originally: https://github.com/darvds/RibbonMenu
	// credit where credits due!
	private void parseXml(int menu){

		menuItemList = new ArrayList<SlideMenuItem>();

		// use 0 id to indicate no menu (as specified in JavaDoc)
		if(menu == 0) return;

		try{
			XmlResourceParser xpp = act.getResources().getXml(menu);

			xpp.next();
			int eventType = xpp.getEventType();


			while(eventType != XmlPullParser.END_DOCUMENT){

				if(eventType == XmlPullParser.START_TAG){

					String elemName = xpp.getName();

					if(elemName.equals("item")){


						String textId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "title");
						String iconId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "icon");
						String resId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "id");

						SlideMenuItem item = new SlideMenuItem();
						item.id = Integer.valueOf(resId.replace("@", ""));
						if (iconId != null) {
							item.icon = act.getResources().getDrawable(Integer.valueOf(iconId.replace("@", "")));
						}
						item.label = resourceIdToString(textId);

						menuItemList.add(item);
					}

				}

				eventType = xpp.next();

			}


		} catch(Exception e){
			e.printStackTrace();
		}

	}



	private String resourceIdToString(String text){
		if(!text.contains("@")){
			return text;
		} else {
			String id = text.replace("@", "");
			return act.getResources().getString(Integer.valueOf(id));

		}
	}


}
