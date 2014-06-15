package com.myappfactory.interfaces;

import android.content.Context;

import java.util.List;

/**
 * Created by admin on 12/29/13.
 */
public interface IDynamicProvider {
    public int getCount ();
    public <T> List<T> getItems ();
    public void populate ();
    public void populate (Context context);
}
