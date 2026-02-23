package pl.punkty.app.web;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.ExcuseStatus;
import pl.punkty.app.model.MonthlyPointsSnapshot;
import pl.punkty.app.model.MonthlyPointsSnapshotItem;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.model.PointsHistory;
import pl.punkty.app.model.PointsSnapshot;
import pl.punkty.app.model.WeeklyAttendance;
import pl.punkty.app.model.WeeklyTable;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.ExcuseRepository;
import pl.punkty.app.repo.MonthlyPointsSnapshotItemRepository;
import pl.punkty.app.repo.MonthlyPointsSnapshotRepository;
import pl.punkty.app.repo.PersonRepository;
import pl.punkty.app.repo.PointsHistoryRepository;
import pl.punkty.app.repo.PointsSnapshotRepository;
import pl.punkty.app.repo.WeeklyAttendanceRepository;
import pl.punkty.app.repo.WeeklyTableRepository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Controller
public class PointsController {
    private static final Locale LOCALE_PL = new Locale("pl", "PL");
    private static final List<String> WEEK_DAYS = List.of(
        "Poniedzialek",
        "Wtorek",
        "Sroda",
        "Czwartek",
        "Piatek",
        "Sobota"
    );

    private final CurrentPointsRepository currentPointsRepository;
    private final PersonRepository personRepository;
    private final PointsSnapshotRepository pointsSnapshotRepository;
    private final WeeklyTableRepository weeklyTableRepository;
    private final WeeklyAttendanceRepository weeklyAttendanceRepository;
    private final ExcuseRepository excuseRepository;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final MonthlyPointsSnapshotRepository monthlyPointsSnapshotRepository;
    private final MonthlyPointsSnapshotItemRepository monthlyPointsSnapshotItemRepository;

    public PointsController(CurrentPointsRepository currentPointsRepository,
                            PersonRepository personRepository,
                            PointsSnapshotRepository pointsSnapshotRepository,
                            WeeklyTableRepository weeklyTableRepository,
                            WeeklyAttendanceRepository weeklyAttendanceRepository,
                            ExcuseRepository excuseRepository,
                            PointsHistoryRepository pointsHistoryRepository,
                            MonthlyPointsSnapshotRepository monthlyPointsSnapshotRepository,
                            MonthlyPointsSnapshotItemRepository monthlyPointsSnapshotItemRepository) {
        this.currentPointsRepository = currentPointsRepository;
        this.personRepository = personRepository;
        this.pointsSnapshotRepository = pointsSnapshotRepository;
        this.weeklyTableRepository = weeklyTableRepository;
        this.weeklyAttendanceRepository = weeklyAttendanceRepository;
        this.excuseRepository = excuseRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
        this.monthlyPointsSnapshotRepository = monthlyPointsSnapshotRepository;
        this.monthlyPointsSnapshotItemRepository = monthlyPointsSnapshotItemRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
            return "redirect:/points/current";
        }
        model.addAttribute("unreadExcuses", excuseRepository.countByStatus(ExcuseStatus.PENDING));
        return "dashboard";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/points/current")
    public String currentPoints(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isGuest = auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"));
        List<Person> people = personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
        Map<Long, Integer> pointsByPerson = new LinkedHashMap<>();
        for (CurrentPoints cp : currentPointsRepository.findAll()) {
            pointsByPerson.put(cp.getPerson().getId(), cp.getPoints());
        }
        List<PersonRow> rows = new ArrayList<>();
        for (Person person : people) {
            int points = pointsByPerson.getOrDefault(person.getId(), 0);
            rows.add(new PersonRow(person, points));
        }
        Optional<PointsSnapshot> snapshot = pointsSnapshotRepository.findTopByOrderBySnapshotDateDesc();
        model.addAttribute("rows", rows);
        model.addAttribute("snapshotDate", snapshot.map(PointsSnapshot::getSnapshotDate).orElse(null));
        model.addAttribute("isGuest", isGuest);
        return "current-points";
    }
    @PostMapping("/points/current/save")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String saveCurrentPoints(@RequestParam Map<String, String> params,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate snapshotDate) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String changedBy = auth != null ? auth.getName() : "user";

