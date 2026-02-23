package pl.punkty.app.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.Excuse;
import pl.punkty.app.model.MonthlyPointsSnapshot;
import pl.punkty.app.model.MonthlyPointsSnapshotItem;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PointsHistory;
import pl.punkty.app.model.PointsSnapshot;
import pl.punkty.app.model.UserAccount;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.ExcuseRepository;
import pl.punkty.app.repo.MonthlyPointsSnapshotItemRepository;
import pl.punkty.app.repo.MonthlyPointsSnapshotRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.PointsHistoryRepository;
import pl.punkty.app.repo.PointsSnapshotRepository;
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
    private final ExcuseRepository excuseRepository;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final PointsSnapshotRepository pointsSnapshotRepository;
    private final MonthlyPointsSnapshotRepository monthlyPointsSnapshotRepository;
    private final MonthlyPointsSnapshotItemRepository monthlyPointsSnapshotItemRepository;

    public ExportController(PersonRepository personRepository,
                            CurrentPointsRepository currentPointsRepository,
                            WeeklyTableRepository weeklyTableRepository,
                            WeeklyAttendanceRepository weeklyAttendanceRepository,
                            UserAccountRepository userAccountRepository,
                            ExcuseRepository excuseRepository,
                            PointsHistoryRepository pointsHistoryRepository,
                            PointsSnapshotRepository pointsSnapshotRepository,
                            MonthlyPointsSnapshotRepository monthlyPointsSnapshotRepository,
                            MonthlyPointsSnapshotItemRepository monthlyPointsSnapshotItemRepository) {
        this.personRepository = personRepository;
        this.currentPointsRepository = currentPointsRepository;
        this.weeklyTableRepository = weeklyTableRepository;
        this.weeklyAttendanceRepository = weeklyAttendanceRepository;
        this.userAccountRepository = userAccountRepository;
        this.excuseRepository = excuseRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
        this.pointsSnapshotRepository = pointsSnapshotRepository;
        this.monthlyPointsSnapshotRepository = monthlyPointsSnapshotRepository;
        this.monthlyPointsSnapshotItemRepository = monthlyPointsSnapshotItemRepository;
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
            writePointsSnapshots(zos, pointsSnapshotRepository.findAll());
            writePointsHistory(zos, pointsHistoryRepository.findAll());
            writeMonthlySnapshots(zos, monthlyPointsSnapshotRepository.findAll());
            writeMonthlySnapshotItems(zos, monthlyPointsSnapshotItemRepository.findAll());
            writeExcuses(zos, excuseRepository.findAll());
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

    private void writePointsSnapshots(ZipOutputStream zos, List<PointsSnapshot> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,snapshot_date\n");
        for (PointsSnapshot ps : list) {
            sb.append(ps.getId()).append(',')
              .append(ps.getSnapshotDate()).append('\n');
        }
        writeEntry(zos, "points_snapshots.csv", sb);
    }

    private void writePointsHistory(ZipOutputStream zos, List<PointsHistory> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,person_id,old_points,new_points,changed_by,changed_at\n");
        for (PointsHistory ph : list) {
            sb.append(ph.getId()).append(',')
              .append(ph.getPerson().getId()).append(',')
              .append(ph.getOldPoints()).append(',')
              .append(ph.getNewPoints()).append(',')
              .append(csv(ph.getChangedBy())).append(',')
              .append(ph.getChangedAt()).append('\n');
        }
        writeEntry(zos, "points_history.csv", sb);
    }

    private void writeMonthlySnapshots(ZipOutputStream zos, List<MonthlyPointsSnapshot> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,month_date,created_by,created_at\n");
        for (MonthlyPointsSnapshot ms : list) {
            sb.append(ms.getId()).append(',')
              .append(ms.getMonthDate()).append(',')
              .append(csv(ms.getCreatedBy())).append(',')
              .append(ms.getCreatedAt()).append('\n');
        }
        writeEntry(zos, "monthly_points_snapshots.csv", sb);
    }

    private void writeMonthlySnapshotItems(ZipOutputStream zos, List<MonthlyPointsSnapshotItem> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,snapshot_id,person_name,base_points,month_points,total_points\n");
        for (MonthlyPointsSnapshotItem item : list) {
            sb.append(item.getId()).append(',')
              .append(item.getSnapshot().getId()).append(',')
              .append(csv(item.getPersonName())).append(',')
              .append(item.getBasePoints()).append(',')
              .append(item.getMonthPoints()).append(',')
              .append(item.getTotalPoints()).append('\n');
        }
        writeEntry(zos, "monthly_points_snapshot_items.csv", sb);
    }

    private void writeExcuses(ZipOutputStream zos, List<Excuse> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,full_name,date_from,date_to,reason,status,created_by,created_at,reviewed_by,reviewed_at\n");
        for (Excuse ex : list) {
            sb.append(ex.getId()).append(',')
              .append(csv(ex.getFullName())).append(',')
              .append(ex.getDateFrom()).append(',')
              .append(ex.getDateTo()).append(',')
              .append(csv(ex.getReason())).append(',')
              .append(ex.getStatus()).append(',')
              .append(csv(ex.getCreatedBy())).append(',')
              .append(ex.getCreatedAt()).append(',')
              .append(csv(ex.getReviewedBy())).append(',')
              .append(ex.getReviewedAt()).append('\n');
        }
        writeEntry(zos, "excuses.csv", sb);
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
