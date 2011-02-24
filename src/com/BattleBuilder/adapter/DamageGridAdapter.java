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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

public class DamageGridAdapter extends DefaultHandler
{
	private static final String TAG = "FactionAdapter";

	private Context mContext;
	private LayoutInflater mInflater;

	/* Buffer post information as we learn it in STATE_IN_ITEM. */
	private HashMap<String, DamageGrid> mGrids;
	private DamageGrid mGridBuffer;
	private ArrayList<String> mBoxTypes;

	/* Efficiency is the name of the game here... */
	private int mState;
	private static final int STATE_IN_GRID = (1 << 2);
	private static final int STATE_IN_BOX = (1 << 3);
	private static final int STATE_IN_TRACK = (1 << 4);
	
	public static final String NAME="name";
	public static final String PARENT="parent";
	public static final String DEFAULT="default";
	public static final String DIMENSIONS="dimensions";
	public static final String LOC_X="x";
	public static final String LOC_Y="y";
	public static final String VALUE="value";
	public static final String ENABLED="enabled";
	public static final String IS_DAMAGE="is_damage";
	public static final String NEXT_LOCATION="next_location";
		
	private static HashMap<String, Integer> mStateMap;
	private final GridLocation sEmpty;

	static
	{
		mStateMap = new HashMap<String, Integer>();
		mStateMap.put("grid", new Integer(STATE_IN_GRID));
		mStateMap.put("box", new Integer(STATE_IN_BOX));
		mStateMap.put("track", new Integer(STATE_IN_TRACK));
	}

	private static DamageGridAdapter mSingle = null;
	
	public static DamageGridAdapter getAdapter(Context context){
		if( mSingle == null ){
			mSingle = new DamageGridAdapter(context);
		}
		return mSingle;
	}
	
	private DamageGridAdapter(Context context)
	{
		super();
		mGrids = new HashMap<String, DamageGrid>();
		mContext = context;
		mInflater = LayoutInflater.from(context);
		mBoxTypes = new ArrayList<String>();
		
		sEmpty = new GridLocation("", true);
		
		//now setup the sax parser for our static xml data
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp;
		XMLReader xr;
		try {
			sp = spf.newSAXParser();
			xr = sp.getXMLReader();
			xr.setContentHandler(this);
			xr.setErrorHandler(this);
			xr.parse(new InputSource(mContext.getResources().openRawResource(R.raw.damage_grids)));
			setupParentage();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotFoundException e) {
			// TODO Auto-generated catch bloc
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

			if (state.intValue() == STATE_IN_GRID){
				mGridBuffer = new DamageGrid();
				String localName;
				String parent=null;
				int x,y;
				for( int i =0; i<attrs.getLength(); i++ ){
					localName = attrs.getLocalName(i);
					if( localName == NAME){
						mGridBuffer.name = attrs.getValue(i);
						
					}else if(localName == PARENT){
						parent = attrs.getValue(i);
						if( mGridBuffer.getSizeX() == 0 || mGridBuffer.getSizeY() == 0 ){
							final DamageGrid parentGrid = mGrids.get(parent);
							x = parentGrid.getSizeX();
							y = parentGrid.getSizeY();
							mGridBuffer.init(x, y, parent);
						}

					}else if(localName == DIMENSIONS){
						String[] dims = attrs.getValue(i).split(",");
						x = Integer.parseInt(dims[0]);
						y = Integer.parseInt(dims[1]);
						mGridBuffer.init(x, y, parent);
					}
				}

			}else if(state.intValue() == STATE_IN_BOX){
				String localName;
				int x=-1;
				int y=-1;
				String value="";
				boolean enabled = true;
				int nextX=-1;
				int nextY=-1;
				for( int i =0; i<attrs.getLength(); i++ ){
					localName = attrs.getLocalName(i);
					if( localName == LOC_X){
						x = Integer.parseInt(attrs.getValue(i));
					}else if(localName == LOC_Y){
						y = Integer.parseInt(attrs.getValue(i));
					}else if(localName == VALUE){
						value = attrs.getValue(i);
					}else if(localName == ENABLED){
						enabled = Boolean.parseBoolean(attrs.getValue(i));
					}else if(localName == NEXT_LOCATION){
						String[] nl= attrs.getValue(i).split(",");
						nextX = Integer.parseInt(nl[0]);
						nextY = Integer.parseInt(nl[1]);
					}
				}
				assert( x>-1 && y>-1);
				GridLocation g = new GridLocation(value, enabled);
				if( nextX > 0){
					g.setNextDamage(nextX, nextY);
				}
				mGridBuffer.setBox(x, y, g);
			}else if(state.intValue() == STATE_IN_TRACK){
				String trackName="";
				int val=0;
				boolean isDamage=false;
				for( int i =0; i<attrs.getLength(); i++ ){
					String localName = attrs.getLocalName(i);
					if( localName == NAME){
						trackName = attrs.getValue(i);
					}else if( localName == VALUE){
						val = Integer.parseInt(attrs.getValue(i));
					}else if( localName == IS_DAMAGE){
						isDamage = Boolean.parseBoolean(attrs.getValue(i));
					}
				}
				mGridBuffer.setTrack(trackName, val, isDamage);
			}
		}
	}

