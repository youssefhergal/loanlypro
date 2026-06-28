package com.projetfilrouge.loanmanagement.service.documents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Générateur PDF minimaliste sans dépendance externe (Sprint 6 — GED).
 *
 * <p>Produit un PDF 1.4 valide (A4 portrait) avec polices standard Helvetica /
 * Helvetica-Bold (encodage WinAnsi, accents français supportés). Suffisant pour
 * les documents crédit, l'échéancier et le relevé de prélèvements du MVP.</p>
 *
 * <p>Volontairement dépendance-free : aucune librairie PDF (iText/PDFBox) n'est
 * ajoutée au projet, donc rien à télécharger ni à configurer.</p>
 */
public final class SimplePdfDocument {

    private static final Charset WIN_ANSI = Charset.forName("windows-1252");

    private static final float PAGE_WIDTH = 595.28f;
    private static final float PAGE_HEIGHT = 841.89f;
    private static final float MARGIN_LEFT = 50f;
    private static final float MARGIN_RIGHT = 50f;
    private static final float MARGIN_TOP = 56f;
    private static final float MARGIN_BOTTOM = 56f;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    private static final String FONT_REGULAR = "F1";
    private static final String FONT_BOLD = "F2";

    private final List<ByteArrayOutputStream> pages = new ArrayList<>();
    private ByteArrayOutputStream current;
    private float cursorY; // distance depuis le haut de la page

    public SimplePdfDocument() {
        newPage();
    }

    /* ----------------------------------------------------------------- API */

    public SimplePdfDocument brandHeader(String brand, String documentTitle) {
        drawText(MARGIN_LEFT, cursorY, brand, FONT_BOLD, 18f);
        cursorY += 22f;
        drawText(MARGIN_LEFT, cursorY, documentTitle, FONT_BOLD, 13f);
        cursorY += 10f;
        horizontalRule();
        cursorY += 14f;
        return this;
    }

    public SimplePdfDocument keyValue(String label, String value) {
        ensureSpace(16f);
        drawText(MARGIN_LEFT, cursorY, label + " :", FONT_BOLD, 10f);
        drawText(MARGIN_LEFT + 170f, cursorY, value == null ? "—" : value, FONT_REGULAR, 10f);
        cursorY += 16f;
        return this;
    }

    public SimplePdfDocument sectionTitle(String title) {
        ensureSpace(26f);
        cursorY += 8f;
        drawText(MARGIN_LEFT, cursorY, title, FONT_BOLD, 12f);
        cursorY += 16f;
        return this;
    }

    public SimplePdfDocument paragraph(String text) {
        for (String line : wrap(text, FONT_REGULAR, 10f, CONTENT_WIDTH)) {
            ensureSpace(14f);
            drawText(MARGIN_LEFT, cursorY, line, FONT_REGULAR, 10f);
            cursorY += 14f;
        }
        return this;
    }

    public SimplePdfDocument spacer(float height) {
        cursorY += height;
        return this;
    }

    /**
     * Table simple : entête en gras + lignes, séparateurs horizontaux, pagination auto.
     *
     * @param headers entêtes de colonnes
     * @param rows    données (chaque ligne = colonnes alignées sur headers)
     * @param weights poids relatif des colonnes (mêmes dimensions que headers)
     */
    public SimplePdfDocument table(List<String> headers, List<List<String>> rows, float[] weights) {
        float[] colX = computeColumns(weights);
        float rowHeight = 16f;

        drawTableHeader(headers, colX, rowHeight);

        for (List<String> row : rows) {
            if (cursorY + rowHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                newPage();
                drawTableHeader(headers, colX, rowHeight);
            }
            for (int c = 0; c < headers.size(); c++) {
                float colWidth = colX[c + 1] - colX[c] - 6f;
                String cell = c < row.size() ? row.get(c) : "";
                drawText(colX[c], cursorY, truncate(cell, 10f, colWidth), FONT_REGULAR, 10f);
            }
            cursorY += rowHeight;
            thinRule();
        }
        cursorY += 6f;
        return this;
    }

