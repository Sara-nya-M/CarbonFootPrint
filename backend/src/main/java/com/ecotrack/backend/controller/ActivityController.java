package com.ecotrack.backend.controller;

import com.ecotrack.backend.dto.ActivityRequest;
import com.ecotrack.backend.model.DailyActivity;
import com.ecotrack.backend.model.User;
import com.ecotrack.backend.repository.UserRepository;
import com.ecotrack.backend.repository.DailyActivityRepository;
import com.ecotrack.backend.service.ActivityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DailyActivityRepository activityRepository;

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @PostMapping
    public ResponseEntity<?> logActivity(@Valid @RequestBody ActivityRequest request) {
        try {
            User user = getAuthenticatedUser();
            DailyActivity savedActivity = activityService.saveActivity(user, request);
            return ResponseEntity.ok(savedActivity);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/today")
    public ResponseEntity<?> getTodayActivity() {
        User user = getAuthenticatedUser();
        return activityRepository.findByUserAndDate(user, LocalDate.now())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.ok(new HashMap<>()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<DailyActivity>> getHistory() {
        User user = getAuthenticatedUser();
        List<DailyActivity> history = activityRepository.findByUserOrderByDateDesc(user);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/report/pdf")
    public void exportToPDF(HttpServletResponse response) {
        response.setContentType("application/pdf");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=ecotrack_report_" + LocalDate.now() + ".pdf";
        response.setHeader(headerKey, headerValue);

        User user = getAuthenticatedUser();
        List<DailyActivity> history = activityRepository.findByUserOrderByDateDesc(user);

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, response.getOutputStream());

            document.open();

            // Font styles
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, new java.awt.Color(46, 125, 50));
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new java.awt.Color(97, 97, 97));
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, java.awt.Color.WHITE);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, java.awt.Color.BLACK);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, new java.awt.Color(158, 158, 158));

            // Title
            Paragraph title = new Paragraph("EcoTrack Carbon Footprint Report", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Subtitle
            Paragraph subtitle = new Paragraph("Generated on: " + LocalDate.now() + " | User: " + user.getEmail(), subTitleFont);
            subtitle.setAlignment(Paragraph.ALIGN_CENTER);
            subtitle.setSpacingAfter(25);
            document.add(subtitle);

            // Table with 7 columns
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setWidths(new float[] {2.0f, 2.0f, 1.8f, 1.8f, 1.8f, 1.8f, 1.8f});

            // Headers
            String[] headers = {"Date", "Commute", "Commute CO2", "Electric CO2", "Food CO2", "Plastic CO2", "Total (kg)"};
            java.awt.Color headerColor = new java.awt.Color(46, 125, 50);

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setBackgroundColor(headerColor);
                cell.setPadding(8);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Populate rows
            double grandTotal = 0.0;
            for (DailyActivity activity : history) {
                table.addCell(new PdfPCell(new Phrase(activity.getDate().toString(), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(activity.getTransportType() + " (" + activity.getTransportDistance() + "km)", bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", activity.getTransportCarbon()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", activity.getElectricityCarbon()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", activity.getFoodCarbon()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", activity.getPlasticCarbon()), bodyFont)));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", activity.getCarbonFootprintTotal()), bodyFont)));
                grandTotal += activity.getCarbonFootprintTotal();
            }

            document.add(table);

            // Total section
            Paragraph totalSection = new Paragraph(String.format("\nGrand Total Carbon Footprint: %.2f kg CO2", grandTotal), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new java.awt.Color(46, 125, 50)));
            totalSection.setAlignment(Paragraph.ALIGN_RIGHT);
            totalSection.setSpacingAfter(30);
            document.add(totalSection);

            // Footer
            Paragraph footer = new Paragraph("Thank you for using EcoTrack to monitor and reduce your carbon footprint.\nEvery small action counts towards a sustainable future!", footerFont);
            footer.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