	public void endElement(String uri, String name, String qName)
	{
		String use_name = name != null ? name : qName;
		
		Integer state = mStateMap.get(use_name);

		if (state != null)
		{				
			if( state.intValue() == STATE_IN_GRID ){
				mGridBuffer.finalize();
				mGrids.put( mGridBuffer.name, mGridBuffer );
			}
			mState &= ~(state.intValue());
		}
	}
	
	private void setupParentage(){
		
	}
	
	public class DamageGrid
	{
		public String name;
		private String mParent=null;
		private int mSizeX=0;
		private int mSizeY=0;
		private GridLocation[] mGrid = new GridLocation[0];
		private int mTotalEnabled;
		
		private int[][] mWhereToDamage=null;
		
		private String mTrackName="";
		private int mDamageTrack=0;
		private boolean mIsDamage=false;
		
		private boolean mDamageVertical = true;
		private boolean mDamageWraps = true;
		
		public DamageGrid() {
		}
		
		public DamageGrid(DamageGrid grid){
			init(grid.getSizeX(), grid.getSizeY(), grid.name);
			mParent = grid.mParent;
		}
		
		public DamageGrid(int x, int y, String parent){
			init(x,y,parent);
		}

		/*package*/  void init(int x, int y, String parent){
			mParent = parent;
			mSizeX = x;
			mSizeY = y;
			mGrid = new GridLocation[ x * y ];
			setUpFromParent();
		}
		
		/*package*/ void setTrack(String name, int value, boolean isDamage){
			mTrackName = name;
			mDamageTrack = value;
			mIsDamage = isDamage;			
		}
		
		/*package*/ void setUpFromParent(){
			DamageGrid parent = mGrids.get(mParent);
			if( parent == null){
				return;
			}
			for( int i=0; i<parent.mGrid.length;i++){
				GridLocation gridLoc = parent.mGrid[i];
				if(gridLoc != null){
					mGrid[i] = gridLoc;
				}
			}
			
		}
		
		public void finalize(){
			for( int i=0; i<mGrid.length; i++){
				if( mGrid[i] !=null && mGrid[i].isEnabled() ){
					mTotalEnabled++;
				}
			}
		}

		/*package*/ void setBox(int x, int y, GridLocation g){
			mGrid[ (y * mSizeX) + x ] = g;
		}
		
		public GridLocation getBox(int x, int y){
			return mGrid[ (y * mSizeX) + x ];
		}
		
		public int getSizeX(){
			return mSizeX;
		}
		
		public int getSizeY(){
			return mSizeY;
		}
		
		public int getTrackDamage(){
			return mDamageTrack;
		}
		
		public boolean isTrackDamage(){
			return mIsDamage;
		}
		
		public boolean damageWraps(){
			return mDamageWraps;
		}
		
		public boolean damageVertical(){
			return mDamageVertical;
		}
		
		public int getNumTracks(){
			return mWhereToDamage.length;
		}
		
		public int totalEnabled(){
			return mTotalEnabled;
		}

	}
	
	public class GridLocation{
		private String mLocationLetter="";
		private boolean mLocationEnabled=false;
		private int[] mNextDamageLocation=null;
	
		GridLocation(){
		}
		
		GridLocation(String locLet, boolean enable){
			this();
			mLocationLetter = locLet;
			mLocationEnabled = enable;
		}
		
		public boolean isEnabled(){
			return mLocationEnabled;
		}
		
		/*package*/ void setEnabled(boolean enabled){
			mLocationEnabled = enabled;
		}
				
		public String getLetter(){
			return mLocationLetter;
		}
		
		/*package*/ void setLetter(String l){
			mLocationLetter = l;
		}
		
