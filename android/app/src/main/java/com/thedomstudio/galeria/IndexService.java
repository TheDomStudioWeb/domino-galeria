package com.thedomstudio.galeria;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Indexa la galería en segundo plano (servicio en primer plano, sigue con la pantalla apagada):
 * recorre las carpetas, descarta repetidas y, con ML Kit en el teléfono, guarda por foto
 * las caras, los objetos y el texto que se lee. Se puede pausar y retoma donde quedó.
 */
public class IndexService extends Service {

    public static final String CANAL = "indexado";
    static final int MIN_KB = 30;
    static final int HILOS = 2;

    public static volatile boolean corriendo = false;
    public static volatile boolean pausar = false;
    public static volatile String fase = "Sin empezar";
    public static volatile int hechas = 0, total = 0;
    public static volatile long inicioAnalisis = 0;
    public static volatile String error = "";

    private PowerManager.WakeLock wl;

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        crearCanal();
        Notification n = notif("Preparando el índice…");
        if (Build.VERSION.SDK_INT >= 29) startForeground(7, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(7, n);
        if (!corriendo) {
            corriendo = true; pausar = false; error = "";
            try {
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "galeria:indexado");
                wl.acquire(6 * 60 * 60 * 1000L);
            } catch (Throwable ignored) { }
            new Thread(this::trabajo, "indexado").start();
        }
        return START_NOT_STICKY;
    }

    /** Android 15 limita este tipo de servicio a unas 6 h por día: se pausa y se retoma después. */
    public void onTimeout(int startId, int fgsType) { pausar = true; }

    private void crearCanal() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CANAL, "Indexado de fotos", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    private Notification notif(String texto) {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CANAL) : new Notification.Builder(this);
        return b.setContentTitle("DOMino Galería").setContentText(texto)
            .setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).build();
    }

    private void avisar(String texto) {
        try { ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(7, notif(texto)); } catch (Throwable ignored) { }
    }

    /* ---------------- Trabajo ---------------- */

    private void trabajo() {
        try {
            SQLiteDatabase db = GaleriaDb.get(this).getWritableDatabase();
            fase = "Recorriendo carpetas"; avisar(fase);
            recorrerYRegistrar(db);
            if (pausar) { fase = "Pausado"; return; }
            fase = "Buscando fotos repetidas"; avisar(fase);
            repetidasExactas(db);
            if (pausar) { fase = "Pausado"; return; }
            analizar(db);
            if (pausar) { fase = "Pausado"; return; }
            fase = "Buscando fotos casi iguales"; avisar(fase);
            repetidasParecidas(db);
            fase = "Listo";
        } catch (Throwable e) {
            error = String.valueOf(e.getMessage());
            fase = "Con error";
        } finally {
            corriendo = false;
            try { if (wl != null && wl.isHeld()) wl.release(); } catch (Throwable ignored) { }
            avisar(fase);
            stopForeground(false);
            stopSelf();
        }
    }

    private static boolean esImagen(String n) {
        String s = n.toLowerCase(Locale.ROOT);
        return s.endsWith(".jpg") || s.endsWith(".jpeg") || s.endsWith(".png") || s.endsWith(".webp") || s.endsWith(".heic");
    }

    static String origen(File f) {
        String base = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        String p = f.getAbsolutePath().replace(base, "");
        String[] t = p.split("/");
        if (p.toLowerCase(Locale.ROOT).contains("whatsapp")) return "WhatsApp";
        if (t.length >= 2 && t[0].equals("DCIM")) return "Cámara (" + t[1] + ")";
        return t.length >= 2 ? t[0] + "/" + t[1] : t[0];
    }

    private static final Pattern FECHA_NOMBRE = Pattern.compile("(20\\d{2})[-_]?(\\d{2})[-_]?(\\d{2})");

    static long fechaDe(File f) {
        try {
            ExifInterface ex = new ExifInterface(f.getAbsolutePath());
            String d = ex.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            if (d == null) d = ex.getAttribute(ExifInterface.TAG_DATETIME);
            if (d != null) {
                java.util.Date dt = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(d.trim());
                if (dt != null && dt.getTime() > 946684800000L) return dt.getTime();
            }
        } catch (Throwable ignored) { }
        Matcher m = FECHA_NOMBRE.matcher(f.getName());
        if (m.find()) {
            try {
                int a = Integer.parseInt(m.group(1)), me = Integer.parseInt(m.group(2)), di = Integer.parseInt(m.group(3));
                if (me >= 1 && me <= 12 && di >= 1 && di <= 31) {
                    java.util.Calendar c = java.util.Calendar.getInstance();
                    c.clear(); c.set(a, me - 1, di, 12, 0, 0);
                    return c.getTimeInMillis();
                }
            } catch (Throwable ignored) { }
        }
        return f.lastModified();
    }

    private void recorrer(File dir, List<File> out) {
        if (pausar) return;
        File[] hijos = dir.listFiles();
        if (hijos == null) return;
        for (File f : hijos) {
            String n = f.getName();
            if (f.isDirectory()) {
                if (n.startsWith(".")) continue;
                if (n.equalsIgnoreCase("Stickers") || n.equalsIgnoreCase("WhatsApp Stickers") || n.equalsIgnoreCase("WhatsApp Profile Photos")) continue;
                String ruta = f.getAbsolutePath();
                if (ruta.endsWith("/Android/data") || ruta.endsWith("/Android/obb")) continue;
                if (n.equals(GaleriaPlugin.CARPETA_EXPORT)) continue;
                recorrer(f, out);
            } else if (!n.startsWith(".") && ((esImagen(n) && f.length() >= MIN_KB * 1024L) || (Img.esVideo(n) && f.length() >= 200 * 1024L))) {
                out.add(f);
            }
        }
    }

    private void recorrerYRegistrar(SQLiteDatabase db) {
        List<File> halladas = new ArrayList<>();
        recorrer(Environment.getExternalStorageDirectory(), halladas);
        if (pausar) return;
        Set<String> conocidas = new HashSet<>();
        Cursor c = db.rawQuery("SELECT ruta FROM fotos", null);
        while (c.moveToNext()) conocidas.add(c.getString(0));
        c.close();
        Set<String> actuales = new HashSet<>();
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            for (File f : halladas) {
                String ruta = f.getAbsolutePath();
                actuales.add(ruta);
                if (conocidas.contains(ruta)) continue;
                cv.clear();
                cv.put("ruta", ruta); cv.put("nombre", f.getName()); cv.put("size", f.length());
                cv.put("mod", f.lastModified()); cv.put("fecha", fechaDe(f)); cv.put("origen", origen(f));
                cv.put("tipo", Img.esVideo(f.getName()) ? 1 : 0);
                db.insertWithOnConflict("fotos", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            }
            // fotos que ya no están en el teléfono
            for (String r : conocidas) if (!actuales.contains(r)) db.delete("fotos", "ruta=? AND borrada=0", new String[]{r});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static String sha(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(f)) {
            byte[] b = new byte[64 * 1024]; int n;
            while ((n = in.read(b)) > 0) md.update(b, 0, n);
        }
        StringBuilder sb = new StringBuilder();
        for (byte x : md.digest()) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    /** Fotos idénticas byte a byte: el hash solo se calcula donde coincide el tamaño. */
    private void repetidasExactas(SQLiteDatabase db) {
        List<Long> tamanos = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT size FROM fotos WHERE dup=0 GROUP BY size HAVING COUNT(*)>1", null);
        while (c.moveToNext()) tamanos.add(c.getLong(0));
        c.close();
        for (Long t : tamanos) {
            if (pausar) return;
            List<long[]> ids = new ArrayList<>();
            List<String> rutas = new ArrayList<>(), hashes = new ArrayList<>();
            Cursor q = db.rawQuery("SELECT id, ruta, hash, mod FROM fotos WHERE dup=0 AND size=? ORDER BY mod", new String[]{String.valueOf(t)});
            while (q.moveToNext()) { ids.add(new long[]{q.getLong(0)}); rutas.add(q.getString(1)); hashes.add(q.getString(2)); }
            q.close();
            Map<String, Long> vistos = new HashMap<>();
            for (int i = 0; i < ids.size(); i++) {
                String h = hashes.get(i);
                if (h == null) {
                    try { h = sha(new File(rutas.get(i))); } catch (Exception e) { continue; }
                    ContentValues cv = new ContentValues(); cv.put("hash", h);
                    db.update("fotos", cv, "id=?", new String[]{String.valueOf(ids.get(i)[0])});
                }
                if (vistos.containsKey(h)) {
                    ContentValues cv = new ContentValues(); cv.put("dup", 1); cv.put("orig", vistos.get(h));
                    db.update("fotos", cv, "id=?", new String[]{String.valueOf(ids.get(i)[0])});
                } else vistos.put(h, ids.get(i)[0]);
            }
        }
    }

    /* ---------------- Análisis con ML Kit ---------------- */

    private void analizar(SQLiteDatabase db) {
        List<long[]> idsL = new ArrayList<>();
        List<String> rutas = new ArrayList<>();
        final List<Boolean> esVid = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT id, ruta, tipo FROM fotos WHERE estado=0 AND dup=0 ORDER BY id", null);
        while (c.moveToNext()) { idsL.add(new long[]{c.getLong(0)}); rutas.add(c.getString(1)); esVid.add(c.getInt(2) == 1); }
        c.close();
        final int n = rutas.size();
        total = n; hechas = 0; inicioAnalisis = System.currentTimeMillis();
        fase = "Analizando fotos"; avisar("Analizando " + n + " fotos…");
        if (n == 0) return;

        final FaceDetector caras = FaceDetection.getClient(new FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setMinFaceSize(0.04f).build());
        final ImageLabeler etiquetas = ImageLabeling.getClient(new ImageLabelerOptions.Builder().setConfidenceThreshold(0.5f).build());
        final TextRecognizer ocr = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        final AtomicInteger sig = new AtomicInteger(0);
        final AtomicInteger cont = new AtomicInteger(0);
        Thread[] hilos = new Thread[HILOS];
        for (int h = 0; h < HILOS; h++) {
            hilos[h] = new Thread(() -> {
                while (!pausar) {
                    int i = sig.getAndIncrement();
                    if (i >= n) return;
                    long id = idsL.get(i)[0];
                    ContentValues cv = new ContentValues();
                    try {
                        int[] dims = new int[2];
                        final boolean video = esVid.get(i);
                        List<Bitmap> bs = new ArrayList<>();
                        long[] info = new long[3];
                        if (video) { bs = Img.fotogramas(rutas.get(i), 800, 3, info); dims[0] = (int) info[0]; dims[1] = (int) info[1]; }
                        else { Bitmap b0 = Img.cargar(rutas.get(i), 800, dims); if (b0 != null) bs.add(b0); }
                        if (bs.isEmpty()) { cv.put("estado", 2); if (video) cv.put("dur", info[2]); }
                        else {
                            int nCaras = 0; long dh = 0;
                            java.util.LinkedHashSet<String> ets = new java.util.LinkedHashSet<>();
                            StringBuilder tx = new StringBuilder();
                            for (int k = 0; k < bs.size(); k++) {
                                Bitmap b = bs.get(k);
                                InputImage img = InputImage.fromBitmap(b, 0);
                                try { nCaras = Math.max(nCaras, Tasks.await(caras.process(img)).size()); } catch (Throwable ignored) { }
                                try { for (ImageLabel l : Tasks.await(etiquetas.process(img))) ets.add(l.getText().toLowerCase(Locale.ROOT)); } catch (Throwable ignored) { }
                                try { String t1 = Tasks.await(ocr.process(img)).getText(); if (t1 != null && !t1.isEmpty()) { if (tx.length() > 0) tx.append(' '); tx.append(t1); } } catch (Throwable ignored) { }
                                if (k == 0 && !video) dh = Img.huella(b);
                                b.recycle();
                            }
                            if (video) ets.add("video");
                            String limpio = Img.norm(tx.toString()).replaceAll("\\s+", " ").trim();
                            cv.put("estado", 1); cv.put("w", dims[0]); cv.put("h", dims[1]);
                            if (!video) cv.put("dh", dh); else cv.put("dur", info[2]);
                            cv.put("caras", nCaras); cv.put("etiquetas", android.text.TextUtils.join(",", ets));
                            cv.put("texto", limpio.length() > 600 ? limpio.substring(0, 600) : limpio);
                            cv.put("textoc", limpio.replace(" ", ""));
                        }
                    } catch (Throwable e) { cv.put("estado", 2); }
                    db.update("fotos", cv, "id=?", new String[]{String.valueOf(id)});
                    int hh = cont.incrementAndGet();
                    hechas = hh;
                    if (hh % 25 == 0) avisar("Analizadas " + hh + " de " + n);
                }
            }, "analisis-" + h);
            hilos[h].start();
        }
        for (Thread t : hilos) { try { t.join(); } catch (InterruptedException ignored) { } }
        try { caras.close(); etiquetas.close(); ocr.close(); } catch (Throwable ignored) { }
    }

    /* ---------------- Casi iguales: huella visual por bandas de 16 bits ---------------- */

    private void repetidasParecidas(SQLiteDatabase db) {
        List<long[]> datos = new ArrayList<>();   // id, dh, size, w, h
        Cursor c = db.rawQuery("SELECT id, dh, size, w, h FROM fotos WHERE estado=1 AND dup=0 AND tipo=0 AND dh IS NOT NULL", null);
        while (c.moveToNext()) datos.add(new long[]{c.getLong(0), c.getLong(1), c.getLong(2), c.getLong(3), c.getLong(4)});
        c.close();
        int n = datos.size();
        Map<Integer, List<Integer>> cubos = new HashMap<>();
        for (int i = 0; i < n; i++) {
            long dh = datos.get(i)[1];
            for (int b = 0; b < 4; b++) {
                int clave = (b << 16) | (int) ((dh >>> (16 * b)) & 0xFFFF);
                List<Integer> l = cubos.get(clave);
                if (l == null) { l = new ArrayList<>(); cubos.put(clave, l); }
                l.add(i);
            }
        }
        boolean[] dup = new boolean[n];
        int[] quien = new int[n];
        for (List<Integer> l : cubos.values()) {
            int m = Math.min(l.size(), 300);
            for (int a = 0; a < m; a++) {
                int i = l.get(a);
                if (dup[i]) continue;
                for (int bb = a + 1; bb < m; bb++) {
                    int j = l.get(bb);
                    if (dup[j] || dup[i]) continue;
                    long[] x = datos.get(i), y = datos.get(j);
                    if (Long.bitCount(x[1] ^ y[1]) > 4) continue;
                    double ax = x[4] == 0 ? 1 : (double) x[3] / x[4], ay = y[4] == 0 ? 1 : (double) y[3] / y[4];
                    if (Math.abs(ax - ay) / Math.max(ax, ay) > 0.04) continue;
                    if (x[2] >= y[2]) { dup[j] = true; quien[j] = i; } else { dup[i] = true; quien[i] = j; }      // queda la más pesada
                }
            }
        }
        db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            for (int i = 0; i < n; i++) if (dup[i]) {
                cv.clear(); cv.put("dup", 1); cv.put("orig", datos.get(quien[i])[0]);
                db.update("fotos", cv, "id=?", new String[]{String.valueOf(datos.get(i)[0])});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
