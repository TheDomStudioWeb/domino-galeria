package com.thedomstudio.galeria;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/** Índice local de fotos: una fila por foto, con lo que se detectó en ella. Vive solo en el teléfono. */
public class GaleriaDb extends SQLiteOpenHelper {

    private static GaleriaDb inst;

    public static synchronized GaleriaDb get(Context c) {
        if (inst == null) inst = new GaleriaDb(c.getApplicationContext());
        return inst;
    }

    private GaleriaDb(Context c) { super(c, "galeria.db", null, 1); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE fotos ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "ruta TEXT UNIQUE NOT NULL,"
            + "nombre TEXT, size INTEGER, mod INTEGER, fecha INTEGER, origen TEXT,"
            + "w INTEGER DEFAULT 0, h INTEGER DEFAULT 0, dh INTEGER, hash TEXT,"
            + "estado INTEGER DEFAULT 0,"      // 0 pendiente, 1 analizada, 2 con error
            + "dup INTEGER DEFAULT 0,"         // 1 = repetida de otra (no se muestra)
            + "caras INTEGER DEFAULT 0,"
            + "etiquetas TEXT DEFAULT '', texto TEXT DEFAULT '', textoc TEXT DEFAULT '',"
            + "marca TEXT)");                  // entrega | plan | descartar | NULL
        db.execSQL("CREATE INDEX i_estado ON fotos(estado, dup)");
        db.execSQL("CREATE INDEX i_marca ON fotos(marca)");
        db.execSQL("CREATE INDEX i_fecha ON fotos(fecha)");
        db.execSQL("CREATE INDEX i_size ON fotos(size)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) { }
}
