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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import pl.punkty.app.model.CurrentPoints;
import pl.punkty.app.model.ExcuseStatus;
import pl.punkty.app.model.MonthlyPointsSnapshot;
import pl.punkty.app.model.MonthlyPointsSnapshotItem;
import pl.punkty.app.model.Person;
import pl.punkty.app.model.PersonRole;
import pl.punkty.app.model.PointsHistory;
import pl.punkty.app.model.PointsSnapshot;
import pl.punkty.app.repo.CurrentPointsRepository;
import pl.punkty.app.repo.ExcuseRepository;
import pl.punkty.app.repo.MonthlyPointsSnapshotItemRepository;
import pl.punkty.app.repo.MonthlyPointsSnapshotRepository;
import pl.punkty.app.repo.PointsHistoryRepository;
import pl.punkty.app.repo.PointsSnapshotRepository;
import pl.punkty.app.service.PointsService;
import pl.punkty.app.service.PeopleService;
import pl.punkty.app.service.ScheduleService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.TextStyle;
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
    private final PointsSnapshotRepository pointsSnapshotRepository;
    private final ExcuseRepository excuseRepository;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final MonthlyPointsSnapshotRepository monthlyPointsSnapshotRepository;
    private final MonthlyPointsSnapshotItemRepository monthlyPointsSnapshotItemRepository;
    private final PointsService pointsService;
    private final PeopleService peopleService;
    private final ScheduleService scheduleService;

    public PointsController(CurrentPointsRepository currentPointsRepository,
                            PointsSnapshotRepository pointsSnapshotRepository,
                            ExcuseRepository excuseRepository,
                            PointsHistoryRepository pointsHistoryRepository,
                            MonthlyPointsSnapshotRepository monthlyPointsSnapshotRepository,
                            MonthlyPointsSnapshotItemRepository monthlyPointsSnapshotItemRepository,
                            PointsService pointsService,
                            PeopleService peopleService,
                            ScheduleService scheduleService) {
        this.currentPointsRepository = currentPointsRepository;
        this.pointsSnapshotRepository = pointsSnapshotRepository;
        this.excuseRepository = excuseRepository;
        this.pointsHistoryRepository = pointsHistoryRepository;
        this.monthlyPointsSnapshotRepository = monthlyPointsSnapshotRepository;
        this.monthlyPointsSnapshotItemRepository = monthlyPointsSnapshotItemRepository;
        this.pointsService = pointsService;
        this.peopleService = peopleService;
        this.scheduleService = scheduleService;
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
        boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("isAdmin", isAdmin);
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
        List<Person> people = peopleService.getPeopleSorted();
        LocalDate now = LocalDate.now();
        Map<Long, Integer> monthPoints = pointsService.monthPoints(now);
        List<PersonRow> rows = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            rows.add(new PersonRow(person, base, month, base + month));
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

        List<Person> people = peopleService.getPeopleSorted();
        Map<Long, CurrentPoints> existing = new LinkedHashMap<>();
        for (CurrentPoints cp : currentPointsRepository.findAll()) {
            existing.put(cp.getPerson().getId(), cp);
        }
        LocalDate now = LocalDate.now();
        Map<Long, Integer> monthPoints = pointsService.monthPoints(now);
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
            if (value < -9999 || value > 9999) {
                continue;
            }
            int oldBase = person.getBasePoints();
            if (oldBase != value) {
                PointsHistory ph = new PointsHistory();
                ph.setPerson(person);
                ph.setOldPoints(oldBase);
                ph.setNewPoints(value);
                ph.setChangedBy(changedBy);
                history.add(ph);
            }
            person.setBasePoints(value);
            peopleService.updatePerson(person.getId(), person.getRole(), value);

            int month = monthPoints.getOrDefault(person.getId(), 0);
            int total = value + month;
            CurrentPoints cp = existing.getOrDefault(person.getId(), new CurrentPoints());
            cp.setPerson(person);
            cp.setPoints(total);
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
    public String pointsHistory(@RequestParam(required = false) String q,
                                @RequestParam(required = false, defaultValue = "0") int page,
                                Model model) {
        int safePage = Math.max(page, 0);
        PageRequest pageRequest = PageRequest.of(safePage, 100, Sort.by("changedAt").descending());
        String query = q == null ? "" : q.trim();
        Page<PointsHistory> pageData = query.isEmpty()
            ? pointsHistoryRepository.findAllByOrderByChangedAtDesc(pageRequest)
            : pointsHistoryRepository.findAllByPersonDisplayNameContainingIgnoreCaseOrderByChangedAtDesc(query, pageRequest);
        model.addAttribute("items", pageData.getContent());
        model.addAttribute("q", query);
        model.addAttribute("page", safePage);
        model.addAttribute("totalPages", pageData.getTotalPages());
        return "points-history";
    }

    @GetMapping("/generator")
    public String generator(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                            @RequestParam(required = false, defaultValue = "monthly") String tab,
                            @RequestParam(required = false, defaultValue = "false") boolean useSchedule,
                            Model model) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        model.addAttribute("date", effectiveDate);
        model.addAttribute("monthName", monthName(effectiveDate));
        model.addAttribute("dateParam", effectiveDate.toString());
        model.addAttribute("tab", tab);
        model.addAttribute("useSchedule", useSchedule);

        Map<String, List<String>> weekdayMinistranciShifted;
        Map<String, List<String>> weekdayLektorzyShifted;
        Map<String, List<String>> weekdayAspiranciShifted;
        if (useSchedule) {
            model.addAttribute("sunday", scheduleService.sundayData(effectiveDate));
            weekdayMinistranciShifted = scheduleService.weekdayMinistranci(effectiveDate);
            weekdayLektorzyShifted = scheduleService.weekdayLektorzy(effectiveDate);
            weekdayAspiranciShifted = scheduleService.weekdayAspiranci(effectiveDate);
        } else {
            model.addAttribute("sunday", scheduleService.sundayDataFromBase(effectiveDate));
            weekdayMinistranciShifted = scheduleService.weekdayMinistranciFromBase(effectiveDate);
            weekdayLektorzyShifted = scheduleService.weekdayLektorzyFromBase(effectiveDate);
            weekdayAspiranciShifted = scheduleService.weekdayAspiranciFromBase(effectiveDate);
        }
        model.addAttribute("weekdayMinistranci", weekdayMinistranciShifted);
        model.addAttribute("weekdayLektorzy", weekdayLektorzyShifted);
        model.addAttribute("weekdayAspiranci", weekdayAspiranciShifted);

        List<Person> people = peopleService.getPeopleSorted();

        Map<Long, Integer> monthPoints = pointsService.monthPoints(effectiveDate);
        List<PointsRow> pointsRows = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            pointsRows.add(new PointsRow(person, base, month, base + month));
        }
        model.addAttribute("pointsRows", pointsRows);
        model.addAttribute("roles", PersonRole.values());
        model.addAttribute("snapshots", monthlyPointsSnapshotRepository.findAllByOrderByCreatedAtDesc(
            PageRequest.of(0, 12, Sort.by("createdAt").descending())
        ).getContent());

        return "generator";
    }

    @GetMapping("/members")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String members(Model model) {
        List<Person> people = peopleService.getPeopleSorted();
        List<PointsRow> rows = new ArrayList<>();
        for (Person person : people) {
            rows.add(new PointsRow(person, person.getBasePoints(), 0, person.getBasePoints()));
        }
        model.addAttribute("pointsRows", rows);
        model.addAttribute("roles", PersonRole.values());
        return "members";
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

        List<Person> people = peopleService.getPeopleSorted();
        Map<Long, Integer> monthPoints = pointsService.monthPoints(date);
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
                            @RequestParam(required = false, defaultValue = "points") String tab,
                            RedirectAttributes redirectAttributes) {
        String name = displayName.trim();
        PeopleService.AddPersonResult result = peopleService.addPerson(name, role, basePoints);
        if (result == PeopleService.AddPersonResult.ADDED) {
            redirectAttributes.addFlashAttribute("notice", "Dodano nowego czlonka.");
        } else if (result == PeopleService.AddPersonResult.DUPLICATE) {
            redirectAttributes.addFlashAttribute("error", "Osoba o takim imieniu i nazwisku juz istnieje.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Niepoprawne dane czlonka.");
        }
        if ("members".equals(tab)) {
            return "redirect:/members";
        }
        return "redirect:/generator?tab=" + tab + (date != null ? "&date=" + date : "");
    }

    @PostMapping("/people/update")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String updatePerson(@RequestParam("personId") Long personId,
                               @RequestParam("displayName") String displayName,
                               @RequestParam("role") PersonRole role,
                               @RequestParam("basePoints") int basePoints,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false, defaultValue = "points") String tab,
                               RedirectAttributes redirectAttributes) {
        PeopleService.UpdatePersonResult result = peopleService.updatePerson(personId, displayName, role, basePoints);
        if (result == PeopleService.UpdatePersonResult.UPDATED) {
            redirectAttributes.addFlashAttribute("notice", "Zapisano zmiany czlonka.");
        } else if (result == PeopleService.UpdatePersonResult.DUPLICATE) {
            redirectAttributes.addFlashAttribute("error", "Taka nazwa czlonka juz istnieje.");
        } else if (result == PeopleService.UpdatePersonResult.NOT_FOUND) {
            redirectAttributes.addFlashAttribute("error", "Nie znaleziono czlonka.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Niepoprawna nazwa czlonka.");
        }
        if ("members".equals(tab)) {
            return "redirect:/members";
        }
        return "redirect:/generator?tab=" + tab + (date != null ? "&date=" + date : "");
    }

    @PostMapping("/people/delete")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public String deletePerson(@RequestParam("personId") Long personId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(required = false, defaultValue = "points") String tab,
                               RedirectAttributes redirectAttributes) {
        peopleService.deletePerson(personId);
        redirectAttributes.addFlashAttribute("notice", "Usunieto czlonka.");
        if ("members".equals(tab)) {
            return "redirect:/members";
        }
        return "redirect:/generator?tab=" + tab + (date != null ? "&date=" + date : "");
    }

    @GetMapping("/generator/print")
    public String generatorPrint(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                 @RequestParam(required = false, defaultValue = "false") boolean useSchedule,
                                 Model model) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        model.addAttribute("date", effectiveDate);
        model.addAttribute("monthName", monthName(effectiveDate));

        Map<String, List<String>> weekdayMinistranciShifted;
        Map<String, List<String>> weekdayLektorzyShifted;
        Map<String, List<String>> weekdayAspiranciShifted;
        if (useSchedule) {
            model.addAttribute("sunday", scheduleService.sundayData(effectiveDate));
            weekdayMinistranciShifted = scheduleService.weekdayMinistranci(effectiveDate);
            weekdayLektorzyShifted = scheduleService.weekdayLektorzy(effectiveDate);
            weekdayAspiranciShifted = scheduleService.weekdayAspiranci(effectiveDate);
        } else {
            model.addAttribute("sunday", scheduleService.sundayDataFromBase(effectiveDate));
            weekdayMinistranciShifted = scheduleService.weekdayMinistranciFromBase(effectiveDate);
            weekdayLektorzyShifted = scheduleService.weekdayLektorzyFromBase(effectiveDate);
            weekdayAspiranciShifted = scheduleService.weekdayAspiranciFromBase(effectiveDate);
        }
        model.addAttribute("weekdayMinistranci", weekdayMinistranciShifted);
        model.addAttribute("weekdayLektorzy", weekdayLektorzyShifted);
        model.addAttribute("weekdayAspiranci", weekdayAspiranciShifted);

        return "generator-print";
    }
    @GetMapping("/points/print")
    public String pointsPrint(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              Model model) {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        model.addAttribute("monthName", monthName(effectiveDate));

        List<Person> people = peopleService.getPeopleSorted();
        Map<Long, Integer> monthPoints = pointsService.monthPoints(effectiveDate);
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

        List<Person> people = peopleService.getPeopleSorted();
        Map<Long, Integer> monthPoints = pointsService.monthPoints(effectiveDate);
        List<PointsRow> pointsRows = new ArrayList<>();
        for (Person person : people) {
            int base = person.getBasePoints();
            int month = monthPoints.getOrDefault(person.getId(), 0);
            pointsRows.add(new PointsRow(person, base, month, base + month));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        response.setHeader("Content-Disposition", "attachment; filename=\"punkty-" +
            effectiveDate.getYear() + "-" + String.format("%02d", effectiveDate.getMonthValue()) + ".docx\"");

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

    @GetMapping("/generator/docm")
    public void generatorDocm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              HttpServletResponse response) throws IOException {
        LocalDate effectiveDate = (date == null) ? LocalDate.now() : date;
        String monthName = monthName(effectiveDate);
        Map<String, List<String>> sunday = scheduleService.sundayData(effectiveDate);
        Map<String, List<String>> weekdayMinistranci = scheduleService.weekdayMinistranci(effectiveDate);
        Map<String, List<String>> weekdayLektorzy = scheduleService.weekdayLektorzy(effectiveDate);
        Map<String, List<String>> weekdayAspiranci = scheduleService.weekdayAspiranci(effectiveDate);

        response.setContentType("application/vnd.ms-word.document.macroEnabled.12");
        response.setHeader("Content-Disposition", "attachment; filename=\"lista-" + effectiveDate + ".docm\"");

        try (InputStream template = PointsController.class.getResourceAsStream("/templates/lista-template.docm");
             OutputStream out = response.getOutputStream()) {
            if (template == null) {
                throw new IOException("Brak szablonu /templates/lista-template.docm");
            }
            writeDocmFromTemplate(
                template,
                out,
                monthName,
                formatDate(effectiveDate),
                sunday,
                weekdayMinistranci,
                weekdayLektorzy,
                weekdayAspiranci
            );
        }
    }

    @GetMapping("/generator/docx")
    public void generatorDocx(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                              HttpServletResponse response) throws IOException {
        generatorDocm(date, response);
    }

    private void writeDocmFromTemplate(InputStream template,
                                       OutputStream out,
                                       String monthName,
                                       String validFromDate,
                                       Map<String, List<String>> sunday,
                                       Map<String, List<String>> weekdayMinistranci,
                                       Map<String, List<String>> weekdayLektorzy,
                                       Map<String, List<String>> weekdayAspiranci) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(template);
             ZipOutputStream zos = new ZipOutputStream(out)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ZipEntry outEntry = new ZipEntry(entry.getName());
                outEntry.setTime(entry.getTime());
                zos.putNextEntry(outEntry);
                if ("word/document.xml".equals(entry.getName())) {
                    String xml = new String(zis.readAllBytes(), StandardCharsets.UTF_8);
                    String patched = patchTemplateXml(
                        xml,
                        monthName,
                        validFromDate,
                        sunday,
                        weekdayMinistranci,
                        weekdayLektorzy,
                        weekdayAspiranci
                    );
                    zos.write(patched.getBytes(StandardCharsets.UTF_8));
                } else {
                    zis.transferTo(zos);
                }
                zos.closeEntry();
                zis.closeEntry();
            }
            zos.finish();
        }
    }

    private String patchTemplateXml(String xml,
                                    String monthName,
                                    String validFromDate,
                                    Map<String, List<String>> sunday,
                                    Map<String, List<String>> weekdayMinistranci,
                                    Map<String, List<String>> weekdayLektorzy,
                                    Map<String, List<String>> weekdayAspiranci) {
        String out = replaceMonthInXml(xml, monthName);
        out = out.replaceFirst("\\d{2}\\.\\d{2}\\.\\d{4}", Matcher.quoteReplacement(validFromDate));
        out = replaceScheduleParagraphs(out, sunday, weekdayMinistranci, weekdayLektorzy, weekdayAspiranci);
        return out;
    }

    private String replaceScheduleParagraphs(String xml,
                                             Map<String, List<String>> sunday,
                                             Map<String, List<String>> weekdayMinistranci,
                                             Map<String, List<String>> weekdayLektorzy,
                                             Map<String, List<String>> weekdayAspiranci) {
        Pattern p = Pattern.compile("(?s)<w:p[^>]*>.*?</w:p>");
        Matcher m = p.matcher(xml);
        StringBuffer sb = new StringBuffer();

        String section = "";
        boolean skipNextSundayContinuation = false;

        while (m.find()) {
            String para = m.group();
            String plain = normalizeSpace(stripTags(para));
            String normalized = normalizeForMatch(plain);

            String sundayLabel = detectSundayLabel(normalized);
            if (sundayLabel != null) {
                String label = sundayLabel + ": ";
                String value = joinNames(sunday.getOrDefault(sundayLabel, List.of()));
                String replaced = rebuildParagraphWithTwoRuns(para, label, value);
                m.appendReplacement(sb, Matcher.quoteReplacement(replaced));
                skipNextSundayContinuation = true;
                continue;
            }

            if (skipNextSundayContinuation && isLikelySundayContinuation(normalized)) {
                String replaced = rebuildParagraphWithSingleRun(para, "");
                m.appendReplacement(sb, Matcher.quoteReplacement(replaced));
                skipNextSundayContinuation = false;
                continue;
            }
            skipNextSundayContinuation = false;

            if (normalized.contains("ministranci:") && !normalized.contains("prymaria") && !normalized.contains("suma")) {
                section = "MINISTRANCI";
            } else if (normalized.contains("lektorzy:") && !normalized.contains("prymaria") && !normalized.contains("suma")) {
                section = "LEKTORZY";
            } else if (normalized.contains("aspiranci:") && !normalized.contains("prymaria") && !normalized.contains("suma")) {
                section = "ASPIRANCI";
            }

            String day = detectWeekDay(normalized);
            if (day != null && !section.isEmpty()) {
                Map<String, List<String>> source = switch (section) {
                    case "MINISTRANCI" -> weekdayMinistranci;
                    case "LEKTORZY" -> weekdayLektorzy;
                    case "ASPIRANCI" -> weekdayAspiranci;
                    default -> Map.of();
                };
                String label = day + " - ";
                String value = joinNames(source.getOrDefault(day, List.of()));
                String replaced = rebuildParagraphWithTwoRuns(para, label, value);
                m.appendReplacement(sb, Matcher.quoteReplacement(replaced));
                continue;
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(para));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String detectSundayLabel(String normalizedLine) {
        for (String key : List.of(
            "PRYMARIA (aspiranci)",
            "PRYMARIA (ministranci)",
            "PRYMARIA (lektorzy)",
            "SUMA (aspiranci)",
            "SUMA (ministranci)",
            "SUMA (lektorzy)",
            "III MSZA (aspiranci)",
            "III MSZA (ministranci)",
            "III MSZA (lektorzy)"
        )) {
            if (normalizedLine.startsWith(normalizeForMatch(key))) {
                return key;
            }
        }
        return null;
    }

    private boolean isLikelySundayContinuation(String normalizedLine) {
        if (normalizedLine.isBlank()) {
            return false;
        }
        if (detectWeekDay(normalizedLine) != null) {
            return false;
        }
        return !normalizedLine.contains(":")
            && !normalizedLine.contains("dni powszednie")
            && !normalizedLine.contains("msze niedzielne");
    }

    private String detectWeekDay(String normalizedLine) {
        for (String day : WEEK_DAYS) {
            if (normalizedLine.startsWith(normalizeForMatch(day))) {
                return day;
            }
            if (normalizedLine.startsWith(normalizeForMatch(withPolishDay(day)))) {
                return day;
            }
        }
        return null;
    }

    private String withPolishDay(String day) {
        return switch (day) {
            case "Poniedzialek" -> "Poniedziałek";
            case "Sroda" -> "Środa";
            case "Piatek" -> "Piątek";
            default -> day;
        };
    }

    private String joinNames(List<String> names) {
        List<String> fixed = new ArrayList<>();
        for (String name : names) {
            fixed.add(fixTextArtifacts(name));
        }
        return String.join(", ", fixed);
    }

    private String rebuildParagraphWithSingleRun(String paragraph, String newText) {
        String startTag = extractStartTag(paragraph);
        String pPr = extractPPr(paragraph);
        String rPr = extractFirstRunPr(paragraph);
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
            .append(escapeXml(fixTextArtifacts(newText)))
            .append("</w:t></w:r></w:p>");
        return rebuilt.toString();
    }

    private String rebuildParagraphWithTwoRuns(String paragraph, String labelText, String valueText) {
        String startTag = extractStartTag(paragraph);
        String pPr = extractPPr(paragraph);
        String firstRPr = extractFirstRunPr(paragraph);
        String lastRPr = extractLastRunPr(paragraph);
        if (lastRPr == null) {
            lastRPr = firstRPr;
        }

        StringBuilder rebuilt = new StringBuilder();
        rebuilt.append(startTag);
        if (pPr != null) {
            rebuilt.append(pPr);
        }
        rebuilt.append("<w:r>");
        if (firstRPr != null) {
            rebuilt.append(firstRPr);
        }
        rebuilt.append("<w:t xml:space=\"preserve\">")
            .append(escapeXml(fixTextArtifacts(labelText)))
            .append("</w:t></w:r>");
        rebuilt.append("<w:r>");
        if (lastRPr != null) {
            rebuilt.append(lastRPr);
        }
        rebuilt.append("<w:t xml:space=\"preserve\">")
            .append(escapeXml(fixTextArtifacts(valueText)))
            .append("</w:t></w:r></w:p>");
        return rebuilt.toString();
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

    private String extractLastRunPr(String paragraph) {
        Matcher m = Pattern.compile("(?s)<w:r>\\s*(<w:rPr>.*?</w:rPr>)").matcher(paragraph);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        return last;
    }

    private String stripTags(String xml) {
        String text = xml.replaceAll("<[^>]+>", "");
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'");
    }

    private String normalizeSpace(String text) {
        return text == null ? "" : text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    private String normalizeForMatch(String text) {
        String fixed = fixTextArtifacts(text).toLowerCase(LOCALE_PL);
        String normalized = Normalizer.normalize(fixed, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return normalized.replace("–", "-")
            .replace("—", "-")
            .replace("‑", "-")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String fixTextArtifacts(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("Ä…", "ą")
            .replace("Ä‡", "ć")
            .replace("Ä™", "ę")
            .replace("Äł", "ł")
            .replace("Ĺ‚", "ł")
            .replace("Ĺ„", "ń")
            .replace("Ăł", "ó")
            .replace("Ĺ›", "ś")
            .replace("Ĺş", "ź")
            .replace("Ĺź", "ź")
            .replace("Ĺ¼", "ż")
            .replace("Å»", "Ż")
            .replace("Åš", "Ś")
            .replace("Åš", "Ś")
            .replace("â€“", "-")
            .replace("â€”", "-")
            .replace("â€ž", "\"")
            .replace("â€ť", "\"")
            .replace("â€ś", "\"")
            .replace("â€", "\"")
            .replace("â€¦", "...")
            .replace("Ă„â€¦", "ą")
            .replace("FrĂ„â€¦czyk", "Frączyk");
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

    // moved to PointsService

    public static class PersonRow {
        private final Person person;
        private final int basePoints;
        private final int monthPoints;
        private final int total;

        public PersonRow(Person person, int basePoints, int monthPoints, int total) {
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
