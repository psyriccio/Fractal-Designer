/*
 * Copyright (C) 2010 Daniel Nilsson
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

package net.margaritov.preference.colorpicker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.WazaBe.HoloEverywhere.HoloAlertDialogBuilder;
import com.resonos.app.library.R;

public class ColorPickerDialog 
	implements
		ColorPickerView.OnColorChangedListener,
		View.OnClickListener {

	private ColorPickerView mColorPicker;

	private ColorPickerPanelView mOldColor;
	private ColorPickerPanelView mNewColor;

	private OnColorChangedListener mListener;
	
	private Dialog mDlg;
	private Context mContext;

	public interface OnColorChangedListener {
		public void onColorChanged(int color);
	}
	
	public ColorPickerDialog(Context context, int initialColor) {
		mContext = context;
		init(initialColor);
	}

	private void init(int color) {
		mDlg = setUp(color);
		// To fight color branding.
		mDlg.getWindow().setFormat(PixelFormat.RGBA_8888);
	}

	private AlertDialog setUp(int color) {
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		View layout = inflater.inflate(R.layout.dialog_color_picker, null);

		HoloAlertDialogBuilder builder = new HoloAlertDialogBuilder(mContext);
		builder.setTitle(R.string.dialog_color_picker);
		builder.setView(layout);
		
		mColorPicker = (ColorPickerView) layout.findViewById(R.id.color_picker_view);
		mOldColor = (ColorPickerPanelView) layout.findViewById(R.id.old_color_panel);
		mNewColor = (ColorPickerPanelView) layout.findViewById(R.id.new_color_panel);
		
		((LinearLayout) mOldColor.getParent()).setPadding(
			Math.round(mColorPicker.getDrawingOffset()), 
			0, 
			Math.round(mColorPicker.getDrawingOffset()), 
			0
		);	
		
		mOldColor.setOnClickListener(this);
		mNewColor.setOnClickListener(this);
		mColorPicker.setOnColorChangedListener(this);
		mOldColor.setColor(color);
		mColorPicker.setColor(color, true);

		return builder.create();
	}

	@Override
	public void onColorChanged(int color) {

		mNewColor.setColor(color);

		/*
		if (mListener != null) {
			mListener.onColorChanged(color);
		}
		*/

	}

	public void setAlphaSliderVisible(boolean visible) {
		mColorPicker.setAlphaSliderVisible(visible);
	}
	
	/**
	 * Set a OnColorChangedListener to get notified when the color
	 * selected by the user has changed.
	 * @param listener
	 */
	public void setOnColorChangedListener(OnColorChangedListener listener){
		mListener = listener;
	}

	public int getColor() {
		return mColorPicker.getColor();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.new_color_panel) {
			if (mListener != null) {
				mListener.onColorChanged(mNewColor.getColor());
			}
		}
		mDlg.dismiss();
	}

	public void show() {
		mDlg.show();
	}
	
}