        List<Person> people = personRepository.findAll();
        Map<Long, CurrentPoints> existing = new LinkedHashMap<>();
        for (CurrentPoints cp : currentPointsRepository.findAll()) {
            existing.put(cp.getPerson().getId(), cp);
        }
        List<CurrentPoints> toSave = new ArrayList<>();
        List<PointsHistory> history = new ArrayList<>();
        for (Person person : people) {
            String key = "p_" + person.getId();
            if (!params.containsKey(key)) {
                continue;
            }
            int value;
            try {
                value = Integer.parseInt(params.get(key));
            } catch (NumberFormatException ex) {
                continue;
            }
            CurrentPoints cp = existing.getOrDefault(person.getId(), new CurrentPoints());
            int old = cp.getId() == null ? 0 : cp.getPoints();
            if (old != value) {
                PointsHistory ph = new PointsHistory();
                ph.setPerson(person);
                ph.setOldPoints(old);
                ph.setNewPoints(value);
                ph.setChangedBy(changedBy);
                history.add(ph);
            }
            cp.setPerson(person);
            cp.setPoints(value);
            toSave.add(cp);
        }
        if (!toSave.isEmpty()) {
            currentPointsRepository.saveAll(toSave);
        }
        if (!history.isEmpty()) {
            pointsHistoryRepository.saveAll(history);
        }

        if (snapshotDate != null) {
            pointsSnapshotRepository.deleteAll();
            PointsSnapshot snapshot = new PointsSnapshot();
            snapshot.setSnapshotDate(snapshotDate);
            pointsSnapshotRepository.save(snapshot);
        }

