package com.myappfactory.preferences;

import com.myappfactory.interfaces.IPurchase;
import com.myappfactory.listpreference.R;

import android.app.AlertDialog.Builder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

public class CustomListPreference extends ListPreference implements IPurchase {

	private CustomListPreferenceAdapter customListPreferenceAdapter;
	private Context mContext;
	private LayoutInflater mInflater;
	private CharSequence[] entries;
	private CharSequence[] entryValues;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	private String key;
	private int valuesDataType;
	private int selectedItemIndex;
	private String defaultValue;
	private CharSequence[] lockedValues;
	private CharSequence[] lockedValuesDependencyKeys;
	
	/**
	 * Finals
	 */
	private static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";
	private static final int INT = 1;
	private static final int FLOAT = 2;
	private static final int LONG = 3;
	
	/**
	 * Extend this class and change the resource ids for the dialog title/message as you desire.
	 */
	public int purchaseDialogTitleResId = R.string.iapDialogTitle;
	public int purchaseDialogMessageResId = R.string.iapDialogMessage;
	
	// controls
	private TextView text = null;
	private RadioButton rButton = null;

	public CustomListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		int keyAttributeResId = attrs.getAttributeResourceValue(ANDROID_SCHEMA, "key", 0);
		key = context.getString(keyAttributeResId);
		valuesDataType = Integer.valueOf(attrs.getAttributeValue(null, "valuesDataType"));
		// I am not using a resource for default value in the xml, so I can
		// retrieve the value directly unlike the 'key' attribute..
		defaultValue = attrs.getAttributeValue(ANDROID_SCHEMA, "defaultValue");
		
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomListPreference);
		lockedValues = typedArray.getTextArray(R.styleable.CustomListPreference_lockedValues);
		lockedValuesDependencyKeys = typedArray.getTextArray(R.styleable.CustomListPreference_lockedValuesDependencyKeys);
		typedArray.recycle();
		
		mContext = context;
		mInflater = LayoutInflater.from(context);
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	@Override
	protected void onPrepareDialogBuilder(Builder builder) {
		entries = getEntries();
		entryValues = getEntryValues();

		if (entries == null || entryValues == null || entries.length != entryValues.length) {
			throw new IllegalStateException("ListPreference requires an entries array and an entryValues array which are both the same length");
		}

		customListPreferenceAdapter = new CustomListPreferenceAdapter(mContext);

		builder.setAdapter(customListPreferenceAdapter, new DialogInterface.OnClickListener() {
			public void onClick (DialogInterface dialog, int which) {

			}
		}).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick (DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
			public void onClick (DialogInterface dialog, int which) {
				setValue(key, (String) entryValues[selectedItemIndex]);

				dialog.dismiss();
			}
		});
	}
	
	/**
	 * Override this in your extension to show your own dialog and also to implement an action for the Buy button.
	 */
	@Override
	public void showPurchaseDialog () {
		new AlertDialog.Builder(mContext).setTitle(purchaseDialogTitleResId ).setMessage(purchaseDialogMessageResId).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).setPositiveButton(R.string.buy, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				//TODO: implement me!
			}
		}).create().show();
	}

	private class CustomListPreferenceAdapter extends BaseAdapter {

		public CustomListPreferenceAdapter (Context context) {

		}

		public int getCount () {
			return entries.length;
		}

		public Object getItem (int position) {
			return position;
		}

		public long getItemId (int position) {
			return position;
		}

		public View getView (final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			boolean locked = false;
			
			row = mInflater.inflate(R.layout.custom_list_preference_row, parent, false);
			
			setTextAndRadioButton(row, position);
			row.setClickable(true);
			// check if this option is locked using the lockedValues[] and
			// lockedValuesDependencyKeys[] to verify if it should remain
			// locked
			// convert value according to data type and compare
			// TODO can this be improved?
			if (lockedValues != null && lockedValuesDependencyKeys != null) {
				for (int i = 0; i < lockedValues.length; i++) {
					// In each case, check if the dependency value is true,
					// otherwise set this row to false
					switch (valuesDataType) {
					case INT:
						if (Integer.valueOf((String) entryValues[position]) == Integer.valueOf(lockedValues[i].toString())) {
							if (!prefs.getBoolean(lockedValuesDependencyKeys[i].toString(), false)) {
								locked = true;
							}
						}
						break;
					case FLOAT:
						if (Float.valueOf((String) entryValues[position]) == Float.valueOf(lockedValues[i].toString())) {
							if (!prefs.getBoolean(lockedValuesDependencyKeys[i].toString(), false)) {
								locked = true;
							}
						}
						break;
					case LONG:
						if (Long.valueOf((String) entryValues[position]) == Long.valueOf(lockedValues[i].toString())) {
							if (!prefs.getBoolean(lockedValuesDependencyKeys[i].toString(), false)) {
								locked = true;
							}
						}
						break;
					case 0:
					default:
						if (entryValues[position].equals(lockedValues[i].toString())) {
							if (!prefs.getBoolean(lockedValuesDependencyKeys[i].toString(), false)) {
								locked = true;
							}
						}
						break;
					}
				}
			}

			if (locked) {
				TextView text = (TextView) row.findViewById(R.id.custom_list_view_row_text_view);
				text.setTextColor(Color.LTGRAY);
				lockRow(row);
				row.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						getDialog().dismiss();
						// show the buy dialog
						showPurchaseDialog();
					}
				});
			}
			else {
				row.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						selectedItemIndex = position;
						setValue(key, (String) entryValues[position]);

						getDialog().dismiss();
					}
				});
			}

			return row;
		}

		private void setTextAndRadioButton (View row, int position) {
			text = (TextView) row.findViewById(R.id.custom_list_view_row_text_view);
			rButton = (RadioButton) row.findViewById(R.id.custom_list_view_row_radio_button);
			text.setText(entries[position]);
			rButton.setId(position);

			// is this the selected value?
			if (prefs.contains(key)) {
				// convert value according to data type and compare
				switch (valuesDataType) {
				case INT:
					if (Integer.valueOf((String) entryValues[position]) == prefs.getInt(key, Integer.valueOf(defaultValue))) {
						rButton.setChecked(true);
						selectedItemIndex = position;
					}
					break;
				case FLOAT:
					if (Float.valueOf((String) entryValues[position]) == prefs.getFloat(key, Float.valueOf(defaultValue))) {
						rButton.setChecked(true);
						selectedItemIndex = position;
					}
					break;
				case LONG:
					if (Long.valueOf((String) entryValues[position]) == prefs.getLong(key, Long.valueOf(defaultValue))) {
						rButton.setChecked(true);
						selectedItemIndex = position;
					}
					break;
				case 0:
				default:
					if (entryValues[position].equals(prefs.getString(key, defaultValue))) {
						rButton.setChecked(true);
						selectedItemIndex = position;
					}
					break;
				}
			}
			else {
				if (entryValues[position].equals(defaultValue)) {
					rButton.setChecked(true);
					selectedItemIndex = position;
				}
			}

			rButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						int index = buttonView.getId();
						selectedItemIndex = index;
						setValue(key, (String) entryValues[index]);

						getDialog().dismiss();
					}
				}
			});
		}

		public void lockRow (View row) {
			rButton.setClickable(false);
			TextView lockedItem = (TextView) row.findViewById(R.id.locked_item);
			lockedItem.setVisibility(View.VISIBLE);
			rButton.setVisibility(View.GONE);
		}
	}

	private void setValue (String key, String value) {
		if (editor == null) {
			editor = prefs.edit();
		}
		
		switch (valuesDataType) {
		case INT:
			editor.putInt(key, Integer.valueOf(value));
			break;
		case FLOAT:
			editor.putFloat(key, Float.valueOf(value));
			break;
		case LONG:
			editor.putLong(key, Long.valueOf(value));
			break;
		case 0:
		default:
			editor.putString(key, value);
			break;
		}
		editor.commit();
	}
}
