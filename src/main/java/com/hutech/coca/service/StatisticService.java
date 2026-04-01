package com.hutech.coca.service;

import com.hutech.coca.dto.MostBookedServiceResponse;
import com.hutech.coca.repository.IBookingRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticService {

    private final IBookingRepository bookingRepository;

    public List<MostBookedServiceResponse> getMostBookedServices(int limit) {
        return bookingRepository.findMostBookedServices(PageRequest.of(0, limit));
    }

    public byte[] exportMostBookedServicesToCsv(int limit) {
        List<MostBookedServiceResponse> stats = getMostBookedServices(limit);
        StringBuilder csvBuilder = new StringBuilder();
        // UTF-8 BOM for Excel compatibility
        csvBuilder.append('\ufeff');
        csvBuilder.append("Service ID,Service Name,Booking Count\n");

        for (MostBookedServiceResponse stat : stats) {
            csvBuilder.append(stat.getServiceId()).append(",");
            // Escape quotes inside the name if necessary
            String name = stat.getServiceName() != null ? stat.getServiceName().replace("\"", "\"\"") : "";
            csvBuilder.append("\"").append(name).append("\",");
            csvBuilder.append(stat.getBookingCount()).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportMostBookedServicesToPdf(int limit) {
        List<MostBookedServiceResponse> stats = getMostBookedServices(limit);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, baos);

            document.open();
            
            // Khởi tạo font hỗ trợ tiếng Việt Unicode
            Font fontTitle;
            Font fontHeader;
            Font fontData;
            try {
                // Lấy font Arial mặc định trên Windows
                String fontPath = "C:\\Windows\\Fonts\\arial.ttf";
                BaseFont bf = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                fontTitle = new Font(bf, 18, Font.BOLD);
                fontHeader = new Font(bf, 12, Font.BOLD);
                fontData = new Font(bf, 12, Font.NORMAL);
            } catch (Exception ex) {
                // Fallback nếu không có font Arial (nếu chạy trên OS khác)
                fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
                fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                fontData = FontFactory.getFont(FontFactory.HELVETICA, 12);
            }

            Paragraph title = new Paragraph("Thống kê Dịch vụ Nổi bật", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // empty line

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] {1.5f, 5.0f, 2.0f});
            table.setSpacingBefore(10);

            // Table Header
            writePdfTableHeader(table, fontHeader);

            // Table Data
            writePdfTableData(table, stats, fontData);

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating PDF export", e);
        }
    }

    private void writePdfTableHeader(PdfPTable table, Font font) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);

        cell.setPhrase(new Phrase("Mã Dịch Vụ", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Tên Dịch Vụ", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Số Lượng Đặt", font));
        table.addCell(cell);
    }

    private void writePdfTableData(PdfPTable table, List<MostBookedServiceResponse> stats, Font font) {
        for (MostBookedServiceResponse stat : stats) {
            table.addCell(new Phrase(String.valueOf(stat.getServiceId()), font));
            table.addCell(new Phrase(stat.getServiceName() != null ? stat.getServiceName() : "", font));
            table.addCell(new Phrase(String.valueOf(stat.getBookingCount()), font));
        }
    }
}
