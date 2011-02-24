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
import com.BattleBuilder.adapter.FactionAdapter.Faction;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class PointLevelAdapter extends DefaultHandler implements SpinnerAdapter
{
	private static final String TAG = "PointsAdapter";

	private Handler mHandler;
	private long mID;
	private String mXMLURL;
	private Context mContext;
	private LayoutInflater mInflater;

	/* Buffer post information as we learn it in STATE_IN_ITEM. */
	private ArrayList<PointLevel> mPointLevels;

	/* Efficiency is the name of the game here... */
	private int mState;
	private static final int STATE_IN_LEVEL = (1 << 2);
//	private static final int STATE_IN_POINTS_LEVEL = (1 << 3);
	
	public static final String MAX_POINTS="points";
	public static final String MAX_CASTERS="casters";

	private static HashMap<String, Integer> mStateMap;

	static
	{
		mStateMap = new HashMap<String, Integer>();
		mStateMap.put("level", new Integer(STATE_IN_LEVEL));
	}

	private static PointLevelAdapter mSingle = null;
	
	public static PointLevelAdapter getAdapter(Context context){
		if(mSingle == null){
			mSingle = new PointLevelAdapter( context );
		}
		return mSingle;
	}
	
	private PointLevelAdapter(Context context)
	{
		super();
		mPointLevels = new ArrayList<PointLevel>();
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
			xr.parse(new InputSource(mContext.getResources().openRawResource(R.raw.point_levels)));
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

			if (state.intValue() == STATE_IN_LEVEL){
				PointLevel pl = new PointLevel();
				for( int i =0; i<attrs.getLength(); i++ ){
					String localName = attrs.getLocalName(i);
					if(localName  == MAX_POINTS){
						pl.maxPoints = Integer.parseInt(attrs.getValue(i));
					}else if(localName == MAX_CASTERS){
						pl.maxCasters = Integer.parseInt(attrs.getValue(i));					
					}
				}
				Log.d(TAG, "Pointlevel loaded: " + pl.maxPoints);
				mPointLevels.add( pl );
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

	public class PointLevel
	{
		public int maxPoints;
		public int maxCasters;

		public PointLevel()
		{
			maxPoints=0;
			maxCasters=0;
		}
		
		public PointLevel(int maxP, int maxC){
			maxPoints=maxP;
			maxCasters=maxC;
		}
		
		public boolean equals(Object o){
			if( o.getClass() == Integer.class){
				return this.maxPoints == (Integer)o;
			}
			return super.equals(o);
		}
	}
	
	static class ViewHolder{
		TextView points;
		TextView casters;
	}
	
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
		if( convertView == null){
			convertView	= mInflater.inflate(R.layout.point_row, null);
			
			holder = new ViewHolder();
			holder.points = (TextView) convertView.findViewById(R.id.point_amount);
			holder.casters = (TextView) convertView.findViewById(R.id.caster_amount);
			
			convertView.setTag(holder);
		}else{
			holder = (ViewHolder) convertView.getTag();
		}
		
		holder.points.setText(mPointLevels.get(position).maxPoints + "");
		holder.casters.setText(mPointLevels.get(position).maxCasters + "");

		return convertView;
	}

	/*
	 * @returns The number of points allowed at the given index
	 */
	public int getPointsAt(int position){
		return mPointLevels.get(position).maxPoints;
	}

	/*
	 * @returns The number of casters allowed at the given index
	 */
	public int getCastersAt(int position){
		return mPointLevels.get(position).maxCasters;
	}
	
	public int getPointsIndex(Integer points){
		for( int i =0; i< mPointLevels.size(); i++){
			PointLevel p = mPointLevels.get(i);
			if( p.maxPoints == points ){
				return i;
			}
		}
		return -1;
	}
	
	public int getCount() {
		return mPointLevels.size();
	}

	public Object getItem(int position) {
		return mPointLevels.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public int getItemViewType(int position) {
		return 0;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		TextView useView;
		
		if( convertView == null){
			useView	= new TextView(mContext);
			useView.setTextColor(mContext.getResources().getColor(R.color.spinner_text));
		}else{
			useView = (TextView) convertView;
		}
		
		useView.setText(mPointLevels.get(position).maxPoints + "");

		return useView;
	}

	public int getViewTypeCount() {
		return 1;
	}

	public boolean hasStableIds() {
		return true;
	}

	public boolean isEmpty() {
		return mPointLevels.isEmpty();
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

