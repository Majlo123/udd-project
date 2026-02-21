package com.udd.forensic.service;

import com.udd.forensic.dto.ForensicReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Generiše PDF forenzički izveštaj po šablonu sa:
 *  - Organization header box (gore-levo)
 *  - Page-fold decoration (gore-desno)
 *  - Naziv obrađenog fajla (centriran u okviru)
 *  - Klasifikacija + hash
 *  - Opis ponašanja u bordered box-u
 *  - Dva potpis-bloka (forenzičar + drugi forenzičar)
 */
@Slf4j
@Service
public class PdfGenerationService {

    private static final float MARGIN = 50;
    private static final float FONT_TITLE = 16;
    private static final float FONT_BODY = 10;
    private static final float FONT_SMALL = 9;
    private static final float LEADING = 14;

    public MultipartFile generatePdf(ForensicReportDTO dto) {
        log.info("Generating forensic report PDF for investigator: {}", dto.getForensicInvestigator());

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            float pw = page.getMediaBox().getWidth();
            float ph = page.getMediaBox().getHeight();
            float contentW = pw - 2 * MARGIN;

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            String datum = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            String hash = dto.getFileHash() != null && !dto.getFileHash().isBlank()
                    ? dto.getFileHash() : "N/A";
            String malwareName = dto.getMalwareName() != null && !dto.getMalwareName().isBlank()
                    ? dto.getMalwareName() : "Nepoznat";
            String description = dto.getDescription() != null && !dto.getDescription().isBlank()
                    ? dto.getDescription() : "Opis nije unet.";

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {

                float y = ph - MARGIN;

                // ================================================================
                // 1. ORGANIZATION BOX (top-left)
                // ================================================================
                float boxW = 230;
                float boxH = 65;
                cs.setLineWidth(0.8f);
                cs.addRect(MARGIN, y - boxH, boxW, boxH);
                cs.stroke();

                float txY = y - 14;
                cs.beginText();
                cs.setFont(bold, FONT_BODY);
                cs.newLineAtOffset(MARGIN + 6, txY);
                cs.showText("Organizacija " + safe(dto.getOrganization()));
                cs.endText();

                txY -= LEADING;
                cs.beginText();
                cs.setFont(regular, FONT_SMALL);
                cs.newLineAtOffset(MARGIN + 6, txY);
                cs.showText(safe(dto.getCity()));
                cs.endText();

                txY -= LEADING - 2;
                cs.beginText();
                cs.setFont(regular, FONT_SMALL);
                cs.newLineAtOffset(MARGIN + 6, txY);
                cs.showText("Datum: " + datum);
                cs.endText();

                // Page-fold triangle (top-right decoration)
                float foldSize = 30;
                cs.setLineWidth(0.5f);
                cs.moveTo(pw - MARGIN - foldSize, ph - MARGIN);
                cs.lineTo(pw - MARGIN, ph - MARGIN);
                cs.lineTo(pw - MARGIN, ph - MARGIN - foldSize);
                cs.lineTo(pw - MARGIN - foldSize, ph - MARGIN);
                cs.stroke();

                y = y - boxH - 30;

                // ================================================================
                // 2. NAZIV OBRAĐENOG FAJLA — centered bordered title
                // ================================================================
                float titleBoxW = contentW * 0.75f;
                float titleBoxH = 30;
                float titleBoxX = (pw - titleBoxW) / 2;

                cs.setLineWidth(1f);
                cs.addRect(titleBoxX, y - titleBoxH, titleBoxW, titleBoxH);
                cs.stroke();

                String titleText = safe(malwareName);
                float titleTextW = bold.getStringWidth(titleText) / 1000 * FONT_TITLE;
                cs.beginText();
                cs.setFont(bold, FONT_TITLE);
                cs.newLineAtOffset((pw - titleTextW) / 2, y - titleBoxH + 9);
                cs.showText(titleText);
                cs.endText();

                y = y - titleBoxH - 25;

                // ================================================================
                // 3. KLASIFIKACIJA + HASH
                // ================================================================
                cs.beginText();
                cs.setFont(regular, FONT_BODY);
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Klasifikacija: ");
                cs.setFont(bold, FONT_BODY);
                cs.showText(safe(dto.getClassification()));
                cs.setFont(regular, FONT_BODY);
                cs.showText(",  ");
                cs.setFont(bold, FONT_BODY);
                cs.showText(hash);
                cs.endText();

                y -= LEADING + 8;

                // ================================================================
                // 4. INTRODUCTORY SENTENCE
                // ================================================================
                String intro = "Prilozen fajl predstavlja artefakt koji ukazuje na "
                        + safe(malwareName)
                        + ". Opis ponasanja malvera/pretnje:";
                y = drawWrappedText(cs, intro, regular, FONT_BODY, MARGIN, contentW, y);
                y -= 12;

                // ================================================================
                // 5. OPIS PONAŠANJA — large bordered box
                // ================================================================
                float descBoxH = 200;
                float descBoxW = contentW - 10;
                float descBoxX = MARGIN + 5;
                float descBoxY = y - descBoxH;

                cs.setLineWidth(0.8f);
                cs.addRect(descBoxX, descBoxY, descBoxW, descBoxH);
                cs.stroke();

                drawWrappedText(cs, description, regular, FONT_BODY,
                        descBoxX + 10, descBoxW - 20, y - 16);

                y = descBoxY - 40;

                // ================================================================
                // 6. POTPISI — two signature boxes
                // ================================================================
                float sigBoxW = 170;
                float sigBoxH = 60;
                float sig1X = MARGIN + 20;
                float sig2X = pw - MARGIN - sigBoxW - 20;
                float sigY = Math.max(y - sigBoxH, 40);

                // --- Signature 1: Forenzičar ---
                cs.setLineWidth(0.8f);
                cs.addRect(sig1X, sigY, sigBoxW, sigBoxH);
                cs.stroke();

                String name1 = safe(dto.getForensicInvestigator());
                float name1W = regular.getStringWidth(name1) / 1000 * FONT_BODY;
                cs.beginText();
                cs.setFont(regular, FONT_BODY);
                cs.newLineAtOffset(sig1X + (sigBoxW - name1W) / 2, sigY + sigBoxH - 18);
                cs.showText(name1);
                cs.endText();

                float lineY = sigY + sigBoxH - 24;
                cs.setLineWidth(0.5f);
                cs.moveTo(sig1X + 15, lineY);
                cs.lineTo(sig1X + sigBoxW - 15, lineY);
                cs.stroke();

                String lbl1 = "Potpis forenzicara";
                float lbl1W = italic.getStringWidth(lbl1) / 1000 * FONT_SMALL;
                cs.beginText();
                cs.setFont(italic, FONT_SMALL);
                cs.newLineAtOffset(sig1X + (sigBoxW - lbl1W) / 2, sigY + 10);
                cs.showText(lbl1);
                cs.endText();

                // --- Signature 2: Drugi forenzičar ---
                cs.addRect(sig2X, sigY, sigBoxW, sigBoxH);
                cs.stroke();

                String name2 = "___________________";
                float name2W = regular.getStringWidth(name2) / 1000 * FONT_BODY;
                cs.beginText();
                cs.setFont(regular, FONT_BODY);
                cs.newLineAtOffset(sig2X + (sigBoxW - name2W) / 2, sigY + sigBoxH - 18);
                cs.showText(name2);
                cs.endText();

                cs.moveTo(sig2X + 15, lineY);
                cs.lineTo(sig2X + sigBoxW - 15, lineY);
                cs.stroke();

                String lbl2 = "Potpis drugog forenzicara";
                float lbl2W = italic.getStringWidth(lbl2) / 1000 * FONT_SMALL;
                cs.beginText();
                cs.setFont(italic, FONT_SMALL);
                cs.newLineAtOffset(sig2X + (sigBoxW - lbl2W) / 2, sigY + 10);
                cs.showText(lbl2);
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            byte[] pdfBytes = baos.toByteArray();

            String filename = "izvestaj-" + sanitize(dto.getForensicInvestigator()) + "-"
                    + System.currentTimeMillis() + ".pdf";

            log.info("PDF generated: {} ({} bytes)", filename, pdfBytes.length);
            return new InMemoryMultipartFile(filename, pdfBytes);

        } catch (Exception e) {
            throw new RuntimeException("Greska pri generisanju PDF-a: " + e.getMessage(), e);
        }
    }