        return "redirect:/points/current";
    }

    @GetMapping("/points/history")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String pointsHistory(@RequestParam(required = false) String q, Model model) {
        List<PointsHistory> all = pointsHistoryRepository.findAllByOrderByChangedAtDesc();
        String query = q == null ? "" : q.trim().toLowerCase(LOCALE_PL);
        List<PointsHistory> filtered = all.stream()
            .filter(h -> query.isEmpty() || h.getPerson().getDisplayName().toLowerCase(LOCALE_PL).contains(query))
            .toList();
        model.addAttribute("items", filtered);
        model.addAttribute("q", q == null ? "" : q);
        return "points-history";
    }

    @GetMapping("/generator")
    public String generator(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false, defaultValue = "monthly") String tab,
                            Model model) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        model.addAttribute("date", effectiveDate);
        model.addAttribute("monthName", monthName(effectiveDate));
        model.addAttribute("dateParam", effectiveDate.toString());
        model.addAttribute("tab", tab);

        model.addAttribute("sunday", sundayData());
        Map<String, List<String>> weekdayMinistranciShifted = shiftWeekday(weekdayMinistranci(), monthOffsetFromBase(effectiveDate));
        Map<String, List<String>> weekdayLektorzyShifted = shiftWeekday(weekdayLektorzy(), monthOffsetFromBase(effectiveDate));
        model.addAttribute("weekdayMinistranci", weekdayMinistranciShifted);
        model.addAttribute("weekdayLektorzy", weekdayLektorzyShifted);
        model.addAttribute("weekdayAspiranci", weekdayAspiranci());

        List<Person> people = personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();

        Map<Long, Integer> monthPoints = monthPoints(effectiveDate);
        List<PointsRow> pointsRows = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            pointsRows.add(new PointsRow(person, base, month, base + month));
        }
        model.addAttribute("pointsRows", pointsRows);
        model.addAttribute("roles", PersonRole.values());
        model.addAttribute("snapshots", monthlyPointsSnapshotRepository.findAllByOrderByCreatedAtDesc());

        return "generator";
    }

    @PostMapping("/points/snapshot/create")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String createPointsSnapshot(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = auth != null ? auth.getName() : "user";

        LocalDate monthDate = LocalDate.of(date.getYear(), date.getMonth(), 1);
        MonthlyPointsSnapshot snapshot = new MonthlyPointsSnapshot();
        snapshot.setMonthDate(monthDate);
        snapshot.setCreatedBy(createdBy);
        snapshot = monthlyPointsSnapshotRepository.save(snapshot);

        List<Person> people = personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
        Map<Long, Integer> monthPoints = monthPoints(date);
        List<MonthlyPointsSnapshotItem> items = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            MonthlyPointsSnapshotItem item = new MonthlyPointsSnapshotItem();
            item.setSnapshot(snapshot);
            item.setPersonName(person.getDisplayName());
            item.setBasePoints(base);
            item.setMonthPoints(month);
            item.setTotalPoints(base + month);
            items.add(item);
        }
        monthlyPointsSnapshotItemRepository.saveAll(items);

        return "redirect:/generator?tab=points&date=" + date;
    }

    @GetMapping("/points/snapshot")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String viewPointsSnapshot(@RequestParam("id") Long id, Model model) {
        MonthlyPointsSnapshot snapshot = monthlyPointsSnapshotRepository.findById(id).orElse(null);
        if (snapshot == null) {
            return "redirect:/generator?tab=points";
        }
        List<MonthlyPointsSnapshotItem> items = monthlyPointsSnapshotItemRepository.findAllBySnapshotOrderByPersonNameAsc(snapshot);
        model.addAttribute("monthName", monthName(snapshot.getMonthDate()));
        model.addAttribute("items", items);
        return "points-snapshot";
    }

    @GetMapping("/points/snapshot/print")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String printPointsSnapshot(@RequestParam("id") Long id, Model model) {
        MonthlyPointsSnapshot snapshot = monthlyPointsSnapshotRepository.findById(id).orElse(null);
        if (snapshot == null) {
            return "redirect:/generator?tab=points";
        }
        List<MonthlyPointsSnapshotItem> items = monthlyPointsSnapshotItemRepository.findAllBySnapshotOrderByPersonNameAsc(snapshot);
        model.addAttribute("monthName", monthName(snapshot.getMonthDate()));
        model.addAttribute("items", items);
        return "points-snapshot-print";
    }

    @PostMapping("/people/add")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String addPerson(@RequestParam("displayName") String displayName,
                            @RequestParam("role") PersonRole role,
                            @RequestParam("basePoints") int basePoints,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false, defaultValue = "points") String tab) {
        String name = displayName.trim();
        if (!name.isEmpty() && personRepository.findByDisplayNameIgnoreCase(name).isEmpty()) {
            Person person = new Person();
            person.setDisplayName(name);
            person.setRole(role);
            person.setBasePoints(basePoints);
            personRepository.save(person);
        }
        return "redirect:/generator?tab=" + tab + (date != null ? "&date=" + date : "");
    }

    @PostMapping("/people/update")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String updatePerson(@RequestParam("personId") Long personId,
                               @RequestParam("role") PersonRole role,
                               @RequestParam("basePoints") int basePoints,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false, defaultValue = "points") String tab) {
        personRepository.findById(personId).ifPresent(person -> {
            person.setRole(role);
            person.setBasePoints(basePoints);
            personRepository.save(person);
        });
        return "redirect:/generator?tab=" + tab + (date != null ? "&date=" + date : "");
    }

    @PostMapping("/people/delete")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String deletePerson(@RequestParam("personId") Long personId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false, defaultValue = "points") String tab) {
        personRepository.deleteById(personId);
        return "redirect:/generator?tab=" + tab + (date != null ? "&date=" + date : "");
    }

    @GetMapping("/generator/print")
    public String generatorPrint(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 Model model) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        model.addAttribute("date", effectiveDate);
        model.addAttribute("monthName", monthName(effectiveDate));

        model.addAttribute("sunday", sundayData());
        Map<String, List<String>> weekdayMinistranciShifted = shiftWeekday(weekdayMinistranci(), monthOffsetFromBase(effectiveDate));
        Map<String, List<String>> weekdayLektorzyShifted = shiftWeekday(weekdayLektorzy(), monthOffsetFromBase(effectiveDate));
        model.addAttribute("weekdayMinistranci", weekdayMinistranciShifted);
        model.addAttribute("weekdayLektorzy", weekdayLektorzyShifted);
        model.addAttribute("weekdayAspiranci", weekdayAspiranci());

        return "generator-print";
    }
    @GetMapping("/points/print")
    public String pointsPrint(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        model.addAttribute("monthName", monthName(effectiveDate));

        List<Person> people = personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
        Map<Long, Integer> monthPoints = monthPoints(effectiveDate);
        List<PointsRow> pointsRows = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            pointsRows.add(new PointsRow(person, base, month, base + month));
        }
        model.addAttribute("pointsRows", pointsRows);
        return "points-print";
    }

    @GetMapping("/points/docm")
    public void pointsDocm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                           HttpServletResponse response) throws IOException {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        String monthName = monthName(effectiveDate);

        List<Person> people = personRepository.findAll().stream()
            .sorted((a, b) -> a.getDisplayName().compareToIgnoreCase(b.getDisplayName()))
            .toList();
        Map<Long, Integer> monthPoints = monthPoints(effectiveDate);
        List<PointsRow> pointsRows = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            pointsRows.add(new PointsRow(person, base, month, base + month));
        }

        response.setContentType("application/vnd.ms-word.document.macroEnabled.12");
        response.setHeader("Content-Disposition", "attachment; filename=\"punkty-" +
            effectiveDate.getYear() + "-" + String.format("%02d", effectiveDate.getMonthValue()) + ".docm\"");

        try (XWPFDocument doc = new XWPFDocument(); OutputStream out = response.getOutputStream()) {
            XWPFParagraph title = doc.createParagraph();
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Punkty - " + monthName);
            titleRun.setFontFamily("Dosis");
            titleRun.setFontSize(16);
            titleRun.setBold(true);

            XWPFTable table = doc.createTable(pointsRows.size() + 1, 4);
            setCellText(table.getRow(0).getCell(0), "Osoba", true);
            setCellText(table.getRow(0).getCell(1), "Punkty od poczatku", true);
            setCellText(table.getRow(0).getCell(2), "Punkty " + monthName, true);
            setCellText(table.getRow(0).getCell(3), "Punkty razem", true);

            int rowIdx = 1;
            for (PointsRow row : pointsRows) {
                XWPFTableRow tr = table.getRow(rowIdx++);
                setCellText(tr.getCell(0), row.getPerson().getDisplayName(), false);
                setCellText(tr.getCell(1), Integer.toString(row.getBasePoints()), false);
                setCellText(tr.getCell(2), Integer.toString(row.getMonthPoints()), false);
                setCellText(tr.getCell(3), Integer.toString(row.getTotal()), false);
            }

            doc.write(out);
        }
    }

    @GetMapping("/generator/docx")
    public void generatorDocx(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              HttpServletResponse response) throws IOException {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        String monthName = monthName(effectiveDate);
        String dateStr = formatDate(effectiveDate);

        response.setContentType("application/vnd.ms-word.document.macroEnabled.12");
        response.setHeader("Content-Disposition", "attachment; filename=\"lista-" + effectiveDate + ".docm\"");

        try (InputStream in = getClass().getResourceAsStream("/templates/lista-template.docm")) {
            if (in == null) {
                throw new IOException("Template not found");
            }
            try (ZipInputStream zin = new ZipInputStream(in);
                 OutputStream out = response.getOutputStream();
                 ZipOutputStream zout = new ZipOutputStream(out)) {
                ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    ZipEntry outEntry = new ZipEntry(entry.getName());
                    zout.putNextEntry(outEntry);
                    byte[] data = zin.readAllBytes();
                    if ("word/document.xml".equals(entry.getName())) {
                        String xml = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                        xml = xml.replace("09.02.2026", dateStr);
                        xml = replaceMonthInXml(xml, monthName);
                        data = xml.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    }
                    zout.write(data);
                    zout.closeEntry();
                }
            }
        }
    }

    private String replaceMonthInXml(String xml, String monthName) {
        for (String month : monthNames()) {
            if (xml.contains(">" + month + "<")) {
                return xml.replace(">" + month + "<", ">" + monthName + "<");
            }
        }
        return xml;
    }

    private String replaceWeekdaysInXml(String xml,
                                        Map<String, List<String>> base,
                                        Map<String, List<String>> target) {
        Pattern p = Pattern.compile("(?s)<w:p[^>]*>.*?</w:p>");
        Matcher m = p.matcher(xml);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String para = m.group();
            String plain = stripTags(para);
            boolean replaced = false;
            for (String day : WEEK_DAYS) {
                List<String> baseNames = base.getOrDefault(day, List.of());
                if (!plain.startsWith(day) || !containsNamesInOrder(plain, baseNames)) {
                    continue;
                }
                String startTag = extractStartTag(para);
                String pPr = extractPPr(para);
                String rPr = extractFirstRunPr(para);
                String newText = day + " - " + String.join(", ", target.getOrDefault(day, List.of()));
                StringBuilder rebuilt = new StringBuilder();
                rebuilt.append(startTag);
                if (pPr != null) {
                    rebuilt.append(pPr);
                }
                rebuilt.append("<w:r>");
                if (rPr != null) {
                    rebuilt.append(rPr);
                }
                rebuilt.append("<w:t xml:space=\"preserve\">")
                    .append(escapeXml(newText))
                    .append("</w:t></w:r></w:p>");
                m.appendReplacement(sb, Matcher.quoteReplacement(rebuilt.toString()));
                replaced = true;
                break;
            }
            if (!replaced) {
                m.appendReplacement(sb, Matcher.quoteReplacement(para));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }
    private String extractStartTag(String paragraph) {
        Matcher m = Pattern.compile("(?s)<w:p[^>]*>").matcher(paragraph);
        return m.find() ? m.group() : "<w:p>";
    }

    private String extractPPr(String paragraph) {
        Matcher m = Pattern.compile("(?s)<w:pPr>.*?</w:pPr>").matcher(paragraph);
        return m.find() ? m.group() : null;
    }

    private String extractFirstRunPr(String paragraph) {
        Matcher m = Pattern.compile("(?s)<w:r>\\s*(<w:rPr>.*?</w:rPr>)").matcher(paragraph);
        return m.find() ? m.group(1) : null;
    }

    private String stripTags(String xml) {
        String text = xml.replaceAll("<[^>]+>", "");
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
    }

    private boolean containsNamesInOrder(String plain, List<String> names) {
        int index = 0;
        for (String name : names) {
            int pos = plain.indexOf(name, index);
            if (pos < 0) {
                return false;
            }
            index = pos + name.length();
        }
        return true;
    }

    private String escapeXml(String text) {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private Map<String, List<String>> sundayData() {
        Map<String, List<String>> sunday = new LinkedHashMap<>();
        sunday.put("PRYMARIA (aspiranci)", List.of("Rafal Opoka"));
        sunday.put("PRYMARIA (ministranci)", List.of("Marcel Smoter", "Krzysztof Florek", "Marcin Opoka", "Tomasz Gancarczyk"));
        sunday.put("PRYMARIA (lektorzy)", List.of("Stanislaw Lubecki", "Kacper Florek", "Michal Furtak"));
        sunday.put("SUMA (aspiranci)", List.of("Wojciech Zelek"));
        sunday.put("SUMA (ministranci)", List.of("Szymon Zelek", "Filip Wierzycki", "Wiktor Wierzycki", "Antoni Gorcowski", "Wojciech Bieniek"));
        sunday.put("SUMA (lektorzy)", List.of("Daniel Nowak", "Jakub Mucha", "Szymon Mucha", "Jan Migacz"));
        sunday.put("III MSZA (aspiranci)", List.of("Krzysztof Wierzycki"));
        sunday.put("III MSZA (ministranci)", List.of("Nikodem Franczyk", "Damian Sopata", "Karol Jez", "Pawel Jez"));
        sunday.put("III MSZA (lektorzy)", List.of("Pawel Wierzycki", "Sebastian Sopata", "Radoslaw Sopata", "Karol Klag"));
        return sunday;
    }

    private Map<String, List<String>> weekdayMinistranci() {
        Map<String, List<String>> weekdayMinistranci = new LinkedHashMap<>();
        weekdayMinistranci.put("Poniedzialek", List.of("Nikodem Franczyk", "Krzysztof Florek"));
        weekdayMinistranci.put("Wtorek", List.of("Tomasz Gancarczyk", "Marcin Opoka"));
        weekdayMinistranci.put("Sroda", List.of("Damian Sopata", "Karol Jez", "Pawel Jez"));
        weekdayMinistranci.put("Czwartek", List.of("Szymon Zelek", "Antoni Gorcowski"));
        weekdayMinistranci.put("Piatek", List.of("Wojciech Bieniek", "Sebastian Wierzycki"));
        weekdayMinistranci.put("Sobota", List.of("Filip Wierzycki", "Wiktor Wierzycki", "Marcel Smoter"));
        return weekdayMinistranci;
    }

    private Map<String, List<String>> weekdayLektorzy() {
        Map<String, List<String>> weekdayLektorzy = new LinkedHashMap<>();
        weekdayLektorzy.put("Poniedzialek", List.of("Kacper Florek", "Karol Klag"));
        weekdayLektorzy.put("Wtorek", List.of("Sebastian Sopata", "Radoslaw Sopata"));
        weekdayLektorzy.put("Sroda", List.of("Pawel Wierzycki", "Daniel Nowak"));
        weekdayLektorzy.put("Czwartek", List.of("Michal Furtak"));
        weekdayLektorzy.put("Piatek", List.of("Stanislaw Lubecki", "Jan Migacz"));
        weekdayLektorzy.put("Sobota", List.of("Szymon Mucha", "Jakub Mucha"));
        return weekdayLektorzy;
    }

    private List<String> weekdayAspiranci() {
        return List.of("Krzysztof Wierzycki", "Rafal Opoka", "Wojciech Zelek");
    }

    private int monthOffsetFromBase(LocalDate date) {
        YearMonth base = YearMonth.of(2026, 2);
        YearMonth target = YearMonth.of(date.getYear(), date.getMonth());
        long monthsBetween = ChronoUnit.MONTHS.between(base, target);
        int offset = (int) (monthsBetween % 6);
        if (offset < 0) {
            offset += 6;
        }
        return offset;
    }

    private Map<String, List<String>> shiftWeekday(Map<String, List<String>> original, int offset) {
        Map<String, List<String>> shifted = new LinkedHashMap<>();
        for (String day : WEEK_DAYS) {
            shifted.put(day, List.of());
        }
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String fromDay = WEEK_DAYS.get(i);
            String toDay = WEEK_DAYS.get((i + offset) % WEEK_DAYS.size());
            shifted.put(toDay, original.getOrDefault(fromDay, List.of()));
        }
        return shifted;
    }

    private String monthName(LocalDate date) {
        return switch (date.getMonthValue()) {
            case 1 -> "STYCZEN";
            case 2 -> "LUTY";
            case 3 -> "MARZEC";
            case 4 -> "KWIECIEN";
            case 5 -> "MAJ";
            case 6 -> "CZERWIEC";
            case 7 -> "LIPIEC";
            case 8 -> "SIERPIEN";
            case 9 -> "WRZESIEN";
            case 10 -> "PAZDZIERNIK";
            case 11 -> "LISTOPAD";
            case 12 -> "GRUDZIEN";
            default -> date.getMonth().getDisplayName(TextStyle.FULL, LOCALE_PL).toUpperCase(LOCALE_PL);
        };
    }

    private List<String> monthNames() {
        return List.of(
            "STYCZEN",
            "LUTY",
            "MARZEC",
            "KWIECIEN",
            "MAJ",
            "CZERWIEC",
            "LIPIEC",
            "SIERPIEN",
            "WRZESIEN",
            "PAZDZIERNIK",
            "LISTOPAD",
            "GRUDZIEN"
        );
    }

    private String formatDate(LocalDate date) {
        return String.format("%02d.%02d.%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }

    private Map<Long, Integer> monthPoints(LocalDate date) {
        YearMonth month = YearMonth.of(date.getYear(), date.getMonth());
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<WeeklyTable> tables = weeklyTableRepository.findAllByWeekStartBetween(start.minusDays(7), end);
        Map<Long, Integer> points = new LinkedHashMap<>();
        if (tables.isEmpty()) {
            return points;
        }
        for (WeeklyAttendance attendance : weeklyAttendanceRepository.findByTableRefIn(tables)) {
            LocalDate weekStart = attendance.getTableRef().getWeekStart();
            if (weekStart == null || weekStart.isBefore(start.minusDays(7)) || weekStart.isAfter(end)) {
                continue;
            }
            Long personId = attendance.getPerson().getId();
            points.put(personId, points.getOrDefault(personId, 0) + 1);
        }
        return points;
    }

    public static class PersonRow {
        private final Person person;
        private final int points;

        public PersonRow(Person person, int points) {
            this.person = person;
            this.points = points;
        }

        public Person getPerson() {
            return person;
        }

        public int getPoints() {
            return points;
        }
    }

    public static class PointsRow {
        private final Person person;
        private final int basePoints;
        private final int monthPoints;
        private final int total;

        public PointsRow(Person person, int basePoints, int monthPoints, int total) {
            this.person = person;
            this.basePoints = basePoints;
            this.monthPoints = monthPoints;
            this.total = total;
        }

        public Person getPerson() {
            return person;
        }

        public int getBasePoints() {
            return basePoints;
        }

        public int getMonthPoints() {
            return monthPoints;
        }

        public int getTotal() {
            return total;
        }
    }

    private void setCellText(org.apache.poi.xwpf.usermodel.XWPFTableCell cell, String text, boolean header) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily("Dosis");
        run.setFontSize(10);
        run.setBold(header);
    }
}