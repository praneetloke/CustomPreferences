package com.myappfactory.preferences;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;

import com.myappfactory.interfaces.IDynamicProvider;
import com.myappfactory.interfaces.IPurchase;
import com.myappfactory.listpreference.R;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CustomListPreference extends ListPreference implements IPurchase {
	private static final String TAG = "CustomListPreference";
	
	private CustomListPreferenceAdapter customListPreferenceAdapter;
	private Context mContext;
	private LayoutInflater mInflater;
	private SharedPreferences mPrefs;
	
	private String mDefaultValue;
	private String mKey;
	
	private int mValuesDataType;
	private int mSelectedItemIndex;
	
	/**
	 * Let's cache the persisted (typed) value so we can use it for comparison for each row
	 */
	private int mPersistedIntValue;
	private float mPersistedFloatValue;
	private long mPersistedLongValue;
	private String mPersistedStringValue;
	
	private CharSequence[] mEntries;
	private CharSequence[] mEntryValues;
	private CharSequence[] lockedValues;
	private CharSequence[] lockedValuesDependencyKeys;
    private String dynamicEntriesProviderName;
    private String dynamicEntryValuesProviderName;

    private IDynamicProvider mDynamicProvider;
    private IDynamicProvider mDynamicEntryValuesProvider;

	/**
	 * Finals
	 */
	private static final String ANDROID_SCHEMA = "http://schemas.android.com/apk/res/android";
	private static final int INT = 1;
	private static final int FLOAT = 2;
	private static final int LONG = 3;

	/**
	 * Extend this class and change the resource ids for the dialog
	 * title/message as you desire.
	 */
	public int purchaseDialogTitleResId = R.string.iapDialogTitle;
	public int purchaseDialogMessageResId = R.string.iapDialogMessage;

	// controls
	private TextView mTextView;
	private RadioButton mRadioButton;

	public CustomListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);

		mKey = getKey();

		mValuesDataType = Integer.valueOf(attrs.getAttributeValue(null, "valuesDataType"));

		// I am not using a resource for default value in the xml, so I can
		// retrieve the value directly
		mDefaultValue = attrs.getAttributeValue(ANDROID_SCHEMA, "defaultValue");

		// get the locked values and it's dependency keys arrays from the custom
		// attributes (see attrs.xml)
		TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomListPreference);
		lockedValues = typedArray.getTextArray(R.styleable.CustomListPreference_lockedValues);
		lockedValuesDependencyKeys = typedArray.getTextArray(R.styleable.CustomListPreference_lockedValuesDependencyKeys);
        dynamicEntriesProviderName = typedArray.getString(R.styleable.CustomListPreference_dynamicEntriesProvider);
        dynamicEntryValuesProviderName = typedArray.getString(R.styleable.CustomListPreference_dynamicEntryValuesProvider);
		typedArray.recycle();

        if (dynamicEntriesProviderName != null && dynamicEntryValuesProviderName != null) {
            initDynamicProviders();
        }

		mContext = context;
		mInflater = LayoutInflater.from(context);
		mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		//load the persisted values for caching
		persistedValue();
	}

    private void initDynamicProviders () {
        try {
            Class<IDynamicProvider> dynamicEntriesProviderClass = (Class<IDynamicProvider>) Class.forName(dynamicEntriesProviderName);
            Class<IDynamicProvider> dynamicEntryValuesProviderClass = (Class<IDynamicProvider>) Class.forName(dynamicEntryValuesProviderName);

            try {
                mDynamicProvider = dynamicEntriesProviderClass.getDeclaredConstructor().newInstance();
                mDynamicEntryValuesProvider = dynamicEntryValuesProviderClass.getDeclaredConstructor().newInstance();

                processDynamicEntries();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

        } catch (ClassNotFoundException e) {
            Log.e(TAG, String.format("Could not find class '%s'. Will not load dynamic providers!", dynamicEntriesProviderName));
        } catch (InstantiationException e) {
            Log.e(TAG, e.getMessage());
        } catch (IllegalAccessException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void processDynamicEntries () {
        mEntries = getEntries();
        mEntryValues = getEntryValues();

        if (mDynamicProvider != null && mDynamicEntryValuesProvider != null) {
            //first populate the items on each provider
            mDynamicEntryValuesProvider.populate();
            mDynamicProvider.populate();
            List<String> entries = mDynamicProvider.getItems();
            List<String> entryValues = mDynamicEntryValuesProvider.getItems();

            if (entries != null && entryValues != null && !entries.isEmpty() && !entryValues.isEmpty()) {
                CharSequence[] dynamicEntries = entries.toArray(new CharSequence[entries.size()]);
                CharSequence[] dynamicEntryValues = entryValues.toArray(new CharSequence[entryValues.size()]);

                //if either of the android attributes for specifying the entries and their values have been left empty, then ignore both and use only the dynamic providers
                if (mEntries == null || mEntryValues == null) {
                    mEntries = dynamicEntries;
                    mEntryValues = dynamicEntryValues;
                } else {
                    CharSequence[] fullEntriesList = new CharSequence[mEntries.length + dynamicEntries.length];
                    CharSequence[] fullEntryValuesList = new CharSequence[mEntryValues.length + dynamicEntryValues.length];

                    int i = 0, j = 0;
                    for (i = 0 ; i <= mEntries.length - 1 ; i++) {
                        fullEntriesList[i] = mEntries[i];
                        fullEntryValuesList[i] = mEntryValues[i];
                    }

                    for (i = mEntries.length, j = 0 ; j <= dynamicEntries.length - 1 ; i++, j++) {
                        fullEntriesList[i] = dynamicEntries[j];
                        fullEntryValuesList[i] = dynamicEntryValues[j];
                    }
                    //replace the entries and entryValues arrays with the new lists
                    mEntries = fullEntriesList;
                    mEntryValues = fullEntryValuesList;

                    setEntries(mEntries);
                    setEntryValues(mEntryValues);

                    dynamicEntries = null;
                    dynamicEntryValues = null;
                    fullEntriesList = null;
                    fullEntryValuesList = null;
                }
            }
            entries = null;
            entryValues = null;
        }
    }

    @Override
	protected void onPrepareDialogBuilder (Builder builder) {
		mEntries = getEntries();
		mEntryValues = getEntryValues();

		if (mEntries == null || mEntryValues == null || mEntries.length != mEntryValues.length) {
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
				setTypedValue((String) mEntryValues[mSelectedItemIndex]);

				dialog.dismiss();
			}
		});
	}

	/**
	 * Override this in your extension to show your own dialog and also to
	 * implement an action for the Buy button.
	 */
	@Override
	public void showPurchaseDialog () {
		new AlertDialog.Builder(mContext).setTitle(purchaseDialogTitleResId).setMessage(purchaseDialogMessageResId).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
			public void onClick (DialogInterface dialog, int which) {
				dialog.cancel();
			}
		}).setPositiveButton(R.string.buy, new DialogInterface.OnClickListener() {
			public void onClick (DialogInterface dialog, int which) {
				dialog.dismiss();
				// TODO: implement me! (if you want to wire up the buy action to
				// something you want you must override this method in your
				// extended class)
			}
		}).create().show();
	}
	
	private void persistedValue () {
		if (TextUtils.isEmpty(mDefaultValue)) {
			mPersistedStringValue = mPrefs.getString(mKey, mDefaultValue);
			
			return;
		}
		
		try {
			switch (mValuesDataType) {
			case INT:
				mPersistedIntValue = mPrefs.getInt(mKey, Integer.valueOf(mDefaultValue));
				break;
			case FLOAT:
				mPersistedFloatValue = mPrefs.getFloat(mKey, Float.valueOf(mDefaultValue));
				break;
			case LONG:
				mPersistedLongValue = mPrefs.getLong(mKey, Long.valueOf(mDefaultValue));
				break;
			case 0:
			default:
				mPersistedStringValue = mPrefs.getString(mKey, mDefaultValue);
				break;
			}
		}
		catch (NumberFormatException ex) {
			Log.e(TAG, String.format("Could not parse default value in to selected data type. Falling back to string."));

			mPersistedStringValue = getPersistedString(mDefaultValue);
		}
	}

	private class CustomListPreferenceAdapter extends BaseAdapter {

		public CustomListPreferenceAdapter(Context context) {

		}

		public int getCount () {
			return mEntries.length;
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

			/**
			 * Check if this option is locked using the lockedValues[] and
			 * lockedValuesDependencyKeys[] to verify if it should remain
			 * locked.
			 * 
			 * Convert value according to data type and compare
			 */
			// TODO can this be improved?
			if (lockedValues != null && lockedValuesDependencyKeys != null) {
				boolean lockedValueDependencyKeyValue = false;

				for (int i = 0; i < lockedValues.length; i++) {
					lockedValueDependencyKeyValue = mPrefs.getBoolean(lockedValuesDependencyKeys[i].toString(), false);

					// In each case, check if the dependency value is true,
					// otherwise set this row to false
					switch (mValuesDataType) {
					case INT:
						if (Integer.valueOf((String) mEntryValues[position]) == Integer.valueOf(lockedValues[i].toString())) {
							if (!lockedValueDependencyKeyValue) {
								locked = true;
							}
						}
						break;
					case FLOAT:
						if (Float.valueOf((String) mEntryValues[position]) == Float.valueOf(lockedValues[i].toString())) {
							if (!lockedValueDependencyKeyValue) {
								locked = true;
							}
						}
						break;
					case LONG:
						if (Long.valueOf((String) mEntryValues[position]) == Long.valueOf(lockedValues[i].toString())) {
							if (!lockedValueDependencyKeyValue) {
								locked = true;
							}
						}
						break;
					case 0:
					default:
						if (mEntryValues[position].equals(lockedValues[i].toString())) {
							if (!lockedValueDependencyKeyValue) {
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
					public void onClick (View v) {
						getDialog().dismiss();

						// show the buy dialog
						showPurchaseDialog();
					}
				});
			}
			else {
				row.setOnClickListener(new View.OnClickListener() {
					public void onClick (View v) {
						mSelectedItemIndex = position;
						setTypedValue((String) mEntryValues[position]);

						getDialog().dismiss();
					}
				});
			}

			return row;
		}

		private void setTextAndRadioButton (View row, int position) {
			mTextView = (TextView) row.findViewById(R.id.custom_list_view_row_text_view);
			mRadioButton = (RadioButton) row.findViewById(R.id.custom_list_view_row_radio_button);
			mTextView.setText(mEntries[position]);
			mRadioButton.setId(position);

			// is the mKey for this preference persisted?
			if (mPrefs.contains(mKey)) {
				//preferences contains this key but is the value blank since you could potentially have a row with value as ""
				if (mPersistedStringValue != null && mPersistedStringValue.equals("")) {
					if (mEntryValues[position].equals(mPersistedStringValue)) {
						mRadioButton.setChecked(true);
						mSelectedItemIndex = position;

						setTypedValue((String) mEntryValues[position]);
					}
				}
				else {
					// if yes, convert value according to data type and compare to see if this current row is the persisted value
					switch (mValuesDataType) {
					case INT:
						if (Integer.valueOf((String) mEntryValues[position]) == mPersistedIntValue) {
							mRadioButton.setChecked(true);
							mSelectedItemIndex = position;
						}
						break;
					case FLOAT:
						if (Float.valueOf((String) mEntryValues[position]) == mPersistedFloatValue) {
							mRadioButton.setChecked(true);
							mSelectedItemIndex = position;
						}
						break;
					case LONG:
						if (Long.valueOf((String) mEntryValues[position]) == mPersistedLongValue) {
							mRadioButton.setChecked(true);
							mSelectedItemIndex = position;
						}
						break;
					case 0:
					default:
						if (mEntryValues[position].equals(mPersistedStringValue)) {
							mRadioButton.setChecked(true);
							mSelectedItemIndex = position;
						}
						break;
					}
				}
			}
			// otherwise, just check if the value in this position is the default value and set it in the preferences
			else {
				if (mEntryValues[position].equals(mDefaultValue)) {
					mRadioButton.setChecked(true);
					mSelectedItemIndex = position;

					setTypedValue((String) mEntryValues[position]);
				}
			}

			mRadioButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged (CompoundButton buttonView, boolean isChecked) {
					if (isChecked) {
						int index = buttonView.getId();
						mSelectedItemIndex = index;
						setTypedValue((String) mEntryValues[index]);

						getDialog().dismiss();
					}
				}
			});
		}

		private void lockRow (View row) {
			mRadioButton.setClickable(false);
			
			TextView lockedItem = (TextView) row.findViewById(R.id.locked_item);
			lockedItem.setVisibility(View.VISIBLE);
			
			mRadioButton.setVisibility(View.GONE);
		}
	}

	private void setTypedValue (String value) {
		SharedPreferences.Editor editor = mPrefs.edit();
		
		//if the value is empty, set the value as a string and return
		if (TextUtils.isEmpty(value)) {
			editor.putString(mKey, value);
			
			mPersistedStringValue = value;
			
			return;
		}
		
		try {
			switch (mValuesDataType) {
			case INT:
				int _intValue = Integer.valueOf(value);
				
				editor.putInt(mKey, _intValue);
				
				mPersistedIntValue = _intValue;
				break;
			case FLOAT:
				float _floatValue = Float.valueOf(value);
				
				editor.putFloat(mKey, _floatValue);
				
				mPersistedFloatValue = _floatValue;
				break;
			case LONG:
				long _longValue = Long.valueOf(value);
				
				editor.putLong(mKey, _longValue);
				
				mPersistedLongValue = _longValue;
				break;
			case 0:
			default:
				editor.putString(mKey, value);
				
				mPersistedStringValue = value;
				
				break;
			}
		}
		catch (NumberFormatException ex) {
			Log.e(TAG, String.format("Could not parse value in to selected data type. Falling back to storing as a string."));

			editor.putString(mKey, value);
			
			mPersistedStringValue = value;
		}
		finally {
			//commit right away!
			editor.commit();
		}
	}
}
