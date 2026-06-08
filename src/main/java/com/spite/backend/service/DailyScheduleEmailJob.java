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
import com.spite.backend.model.WorkoutReminder;
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

        LocalDate today = LocalDate.now(ZONE);
        long start = today.atStartOfDay(ZONE).toInstant().toEpochMilli();
        long end = today.plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
        String dateStr = DATE_FMT.format(Instant.ofEpochMilli(start));
        int todayIso = today.getDayOfWeek().getValue(); // 1=Pon ... 7=Ned

        // ── 1) TRENERI: dnevni raspored termina ──
        List<User> trainers = userRepo.findByDailyReminderEnabledTrueAndDailyReminderTime(now);
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

        // ── 2) KLIJENTI: podsetnik za trening (SESSIONS ili CUSTOM) ──
        processClientReminders(now, todayIso, start, end, dateStr);
    }

    private void processClientReminders(String now, int todayIso, long start, long end, String dateStr) {
        List<User> clients = userRepo.findByClientReminderEnabledTrue();
        for (User u : clients) {
            if (!u.isEmailVerified() || u.getEmail() == null || u.getEmail().isBlank()) {
                continue;
            }

            String mode = u.getClientReminderMode() == null ? "SESSIONS" : u.getClientReminderMode();

            if ("CUSTOM".equals(mode)) {
                if (u.getCustomReminders() == null) continue;
                for (WorkoutReminder r : u.getCustomReminders()) {
                    if (r.getDays() == null || !r.getDays().contains(todayIso)) continue;
                    if (!now.equals(r.getTime())) continue;
                    emailService.send(u.getEmail(),
                            "Workout reminder - " + dateStr,
                            buildCustomReminderEmail(dateStr, r.getNote()));
                }
            } else { // SESSIONS — trenerovi zakazani termini gde je korisnik klijent
                String time = u.getClientReminderTime() == null ? "07:00" : u.getClientReminderTime();
                if (!now.equals(time)) continue;

                List<ScheduledSession> list = sessionRepo.findByClientUsername(u.getUsername())
                        .stream()
                        .filter(s -> s.getStartTime() >= start && s.getStartTime() < end)
                        .sorted(Comparator.comparingLong(ScheduledSession::getStartTime))
                        .toList();
                if (list.isEmpty()) continue;

                emailService.send(u.getEmail(),
                        "Today's training - " + dateStr,
                        buildClientSessionsEmail(dateStr, list));
            }
        }
    }

    private String buildCustomReminderEmail(String dateStr, String note) {
        String body = (note == null || note.isBlank())
                ? "<p style=\"color:#333;font-size:15px;line-height:1.7\">Time to train! 💪</p>"
                : "<div style=\"background:#f5f5f5;border-radius:10px;padding:16px;color:#222;"
                        + "font-size:15px;line-height:1.7;white-space:pre-line\">" + escape(note) + "</div>";

        return "<div style=\"font-family:sans-serif;max-width:520px;margin:auto\">"
                + "<h2 style=\"color:#111;margin-bottom:4px\">Workout reminder 💪</h2>"
                + "<p style=\"color:#555;margin-top:0\">" + dateStr + "</p>"
                + body
                + "<p style=\"color:#888;font-size:12px;margin-top:20px\">Spite</p></div>";
    }

    private String buildClientSessionsEmail(String dateStr, List<ScheduledSession> list) {
        StringBuilder blocks = new StringBuilder();
        for (ScheduledSession s : list) {
            String time = TIME_FMT.format(Instant.ofEpochMilli(s.getStartTime()));
            blocks.append("<div style=\"margin-bottom:14px\">")
                  .append("<div style=\"font-size:16px;font-weight:700;color:#111;")
                  .append("border-bottom:2px solid #111;padding-bottom:6px;margin-bottom:6px\">")
                  .append(time)
                  .append(" <span style=\"color:#888;font-weight:400;font-size:13px\">&middot; ")
                  .append(s.getDurationMinutes()).append(" min</span></div>")
                  .append("<p style=\"margin:0;color:#333;font-size:14px\">with ")
                  .append(escape(s.getTrainerUsername())).append("</p>");
            if (s.getNote() != null && !s.getNote().isBlank()) {
                blocks.append("<p style=\"margin:4px 0 0;color:#666;font-size:13px\">")
                      .append(escape(s.getNote())).append("</p>");
            }
            blocks.append("</div>");
        }

        return "<div style=\"font-family:sans-serif;max-width:520px;margin:auto\">"
                + "<h2 style=\"color:#111;margin-bottom:4px\">Today's training 📅</h2>"
                + "<p style=\"color:#555;margin-top:0\">" + dateStr + " &mdash; "
                + list.size() + " session" + (list.size() == 1 ? "" : "s") + "</p>"
                + blocks
                + "<p style=\"color:#888;font-size:12px;margin-top:20px\">Spite</p></div>";
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
