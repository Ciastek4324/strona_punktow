package pl.punkty.app.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.UserAccount;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.UserAccountRepository;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Controller
public class ExportController {
    private final PersonRepository personRepository;
    private final CurrentPointsRepository currentPointsRepository;
    private final WeeklyTableRepository weeklyTableRepository;
    private final WeeklyAttendanceRepository weeklyAttendanceRepository;
    private final UserAccountRepository userAccountRepository;

    public ExportController(PersonRepository personRepository,
                            CurrentPointsRepository currentPointsRepository,
                            WeeklyTableRepository weeklyTableRepository,
                            WeeklyAttendanceRepository weeklyAttendanceRepository,
                            UserAccountRepository userAccountRepository) {
        this.personRepository = personRepository;
        this.currentPointsRepository = currentPointsRepository;
        this.weeklyTableRepository = weeklyTableRepository;
        this.weeklyAttendanceRepository = weeklyAttendanceRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/admin/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportZip() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            writePeople(zos, personRepository.findAll());
            writeCurrentPoints(zos, currentPointsRepository.findAll());
            writeWeeklyTables(zos, weeklyTableRepository.findAll());
            writeWeeklyAttendance(zos, weeklyAttendanceRepository.findAll());
            writeUsers(zos, userAccountRepository.findAll());
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = "backup_" + timestamp + ".zip";

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(baos.toByteArray());
    }

    private void writePeople(ZipOutputStream zos, List<Person> people) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,display_name\n");
        for (Person p : people) {
            sb.append(p.getId()).append(',')
              .append(csv(p.getDisplayName())).append('\n');
        }
        writeEntry(zos, "people.csv", sb);
    }

    private void writeCurrentPoints(ZipOutputStream zos, List<CurrentPoints> points) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,person_id,points\n");
        for (CurrentPoints cp : points) {
            sb.append(cp.getId()).append(',')
              .append(cp.getPerson().getId()).append(',')
              .append(cp.getPoints()).append('\n');
        }
        writeEntry(zos, "current_points.csv", sb);
    }

    private void writeWeeklyTables(ZipOutputStream zos, List<WeeklyTable> tables) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,week_start\n");
        for (WeeklyTable wt : tables) {
            sb.append(wt.getId()).append(',')
              .append(wt.getWeekStart()).append('\n');
        }
        writeEntry(zos, "weekly_tables.csv", sb);
    }

    private void writeWeeklyAttendance(ZipOutputStream zos, List<WeeklyAttendance> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,table_id,person_id,day_of_week,present\n");
        for (WeeklyAttendance wa : list) {
            sb.append(wa.getId()).append(',')
              .append(wa.getTableRef().getId()).append(',')
              .append(wa.getPerson().getId()).append(',')
              .append(wa.getDayOfWeek()).append(',')
              .append(wa.isPresent()).append('\n');
        }
        writeEntry(zos, "weekly_attendance.csv", sb);
    }

    private void writeUsers(ZipOutputStream zos, List<UserAccount> users) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,username,role\n");
        for (UserAccount u : users) {
            sb.append(u.getId()).append(',')
              .append(csv(u.getUsername())).append(',')
              .append(csv(u.getRole())).append('\n');
        }
        writeEntry(zos, "users.csv", sb);
    }

    private void writeEntry(ZipOutputStream zos, String name, StringBuilder sb) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zos.putNextEntry(entry);
        zos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\n") || v.contains("\r")) {
            return "\"" + v + "\"";
        }
        return v;
    }
}