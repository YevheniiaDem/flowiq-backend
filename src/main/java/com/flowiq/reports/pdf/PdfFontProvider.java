package com.flowiq.reports.pdf;

import com.flowiq.exception.BadRequestException;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Embedded Unicode fonts for OpenPDF.
 * DejaVu Sans is loaded from jasperreports-fonts (SIL Open Font License).
 */
@Component
@Slf4j
public class PdfFontProvider {

    private static final String DEJAVU_REGULAR = "/net/sf/jasperreports/fonts/dejavu/DejaVuSans.ttf";
    private static final String DEJAVU_BOLD = "/net/sf/jasperreports/fonts/dejavu/DejaVuSans-Bold.ttf";
    private static final String LOCAL_REGULAR = "/fonts/DejaVuSans.ttf";
    private static final String LOCAL_BOLD = "/fonts/DejaVuSans-Bold.ttf";

    private final BaseFont regular;
    private final BaseFont bold;

    public PdfFontProvider() {
        this.regular = loadFont(DEJAVU_REGULAR, LOCAL_REGULAR);
        this.bold = loadFont(DEJAVU_BOLD, LOCAL_BOLD);
        log.info("PDF fonts initialized (Unicode: {})", regular != null);
    }

    public Font regular(float size) {
        return font(regular, size, Font.NORMAL);
    }

    public Font bold(float size) {
        BaseFont base = bold != null ? bold : regular;
        return font(base, size, Font.BOLD);
    }

    public Font muted(float size) {
        Font font = regular(size);
        font.setColor(100, 116, 139);
        return font;
    }

    private Font font(BaseFont base, float size, int style) {
        if (base == null) {
            throw new BadRequestException("PDF font is not available");
        }
        return new Font(base, size, style);
    }

    private BaseFont loadFont(String primaryPath, String fallbackPath) {
        BaseFont fromPrimary = tryLoad(primaryPath);
        if (fromPrimary != null) {
            return fromPrimary;
        }
        if (fallbackPath != null) {
            BaseFont fromFallback = tryLoad(fallbackPath);
            if (fromFallback != null) {
                return fromFallback;
            }
        }
        if (primaryPath.equals(DEJAVU_REGULAR)) {
            log.warn("Unicode PDF font not found; falling back to Helvetica (Cyrillic may not render)");
            try {
                return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            } catch (IOException e) {
                throw new BadRequestException("Failed to initialize PDF fonts");
            }
        }
        return null;
    }

    private BaseFont tryLoad(String classpath) {
        try (InputStream input = getClass().getResourceAsStream(classpath)) {
            if (input == null) {
                return null;
            }
            byte[] bytes = input.readAllBytes();
            return BaseFont.createFont(
                    classpath,
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,
                    bytes,
                    null
            );
        } catch (IOException e) {
            log.debug("Could not load PDF font from {}", classpath, e);
            return null;
        }
    }
}
