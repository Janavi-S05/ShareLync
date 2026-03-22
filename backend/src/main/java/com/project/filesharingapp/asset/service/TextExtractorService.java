package com.project.filesharingapp.asset.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class TextExtractorService {

    public String extractText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return "";

        String ext = getExtension(filename).toLowerCase();
        log.info("Extracting text from file: {} (type: {})", filename, ext);

        try (InputStream is = file.getInputStream()) {
            return switch (ext) {
                case "pdf"        -> extractFromPdf(is);
                case "docx", "doc" -> extractFromDocx(is);
                case "txt", "md", "json" -> extractFromPlainText(is);
                default -> "";
            };
        } catch (Exception e) {
            log.warn("Text extraction failed for {}: {}", filename, e.getMessage());
            return "";
        }
    }

    private String extractFromPdf(InputStream is) throws Exception {
        try (PDDocument doc = Loader.loadPDF(is.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setEndPage(3); // read first 3 pages only for speed
            String text = stripper.getText(doc);
            return truncate(text);
        }
    }

    private String extractFromDocx(InputStream is) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return truncate(extractor.getText());
        }
    }

    private String extractFromPlainText(InputStream is) throws Exception {
        String text = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        return truncate(text);
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 3000 ? text.substring(0, 3000) : text;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }
}