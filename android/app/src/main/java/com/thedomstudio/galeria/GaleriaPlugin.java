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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Puente entre la pantalla y el índice local: indexar, buscar, marcar y exportar. */
@CapacitorPlugin(name = "Galeria")
public class GaleriaPlugin extends Plugin {

    public static final String CARPETA_EXPORT = "X5-Historia";

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
        o.put("registradas", contar("SELECT COUNT(*) FROM fotos"));
        o.put("repetidas", contar("SELECT COUNT(*) FROM fotos WHERE dup=1"));
        o.put("analizadas", contar("SELECT COUNT(*) FROM fotos WHERE estado=1 AND dup=0"));
        o.put("pendientes", contar("SELECT COUNT(*) FROM fotos WHERE estado=0 AND dup=0"));
        o.put("errores", contar("SELECT COUNT(*) FROM fotos WHERE estado=2 AND dup=0"));
        o.put("entregas", contar("SELECT COUNT(*) FROM fotos WHERE marca='entrega'"));
        o.put("planes", contar("SELECT COUNT(*) FROM fotos WHERE marca='plan'"));
        o.put("descartadas", contar("SELECT COUNT(*) FROM fotos WHERE marca='descartar'"));
        call.resolve(o);
    }

    /* ---------------- Búsqueda ---------------- */

    private static String likeEsc(String s) { return "%" + s.replace("%", "").replace("_", "") + "%"; }

    @PluginMethod
    public void buscar(PluginCall call) {
        try {
            StringBuilder w = new StringBuilder("estado=1 AND dup=0");
            List<String> args = new ArrayList<>();

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
            Cursor c = db().rawQuery("SELECT id, nombre, fecha, origen, caras, etiquetas, texto, marca FROM fotos WHERE " + w
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

    @PluginMethod
    public void miniaturas(PluginCall call) {
        JSArray ids = call.getArray("ids");
        JSObject res = new JSObject();
        if (ids != null) {
            for (int i = 0; i < ids.length() && i < 30; i++) {
                long id = ids.optLong(i);
                String r = rutaDe(id);
                if (r == null) continue;
                Bitmap b = Img.cargar(r, 260, null);
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
        Bitmap b = Img.cargar(r, 1400, null);
        if (b == null) { call.reject("No se pudo abrir la foto."); return; }
        JSObject res = new JSObject();
        res.put("img", aDataUrl(b, 85));
        b.recycle();
        call.resolve(res);
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
}
