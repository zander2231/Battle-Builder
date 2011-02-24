package com.BattleBuilder.adapter;

/*
*  Copyright (C) 2010  Alex Badion
*
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import com.BattleBuilder.R;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class FactionAdapter extends DefaultHandler implements SpinnerAdapter
{
	private static final String TAG = "FactionAdapter";

	private Handler mHandler;
	private long mID;
	private String mXMLURL;
	private Context mContext;
	private LayoutInflater mInflater;

	/* Buffer post information as we learn it in STATE_IN_ITEM. */
	private ArrayList<Faction> mFactions;

	/* Efficiency is the name of the game here... */
	private int mState;
	private static final int STATE_IN_FACTION = (1 << 2);
	
	public static final String FACTION_SYMBOL_URI="fac_symbol";
	public static final String FACTION_NAME="name";
	public static final String FACTION__DISPLAY_NAME="display_name";
	public static final String FACTION__MERC="merc";

	private static HashMap<String, Integer> mStateMap;

	static
	{
		mStateMap = new HashMap<String, Integer>();
		mStateMap.put("faction", new Integer(STATE_IN_FACTION));
	}

	private static FactionAdapter mSingle = null;
	
	public static FactionAdapter getAdapter(Context context){
		if( mSingle == null ){
			mSingle = new FactionAdapter(context);
		}
		return mSingle;
	}
	
	private FactionAdapter(Context context)
	{
		super();
		mFactions = new ArrayList<Faction>();
		mContext = context;
		mInflater = LayoutInflater.from(context);
		
		//now setup the sax parser for our static xml data
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		XMLReader xr;
		try {
			sp = spf.newSAXParser();
			xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.setErrorHandler(this);
			xr.parse(new InputSource(mContext.getResources().openRawResource(R.raw.factions)));
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}

	public void startElement(String uri, String name, String qName,
			Attributes attrs)
	{
		String use_name = name != null ? name : qName;
		
		Integer state = mStateMap.get(use_name);

		if (state != null)
		{
			mState |= state.intValue();

			if (state.intValue() == STATE_IN_FACTION){
				Faction fac = new Faction();
				for( int i =0; i<attrs.getLength(); i++ ){
					String localName = attrs.getLocalName(i);
					if( localName == FACTION_SYMBOL_URI){
						InputStream imageStream = null;
						try {
							imageStream = mContext.getAssets().open(attrs.getValue(i));
							fac.symbol = BitmapFactory.decodeStream(imageStream);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}else if(localName == FACTION_NAME){
						fac.name = attrs.getValue(i);					
					}
					else if(localName == FACTION__DISPLAY_NAME){
						fac.displayName = attrs.getValue(i);
					}
					else if(localName == FACTION__MERC){
						fac.isMerc = Boolean.getBoolean( attrs.getValue(i) );
					}
				}
				Log.d(TAG, "Faction loaded: " + fac.displayName);
				mFactions.add( fac );
			}
		}
	}

	public void endElement(String uri, String name, String qName)
	{
		String use_name = name != null ? name : qName;
		
		Integer state = mStateMap.get(use_name);

		if (state != null)
		{
			mState &= ~(state.intValue());
		}
	}

	public class Faction
	{
		public String name;
		public String displayName;
		public Bitmap symbol;
		public boolean isMerc=false;
		
		public boolean equals(Object o){
			if(o.getClass() == String.class){
				return o.equals(name);
			}
			return super.equals(o);
		}
	}
	
	static class ViewHolder{
		TextView name;
		ImageView symbol;
	}
	
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
				
		if( convertView == null){
			convertView	= mInflater.inflate(R.layout.faction_row, null);
			
			holder = new ViewHolder();
			holder.name = (TextView) convertView.findViewById(R.id.faction_name);
			holder.symbol = (ImageView) convertView.findViewById(R.id.faction_symbol);
			
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.name.setText(mFactions.get(position).displayName);
		holder.symbol.setImageBitmap(mFactions.get(position).symbol);

		return convertView;
	}

	public String getNameAt(int position){
		return mFactions.get(position).name;
	}
	
	public Bitmap getImageAt(int postition){
		return mFactions.get(postition).symbol;
	}

	public String getDisplayNameAt(int position){
		return mFactions.get(position).displayName;
	}
	
	public int getNameIndex(String name){
		for( int i =0; i< mFactions.size(); i++){
			Faction f = mFactions.get(i);
			if( f.name.equalsIgnoreCase(name) || f.displayName.equalsIgnoreCase(name) ){
				return i;
			}
		}
		return -1;
	}
	
	public boolean getMerc(String name){
		int i = getNameIndex(name);
		if( i >=0 ){
			return mFactions.get(i).isMerc;
		}else{
			return false;
		}
	}
	
	public Object[] toArray(){
		return mFactions.toArray();
	}
	
	public int getCount() {
		return mFactions.size();
	}

	public Object getItem(int position) {
		return mFactions.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView useView;
		
		if( convertView == null){
			useView	= new ImageView(mContext);
		}else{
			useView = (ImageView) convertView;
		}
		
		useView.setImageBitmap(mFactions.get(position).symbol);

		return useView;
	}

	public int getViewTypeCount() {
		// TODO Auto-generated method stub
		return 1;
	}

	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return true;
	}

	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return mFactions.isEmpty();
	}

	/*
	 * Data will never change so there is no point 
	 * maintaining observers.
	 */
	public void registerDataSetObserver(DataSetObserver observer) {
		// TODO Auto-generated method stub
	}

	public void unregisterDataSetObserver(DataSetObserver observer) {
		// TODO Auto-generated method stub
		
	}
}

