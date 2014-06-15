package com.myappfactory.preferences;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import android.util.Log;

import com.myappfactory.interfaces.IDynamicProvider;
import com.myappfactory.listpreference.R;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ploke on 6/15/14.
 */
@TargetApi(11)
public class CustomMultiSelectListPreference extends MultiSelectListPreference {
    private static final String TAG = CustomMultiSelectListPreference.class.getName();

    private String dynamicEntriesProviderName;
    private String dynamicEntryValuesProviderName;
    private boolean selectAllValuesByDefault;

    private IDynamicProvider mDynamicEntriesProvider;
    private IDynamicProvider mDynamicEntryValuesProvider;

    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;

    public CustomMultiSelectListPreference (Context context, AttributeSet attrs) {
        super(context, attrs);

        // get the locked values and it's dependency keys arrays from the custom
        // attributes (see attrs.xml)
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CustomListPreference);
        dynamicEntriesProviderName = typedArray.getString(R.styleable.CustomListPreference_dynamicEntriesProvider);
        dynamicEntryValuesProviderName = typedArray.getString(R.styleable.CustomListPreference_dynamicEntryValuesProvider);
        selectAllValuesByDefault = typedArray.getBoolean(R.styleable.CustomListPreference_selectAllValuesByDefault, false);
        typedArray.recycle();

        if (dynamicEntriesProviderName != null && dynamicEntryValuesProviderName != null) {
            initDynamicProviders(context);
        }
    }

    public CustomMultiSelectListPreference (Context context) {
        super(context);
    }

    private void initDynamicProviders (Context context) {
        try {
            Class<IDynamicProvider> dynamicEntriesProviderClass = (Class<IDynamicProvider>) Class.forName(dynamicEntriesProviderName);
            Class<IDynamicProvider> dynamicEntryValuesProviderClass = (Class<IDynamicProvider>) Class.forName(dynamicEntryValuesProviderName);

            try {
                mDynamicEntriesProvider = dynamicEntriesProviderClass.getDeclaredConstructor().newInstance();
                mDynamicEntryValuesProvider = dynamicEntryValuesProviderClass.getDeclaredConstructor().newInstance();

                processDynamicEntries(context);
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

    private void processDynamicEntries (Context context) {
        mEntries = getEntries();
        mEntryValues = getEntryValues();

        if (mDynamicEntriesProvider != null && mDynamicEntryValuesProvider != null) {
            //first populate the items on each provider
            mDynamicEntryValuesProvider.populate(context);
            mDynamicEntriesProvider.populate(context);
            List<String> entries = mDynamicEntriesProvider.getItems();
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
                }
            }
        }
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        if (selectAllValuesByDefault || mEntryValues != null) {
            final int valueCount = mEntryValues.length;
            final Set<String> result = new HashSet<String>();

            for (int i = 0; i < valueCount; i++) {
                result.add(mEntryValues[i].toString());
            }

            setValues(restoreValue ? result : (Set<String>) defaultValue);

            return;
        }

        super.onSetInitialValue(restoreValue, defaultValue);
    }
}
