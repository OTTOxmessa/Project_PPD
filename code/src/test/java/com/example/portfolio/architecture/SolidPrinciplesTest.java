package com.example.portfolio.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ตรวจกฎ SOLID ที่ใบงานกำหนด (ข้อ 4) จากซอร์สโค้ดจริง — ใครเผลอทำผิดกฎ test จะล้มทันที
// รายละเอียดและเหตุผลของแต่ละหลักการอยู่ใน doc/solid-analysis.md
class SolidPrinciplesTest {

    private static final Path ROOT = Path.of("src", "main", "java", "com", "example", "portfolio");

    // ประเภทที่สร้างภายในคลาสเอง (ไม่ได้ฉีดเข้ามา) จึงไม่ต้องเป็น interface
    private static final Set<String> BUILT_INTERNALLY = Set.of("StrategyRegistry");

    // code = ซอร์สที่ตัดคอมเมนต์ // ออกแล้ว (คอมเมนต์ที่พูดถึงคำว่า switch ไม่นับ), lines = จำนวนบรรทัดจริง
    private record SourceFile(String path, String simpleName, String code, long lines) {
    }

    private static List<SourceFile> sources;
    private static Map<String, SourceFile> byName;

    @BeforeAll
    static void readSources() throws IOException {
        List<SourceFile> result = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                String path = ROOT.relativize(file).toString().replace('\\', '/');
                String raw = Files.readString(file, StandardCharsets.UTF_8);
                result.add(new SourceFile(path, name, raw.replaceAll("//[^\\n]*", ""), raw.lines().count()));
            }
        }
        sources = result;
        byName = new HashMap<>();
        result.forEach(s -> byName.put(s.simpleName(), s));
    }

    private static boolean isAbstraction(SourceFile file) {
        return file.code().contains("public interface " + file.simpleName())
                || file.code().contains("public abstract class " + file.simpleName());
    }

    private static List<String> matches(Pattern pattern, String pathPrefix) {
        List<String> found = new ArrayList<>();
        for (SourceFile s : sources) {
            if (!s.path().startsWith(pathPrefix)) {
                continue;
            }
            Matcher m = pattern.matcher(s.code());
            while (m.find()) {
                found.add(s.path() + ": " + m.group().trim());
            }
        }
        return found;
    }

    @Test
    @DisplayName("S — Service แต่ละตัวมีขนาดพอดีหน้าที่เดียว (ไม่เกิน 150 บรรทัด)")
    void servicesStaySmall() {
        List<String> large = sources.stream()
                .filter(s -> s.path().startsWith("service/impl/"))
                .filter(s -> s.lines() > 150)
                .map(s -> s.path() + " (" + s.lines() + " บรรทัด)")
                .toList();
        assertThat(large).isEmpty();
    }

    @Test
    @DisplayName("O — Service layer ไม่มี switch ตามประเภท (ใช้ Strategy / State / ตารางจับคู่แทน)")
    void noSwitchInServiceLayer() {
        assertThat(matches(Pattern.compile("switch\\s*\\("), "service/")).isEmpty();
    }

    @Test
    @DisplayName("L — ไม่มี implementation ไหน throw UnsupportedOperationException")
    void noUnsupportedOperations() {
        assertThat(matches(Pattern.compile("UnsupportedOperationException"), "")).isEmpty();
    }

    @Test
    @DisplayName("I — Interface ของ Service มีไม่เกิน 6 เมธอด (ไม่มี Fat Interface)")
    void serviceInterfacesAreSmall() {
        Pattern abstractMethod = Pattern.compile("(?m)^\\s+[\\w<>\\[\\], ?]+\\s+\\w+\\([^)]*\\);");
        List<String> fat = new ArrayList<>();
        for (SourceFile s : sources) {
            if (s.path().startsWith("service/") && s.code().contains("public interface " + s.simpleName())) {
                long count = abstractMethod.matcher(s.code()).results().count();
                if (count > 6) {
                    fat.add(s.path() + " มี " + count + " เมธอด");
                }
            }
        }
        assertThat(fat).isEmpty();
    }

    @Test
    @DisplayName("D — ไม่มีการฉีดค่าเข้า field (@Autowired / @Value บน field) — ใช้ Constructor Injection เท่านั้น")
    void constructorInjectionOnly() {
        assertThat(matches(Pattern.compile("@Autowired"), "")).isEmpty();
        assertThat(matches(Pattern.compile("@Value\\(\"[^\"]*\"\\)\\s+(private|protected|public)\\s+[^;(]*;"), ""))
                .isEmpty();
    }

    @Test
    @DisplayName("D — dependency ที่ฉีดเข้ามา (private final) ซึ่งเป็นคลาสของโปรเจกต์ ต้องเป็น interface หรือ abstract class")
    void dependOnAbstractions() {
        Pattern dependency = Pattern.compile("private final ([A-Z]\\w*) \\w+;");
        List<String> concrete = new ArrayList<>();
        for (SourceFile s : sources) {
            // config/ คือจุดประกอบระบบ (composition root) ที่ต้องรู้จักคลาสตัวจริงเพื่อต่อสายเข้าด้วยกัน
            if (s.path().startsWith("config/")) {
                continue;
            }
            Matcher m = dependency.matcher(s.code());
            while (m.find()) {
                String type = m.group(1);
                SourceFile target = byName.get(type);
                if (target != null && !BUILT_INTERNALLY.contains(type) && !isAbstraction(target)) {
                    concrete.add(s.path() + " -> " + type);
                }
            }
        }
        assertThat(concrete).isEmpty();
    }

    @Test
    @DisplayName("D — ทุกคลาสใน service/impl implement interface ของตัวเอง")
    void everyServiceImplementsAnInterface() {
        List<String> missing = sources.stream()
                .filter(s -> s.path().startsWith("service/impl/"))
                .filter(s -> !s.code().contains("class " + s.simpleName() + " implements "))
                .map(SourceFile::path)
                .toList();
        assertThat(missing).isEmpty();
    }
}
