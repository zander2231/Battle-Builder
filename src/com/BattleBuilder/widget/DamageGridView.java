package com.BattleBuilder.widget;

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

import android.content.Context;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import com.BattleBuilder.R;
import com.BattleBuilder.adapter.DamageGridAdapter;
import com.BattleBuilder.adapter.DamageGridAdapter.DamageGrid;
import com.BattleBuilder.adapter.DamageGridAdapter.PlayableDamageGrid;
import com.BattleBuilder.adapter.ModelAdapter;

public class DamageGridView extends View{

	private static final String TAG = "DamageGridView";
	private PlayableDamageGrid mGrid=null;
	private boolean mFingerIsDown=false;
	private boolean[] mBoxIsDamaged;
	private boolean mSetToDamaged;
	private boolean mDamagedRemoved=false;
	private Paint mLightPaint;
	private Paint mDarkPaint;
	private Paint mRedPaint;

	public DamageGridView(Context context) {
		this(context, null);
	}
	
	public DamageGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mLightPaint = new Paint();
		mLightPaint.setAntiAlias(true);
		mLightPaint.setColor(Color.WHITE);
		mLightPaint.setTextAlign(Paint.Align.CENTER);
		
		mDarkPaint = new Paint();
		mDarkPaint.setAntiAlias(true);
		mDarkPaint.setColor(Color.BLACK);
		mDarkPaint.setTextAlign(Paint.Align.CENTER);

		mRedPaint = new Paint();
		mRedPaint.setAntiAlias(true);
		mRedPaint.setColor(Color.RED);
		mRedPaint.setTextAlign(Paint.Align.CENTER);

	}

	public void setGrid( String grid ){
		mGrid = DamageGridAdapter.getAdapter(getContext()).getPlayableGrid(grid);
		mBoxIsDamaged = new boolean[mGrid.getSizeX() * mGrid.getSizeY()];
	}
	
	public void setGrid( PlayableDamageGrid grid ){
		mGrid = grid;
		mBoxIsDamaged = new boolean[mGrid.getSizeX() * mGrid.getSizeY()];
	}
	
	public void onDraw(Canvas canvas){
		//clear the canvas TODO make this take a background color
		canvas.drawColor(Color.GRAY);
		
		if( mGrid == null){
			return;
		}
		
		final int sizeX = mGrid.getSizeX();
		final int sizeY = mGrid.getSizeY();
		final int w = this.getWidth() - getPaddingLeft() - getPaddingRight();
		final int h = this.getHeight() - getPaddingTop() - getPaddingBottom();
		final int boxPad = 1;
		final int boxW = w/sizeX;
		final int boxH = h/sizeY;
		final int startAtX =boxPad + getPaddingLeft() + (w -(boxW * sizeX))/2;
		final int startAtY =boxPad + getPaddingTop() + (h- (boxH * sizeY))/2;

		mLightPaint.setTextSize(boxH * 2 / 3);
		mDarkPaint.setTextSize(boxH * 2 / 3);
		mRedPaint.setTextSize(boxH);
				
		boolean boxEnabled;
		boolean boxDamaged;
		String boxName;
				
		for( int i=0; i<sizeY; i++){		mRedPaint.setTextSize(boxH);

			for( int j=0; j<sizeX; j++){
				boxEnabled = mGrid.getEnabled(j, i);
				boxName = mGrid.getBoxName(j,i);
				boxDamaged = mGrid.getBoxDamaged(j,i);
				int left = startAtX + (boxW * j);
				int right = left + boxW - boxPad;
				int top = startAtY + (boxH * i);
				int bottom = top + boxH - boxPad;
				int textLeft = left + boxW/2;
				int textTop = top + boxH/2 + (int)mLightPaint.getTextSize() / 2;
				
				if( boxEnabled ){
					canvas.drawRect(left, top, right, bottom, mLightPaint);
					canvas.drawText( boxName, textLeft, textTop, mDarkPaint);
					if( getBoxPreDamaged(j,i) ){
						mRedPaint.setAlpha(100);
						canvas.drawText( "X", textLeft, textTop, mRedPaint);
					}else if(boxDamaged){
						mRedPaint.setAlpha(255);
						canvas.drawText( "X", textLeft, textTop, mRedPaint);
					}
				}else if(!boxName.equals("")){
					canvas.drawText( boxName, textLeft, textTop, mDarkPaint);
				}
			}
		}
	}
		
//	public void dealDamage(int colNum, int damage){
//		int[] spotsDamaged = mGrid.dealDamage(colNum, damage);
//		for( int i=0; i< spotsDamaged.length; i++){
//			mBoxIsDamaged[ spotsDamaged[i] ] = true;
//		}
//	}
	
	public int getNumTracks(){
		return mGrid.getBaseGrid().getNumTracks();
	}
	
	public void finalizeDamage(){
		final int sizeX = mGrid.getSizeX();

		for( int i=0; i< mBoxIsDamaged.length; i++){
			if(mBoxIsDamaged[i]){
				mGrid.flipDamaged( i%sizeX, i/sizeX);
				mBoxIsDamaged[i] = false;
			}
		}
    	mDamagedRemoved = true;
		invalidate();
	}
	
	@Override
    public boolean onTouchEvent(MotionEvent event){
    	
		if( !isEnabled()){
			return false;
		}
		
    	float atX = event.getX();
    	float atY = event.getY();
    	
    	if( event.getAction() == MotionEvent.ACTION_DOWN){
    		mFingerIsDown = true;
    	
    		mSetToDamaged = ! getBoxPreDamaged(atX, atY);
    		setBoxPreDamaged(atX, atY, mSetToDamaged);
    		invalidate();
    		return true;
    	}else if(event.getAction() == MotionEvent.ACTION_UP){
    		mFingerIsDown = false;
    		return true;
    	}else if(event.getAction() == MotionEvent.ACTION_MOVE && mFingerIsDown){
    		setBoxPreDamaged(atX, atY, mSetToDamaged);    		
    		invalidate();
    	}
    	    	
    	return false;
    }
    
    private void setBoxPreDamaged(float x, float y, boolean damaged){
		setBoxPreDamaged( (int)(x/getWidth() * mGrid.getSizeX()),
						  (int)(y/getHeight()* mGrid.getSizeY()),
						  damaged);
    }
    
    private boolean getBoxPreDamaged(float x, float y){
		return getBoxPreDamaged( (int)(x/getWidth() * mGrid.getSizeX()),
								 (int)(y/getHeight()* mGrid.getSizeY()) );
    }
    
    public boolean getBoxPreDamaged(int x, int y){
		final int sizeX = mGrid.getSizeX();

		return mBoxIsDamaged[ ((y * sizeX) + x) ];
    }
    
    public void setBoxPreDamaged(int x, int y, boolean damaged){
		final int sizeX = mGrid.getSizeX();

		Log.d(TAG, "setting box x: "+ x + ", y:" + y+ " to:" + damaged);
		mBoxIsDamaged[ ((y * sizeX) + x) ] = damaged;
    }
    
    public void removeAllDamage(){
    	for(int i=0; i<mBoxIsDamaged.length; i++){
    		if( mGrid.getBoxDamaged(i) ){
    			mBoxIsDamaged[i] = mDamagedRemoved;
    		}
    	}
    	mDamagedRemoved = !mDamagedRemoved;
    	invalidate();
    }

}
