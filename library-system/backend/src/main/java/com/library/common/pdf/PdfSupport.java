package com.library.common.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.oned.Code128Writer;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

public final class PdfSupport {

    private PdfSupport() {
    }

    public static byte[] barcodeLabels(List<Label> labels) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 8);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        for (Label label : labels) {
            PdfPCell cell = new PdfPCell();
            cell.setPadding(8);
            cell.setBorderColor(Color.LIGHT_GRAY);
            Paragraph p = new Paragraph(label.title(), titleFont);
            p.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(p);
            Image barcode = Image.getInstance(code128Png(label.barcode(), 220, 60));
            barcode.scaleToFit(140, 40);
            barcode.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(barcode);
            Paragraph code = new Paragraph(label.barcode(), small);
            code.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(code);
            table.addCell(cell);
        }
        int rem = labels.size() % 3;
        if (rem != 0) {
            for (int i = 0; i < 3 - rem; i++) {
                PdfPCell empty = new PdfPCell(new Phrase(" "));
                empty.setBorder(PdfPCell.NO_BORDER);
                table.addCell(empty);
            }
        }
        document.add(table);
        document.close();
        return out.toByteArray();
    }

    public static byte[] borrowSlipPdf(String libraryName, String slipCode, String readerName,
                                       String readerCode, List<String[]> itemRows) throws Exception {
        Document document = new Document(PageSize.A5, 28, 28, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        Font h1 = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
        document.add(new Paragraph(libraryName == null ? "Thư viện" : libraryName, h1));
        document.add(new Paragraph("PHIẾU MƯỢN SÁCH", h1));
        document.add(new Paragraph("Mã phiếu: " + slipCode, normal));
        document.add(new Paragraph("Độc giả: " + readerName + " (" + readerCode + ")", normal));
        Image qr = Image.getInstance(qrPng(slipCode, 120, 120));
        qr.scaleAbsolute(80, 80);
        document.add(qr);
        document.add(new Paragraph(" "));
        PdfPTable table = new PdfPTable(new float[]{3, 2, 2});
        table.setWidthPercentage(100);
        table.addCell(header("Tên sách"));
        table.addCell(header("Barcode"));
        table.addCell(header("Hạn trả"));
        for (String[] row : itemRows) {
            table.addCell(new Phrase(row[0], normal));
            table.addCell(new Phrase(row[1], normal));
            table.addCell(new Phrase(row[2], normal));
        }
        document.add(table);
        document.add(new Paragraph(" "));
        document.add(new Paragraph("Vui lòng trả đúng hạn. Trả trễ sẽ bị phạt theo quy định.", normal));
        document.close();
        return out.toByteArray();
    }

    private static PdfPCell header(String text) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(Color.LIGHT_GRAY);
        return cell;
    }

    private static byte[] code128Png(String content, int width, int height) throws Exception {
        BitMatrix matrix = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private static byte[] qrPng(String content, int width, int height) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    public record Label(String title, String barcode) {
    }
}
