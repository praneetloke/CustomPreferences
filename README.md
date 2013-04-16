![](http://imageshack.us/scaled/medium/259/device20130415220640.png)

![](http://imageshack.us/scaled/medium/853/device20130415220617.png)

###What is this?###
CustomPreferences is collection of (right now, only two) Android preference classes that provide common features that are missing from the standard list of preference classes available in the SDK.

###What does it do now?###
- CustomListPreference
    - Allows locking individual list items.
    - Store list items in a type you want so you don't have to manually type cast the values when retrieving from sharedpreferences every time.
    - Locked items will unlock using a dependency key check.
    - Show a custom dialog when user selects a locked item.
- TimePreference
    - Shows the standard OS time picker.

###Usage###
First, declare a custom namespace in your preferences xml root node like this:
```
xmlns:customPreference="http://schemas.android.com/apk/res-auto"
```

####CustomListPreference####
(Optional) Extend the CustomListPreference and override the default behavior for the locked item purchase request dialog
```
public class MyExtendedCustomListPreference extends CustomListPreference {

    public SquigglyListPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public int purchaseDialogTitleResId = R.string.locationSentryIAPTitle;
	public int purchaseDialogMessageResId = R.string.locationSentryIAPMessage;
	
	@Override
	public void showPurchaseDialog () {
		new AlertDialog.Builder(getContext()).setTitle(purchaseDialogTitleResId).setMessage(purchaseDialogMessageResId).setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		}).setPositiveButton(R.string.buy, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				Intent intent = new Intent();
				// launch the activity that handles your in-app purchase
				intent.setClassName(getContext(), SquigglyInAppPurchase.class.getName());
				getContext().startActivity(intent);
			}
		}).create().show();
	}
}
```

Then, you can do this:

```
<com.mypackage.preferences.MyExtendedCustomListPreference android:key="@string/myPreferenceKey"
            android:title="@string/myTitle"
            android:summary="@string/mySummary"
            android:entries="@array/myEntries"
            android:entryValues="@array/myValues"
            android:defaultValue="my_default_value_as_string"
            customPreference:lockedValues="@array/myArrayOfLockedValues"
            customPreference:lockedValuesDependencyKeys="@array/myArrayOfLockedValuesDependencyKeyValues"
            valuesDataType="0"/>
```

...or if you do not extend the CustomListPreference class and would just want to use the defaults...
```
<com.myappfactory.preferences.CustomListPreference android:key="@string/myPreferenceKey"
            android:title="@string/myTitle"
            android:summary="@string/mySummary"
            android:entries="@array/myEntries"
            android:entryValues="@array/myValues"
            android:defaultValue="my_default_value_as_string"
            valuesDataType="0"/>
```

> *valuesDataType* can be 0 (String), 1 (Integer), 2 (Float), 3 (Long). This basically tells CustomListPreference to save the value of the selected list item as that type

####TimePreference####
```
<com.myappfactory.preferences.TimePreference android:key="@string/myOtherPreferenceKey"
                android:title="@string/myTitle"
	            android:dialogTitle="@string/myDialogTitle"
	            android:summary="@string/mySummary"
	            android:defaultValue="07:00"
	            android:layout="@layout/time_preference_layout"
	            android:paddingLeft="0dip"
	            android:textSize="18dp"/>
```

###What are the supported platform versions?###
I have tested these on Gingerbread, ICS and above.
