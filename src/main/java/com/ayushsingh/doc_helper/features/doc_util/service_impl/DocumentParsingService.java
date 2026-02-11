package com.ayushsingh.doc_helper.features.doc_util.service_impl;

import com.ayushsingh.doc_helper.core.exception_handling.ExceptionCodes;
import com.ayushsingh.doc_helper.core.exception_handling.exceptions.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.InputStream;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DocumentParsingService {

    public String extractText(Resource resource, String originalFilename) {
        if (resource == null) {
            throw new BaseException("Failed to parse document", ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }

        String name = originalFilename != null ? originalFilename.toLowerCase() : "";
        // TODO: Add GROBID-based parsing for research papers
        // TODO: Add OCR for image/chart analysis

        try {
            if (name.endsWith(".pdf")) {
                return sanitize(parsePdf(resource));
            }
            if (name.endsWith(".docx")) {
                return sanitize(parseDocx(resource));
            }
            if (name.endsWith(".txt")) {
                return sanitize(parseTxt(resource));
            }

            return sanitize(parseWithTika(resource));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse document: {}", originalFilename, e);
            throw new BaseException("Failed to parse document", ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }
    }

    private String parsePdf(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {
            StringBuilder sb = new StringBuilder();
            ObjectExtractor extractor = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm spreadsheetAlgorithm =
                    new SpreadsheetExtractionAlgorithm();
            BasicExtractionAlgorithm basicAlgorithm =
                    new BasicExtractionAlgorithm();

            int totalPages = document.getNumberOfPages();
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                int pageNumber = pageIndex + 1;
                PDPage page = document.getPage(pageIndex);

                String pageText = extractPageText(document, page, pageNumber);
                if (!pageText.isBlank()) {
                    sb.append(pageText).append("\n\n");
                }

                List<String> tables = extractTablesForPage(
                        extractor,
                        spreadsheetAlgorithm,
                        basicAlgorithm,
                        pageNumber);
                if (!tables.isEmpty()) {
                    for (String table : tables) {
                        sb.append("\n[TABLE_START]\n")
                                .append(table)
                                .append("\n[TABLE_END]\n\n");
                    }
                }
            }

            return sb.toString();
        }
    }

    private String parseDocx(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder sb = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    appendParagraph(sb, paragraph);
                } else if (element instanceof XWPFTable table) {
                    appendTable(sb, table);
                }
            }
            return sb.toString();
        }
    }

    private void appendParagraph(StringBuilder sb, XWPFParagraph paragraph) {
        String text = paragraph.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        if (isHeading(paragraph)) {
            sb.append("\n").append(text.trim()).append("\n\n");
        } else {
            sb.append(text.trim()).append("\n\n");
        }
    }

    private void appendTable(StringBuilder sb, XWPFTable table) {
        sb.append("\n[TABLE_START]\n");
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = row.getTableCells().stream()
                    .map(XWPFTableCell::getText)
                    .map(text -> text == null ? "" : text.trim())
                    .toList();
            String line = String.join(" | ", cells).trim();
            if (!line.isBlank()) {
                sb.append(line).append("\n");
            }
        }
        sb.append("[TABLE_END]\n\n");
    }

    private boolean isHeading(XWPFParagraph paragraph) {
        String style = paragraph.getStyle();
        if (style == null) {
            return false;
        }
        String normalized = style.toLowerCase();
        return normalized.contains("heading")
                || normalized.contains("title")
                || normalized.contains("subtitle");
    }

    private String parseTxt(Resource resource) throws Exception {
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            CharsetDetector detector = new CharsetDetector();
            detector.setText(bytes);
            CharsetMatch match = detector.detect();
            Charset charset = match != null
                    ? Charset.forName(match.getName())
                    : StandardCharsets.UTF_8;
            return new String(bytes, charset);
        }
    }

    private String parseWithTika(Resource resource) {
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<org.springframework.ai.document.Document> documents = reader.get();
        if (documents.isEmpty()) {
            throw new BaseException("Failed to parse document", ExceptionCodes.DOCUMENT_PARSING_FAILED);
        }
        return documents.stream()
                .map(org.springframework.ai.document.Document::getFormattedContent)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.joining("\n"));
    }

    private String sanitize(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        normalized = normalized.replaceAll("\\n{3,}", "\n\n");
        return normalized.trim();
    }

    private String extractPageText(PDDocument document, PDPage page, int pageNumber) throws IOException {
        if (isLikelyTwoColumn(page)) {
            return extractTwoColumnText(page);
        }
        PDFTextStripper stripper = newConfiguredStripper();
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        return stripper.getText(document).trim();
    }

    private PDFTextStripper newConfiguredStripper() throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setLineSeparator("\n");
        stripper.setParagraphStart("\n\n");
        stripper.setParagraphEnd("\n\n");
        stripper.setPageStart("\n\n");
        stripper.setPageEnd("\n\n");
        return stripper;
    }

    private boolean isLikelyTwoColumn(PDPage page) throws IOException {
        float width = page.getMediaBox().getWidth();
        ColumnStatsStripper stripper = new ColumnStatsStripper(width);
        stripper.process(page);
        return stripper.isTwoColumn();
    }

    private String extractTwoColumnText(PDPage page) throws IOException {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float mid = width / 2f;
        float gap = 10f;

        Rectangle2D leftRect = new Rectangle2D.Float(
                0f, 0f, Math.max(0f, mid - gap), height);
        Rectangle2D rightRect = new Rectangle2D.Float(
                mid + gap, 0f, Math.max(0f, width - mid - gap), height);

        PDFTextStripperByArea stripper = new PDFTextStripperByArea();
        stripper.setSortByPosition(true);
        stripper.addRegion("left", leftRect);
        stripper.addRegion("right", rightRect);
        stripper.extractRegions(page);

        String left = stripper.getTextForRegion("left");
        String right = stripper.getTextForRegion("right");
        return (left + "\n\n" + right).trim();
    }

    private List<String> extractTablesForPage(
            ObjectExtractor extractor,
            SpreadsheetExtractionAlgorithm spreadsheetAlgorithm,
            BasicExtractionAlgorithm basicAlgorithm,
            int pageNumber
    ) {
        try {
            Page page = extractor.extract(pageNumber);
            List<Table> tables = spreadsheetAlgorithm.extract(page);
            if (tables == null || tables.isEmpty()) {
                tables = basicAlgorithm.extract(page);
            }
            if (tables == null || tables.isEmpty()) {
                return List.of();
            }

            List<String> formatted = new ArrayList<>();
            for (Table table : tables) {
                if (looksLikeTable(table)) {
                    String formattedTable = formatTable(table);
                    if (!formattedTable.isBlank()) {
                        formatted.add(formattedTable);
                    }
                }
            }
            return formatted;
        } catch (Exception e) {
            log.debug("Table extraction failed on page {}", pageNumber, e);
            return List.of();
        }
    }

    private boolean looksLikeTable(Table table) {
        List<List<RectangularTextContainer<?>>> rows = getTableRows(table);
        if (rows == null || rows.size() < 2) {
            return false;
        }
        int maxCols = rows.stream().mapToInt(List::size).max().orElse(0);
        if (maxCols < 2) {
            return false;
        }
        long nonEmptyCells = rows.stream()
                .flatMap(List::stream)
                .map(cell -> cell == null ? "" : cell.getText().trim())
                .filter(text -> !text.isBlank())
                .count();
        return nonEmptyCells >= 4;
    }

    private String formatTable(Table table) {
        StringBuilder sb = new StringBuilder();
        for (List<RectangularTextContainer<?>> row : getTableRows(table)) {
            List<String> cells = row.stream()
                    .map(cell -> cell == null ? "" : cell.getText().trim())
                    .map(text -> text.replaceAll("\\s+", " "))
                    .toList();
            String line = String.join(" | ", cells).trim();
            if (!line.isBlank()) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }

    @SuppressWarnings("unchecked")
    private List<List<RectangularTextContainer<?>>> getTableRows(Table table) {
        return (List<List<RectangularTextContainer<?>>>)(List<?>) table.getRows();
    }

    private static final class ColumnStatsStripper extends PDFTextStripper {
        private final float leftBoundary;
        private final float rightBoundary;
        private int leftCount;
        private int rightCount;
        private int middleCount;

        ColumnStatsStripper(float pageWidth) throws IOException {
            this.leftBoundary = pageWidth * 0.45f;
            this.rightBoundary = pageWidth * 0.55f;
            setSortByPosition(true);
        }

        void process(PDPage page) throws IOException {
            super.processPage(page);
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            float center = text.getXDirAdj() + (text.getWidthDirAdj() / 2f);
            if (center < leftBoundary) {
                leftCount++;
            } else if (center > rightBoundary) {
                rightCount++;
            } else {
                middleCount++;
            }
        }

        boolean isTwoColumn() {
            int total = leftCount + rightCount + middleCount;
            if (total < 50) {
                return false;
            }
            double leftRatio = leftCount / (double) total;
            double rightRatio = rightCount / (double) total;
            double middleRatio = middleCount / (double) total;
            return leftRatio > 0.25 && rightRatio > 0.25 && middleRatio < 0.2;
        }
    }
}
