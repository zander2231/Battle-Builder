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

import com.BattleBuilder.R;
import android.content.Context;
import android.media.AudioManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NumberPicker extends LinearLayout {

    public interface OnChangedListener {
        void onChanged(NumberPicker picker, int newVal, int oldVal);
    }

	private Button mPlusButton;
	private Button mMinusButton;
	private TextView mTextCount;
	private int mCurrentCount=0;
	private int mPreviousCount=0;
	private static final String TAG="NumberPicker";
	private OnChangedListener mOnChangeListener;
	private int mDataIndex;
	private int mLocationIndex;
	
	public NumberPicker(Context context) {
		this(context, null);
	}

	public NumberPicker(Context context, int modelIndex){
		this(context);
		mDataIndex = modelIndex;
	}
	
	public NumberPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.setOrientation(LinearLayout.HORIZONTAL);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.number_picker, this, true);

        float textSize = attrs.getAttributeFloatValue("android", "textSize", -1);
        
		mPlusButton = (Button)findViewById(R.id.numpick_plus);
		mPlusButton.setOnClickListener(mButtonListener);
		mPlusButton.setFocusable(false);
		mMinusButton = (Button)findViewById(R.id.numpick_minus);
		mMinusButton.setOnClickListener(mButtonListener);
		mMinusButton.setFocusable(false);
		mTextCount = (TextView)findViewById(R.id.numpick_input);
		mTextCount.setText("0");
		if( textSize > -1){
			mTextCount.setTextSize(textSize);
		}
		mTextCount.setClickable(false);
		mMinusButton.setEnabled(false);
	}
	
	public NumberPicker(Context context, AttributeSet attrs, int modelIndex){
		this(context, attrs);
		mDataIndex = modelIndex;
	}
	
    private View.OnClickListener mButtonListener = new View.OnClickListener() {
        public void onClick(View v) {
    		Log.d(TAG, "Clicklistener called with view " + v);
        	if( v == mPlusButton){
        		setCount(mCurrentCount + 1);
    		}else if(v == mMinusButton){
    			setCount(mCurrentCount - 1);
			}
        }
    };

    public int getCount(){
    	return mCurrentCount;
    }
    
    public void setCountNoUpdate(int newCount){
    	mPreviousCount = mCurrentCount;
		mCurrentCount = newCount;
    	mTextCount.setText(mCurrentCount + "");
    }
    
    public void updateButtons(){
    	if( mCurrentCount > 0 ){
    		mMinusButton.setEnabled(true);
    	}
    	else{
    		mMinusButton.setEnabled(false);    		
    	}
    }
    
    public void setCount(int newCount){
    	setCountNoUpdate(newCount);
    	notifyChange();
    }
    
    public void setEnabled(boolean plus, boolean minus){
		mPlusButton.setEnabled(plus);
		mMinusButton.setEnabled(minus);    		
    }
    
    public void setDataIndex(int index){
    	mDataIndex = index;
    }
    
    public int getDataIndex(){
    	return mDataIndex;
    }
    
    public void setLocationIndex(int index){
    	mLocationIndex = index;
    }

    public int getLocationIndex(){
    	return mLocationIndex;
    }
    
    public void setOnChangedListener( OnChangedListener listener){
    	mOnChangeListener = listener;
    }
    
    private void notifyChange() {
        if (mOnChangeListener != null) {
        	mOnChangeListener.onChanged(this, mCurrentCount, mPreviousCount);
        }else{
        	updateButtons();
        }
    }
}