		public void setNextDamage(int x, int y){
			mNextDamageLocation = new int[]{x,y};
		}
		
		public int[] getNextDamage(){
			return mNextDamageLocation;
		}
	}
	
	public static class PlayableDamageGrid{
		private boolean[] mDamaged;
		private DamageGrid mGrid;
		private int mSizeX, mSizeY;
		private int mTrackCurrent=0;
		private int mTrackMax;
		
		public PlayableDamageGrid(DamageGrid grid){
			init(grid);
		}

		/*package*/  void init(DamageGrid grid){
			mGrid = grid;
			mSizeX = grid.getSizeX();
			mSizeY = grid.getSizeY();
			mDamaged = new boolean[ mSizeX * mSizeY ];
			mTrackMax = grid.getTrackDamage();
		}

		public boolean getBoxDamaged(int index){
			return mDamaged[ index ];
		}
		
		public boolean getBoxDamaged(int x, int y){
			return mDamaged[ (y * mSizeX) + x ];
		}
		
		public void setDamaged(int x, int y, boolean damaged){
			setDamaged(y * mSizeX + x, damaged);
		}
		
		public void setDamaged(int index, boolean damaged){
			mDamaged[ index ] = damaged;
		}
		
		public void flipDamaged(int x, int y){
			int index = (y * mSizeX) + x;
			mDamaged[ index ] = !mDamaged[ index ];
		}

		public boolean getEnabled(int x, int y){
			GridLocation gl = mGrid.getBox(x, y);
			if( gl == null){
				return false;
			}
			return gl.isEnabled();
		}
		
		public String getBoxName(int x, int y){
			GridLocation gl = mGrid.getBox(x, y);
			if( gl == null){
				return "";
			}
			return gl.getLetter();
		}
		
		public String getName(){
			return mGrid.name;
		}
		
		public void setTrackDamage(int damage){
			mTrackCurrent = Math.max(0, Math.min(mTrackMax, damage));
		}
		
		public String getTrackName(){
			return mGrid.mTrackName;
		}
		
		public int getTrackDamage(){
			return mTrackCurrent;
		}
		
		public int getTrackMax(){
			return mTrackMax;
		}
		
		public int getSizeX(){
			return mSizeX;
		}
		
		public int getSizeY(){
			return mSizeY;
		}
		
		public void dealDamage(int column, int amount){
			//int[] spotsDamaged = new int[amount];
			if( mTrackCurrent>0 && mGrid.isTrackDamage()){
				int remainder = mTrackCurrent - amount;
				if( remainder >= 0){
					return;
				}else{
					amount += remainder;
				}
			}
			
			int x =	mGrid.mWhereToDamage[column][0];
			int y = mGrid.mWhereToDamage[column][1];
			GridLocation gl;
			for(int i=0; amount>0 && i<mDamaged.length; i++ ){
				gl = mGrid.getBox(x, y);
				
				if( gl != null && gl.isEnabled() && !getBoxDamaged(x, y) ){
					mDamaged[ (y*mSizeX) + x ] = true;
					amount--;
				}
			
				if( gl != null){
					int[] locs = gl.getNextDamage();
					if( locs != null && locs[0] > -1){
						x = locs[0];
						y = locs[1];
						continue;
					}
				}
				
				if( mGrid.mDamageVertical){
					if(++y >= mSizeY && mGrid.mDamageWraps){
						y=0;
						if( ++x >= mSizeX ){
							x=0;
						}
					}
				}else{
					if(++x >= mSizeX && mGrid.mDamageWraps){
						x=0;
						if( ++y >= mSizeY ){
							y=0;
						}
					}
				}
			}
			
			return;
		}
		
		public void removeAll(){
			for( int i=0; i< mDamaged.length;i++){
				mDamaged[i]=false;
			}
			mTrackCurrent = 0;
		}

		public final DamageGrid getBaseGrid(){
			return mGrid;
		}
		
		public int getDamageRemaining(){
			int count = 0;
			for(int i=0; i<mDamaged.length; i++){
				if( mDamaged[i]){
					count++;
				}
			}
			return count;
		}

		public String packUp() {
			StringBuffer damagedBoxes = new StringBuffer();
			for( int i=0; i<mDamaged.length; i++){
				if( mDamaged[i] ){
					damagedBoxes.append(i + ";");
				}
			}
			return damagedBoxes.toString();
		}

