package pl.punkty.app.service;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ScheduleService {
    private static final List<String> WEEK_DAYS = List.of(
        "Poniedzialek",
        "Wtorek",
        "Sroda",
        "Czwartek",
        "Piatek",
        "Sobota"
    );

    public Map<String, List<String>> sundayData() {
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

    public Map<String, List<String>> weekdayMinistranci(LocalDate date) {
        return shiftWeekday(baseWeekdayMinistranci(), monthOffsetFromBase(date));
    }

    public Map<String, List<String>> weekdayLektorzy(LocalDate date) {
        return shiftWeekday(baseWeekdayLektorzy(), monthOffsetFromBase(date));
    }

    public List<String> weekdayAspiranci() {
        return List.of("Krzysztof Wierzycki", "Rafal Opoka", "Wojciech Zelek");
    }

    public Map<Integer, Set<String>> scheduledByDay(LocalDate date) {
        Map<String, List<String>> ministranci = weekdayMinistranci(date);
        Map<String, List<String>> lektorzy = weekdayLektorzy(date);
        Set<String> aspiranci = new HashSet<>(weekdayAspiranci());

        Map<Integer, Set<String>> byDay = new HashMap<>();
        for (int i = 0; i < WEEK_DAYS.size(); i++) {
            String day = WEEK_DAYS.get(i);
            Set<String> names = new HashSet<>();
            names.addAll(ministranci.getOrDefault(day, List.of()));
            names.addAll(lektorzy.getOrDefault(day, List.of()));
            names.addAll(aspiranci);
            byDay.put(i + 1, names);
        }
        return byDay;
    }

    private Map<String, List<String>> baseWeekdayMinistranci() {
        Map<String, List<String>> weekdayMinistranci = new LinkedHashMap<>();
        weekdayMinistranci.put("Poniedzialek", List.of("Nikodem Franczyk", "Krzysztof Florek"));
        weekdayMinistranci.put("Wtorek", List.of("Tomasz Gancarczyk", "Marcin Opoka"));
        weekdayMinistranci.put("Sroda", List.of("Damian Sopata", "Karol Jez", "Pawel Jez"));
        weekdayMinistranci.put("Czwartek", List.of("Szymon Zelek", "Antoni Gorcowski"));
        weekdayMinistranci.put("Piatek", List.of("Wojciech Bieniek", "Sebastian Wierzycki"));
        weekdayMinistranci.put("Sobota", List.of("Filip Wierzycki", "Wiktor Wierzycki", "Marcel Smoter"));
        return weekdayMinistranci;
    }

    private Map<String, List<String>> baseWeekdayLektorzy() {
        Map<String, List<String>> weekdayLektorzy = new LinkedHashMap<>();
        weekdayLektorzy.put("Poniedzialek", List.of("Kacper Florek", "Karol Klag"));
        weekdayLektorzy.put("Wtorek", List.of("Sebastian Sopata", "Radoslaw Sopata"));
        weekdayLektorzy.put("Sroda", List.of("Pawel Wierzycki", "Daniel Nowak"));
        weekdayLektorzy.put("Czwartek", List.of("Michal Furtak"));
        weekdayLektorzy.put("Piatek", List.of("Stanislaw Lubecki", "Jan Migacz"));
        weekdayLektorzy.put("Sobota", List.of("Szymon Mucha", "Jakub Mucha"));
        return weekdayLektorzy;
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
}