    public SimplePdfDocument footerNote(String text) {
        float y = PAGE_HEIGHT - 36f;
        for (ByteArrayOutputStream page : pages) {
            writeText(page, MARGIN_LEFT, PAGE_HEIGHT - y, text, FONT_REGULAR, 8f, 0.45f);
        }
        return this;
    }

    public byte[] build() {
        return assemble();
    }

    /* -------------------------------------------------------------- drawing */

    private void newPage() {
        current = new ByteArrayOutputStream();
        pages.add(current);
        cursorY = MARGIN_TOP;
    }

    private void ensureSpace(float needed) {
        if (cursorY + needed > PAGE_HEIGHT - MARGIN_BOTTOM) {
            newPage();
        }
    }

    private void drawTableHeader(List<String> headers, float[] colX, float rowHeight) {
        ensureSpace(rowHeight + 4f);
        for (int c = 0; c < headers.size(); c++) {
            drawText(colX[c], cursorY, headers.get(c), FONT_BOLD, 10f);
        }
        cursorY += rowHeight;
        horizontalRule();
    }

    private float[] computeColumns(float[] weights) {
        float total = 0f;
        for (float w : weights) {
            total += w;
        }
        float[] colX = new float[weights.length + 1];
        colX[0] = MARGIN_LEFT;
        for (int i = 0; i < weights.length; i++) {
            colX[i + 1] = colX[i] + (weights[i] / total) * CONTENT_WIDTH;
        }
        return colX;
    }

    private void drawText(float x, float yFromTop, String text, String font, float size) {
        writeText(current, x, PAGE_HEIGHT - yFromTop, text, font, size, 0f);
    }

    private void writeText(ByteArrayOutputStream page, float x, float pdfY,
                           String text, String font, float size, float gray) {
        try {
            StringBuilder pre = new StringBuilder();
            pre.append("BT\n");
            if (gray > 0f) {
                pre.append(format(gray)).append(" g\n");
            }
            pre.append('/').append(font).append(' ').append(format(size)).append(" Tf\n");
            pre.append(format(x)).append(' ').append(format(pdfY)).append(" Td\n(");
            page.write(pre.toString().getBytes(WIN_ANSI));
            page.write(escape(text));
            String post = ") Tj\n" + (gray > 0f ? "0 g\n" : "") + "ET\n";
            page.write(post.getBytes(WIN_ANSI));
        } catch (IOException e) {
            throw new IllegalStateException("Erreur d'écriture du contenu PDF", e);
        }
    }

    private void horizontalRule() {
        rule(0.8f, PAGE_HEIGHT - cursorY);
    }

    private void thinRule() {
        rule(0.3f, PAGE_HEIGHT - cursorY + 12f);
    }

    private void rule(float width, float pdfY) {
        String op = format(width) + " w 0.80 G\n"
                + format(MARGIN_LEFT) + ' ' + format(pdfY) + " m "
                + format(PAGE_WIDTH - MARGIN_RIGHT) + ' ' + format(pdfY) + " l S\n0 G\n";
        current.writeBytes(op.getBytes(WIN_ANSI));
    }

    /* --------------------------------------------------------------- text utils */