		public void unpack(String damage) {
			String[] damagedBoxes = damage.split(";");
			for( int i=0; i<damagedBoxes.length;i++){
				setDamaged(Integer.parseInt(damagedBoxes[i]), true);
			}
		}
	}
		
	public int getCount() {
		
		return mGrids.size();
	}
	
	public DamageGrid getGrid(String name){
		DamageGrid rGrid = mGrids.get(name);
		if( rGrid.mWhereToDamage != null){
			return rGrid;
		}
		
		int[][] gs = new int[6][2];
		for(int i=0; i<gs.length; i++ ){
			gs[i] = new int[]{i,0};
		}
		rGrid.mWhereToDamage = gs;
		
		return rGrid;
	}
	
	public DamageGrid getSpiral(String name){
		String branchNames = name;
		name = "spiral"+name;
		
		DamageGrid rGrid = mGrids.get(name);
		if( rGrid != null){
			return rGrid;
		}else{
			rGrid = new DamageGrid(mGrids.get("BASIC_SPIRAL"));
		}
		
		String[] branches = branchNames.split(",");
		int[] branchSizes = new int[branches.length];
		
		for( int i=0; i< branchSizes.length; i++){
			branchSizes[i] = Integer.parseInt(branches[i]) -4;
		}		
		
		int left=2;
		for( int i=0; i< 10; i++){
			if( left == 0){
				left = 2;
			}else{
				left = 0;
			}
			
			int y = 4 -(i/2);
			
			if( branchSizes[0]-- > 0){
				rGrid.setBox(left, y, sEmpty);
			}
			if( branchSizes[1]-- > 0 ){
				rGrid.setBox(left + 3, y, sEmpty);
			}
			if( branchSizes[2]-- > 0 ){
				rGrid.setBox(left + 6, y, sEmpty);
			}
		}
		
		int[][] gs = new int[6][2];
		gs[0] = new int[]{0,0};
		gs[1] = new int[]{2,0};
		gs[2] = new int[]{3,0};
		gs[3] = new int[]{5,0};
		gs[4] = new int[]{6,0};
		gs[5] = new int[]{8,0};
		rGrid.mWhereToDamage = gs;
		
		rGrid.name = name;
		mGrids.put(name, rGrid);
		return rGrid;
	}
	
	public DamageGrid getDamage(String name){
		String[] damageTracks = name.split(",");
		
		name = "model"+name;
		
		DamageGrid rGrid = mGrids.get(name);
		if( rGrid != null){
			return rGrid;
		}else{
			rGrid = new DamageGrid(6,6,"");
		}
				
		rGrid.name = name;
		
		for( int i=0; i< damageTracks.length; i++){
			int track = Integer.parseInt(damageTracks[i]);
			
			for( int j=0; j<track;j++){
				int x = j/5;
				int y = j%5;
				rGrid.setBox(x, y, sEmpty);
			}
		}
		
		rGrid.mWhereToDamage = new int[1][2];
		rGrid.mWhereToDamage[0] = new int[]{0,0};
		
		mGrids.put(name, rGrid);
		return rGrid;
	}
	
	public DamageGrid getUnitDamage(String name, int[] models){
		int highest=0;
		for(int i=0; i<models.length; i++){
			if( models[i] > highest ){
				highest = models[i];
			}
		}
		int boxes = Integer.parseInt(name);
		
		name = "unit"+name+highest;
		DamageGrid rGrid = mGrids.get(name);

		if( rGrid != null){
			return rGrid;
		}else{
			rGrid = new DamageGrid( boxes + 2, highest +1, "");
		}
		rGrid.name = name;
		rGrid.mDamageVertical = false;
		rGrid.mDamageWraps = false;
		rGrid.mWhereToDamage = new int[highest][2];

		String[] letters = {"A","B","C","D","E","F","G"};
		
		rGrid.setBox(1, 0, new GridLocation("L", false));
		
		for(int i=1; i<highest+1; i++){
			rGrid.setBox(0, i, new GridLocation(letters[i-1], false));
			rGrid.setBox(1, i, sEmpty);
			rGrid.mWhereToDamage[0] = new int[]{2,i};
			for(int j=0; j<boxes; j++){
				rGrid.setBox(j+2, i, sEmpty);				
			}
		}

		mGrids.put(name, rGrid);
		return rGrid;
	}
	
	public PlayableDamageGrid getPlayableGrid(String name){
		return new PlayableDamageGrid(mGrids.get(name));
	}
	
	public boolean isEmpty() {
		return mGrids.isEmpty();
	}

}

