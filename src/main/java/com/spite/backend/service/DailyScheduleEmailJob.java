package com.spite.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.spite.backend.model.ScheduledSession;
import com.spite.backend.model.User;
import com.spite.backend.repository.ScheduledSessionRepository;
import com.spite.backend.repository.UserRepository;

@Component
public class DailyScheduleEmailJob {

    private static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONE);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy.").withZone(ZONE);

    private final ScheduledSessionRepository sessionRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;

    public DailyScheduleEmailJob(ScheduledSessionRepository sessionRepo,
            UserRepository userRepo,
            EmailService emailService) {
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    // Svaki dan u 00:00 (Europe/Belgrade) — trener dobije listu termina za taj dan
    @Scheduled(cron = "0 0 0 * * *", zone = "Europe/Belgrade")
    public void sendDailySchedules() {
        if (!emailService.isConfigured()) return;

        LocalDate today = LocalDate.now(ZONE);
        long start = today.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long end = today.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();

        List<ScheduledSession> todays = sessionRepo.findByStartTimeBetween(start, end);
        if (todays.isEmpty()) return;

        Map<String, List<ScheduledSession>> byTrainer = new LinkedHashMap<>();
        for (ScheduledSession s : todays) {
            byTrainer.computeIfAbsent(s.getTrainerUsername(), k -> new ArrayList<>()).add(s);
        }

        String dateStr = DATE_FMT.format(Instant.ofEpochMilli(start));

        for (Map.Entry<String, List<ScheduledSession>> e : byTrainer.entrySet()) {
            User trainer = userRepo.findByUsername(e.getKey()).orElse(null);
            if (trainer == null || !trainer.isEmailVerified()
                    || trainer.getEmail() == null || trainer.getEmail().isBlank()) {
                continue;
            }

            List<ScheduledSession> list = e.getValue();
            list.sort(Comparator.comparingLong(ScheduledSession::getStartTime));

            StringBuilder rows = new StringBuilder();
            for (ScheduledSession s : list) {
                rows.append("<tr>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #eee;font-weight:700\">")
                    .append(TIME_FMT.format(Instant.ofEpochMilli(s.getStartTime())))
                    .append("</td>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #eee\">")
                    .append(escape(s.getClientUsername()))
                    .append(s.isCustom() ? " <span style=\"color:#999;font-size:11px\">(custom)</span>" : "")
                    .append("</td>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #eee;color:#888\">")
                    .append(s.getDurationMinutes()).append(" min")
                    .append("</td>")
                    .append("</tr>");
            }

            String html = "<div style=\"font-family:sans-serif;max-width:520px;margin:auto\">"
                    + "<h2 style=\"color:#111\">Today's schedule 📅</h2>"
                    + "<p style=\"color:#555\">" + dateStr + " &mdash; " + list.size() + " session(s)</p>"
                    + "<table style=\"width:100%;border-collapse:collapse;font-size:14px\">"
                    + "<tr>"
                    + "<th style=\"text-align:left;padding:8px 12px;border-bottom:2px solid #111\">Time</th>"
                    + "<th style=\"text-align:left;padding:8px 12px;border-bottom:2px solid #111\">Client</th>"
                    + "<th style=\"text-align:left;padding:8px 12px;border-bottom:2px solid #111\">Duration</th>"
                    + "</tr>"
                    + rows
                    + "</table>"
                    + "<p style=\"color:#888;font-size:12px;margin-top:20px\">Spite</p></div>";

            emailService.send(trainer.getEmail(), "Today's schedule - " + dateStr, html);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
