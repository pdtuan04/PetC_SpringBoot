package com.hutech.coca.service;

import com.hutech.coca.dto.MostBookedServiceResponse;
import com.hutech.coca.repository.IBookingRepository;
import com.lowagie.text.*;
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
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD);
            fontTitle.setSize(18);

            Paragraph title = new Paragraph("Most Booked Services Statistics", fontTitle);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" ")); // empty line

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100f);
            table.setWidths(new float[] {1.5f, 5.0f, 2.0f});
            table.setSpacingBefore(10);

            // Table Header
            writePdfTableHeader(table);

            // Table Data
            writePdfTableData(table, stats);

            document.add(table);
            document.close();

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error while generating PDF export", e);
        }
    }

    private void writePdfTableHeader(PdfPTable table) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(5);
        Font font = FontFactory.getFont(FontFactory.HELVETICA_BOLD);

        cell.setPhrase(new Phrase("Service ID", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Service Name", font));
        table.addCell(cell);

        cell.setPhrase(new Phrase("Booking Count", font));
        table.addCell(cell);
    }

    private void writePdfTableData(PdfPTable table, List<MostBookedServiceResponse> stats) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA);
        for (MostBookedServiceResponse stat : stats) {
            table.addCell(new Phrase(String.valueOf(stat.getServiceId()), font));
            table.addCell(new Phrase(stat.getServiceName() != null ? stat.getServiceName() : "", font));
            table.addCell(new Phrase(String.valueOf(stat.getBookingCount()), font));
        }
    }
}