    private List<String> wrap(String text, String font, float size, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (approxWidth(candidate, size) > maxWidth && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    private String truncate(String text, float size, float maxWidth) {
        if (text == null) {
            return "";
        }
        if (approxWidth(text, size) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int max = Math.max(1, (int) (maxWidth / (size * 0.5f)) - suffix.length());
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max) + suffix;
    }

    private float approxWidth(String text, float size) {
        return text.length() * size * 0.5f;
    }

    private byte[] escape(String text) {
        String safe = text == null ? "" : text;
        byte[] raw = safe.getBytes(WIN_ANSI);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte b : raw) {
            if (b == '(' || b == ')' || b == '\\') {
                out.write('\\');
            }
            out.write(b);
        }
        return out.toByteArray();
    }

    private static String format(float value) {
        if (value == Math.rint(value)) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    /* ------------------------------------------------------------- assembling */

    private byte[] assemble() {
        List<byte[]> objects = new ArrayList<>();
        int fontRegularId = 0; // placeholder, real numbering below
        // Object plan:
        // 1 Catalog, 2 Pages, 3 FontRegular, 4 FontBold, then per page: content + page object.
        int catalogId = 1;
        int pagesId = 2;
        int fontReg = 3;
        int fontBold = 4;
        int firstPageObj = 5;

        List<Integer> pageObjIds = new ArrayList<>();
        List<byte[]> pageContents = new ArrayList<>();

        int nextId = firstPageObj;
        List<int[]> pagePairs = new ArrayList<>(); // {contentId, pageId}
        for (int i = 0; i < pages.size(); i++) {
            int contentId = nextId++;
            int pageId = nextId++;
            pagePairs.add(new int[]{contentId, pageId});
            pageObjIds.add(pageId);
        }

        StringBuilder kids = new StringBuilder();
        for (int id : pageObjIds) {
            kids.append(id).append(" 0 R ");
        }

        // Build object bodies in id order.
        List<byte[]> bodies = new ArrayList<>();
        bodies.add(obj(catalogId, "<< /Type /Catalog /Pages " + pagesId + " 0 R >>"));
        bodies.add(obj(pagesId, "<< /Type /Pages /Count " + pages.size()
                + " /Kids [ " + kids.toString().trim() + " ] >>"));
        bodies.add(obj(fontReg, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>"));
        bodies.add(obj(fontBold, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>"));

        for (int i = 0; i < pages.size(); i++) {
            int contentId = pagePairs.get(i)[0];
            int pageId = pagePairs.get(i)[1];
            byte[] streamBytes = pages.get(i).toByteArray();
            bodies.add(streamObj(contentId, streamBytes));
            String pageDict = "<< /Type /Page /Parent " + pagesId + " 0 R "
                    + "/MediaBox [0 0 " + format(PAGE_WIDTH) + ' ' + format(PAGE_HEIGHT) + "] "
                    + "/Resources << /Font << /F1 " + fontReg + " 0 R /F2 " + fontBold + " 0 R >> >> "
                    + "/Contents " + contentId + " 0 R >>";
            bodies.add(obj(pageId, pageDict));
        }

        int totalObjects = nextId - 1;

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        write(pdf, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");
        int[] offsets = new int[totalObjects + 1];

        // Bodies were created out of id order for stream objects; sort by id.
        bodies.sort((a, b) -> Integer.compare(objId(a), objId(b)));
        for (byte[] body : bodies) {
            offsets[objId(body)] = pdf.size();
            pdf.writeBytes(stripIdMarker(body));
        }

        int xrefStart = pdf.size();
        StringBuilder xref = new StringBuilder();
        xref.append("xref\n0 ").append(totalObjects + 1).append('\n');
        xref.append("0000000000 65535 f \n");
        for (int id = 1; id <= totalObjects; id++) {
            xref.append(String.format("%010d 00000 n \n", offsets[id]));
        }
        write(pdf, xref.toString());
        write(pdf, "trailer\n<< /Size " + (totalObjects + 1) + " /Root " + catalogId + " 0 R >>\n");
        write(pdf, "startxref\n" + xrefStart + "\n%%EOF");
        return pdf.toByteArray();
    }

    // Encode object id in a 4-byte prefix marker so we can sort & strip it.
    private byte[] obj(int id, String dict) {
        String body = id + " 0 obj\n" + dict + "\nendobj\n";
        return withMarker(id, body.getBytes(WIN_ANSI));
    }

    private byte[] streamObj(int id, byte[] stream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, id + " 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
        out.writeBytes(stream);
        write(out, "\nendstream\nendobj\n");
        return withMarker(id, out.toByteArray());
    }

    private byte[] withMarker(int id, byte[] body) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write((id >>> 24) & 0xFF);
        out.write((id >>> 16) & 0xFF);
        out.write((id >>> 8) & 0xFF);
        out.write(id & 0xFF);
        out.writeBytes(body);
        return out.toByteArray();
    }

    private int objId(byte[] marked) {
        return ((marked[0] & 0xFF) << 24) | ((marked[1] & 0xFF) << 16)
                | ((marked[2] & 0xFF) << 8) | (marked[3] & 0xFF);
    }

    private byte[] stripIdMarker(byte[] marked) {
        byte[] out = new byte[marked.length - 4];
        System.arraycopy(marked, 4, out, 0, out.length);
        return out;
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(WIN_ANSI));
    }
}
