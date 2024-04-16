package com.bible.tools.reader;

import java.io.Serializable;

import androidx.annotation.Nullable;

public abstract class MVersion implements Serializable {

    public String locale;
    public String shortName;
    public String longName;
    public String description;
    public int ordering;

    /**
     * unique id for comparison purposes
     */
    public abstract String getVersionId();

    /**
     * return version so that it can be read. Null when not possible
     */
    @Nullable
    public abstract Version getVersion();

    public abstract boolean getActive();

    public abstract boolean hasDataFile();
}
