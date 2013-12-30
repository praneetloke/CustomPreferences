package com.myappfactory.interfaces;

import java.util.List;

/**
 * Created by admin on 12/29/13.
 */
public interface DynamicEntryValuesProvider {
    public int getCount ();
    public List<String> getItems ();
    public void populate ();
}
