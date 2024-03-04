package com.raiadnan.quranreader.prayertimes.locations.helper;

import static android.database.sqlite.SQLiteDatabase.*;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.prayertimes.locations.model.City;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import timber.log.Timber;

public class CityDatabase extends SQLiteOpenHelper {
    private static final String DB_NAME = "cityData.db";
    private static String DB_PATH ;
    private static final int DB_VERSION = 1;
    private static CityDatabase cityDatabase;
    private SQLiteDatabase database;

    public void onCreate(SQLiteDatabase sQLiteDatabase) {
    }

    public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
    }

    public static CityDatabase get() {
        if (cityDatabase == null) {
            cityDatabase = new CityDatabase();
        }
        return cityDatabase;
    }

    private CityDatabase() {
        super(App.get(), DB_NAME, (CursorFactory) null, 1);
        DB_PATH = App.get().getDatabasePath(getDatabaseName()).getAbsolutePath();
        openDataBase();
    }

    private void openDataBase() throws SQLException {
        String str = DB_PATH + DB_NAME;
        if (this.database == null) {
            createDataBase();
            this.database = openDatabase(str, null, 0);
        }
    }

    private void createDataBase() {
        if (!checkDataBase()) {
            getReadableDatabase();
            try {
                copyDataBase();
            } catch (IOException unused) {
                Log.e(getClass().toString(), "Copying error");
                throw new Error("Error copying database!");
            }
        } else {
            Log.i(getClass().toString(), "Database already exists");
        }
    }

    private boolean checkDataBase() {
        SQLiteDatabase sQLiteDatabase = null;
        try {
            sQLiteDatabase = openDatabase(DB_PATH + DB_NAME, null, OPEN_READONLY);
        } catch (SQLException unused) {
            Timber.tag(getClass().toString()).e("Error while checking db");
        }
        if (sQLiteDatabase != null) {
            sQLiteDatabase.close();
        }
        if (sQLiteDatabase != null) {
            return true;
        }
        return false;
    }

    private void copyDataBase() throws IOException {
        InputStream open = App.get().getAssets().open(DB_NAME);
        FileOutputStream fileOutputStream = new FileOutputStream(DB_PATH + DB_NAME);
        byte[] bArr = new byte[1024];
        while (true) {
            int read = open.read(bArr);
            if (read > 0) {
                fileOutputStream.write(bArr, 0, read);
            } else {
                fileOutputStream.close();
                open.close();
                return;
            }
        }
    }

    public synchronized void close() {
        if (this.database != null) {
            this.database.close();
        }
        super.close();
    }

    public ArrayList<City> findAll() {
        ArrayList<City> arrayList = new ArrayList<>();
        Cursor rawQuery = this.database.rawQuery("select * from cities_info", null);
        if (rawQuery != null && rawQuery.moveToFirst()) {
            do {
                City city = new City();
                city.setId(rawQuery.getInt(0));
                city.setCity(rawQuery.getString(1));
                city.setTimeZone(Double.parseDouble(rawQuery.getString(2)));
                city.setCountry(rawQuery.getString(3));
                city.setLat(rawQuery.getString(4));
                city.setLon(rawQuery.getString(5));
                city.setTimeZoneName(rawQuery.getString(6));
                arrayList.add(city);
            } while (rawQuery.moveToNext());
            rawQuery.close();
        }
        return arrayList;
    }

    public ArrayList<City> search(String str) {
        ArrayList<City> arrayList = new ArrayList<>();
        SQLiteDatabase sQLiteDatabase = this.database;
        Cursor rawQuery = sQLiteDatabase.rawQuery("select * from cities_info WHERE city like ? or country like ? ", new String[]{"%" + str + "%", "%" + str + "%"});
        if (rawQuery != null && rawQuery.moveToFirst()) {
            do {
                City city = new City();
                city.setId(rawQuery.getInt(0));
                city.setCity(rawQuery.getString(1));
                city.setTimeZone(Double.parseDouble(rawQuery.getString(2)));
                city.setCountry(rawQuery.getString(3));
                city.setLat(rawQuery.getString(4));
                city.setLon(rawQuery.getString(5));
                city.setTimeZoneName(rawQuery.getString(6));
                arrayList.add(city);
            } while (rawQuery.moveToNext());
            rawQuery.close();
        }
        return arrayList;
    }
}