    // ==================== In-memory MultipartFile ====================

    private record InMemoryMultipartFile(String filename, byte[] data) implements MultipartFile {
        @Override public String getName() { return "file"; }
        @Override public String getOriginalFilename() { return filename; }
        @Override public String getContentType() { return "application/pdf"; }
        @Override public boolean isEmpty() { return data.length == 0; }
        @Override public long getSize() { return data.length; }
        @Override public byte[] getBytes() { return data; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(data); }
        @Override public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) { fos.write(data); }
        }
    }

    // ==================== Helpers ====================

    private float drawWrappedText(PDPageContentStream cs, String text,
                                  PDType1Font font, float fontSize,
                                  float startX, float maxWidth, float y) throws Exception {
        if (text == null || text.isBlank()) return y;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(startX, y);

        for (String word : words) {
            String testLine = line.isEmpty() ? word : line + " " + word;
            float testWidth = font.getStringWidth(testLine) / 1000 * fontSize;

            if (testWidth > maxWidth && !line.isEmpty()) {
                cs.showText(line.toString());
                cs.newLineAtOffset(0, -LEADING);
                y -= LEADING;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (!line.isEmpty()) {
            cs.showText(line.toString());
            y -= LEADING;
        }
        cs.endText();
        return y;
    }

    private String safe(String input) {
        return (input == null || input.isBlank()) ? "N/A" : input;
    }

    private String sanitize(String input) {
        if (input == null) return "unknown";
        return input.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase();
    }
}
