package com.spite.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

            // Grupiši po vremenu (jedan termin = jedan blok, klijenti ispod)
            Map<Long, List<ScheduledSession>> byTime = new LinkedHashMap<>();
            for (ScheduledSession s : list) {
                byTime.computeIfAbsent(s.getStartTime(), k -> new ArrayList<>()).add(s);
            }

            StringBuilder blocks = new StringBuilder();
            for (Map.Entry<Long, List<ScheduledSession>> te : byTime.entrySet()) {
                List<ScheduledSession> group = te.getValue();
                String time = TIME_FMT.format(Instant.ofEpochMilli(te.getKey()));
                int dur = group.get(0).getDurationMinutes();

                blocks.append("<div style=\"margin-bottom:18px\">")
                      .append("<div style=\"font-size:16px;font-weight:700;color:#111;")
                      .append("border-bottom:2px solid #111;padding-bottom:6px;margin-bottom:8px\">")
                      .append(time)
                      .append(" <span style=\"color:#888;font-weight:400;font-size:13px\">&middot; ")
                      .append(dur).append(" min</span></div>")
                      .append("<ul style=\"margin:0;padding-left:20px;color:#333;font-size:14px;line-height:1.9\">");
                for (ScheduledSession s : group) {
                    blocks.append("<li>")
                          .append(escape(s.getClientUsername()))
                          .append(s.isCustom() ? " <span style=\"color:#999;font-size:11px\">(custom)</span>" : "")
                          .append("</li>");
                }
                blocks.append("</ul></div>");
            }

            int terminCount = byTime.size();
            String html = "<div style=\"font-family:sans-serif;max-width:520px;margin:auto\">"
                    + "<h2 style=\"color:#111;margin-bottom:4px\">Today's schedule 📅</h2>"
                    + "<p style=\"color:#555;margin-top:0\">" + dateStr + " &mdash; "
                    + terminCount + " session" + (terminCount == 1 ? "" : "s") + "</p>"
                    + blocks
                    + "<p style=\"color:#888;font-size:12px;margin-top:20px\">Spite</p></div>";

            emailService.send(trainer.getEmail(), "Today's schedule - " + dateStr, html);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
