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

    private GaleriaDb(Context c) { super(c, "galeria.db", null, 3); }

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
            + "marca TEXT,"                    // entrega | plan | descartar | NULL
            + "orig INTEGER,"                  // si es repetida: id de la foto que se queda como original
            + "borrada INTEGER DEFAULT 0,"     // 1 = está en la papelera de la app
            + "ruta_orig TEXT,"                // de dónde vino, para restaurarla
            + "tipo INTEGER DEFAULT 0,"        // 0 foto, 1 video
            + "dur INTEGER DEFAULT 0)");       // duración del video en milisegundos
        db.execSQL("CREATE INDEX i_estado ON fotos(estado, dup)");
        db.execSQL("CREATE INDEX i_marca ON fotos(marca)");
        db.execSQL("CREATE INDEX i_fecha ON fotos(fecha)");
        db.execSQL("CREATE INDEX i_size ON fotos(size)");
        db.execSQL("CREATE INDEX i_borrada ON fotos(borrada)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {
        if (o < 2) {
            db.execSQL("ALTER TABLE fotos ADD COLUMN orig INTEGER");
            db.execSQL("ALTER TABLE fotos ADD COLUMN borrada INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE fotos ADD COLUMN ruta_orig TEXT");
            db.execSQL("CREATE INDEX IF NOT EXISTS i_borrada ON fotos(borrada)");
        }
        if (o < 3) {
            db.execSQL("ALTER TABLE fotos ADD COLUMN tipo INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE fotos ADD COLUMN dur INTEGER DEFAULT 0");
        }
    }
}
