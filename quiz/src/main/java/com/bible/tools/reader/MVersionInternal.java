package com.bible.tools.reader;

/**
 * Internal version, only one
 */
public class MVersionInternal extends MVersion {
    public static final int DEFAULT_ORDERING = 1;

    public static String getVersionInternalId() {
        return "bible_en";
    }

    @Override
    public String getVersionId() {
        return getVersionInternalId();
    }

    @Override
    public Version getVersion() {
        return null;//VersionImpl.getInternalVersion();
    }

    @Override
    public boolean getActive() {
        return true; // always active
    }

    @Override
    public boolean hasDataFile() {
        return true; // always has
    }
}
