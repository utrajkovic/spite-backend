package com.spite.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.spite.backend.model.ScheduledSession;
import com.spite.backend.model.User;
import com.spite.backend.repository.ScheduledSessionRepository;
import com.spite.backend.repository.UserRepository;

@Component
public class DailyScheduleEmailJob {

    private static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
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

    // Radi svakog minuta; šalje trenerima čije izabrano vreme se poklapa sa sada
    @Scheduled(cron = "0 * * * * *", zone = "Europe/Belgrade")
    public void tick() {
        if (!emailService.isConfigured()) return;

        String now = HHMM.format(ZonedDateTime.now(ZONE));
        List<User> trainers = userRepo.findByDailyReminderEnabledTrueAndDailyReminderTime(now);
        if (trainers.isEmpty()) return;

        LocalDate today = LocalDate.now(ZONE);
        long start = today.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long end = today.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
        String dateStr = DATE_FMT.format(Instant.ofEpochMilli(start));

        for (User trainer : trainers) {
            if (!trainer.isEmailVerified() || trainer.getEmail() == null || trainer.getEmail().isBlank()) {
                continue;
            }

            List<ScheduledSession> list = sessionRepo.findByTrainerUsername(trainer.getUsername())
                    .stream()
                    .filter(s -> s.getStartTime() >= start && s.getStartTime() < end)
                    .sorted(Comparator.comparingLong(ScheduledSession::getStartTime))
                    .toList();

            if (list.isEmpty()) continue;

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
