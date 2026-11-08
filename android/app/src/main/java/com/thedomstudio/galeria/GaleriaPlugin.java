package com.thedomstudio.galeria;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Puente entre la pantalla y el índice local: indexar, buscar, marcar y exportar. */
@CapacitorPlugin(name = "Galeria")
public class GaleriaPlugin extends Plugin {

    public static final String CARPETA_EXPORT = "X5-Historia";
    public static final String CARPETA_GRUPOS = "DOMino-Carpetas";

    private SQLiteDatabase db() { return GaleriaDb.get(getContext()).getWritableDatabase(); }

    /* ---------------- Permiso ---------------- */

    private boolean tienePermiso() {
        if (Build.VERSION.SDK_INT >= 30) return Environment.isExternalStorageManager();
        return getContext().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            && getContext().checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    @PluginMethod
    public void permiso(PluginCall call) {
        JSObject o = new JSObject();
        o.put("ok", tienePermiso());
        call.resolve(o);
    }

    @PluginMethod
    public void pedirPermiso(PluginCall call) {
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                Intent i = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getContext().getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
            } else {
                getActivity().requestPermissions(new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4711);
            }
            call.resolve();
        } catch (Exception e) {
            call.reject(String.valueOf(e.getMessage()));
        }
    }

    /* ---------------- Indexado ---------------- */

    @PluginMethod
    public void iniciar(PluginCall call) {
        if (!tienePermiso()) { call.reject("SIN_PERMISO"); return; }
        try {
            if (Build.VERSION.SDK_INT >= 33
                && getContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                getActivity().requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 4712);
            }
            Intent i = new Intent(getContext(), IndexService.class);
            if (Build.VERSION.SDK_INT >= 26) getContext().startForegroundService(i); else getContext().startService(i);
            call.resolve();
        } catch (Throwable e) {
            call.reject("No se pudo iniciar: " + e.getMessage());
        }
    }

    @PluginMethod
    public void pausar(PluginCall call) {
        IndexService.pausar = true;
        call.resolve();
    }

    private int contar(String sql) {
        Cursor c = db().rawQuery(sql, null);
        int n = c.moveToFirst() ? c.getInt(0) : 0;
        c.close();
        return n;
    }

    @PluginMethod
    public void estado(PluginCall call) {
        JSObject o = new JSObject();
        o.put("corriendo", IndexService.corriendo);
        o.put("fase", IndexService.fase);
        o.put("hechas", IndexService.hechas);
        o.put("total", IndexService.total);
        o.put("error", IndexService.error);
        long t = IndexService.inicioAnalisis;
        o.put("segundos", IndexService.corriendo && t > 0 ? (System.currentTimeMillis() - t) / 1000 : 0);
        o.put("registradas", contar("SELECT COUNT(*) FROM fotos WHERE borrada=0"));
        o.put("repetidas", contar("SELECT COUNT(*) FROM fotos WHERE dup=1 AND borrada=0"));
        o.put("analizadas", contar("SELECT COUNT(*) FROM fotos WHERE estado=1 AND dup=0"));
        o.put("pendientes", contar("SELECT COUNT(*) FROM fotos WHERE estado=0 AND dup=0"));
        o.put("errores", contar("SELECT COUNT(*) FROM fotos WHERE estado=2 AND dup=0"));
        o.put("entregas", contar("SELECT COUNT(*) FROM fotos WHERE marca='entrega'"));
        o.put("planes", contar("SELECT COUNT(*) FROM fotos WHERE marca='plan'"));
        o.put("descartadas", contar("SELECT COUNT(*) FROM fotos WHERE marca='descartar' AND borrada=0"));
        o.put("papelera", contar("SELECT COUNT(*) FROM fotos WHERE borrada=1"));
        o.put("papeleraMB", contar("SELECT COALESCE(SUM(size)/1048576,0) FROM fotos WHERE borrada=1"));
        call.resolve(o);
    }

    /* ---------------- Búsqueda ---------------- */

    private static String likeEsc(String s) { return "%" + s.replace("%", "").replace("_", "") + "%"; }

    @PluginMethod
    public void buscar(PluginCall call) {
        try {
            StringBuilder w = new StringBuilder();
            List<String> args = new ArrayList<>();
            String carpeta = call.getString("carpeta", "");
            if (call.getBoolean("papelera", false)) w.append("borrada=1");
            else if (!carpeta.isEmpty()) {
                String pre = new File(gruposDir(), limpioNombre(carpeta)).getAbsolutePath() + "/";
                w.append("borrada=0 AND substr(ruta,1,").append(pre.length()).append(")=?");
                args.add(pre);
            } else if (call.getBoolean("todas", false)) w.append("dup=0 AND borrada=0");
            else w.append("estado=1 AND dup=0 AND borrada=0");

            JSArray grupos = call.getArray("grupos");
            if (grupos != null) {
                for (int g = 0; g < grupos.length(); g++) {
                    JSONArray terminos = grupos.optJSONArray(g);
                    if (terminos == null || terminos.length() == 0) continue;
                    w.append(" AND (");
                    for (int t = 0; t < terminos.length(); t++) {
                        String term = Img.norm(terminos.optString(t)).trim();
                        if (t > 0) w.append(" OR ");
                        if (term.equals("@persona")) { w.append("caras>0"); continue; }
                        if (term.equals("@sinpersona")) { w.append("caras=0"); continue; }
                        if (term.startsWith("#")) {   // etiqueta exacta (no por pedazos de palabra)
                            w.append("(',' || etiquetas || ',') LIKE ?");
                            args.add("%," + term.substring(1).replace("%", "").replace("_", "") + ",%");
                            continue;
                        }
                        String compacto = term.replace(" ", "");
                        w.append("(texto LIKE ? OR textoc LIKE ? OR etiquetas LIKE ? OR nombre LIKE ?)");
                        args.add(likeEsc(term)); args.add(likeEsc(compacto)); args.add(likeEsc(term)); args.add(likeEsc(term));
                    }
                    w.append(")");
                }
            }
            String origen = call.getString("origen", "");
            if ("whatsapp".equals(origen)) w.append(" AND origen LIKE 'WhatsApp%'");
            else if ("camara".equals(origen)) w.append(" AND origen LIKE 'C%mara%'");
            String tipo = call.getString("tipo", "");
            if ("foto".equals(tipo)) w.append(" AND tipo=0");
            else if ("video".equals(tipo)) w.append(" AND tipo=1");
            String marca = call.getString("marca", "");
            if ("sin".equals(marca)) w.append(" AND marca IS NULL");
            else if ("cualquiera".equals(marca)) w.append(" AND marca IS NOT NULL AND marca<>'descartar'");
            else if ("entrega".equals(marca) || "plan".equals(marca) || "descartar".equals(marca)) { w.append(" AND marca=?"); args.add(marca); }
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            String desde = call.getString("desde", ""), hasta = call.getString("hasta", "");
            if (!desde.isEmpty()) { w.append(" AND fecha>=?"); args.add(String.valueOf(f.parse(desde).getTime())); }
            if (!hasta.isEmpty()) { w.append(" AND fecha<=?"); args.add(String.valueOf(f.parse(hasta).getTime() + 86399999L)); }

            String[] a = args.toArray(new String[0]);
            Cursor cn = db().rawQuery("SELECT COUNT(*) FROM fotos WHERE " + w, a);
            int total = cn.moveToFirst() ? cn.getInt(0) : 0;
            cn.close();

            int offset = call.getInt("offset", 0), limite = Math.max(1, Math.min(200, call.getInt("limite", 60)));
            Cursor c = db().rawQuery("SELECT id, nombre, fecha, origen, caras, etiquetas, texto, marca, tipo, dur FROM fotos WHERE " + w
                + " ORDER BY fecha DESC LIMIT " + limite + " OFFSET " + offset, a);
            JSArray items = new JSArray();
            while (c.moveToNext()) {
                JSObject o = new JSObject();
                o.put("id", c.getLong(0));
                o.put("nombre", c.getString(1));
                o.put("fecha", f.format(new Date(c.getLong(2))));
                o.put("origen", c.getString(3));
                o.put("caras", c.getInt(4));
                String et = c.getString(5), tx = c.getString(6);
                o.put("etiquetas", et == null ? "" : et);
                o.put("texto", tx == null ? "" : (tx.length() > 80 ? tx.substring(0, 80) : tx));
                o.put("marca", c.isNull(7) ? "" : c.getString(7));
                o.put("tipo", c.getInt(8)); o.put("dur", c.getLong(9));
                items.put(o);
            }
            c.close();
            JSObject res = new JSObject();
            res.put("total", total);
            res.put("items", items);
            call.resolve(res);
        } catch (Throwable e) {
            call.reject("No se pudo buscar: " + e.getMessage());
        }
    }

    /* ---------------- Imágenes ---------------- */

    private String rutaDe(long id) {
        Cursor c = db().rawQuery("SELECT ruta FROM fotos WHERE id=?", new String[]{String.valueOf(id)});
        String r = c.moveToFirst() ? c.getString(0) : null;
        c.close();
        return r;
    }

    private static String aDataUrl(Bitmap b, int calidad) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, calidad, bos);
        return "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }

    private static Bitmap primero(java.util.List<Bitmap> l) { return l.isEmpty() ? null : l.get(0); }

    /** Ruta del video para reproducirlo dentro de la app. */
    @PluginMethod
    public void rutaVideo(PluginCall call) {
        String r = rutaDe((long) call.getInt("id", -1));
        if (r == null) { call.reject("Video inexistente."); return; }
        JSObject res = new JSObject();
        res.put("ruta", r);
        call.resolve(res);
    }

    /** Abre el video con el reproductor del teléfono (plan B si no anda el reproductor interno). */
    @PluginMethod
    public void abrirVideo(PluginCall call) {
        try {
            String r = rutaDe((long) call.getInt("id", -1));
            if (r == null) { call.reject("Video inexistente."); return; }
            Uri uri = androidx.core.content.FileProvider.getUriForFile(getContext(), getContext().getPackageName() + ".fileprovider", new File(r));
            Intent i = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/*");
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(i);
            call.resolve();
        } catch (Throwable e) {
            call.reject("No se pudo abrir el video: " + e.getMessage());
        }
    }

    @PluginMethod
    public void miniaturas(PluginCall call) {
        JSArray ids = call.getArray("ids");
        JSObject res = new JSObject();
        if (ids != null) {
            for (int i = 0; i < ids.length() && i < 30; i++) {
                long id = ids.optLong(i);
                String r = rutaDe(id);
                if (r == null) continue;
                Bitmap b = Img.esVideo(r) ? primero(Img.fotogramas(r, 260, 1, null)) : Img.cargar(r, 260, null);
                if (b == null) continue;
                res.put(String.valueOf(id), aDataUrl(b, 70));
                b.recycle();
            }
        }
        call.resolve(res);
    }

    @PluginMethod
    public void vista(PluginCall call) {
        String r = rutaDe((long) call.getInt("id", -1));
        if (r == null) { call.reject("Foto inexistente."); return; }
        int max = Math.max(600, Math.min(3200, call.getInt("max", 1400)));
        Bitmap b = Img.esVideo(r) ? primero(Img.fotogramas(r, max, 1, null)) : Img.cargar(r, max, null);
        if (b == null) { call.reject("No se pudo abrir la foto."); return; }
        JSObject res = new JSObject();
        res.put("img", aDataUrl(b, 85));
        b.recycle();
        call.resolve(res);
    }

    /* ---------------- Editor básico ---------------- */

    /** Matriz de color idéntica a la de la vista previa del editor (JS): saturación, calidez, contraste y brillo. */
    private static android.graphics.ColorMatrix matrizEdicion(int br, int ct, int sa, int wa) {
        float S = 1f + sa / 100f, W = wa * 0.5f, K = (100 + ct) / 100f, B = br * 1.27f;
        float lr = 0.299f, lg = 0.587f, lb = 0.114f;
        android.graphics.ColorMatrix m = new android.graphics.ColorMatrix(new float[]{
            lr * (1 - S) + S, lg * (1 - S), lb * (1 - S), 0, 0,
            lr * (1 - S), lg * (1 - S) + S, lb * (1 - S), 0, 0,
            lr * (1 - S), lg * (1 - S), lb * (1 - S) + S, 0, 0,
            0, 0, 0, 1, 0});
        m.postConcat(new android.graphics.ColorMatrix(new float[]{1, 0, 0, 0, W, 0, 1, 0, 0, 0, 0, 0, 1, 0, -W, 0, 0, 0, 1, 0}));
        m.postConcat(new android.graphics.ColorMatrix(new float[]{K, 0, 0, 0, 128 * (1 - K), 0, K, 0, 0, 128 * (1 - K), 0, 0, K, 0, 128 * (1 - K), 0, 0, 0, 1, 0}));
        m.postConcat(new android.graphics.ColorMatrix(new float[]{1, 0, 0, 0, B, 0, 1, 0, 0, B, 0, 0, 1, 0, B, 0, 0, 0, 1, 0}));
        return m;
    }

    /** Guarda una COPIA editada (gira, espeja, recorta, ajusta color). La foto original no se toca. */
    @PluginMethod
    public void editar(PluginCall call) {
        Bitmap src = null, t = null, rec = null, out = null;
        try {
            long id = (long) call.getInt("id", -1);
            String ruta = rutaDe(id);
            if (ruta == null) { call.reject("Foto inexistente."); return; }
            int rot = ((call.getInt("rot", 0) % 4) + 4) % 4;
            boolean flip = call.getBoolean("flip", false);
            int br = call.getInt("br", 0), ct = call.getInt("ct", 0), sa = call.getInt("sa", 0), wa = call.getInt("wa", 0);
            JSObject crop = call.getObject("crop");

            src = Img.cargar(ruta, 3000, null);
            if (src == null) { call.reject("No se pudo abrir la foto."); return; }
            android.graphics.Matrix m = new android.graphics.Matrix();
            if (flip) m.postScale(-1f, 1f);
            if (rot != 0) m.postRotate(rot * 90f);
            t = (flip || rot != 0) ? Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true) : src;
            if (t != src) { src.recycle(); src = null; }

            int W = t.getWidth(), H = t.getHeight(), cx = 0, cy = 0, cw = W, ch = H;
            if (crop != null) {
                cx = (int) Math.round(crop.optDouble("x", 0) * W); cy = (int) Math.round(crop.optDouble("y", 0) * H);
                cw = (int) Math.round(crop.optDouble("w", 1) * W); ch = (int) Math.round(crop.optDouble("h", 1) * H);
                cx = Math.max(0, Math.min(W - 1, cx)); cy = Math.max(0, Math.min(H - 1, cy));
                cw = Math.max(1, Math.min(W - cx, cw)); ch = Math.max(1, Math.min(H - cy, ch));
            }
            rec = (cx == 0 && cy == 0 && cw == W && ch == H) ? t : Bitmap.createBitmap(t, cx, cy, cw, ch);
            if (rec != t) { t.recycle(); t = null; }

            out = Bitmap.createBitmap(rec.getWidth(), rec.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas c = new android.graphics.Canvas(out);
            android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
            if (br != 0 || ct != 0 || sa != 0 || wa != 0) p.setColorFilter(new android.graphics.ColorMatrixColorFilter(matrizEdicion(br, ct, sa, wa)));
            c.drawBitmap(rec, 0, 0, p);

            File orig = new File(ruta), dir = orig.getParentFile();
            String base = orig.getName(); int pt = base.lastIndexOf('.'); if (pt > 0) base = base.substring(0, pt);
            File dest = new File(dir, base + "_editada.jpg");
            for (int n = 2; dest.exists(); n++) dest = new File(dir, base + "_editada" + n + ".jpg");
            try (FileOutputStream fo = new FileOutputStream(dest)) { out.compress(Bitmap.CompressFormat.JPEG, 93, fo); }

            ContentValues cv = new ContentValues();
            cv.put("ruta", dest.getAbsolutePath()); cv.put("nombre", dest.getName()); cv.put("size", dest.length());
            cv.put("mod", dest.lastModified()); cv.put("fecha", System.currentTimeMillis()); cv.put("origen", IndexService.origen(dest));
            long nuevo = db().insertWithOnConflict("fotos", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            try { MediaScannerConnection.scanFile(getContext(), new String[]{dest.getAbsolutePath()}, null, null); } catch (Throwable ignored) { }
            JSObject res = new JSObject();
            res.put("id", nuevo); res.put("nombre", dest.getName()); res.put("carpeta", dir.getAbsolutePath());
            call.resolve(res);
        } catch (Throwable e) {
            call.reject("No se pudo guardar la edición: " + e.getMessage());
        } finally {
            try { if (src != null) src.recycle(); } catch (Throwable ignored) { }
            try { if (t != null) t.recycle(); } catch (Throwable ignored) { }
            try { if (rec != null) rec.recycle(); } catch (Throwable ignored) { }
            try { if (out != null) out.recycle(); } catch (Throwable ignored) { }
        }
    }

    /* ---------------- Marcar ---------------- */

    @PluginMethod
    public void marcar(PluginCall call) {
        JSArray ids = call.getArray("ids");
        String marca = call.getString("marca", "");
        if (ids == null || ids.length() == 0) { call.resolve(); return; }
        SQLiteDatabase d = db();
        d.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            if (marca.isEmpty()) cv.putNull("marca"); else cv.put("marca", marca);
            for (int i = 0; i < ids.length(); i++) d.update("fotos", cv, "id=?", new String[]{String.valueOf(ids.optLong(i))});
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
        call.resolve();
    }

    /* ---------------- Exportar lo marcado (copia + zip; los originales no se tocan) ---------------- */

    private static File libre(File dir, String nombre) {
        File f = new File(dir, nombre);
        if (!f.exists()) return f;
        int dot = nombre.lastIndexOf('.');
        String base = dot > 0 ? nombre.substring(0, dot) : nombre, ext = dot > 0 ? nombre.substring(dot) : "";
        for (int i = 2; i < 1000; i++) { File g = new File(dir, base + " (" + i + ")" + ext); if (!g.exists()) return g; }
        return new File(dir, System.currentTimeMillis() + ext);
    }

    @PluginMethod
    public void exportar(PluginCall call) {
        if (!tienePermiso()) { call.reject("SIN_PERMISO"); return; }
        try {
            File base = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CARPETA_EXPORT);
            SimpleDateFormat dia = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Cursor c = db().rawQuery("SELECT ruta, fecha, marca, size FROM fotos WHERE marca IN ('entrega','plan') ORDER BY fecha", null);
            int copiadas = 0, fallas = 0, entregas = 0, planes = 0;
            List<File> copias = new ArrayList<>(), nombresZip = new ArrayList<>();
            StringBuilder csv = new StringBuilder("fecha,categoria,archivo,origen\n");
            while (c.moveToNext()) {
                File src = new File(c.getString(0));
                String marca = c.getString(2);
                File dir = new File(base, "entrega".equals(marca) ? "Entregas" : "Planes");
                if (!dir.exists() && !dir.mkdirs()) { fallas++; continue; }
                String fecha = dia.format(new Date(c.getLong(1)));
                File dst = libre(dir, fecha + "_" + src.getName());
                try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
                    byte[] b = new byte[64 * 1024]; int n;
                    while ((n = in.read(b)) > 0) out.write(b, 0, n);
                    out.flush();
                } catch (Throwable e) { fallas++; continue; }
                if (dst.length() != c.getLong(3)) { dst.delete(); fallas++; continue; }
                dst.setLastModified(c.getLong(1));
                copias.add(dst);
                copiadas++;
                if ("entrega".equals(marca)) entregas++; else planes++;
                csv.append(fecha).append(',').append(marca).append(",\"").append(dst.getName().replace("\"", "'")).append("\",\"")
                    .append(src.getAbsolutePath().replace("\"", "'")).append("\"\n");
            }
            c.close();
            if (copiadas == 0) { call.reject("No hay fotos marcadas como Entrega o Plan."); return; }
            try (OutputStream out = new FileOutputStream(new File(base, "indice.csv"))) { out.write(csv.toString().getBytes("UTF-8")); }
            File zip = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                CARPETA_EXPORT + "_" + new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date()) + ".zip");
            try (ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zip))) {
                z.setLevel(0);   // las fotos ya están comprimidas
                for (File f : copias) {
                    z.putNextEntry(new ZipEntry(f.getParentFile().getName() + "/" + f.getName()));
                    try (InputStream in = new FileInputStream(f)) { byte[] b = new byte[64 * 1024]; int n; while ((n = in.read(b)) > 0) z.write(b, 0, n); }
                    z.closeEntry();
                }
            }
            String[] rutas = new String[copias.size() + 1];
            for (int i = 0; i < copias.size(); i++) rutas[i] = copias.get(i).getAbsolutePath();
            rutas[copias.size()] = zip.getAbsolutePath();
            MediaScannerConnection.scanFile(getContext(), rutas, null, null);
            JSObject res = new JSObject();
            res.put("copiadas", copiadas); res.put("fallas", fallas); res.put("entregas", entregas); res.put("planes", planes);
            res.put("carpeta", base.getAbsolutePath()); res.put("zip", zip.getAbsolutePath());
            call.resolve(res);
        } catch (Throwable e) {
            call.reject("No se pudo exportar: " + e.getMessage());
        }
    }

    /* ================= Repetidas, papelera y carpetas ================= */

    private File gruposDir() { return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), CARPETA_GRUPOS); }

    /** Carpeta oculta (sin .nomedia no la mostraría la galería, con él queda fuera de ella) donde van las fotos eliminadas. */
    private File papeleraDir() {
        File d = new File(Environment.getExternalStorageDirectory(), ".DOMino-Papelera");
        if (!d.exists()) d.mkdirs();
        try { new File(d, ".nomedia").createNewFile(); } catch (Throwable ignored) { }
        return d;
    }

    private static String limpioNombre(String n) {
        String s = n == null ? "" : n.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").trim();
        while (s.startsWith(".")) s = s.substring(1).trim();
        return s.length() > 60 ? s.substring(0, 60).trim() : s;
    }

    /** Mueve un archivo: renombra y, si está en otro volumen, copia, verifica el tamaño y borra el origen. */
    private static boolean moverArchivo(File src, File dst) {
        if (src.renameTo(dst)) return true;
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dst)) {
            byte[] b = new byte[64 * 1024]; int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            out.flush();
        } catch (Throwable e) { dst.delete(); return false; }
        if (dst.length() != src.length()) { dst.delete(); return false; }
        dst.setLastModified(src.lastModified());
        if (!src.delete()) { dst.delete(); return false; }
        return true;
    }

    private void escanear(List<String> rutas) {
        if (rutas.isEmpty()) return;
        try { MediaScannerConnection.scanFile(getContext(), rutas.toArray(new String[0]), null, null); } catch (Throwable ignored) { }
    }

    private static class Fila { long id, orig, fecha, size; String nombre = "", origen = ""; }

    private final Map<Long, List<Fila>> gruposRep = new LinkedHashMap<>();
    private final Map<Long, Fila> raicesRep = new HashMap<>();

    /** A las repetidas viejas (marcadas antes de guardar el vínculo) les busca la foto que se queda como original. */
    private void enlazar() {
        SQLiteDatabase d = db();
        List<long[]> pend = new ArrayList<>();      // id, dh, tieneDh, w, h
        List<String> hashes = new ArrayList<>();
        Cursor c = d.rawQuery("SELECT id, dh, hash, w, h FROM fotos WHERE dup=1 AND borrada=0 AND orig IS NULL", null);
        while (c.moveToNext()) {
            pend.add(new long[]{c.getLong(0), c.isNull(1) ? 0 : c.getLong(1), c.isNull(1) ? 0 : 1, c.getLong(3), c.getLong(4)});
            hashes.add(c.getString(2));
        }
        c.close();
        if (pend.isEmpty()) return;
        Map<String, Long> porHash = new HashMap<>();
        List<long[]> raices = new ArrayList<>();    // id, dh, w, h
        c = d.rawQuery("SELECT id, hash, dh, w, h FROM fotos WHERE dup=0 AND borrada=0 AND (hash IS NOT NULL OR dh IS NOT NULL)", null);
        while (c.moveToNext()) {
            if (!c.isNull(1) && !porHash.containsKey(c.getString(1))) porHash.put(c.getString(1), c.getLong(0));
            if (!c.isNull(2)) raices.add(new long[]{c.getLong(0), c.getLong(2), c.getLong(3), c.getLong(4)});
        }
        c.close();
        c = d.rawQuery("SELECT id, hash FROM fotos WHERE dup=1 AND borrada=0 AND orig IS NOT NULL AND hash IS NOT NULL", null);
        while (c.moveToNext()) if (!porHash.containsKey(c.getString(1))) porHash.put(c.getString(1), c.getLong(0));
        c.close();
        Map<Integer, List<Integer>> cubos = new HashMap<>();
        for (int i = 0; i < raices.size(); i++) {
            long dh = raices.get(i)[1];
            for (int b = 0; b < 4; b++) {
                int clave = (b << 16) | (int) ((dh >>> (16 * b)) & 0xFFFF);
                List<Integer> l = cubos.get(clave);
                if (l == null) { l = new ArrayList<>(); cubos.put(clave, l); }
                l.add(i);
            }
        }
        d.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            for (int i = 0; i < pend.size(); i++) {
                long[] p = pend.get(i);
                Long o = hashes.get(i) != null ? porHash.get(hashes.get(i)) : null;
                if (o != null && o == p[0]) o = null;
                if (o == null && p[2] == 1) {
                    long mejor = -1; int dmin = 9;
                    double ap = p[4] == 0 ? 1 : (double) p[3] / p[4];
                    for (int b = 0; b < 4; b++) {
                        List<Integer> l = cubos.get((b << 16) | (int) ((p[1] >>> (16 * b)) & 0xFFFF));
                        if (l == null) continue;
                        for (int k = 0; k < l.size() && k < 400; k++) {
                            long[] r = raices.get(l.get(k));
                            int dist = Long.bitCount(p[1] ^ r[1]);
                            if (dist >= dmin) continue;
                            double ar = r[3] == 0 ? 1 : (double) r[2] / r[3];
                            if (Math.abs(ap - ar) / Math.max(ap, ar) > 0.04) continue;
                            dmin = dist; mejor = r[0];
                        }
                    }
                    if (mejor >= 0) o = mejor;
                }
                if (o != null) { cv.clear(); cv.put("orig", o); d.update("fotos", cv, "id=?", new String[]{String.valueOf(p[0])}); }
            }
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
    }

    /** Arma los grupos: cada original con sus copias. Devuelve cuántas repetidas no tienen original localizado (esas no se tocan). */
    private int armarGrupos() {
        enlazar();
        SQLiteDatabase d = db();
        gruposRep.clear(); raicesRep.clear();
        Map<Long, Fila> copias = new LinkedHashMap<>();
        Cursor c = d.rawQuery("SELECT id, orig, nombre, fecha, origen, size FROM fotos WHERE dup=1 AND borrada=0 AND orig IS NOT NULL ORDER BY fecha DESC", null);
        while (c.moveToNext()) {
            Fila f = new Fila();
            f.id = c.getLong(0); f.orig = c.getLong(1); f.nombre = c.getString(2) == null ? "" : c.getString(2);
            f.fecha = c.getLong(3); f.origen = c.getString(4) == null ? "" : c.getString(4); f.size = c.getLong(5);
            copias.put(f.id, f);
        }
        c.close();
        int sin = contar("SELECT COUNT(*) FROM fotos WHERE dup=1 AND borrada=0 AND orig IS NULL");
        Map<Long, List<Fila>> tmp = new LinkedHashMap<>();
        for (Fila f : copias.values()) {
            long r = f.orig; int pasos = 0;
            while (copias.containsKey(r) && pasos++ < 12) r = copias.get(r).orig;
            List<Fila> l = tmp.get(r);
            if (l == null) { l = new ArrayList<>(); tmp.put(r, l); }
            l.add(f);
        }
        for (Map.Entry<Long, List<Fila>> e : tmp.entrySet()) {
            Cursor q = d.rawQuery("SELECT id, nombre, fecha, origen, size FROM fotos WHERE id=? AND dup=0 AND borrada=0", new String[]{String.valueOf(e.getKey())});
            if (q.moveToFirst()) {
                Fila r = new Fila();
                r.id = q.getLong(0); r.nombre = q.getString(1) == null ? "" : q.getString(1); r.fecha = q.getLong(2);
                r.origen = q.getString(3) == null ? "" : q.getString(3); r.size = q.getLong(4);
                raicesRep.put(r.id, r);
                gruposRep.put(r.id, e.getValue());
            } else sin += e.getValue().size();
            q.close();
        }
        return sin;
    }

    private static JSObject datosFila(Fila f, SimpleDateFormat fmt) {
        JSObject o = new JSObject();
        o.put("id", f.id); o.put("nombre", f.nombre); o.put("origen", f.origen);
        o.put("fecha", fmt.format(new Date(f.fecha))); o.put("mb", Math.round(f.size / 104857.6) / 10.0);
        return o;
    }

    @PluginMethod
    public void repetidas(PluginCall call) {
        try {
            int sin = armarGrupos();
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            List<Long> raices = new ArrayList<>(gruposRep.keySet());
            int total = 0;
            for (List<Fila> l : gruposRep.values()) total += l.size();
            int off = Math.max(0, call.getInt("offset", 0)), lim = Math.max(1, Math.min(60, call.getInt("limite", 20)));
            JSArray arr = new JSArray();
            for (int i = off; i < raices.size() && i < off + lim; i++) {
                long r = raices.get(i);
                JSObject g = new JSObject();
                g.put("orig", datosFila(raicesRep.get(r), fmt));
                JSArray cs = new JSArray();
                for (Fila f : gruposRep.get(r)) cs.put(datosFila(f, fmt));
                g.put("copias", cs);
                arr.put(g);
            }
            JSObject res = new JSObject();
            res.put("totalGrupos", raices.size()); res.put("totalCopias", total); res.put("sinOriginal", sin); res.put("grupos", arr);
            call.resolve(res);
        } catch (Throwable e) {
            call.reject("No se pudieron armar los grupos: " + e.getMessage());
        }
    }

    @PluginMethod
    public void idsRepetidas(PluginCall call) {
        try {
            armarGrupos();
            JSArray ids = new JSArray();
            for (List<Fila> l : gruposRep.values()) for (Fila f : l) ids.put(f.id);
            JSObject res = new JSObject();
            res.put("ids", ids);
            call.resolve(res);
        } catch (Throwable e) {
            call.reject("No se pudo leer la lista: " + e.getMessage());
        }
    }

    /** Una repetida solo se elimina si su original sigue existiendo en el teléfono. */
    private boolean originalVigente(long id) {
        SQLiteDatabase d = db();
        for (int paso = 0; paso < 12; paso++) {
            Cursor c = d.rawQuery("SELECT orig FROM fotos WHERE id=?", new String[]{String.valueOf(id)});
            boolean tiene = c.moveToFirst() && !c.isNull(0);
            long o = tiene ? c.getLong(0) : -1;
            c.close();
            if (!tiene) return false;
            Cursor q = d.rawQuery("SELECT dup, ruta, borrada FROM fotos WHERE id=?", new String[]{String.valueOf(o)});
            if (!q.moveToFirst()) { q.close(); return false; }
            int dup = q.getInt(0); String ruta = q.getString(1); int bor = q.getInt(2);
            q.close();
            if (bor != 0) return false;
            if (dup == 0) return ruta != null && new File(ruta).exists();
            id = o;
        }
        return false;
    }

    /** Eliminar = mover a la papelera de la app (se puede restaurar hasta que se vacíe). */
    @PluginMethod
    public void eliminar(PluginCall call) {
        if (!tienePermiso()) { call.reject("SIN_PERMISO"); return; }
        JSArray ids = call.getArray("ids");
        boolean soloRepetidas = call.getBoolean("dup", false);
        int movidas = 0, fallas = 0, sinOriginal = 0; long bytes = 0;
        List<String> viejas = new ArrayList<>();
        SQLiteDatabase d = db();
        File pap = papeleraDir();
        d.beginTransaction();
        try {
            for (int i = 0; ids != null && i < ids.length(); i++) {
                long id = ids.optLong(i);
                Cursor c = d.rawQuery("SELECT ruta, nombre, size, dup FROM fotos WHERE id=? AND borrada=0", new String[]{String.valueOf(id)});
                if (!c.moveToFirst()) { c.close(); continue; }
                String ruta = c.getString(0), nombre = c.getString(1); long size = c.getLong(2); int dup = c.getInt(3);
                c.close();
                if (soloRepetidas && (dup != 1 || !originalVigente(id))) { sinOriginal++; continue; }
                File src = new File(ruta);
                if (!src.exists()) { d.delete("fotos", "id=?", new String[]{String.valueOf(id)}); continue; }
                File dst = new File(pap, id + "_" + (nombre == null ? src.getName() : nombre));
                if (!moverArchivo(src, dst)) { fallas++; continue; }
                ContentValues cv = new ContentValues();
                cv.put("ruta", dst.getAbsolutePath()); cv.put("ruta_orig", ruta); cv.put("borrada", 1);
                d.update("fotos", cv, "id=?", new String[]{String.valueOf(id)});
                viejas.add(ruta); movidas++; bytes += size;
            }
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
        escanear(viejas);
        JSObject res = new JSObject();
        res.put("movidas", movidas); res.put("fallas", fallas); res.put("sinOriginal", sinOriginal); res.put("mb", bytes / 1048576);
        call.resolve(res);
    }

    @PluginMethod
    public void restaurar(PluginCall call) {
        if (!tienePermiso()) { call.reject("SIN_PERMISO"); return; }
        JSArray ids = call.getArray("ids");
        boolean todas = call.getBoolean("todas", false);
        SQLiteDatabase d = db();
        List<long[]> lista = new ArrayList<>();
        List<String[]> rutas = new ArrayList<>();
        Cursor c = d.rawQuery("SELECT id, ruta, ruta_orig FROM fotos WHERE borrada=1", null);
        java.util.Set<Long> pedidas = new java.util.HashSet<>();
        for (int i = 0; ids != null && i < ids.length(); i++) pedidas.add(ids.optLong(i));
        while (c.moveToNext()) {
            if (!todas && !pedidas.contains(c.getLong(0))) continue;
            lista.add(new long[]{c.getLong(0)}); rutas.add(new String[]{c.getString(1), c.getString(2)});
        }
        c.close();
        int ok = 0, fallas = 0;
        List<String> nuevas = new ArrayList<>();
        d.beginTransaction();
        try {
            for (int i = 0; i < lista.size(); i++) {
                File src = new File(rutas.get(i)[0]);
                String origen = rutas.get(i)[1];
                if (origen == null || !src.exists()) { fallas++; continue; }
                File dst = new File(origen);
                File padre = dst.getParentFile();
                if (padre != null && !padre.exists()) padre.mkdirs();
                if (dst.exists()) dst = libre(padre, dst.getName());
                if (!moverArchivo(src, dst)) { fallas++; continue; }
                ContentValues cv = new ContentValues();
                cv.put("ruta", dst.getAbsolutePath()); cv.putNull("ruta_orig"); cv.put("borrada", 0);
                d.update("fotos", cv, "id=?", new String[]{String.valueOf(lista.get(i)[0])});
                nuevas.add(dst.getAbsolutePath()); ok++;
            }
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
        escanear(nuevas);
        JSObject res = new JSObject();
        res.put("restauradas", ok); res.put("fallas", fallas);
        call.resolve(res);
    }

    /** Borrado definitivo: solo de lo que ya está en la papelera. */
    @PluginMethod
    public void vaciarPapelera(PluginCall call) {
        JSArray ids = call.getArray("ids");
        java.util.Set<Long> pedidas = new java.util.HashSet<>();
        for (int i = 0; ids != null && i < ids.length(); i++) pedidas.add(ids.optLong(i));
        SQLiteDatabase d = db();
        List<long[]> lista = new ArrayList<>();
        List<String> rutas = new ArrayList<>();
        Cursor c = d.rawQuery("SELECT id, ruta, size FROM fotos WHERE borrada=1", null);
        while (c.moveToNext()) {
            if (ids != null && !pedidas.contains(c.getLong(0))) continue;
            lista.add(new long[]{c.getLong(0), c.getLong(2)}); rutas.add(c.getString(1));
        }
        c.close();
        int borradas = 0; long bytes = 0;
        d.beginTransaction();
        try {
            for (int i = 0; i < lista.size(); i++) {
                File f = new File(rutas.get(i));
                if (f.exists() && !f.delete()) continue;
                d.delete("fotos", "id=?", new String[]{String.valueOf(lista.get(i)[0])});
                borradas++; bytes += lista.get(i)[1];
            }
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
        JSObject res = new JSObject();
        res.put("borradas", borradas); res.put("mb", bytes / 1048576);
        call.resolve(res);
    }

    /* ---------------- Carpetas propias: Imágenes / DOMino-Carpetas / <nombre> ---------------- */

    @PluginMethod
    public void carpeta(PluginCall call) {
        if (!tienePermiso()) { call.reject("SIN_PERMISO"); return; }
        String nombre = limpioNombre(call.getString("nombre", ""));
        JSArray ids = call.getArray("ids");
        if (nombre.isEmpty()) { call.reject("Escribí un nombre para la carpeta."); return; }
        if (ids == null || ids.length() == 0) { call.reject("No hay fotos seleccionadas."); return; }
        File dir = new File(gruposDir(), nombre);
        if (!dir.exists() && !dir.mkdirs()) { call.reject("No se pudo crear la carpeta."); return; }
        SQLiteDatabase d = db();
        int movidas = 0, fallas = 0;
        List<String> rutas = new ArrayList<>();
        d.beginTransaction();
        try {
            for (int i = 0; i < ids.length(); i++) {
                long id = ids.optLong(i);
                Cursor c = d.rawQuery("SELECT ruta, nombre FROM fotos WHERE id=? AND borrada=0", new String[]{String.valueOf(id)});
                if (!c.moveToFirst()) { c.close(); continue; }
                String ruta = c.getString(0), nom = c.getString(1);
                c.close();
                File src = new File(ruta);
                if (!src.exists()) { fallas++; continue; }
                if (src.getParentFile() != null && src.getParentFile().equals(dir)) { movidas++; continue; }
                File dst = libre(dir, nom == null ? src.getName() : nom);
                if (!moverArchivo(src, dst)) { fallas++; continue; }
                ContentValues cv = new ContentValues();
                cv.put("ruta", dst.getAbsolutePath()); cv.put("nombre", dst.getName()); cv.put("origen", CARPETA_GRUPOS + "/" + nombre);
                d.update("fotos", cv, "id=?", new String[]{String.valueOf(id)});
                rutas.add(ruta); rutas.add(dst.getAbsolutePath()); movidas++;
            }
            d.setTransactionSuccessful();
        } finally { d.endTransaction(); }
        escanear(rutas);
        JSObject res = new JSObject();
        res.put("movidas", movidas); res.put("fallas", fallas); res.put("carpeta", dir.getAbsolutePath());
        call.resolve(res);
    }

    @PluginMethod
    public void carpetas(PluginCall call) {
        Map<String, Integer> cuenta = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File base = gruposDir();
        File[] hijos = base.listFiles();
        if (hijos != null) for (File f : hijos) if (f.isDirectory() && !f.getName().startsWith(".")) cuenta.put(f.getName(), 0);
        String pre = base.getAbsolutePath() + "/";
        Cursor c = db().rawQuery("SELECT ruta FROM fotos WHERE borrada=0 AND substr(ruta,1," + pre.length() + ")=?", new String[]{pre});
        while (c.moveToNext()) {
            String resto = c.getString(0).substring(pre.length());
            int k = resto.indexOf('/');
            if (k <= 0) continue;
            String n = resto.substring(0, k);
            Integer v = cuenta.get(n);
            cuenta.put(n, v == null ? 1 : v + 1);
        }
        c.close();
        JSArray arr = new JSArray();
        for (Map.Entry<String, Integer> e : cuenta.entrySet()) {
            JSObject o = new JSObject();
            o.put("nombre", e.getKey()); o.put("cantidad", e.getValue());
            arr.put(o);
        }
        JSObject res = new JSObject();
        res.put("carpetas", arr); res.put("ruta", base.getAbsolutePath());
        call.resolve(res);
    }
}
