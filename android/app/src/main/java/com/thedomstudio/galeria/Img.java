package com.thedomstudio.galeria;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de imagen y texto compartidas. */
public class Img {

    /** Decodifica achicada (lado largo ≈ maxLado) y ya derecha. dims (opcional) recibe {ancho, alto} originales. */
    public static Bitmap cargar(String ruta, int maxLado, int[] dims) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(ruta, o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return null;
            if (dims != null) { dims[0] = o.outWidth; dims[1] = o.outHeight; }
            int lado = Math.max(o.outWidth, o.outHeight), s = 1;
            while (lado / (s * 2) >= maxLado) s *= 2;
            o.inJustDecodeBounds = false; o.inSampleSize = s;
            Bitmap b = BitmapFactory.decodeFile(ruta, o);
            if (b == null) return null;
            int rot = 0;
            try {
                int ori = new ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (ori == ExifInterface.ORIENTATION_ROTATE_90) rot = 90;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_180) rot = 180;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_270) rot = 270;
            } catch (Throwable ignored) { }
            int mayor = Math.max(b.getWidth(), b.getHeight());
            float esc = mayor > maxLado ? (float) maxLado / mayor : 1f;
            if (rot != 0 || esc != 1f) {
                Matrix m = new Matrix();
                if (rot != 0) m.postRotate(rot);
                if (esc != 1f) m.postScale(esc, esc);
                Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                if (r != b) { b.recycle(); b = r; }
            }
            return b;
        } catch (Throwable e) {
            return null;
        }
    }

    /** Huella visual de 64 bits (dHash) para detectar fotos casi iguales. */
    public static long huella(Bitmap b) {
        Bitmap c = Bitmap.createScaledBitmap(b, 9, 8, true);
        long h = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int p1 = c.getPixel(x, y), p2 = c.getPixel(x + 1, y);
                int g1 = ((p1 >> 16 & 255) * 3 + (p1 >> 8 & 255) * 6 + (p1 & 255)) / 10;
                int g2 = ((p2 >> 16 & 255) * 3 + (p2 >> 8 & 255) * 6 + (p2 & 255)) / 10;
                h = (h << 1) | (g1 > g2 ? 1 : 0);
            }
        }
        if (c != b) c.recycle();
        return h;
    }

    /** Minúsculas y sin tildes. */
    public static String norm(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    static boolean esVideo(String nombre) {
        String s = nombre.toLowerCase(Locale.ROOT);
        return s.endsWith(".mp4") || s.endsWith(".mov") || s.endsWith(".3gp") || s.endsWith(".mkv") || s.endsWith(".webm") || s.endsWith(".m4v") || s.endsWith(".avi");
    }

    /** Cuadros de un video, ya derechos y achicados (lado largo ≈ maxLado). info (opcional) recibe {ancho, alto, duración en ms}. */
    public static java.util.List<Bitmap> fotogramas(String ruta, int maxLado, int cuantos, long[] info) {
        java.util.List<Bitmap> out = new java.util.ArrayList<>();
        android.media.MediaMetadataRetriever mr = new android.media.MediaMetadataRetriever();
        try {
            mr.setDataSource(ruta);
            long dur = 0; int w = 0, h = 0, rot = 0;
            try { dur = Long.parseLong(mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)); } catch (Throwable ignored) { }
            try { w = Integer.parseInt(mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)); } catch (Throwable ignored) { }
            try { h = Integer.parseInt(mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)); } catch (Throwable ignored) { }
            try { rot = Integer.parseInt(mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)); } catch (Throwable ignored) { }
            if (info != null) { info[0] = (rot == 90 || rot == 270) ? h : w; info[1] = (rot == 90 || rot == 270) ? w : h; info[2] = dur; }
            for (int i = 0; i < cuantos; i++) {
                long t = cuantos == 1 ? Math.min(dur, 1000) : dur * (2 * i + 1) / (2L * cuantos);   // 1 cuadro: a 1 s; varios: parejos
                Bitmap b = null;
                try { b = mr.getFrameAtTime(Math.max(0, t) * 1000L, android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC); } catch (Throwable ignored) { }
                if (b == null) continue;   // getFrameAtTime ya entrega el cuadro con la rotación aplicada
                int mayor = Math.max(b.getWidth(), b.getHeight());
                if (mayor > maxLado) {
                    float e = (float) maxLado / mayor;
                    Bitmap r = Bitmap.createScaledBitmap(b, Math.max(1, Math.round(b.getWidth() * e)), Math.max(1, Math.round(b.getHeight() * e)), true);
                    if (r != b) { b.recycle(); b = r; }
                }
                out.add(b);
            }
        } catch (Throwable ignored) {
        } finally { try { mr.release(); } catch (Throwable ignored) { } }
        return out;
    }
}
