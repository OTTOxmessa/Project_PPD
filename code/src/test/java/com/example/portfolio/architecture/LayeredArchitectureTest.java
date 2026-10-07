package com.example.portfolio.architecture;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// ตรวจกฎของ Layered Architecture จาก import จริงในซอร์สโค้ด (ใบงานข้อ 3: ห้ามข้าม layer)
//
//   controller / scheduler  →  service (interface)  →  repository  →  domain
//          ↘ dto + mapper ↙
//
// ถ้ามีใครเผลอให้ controller เรียก repository ตรง ๆ หรือให้ service ไปรู้จัก DTO ของ API test นี้จะล้มทันที
// Maven รัน test โดยใช้โฟลเดอร์ code/ เป็น working directory จึงอ้าง path แบบ relative ได้
class LayeredArchitectureTest {

    private static final Path ROOT = Path.of("src", "main", "java", "com", "example", "portfolio");
    private static final String BASE = "com.example.portfolio.";

    private record SourceFile(String name, String layer, List<String> imports) {
    }

    private static List<SourceFile> sources;

    @BeforeAll
    static void readSources() throws IOException {
        List<SourceFile> result = new ArrayList<>();
        try (Stream<Path> files = Files.walk(ROOT)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Path relative = ROOT.relativize(file);
                String layer = relative.getNameCount() > 1 ? relative.getName(0).toString() : "root";
                List<String> imports = Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                        .map(String::trim)
                        .filter(line -> line.startsWith("import " + BASE))
                        .map(line -> line.substring(("import " + BASE).length()).replace(";", ""))
                        .toList();
                result.add(new SourceFile(relative.toString().replace('\\', '/'), layer, imports));
            }
        }
        sources = result;
    }

    // คืนรายการ "ไฟล์ -> import ที่ผิดกฎ" ของ layer ที่กำหนด
    private static List<String> violations(String layer, Predicate<String> forbiddenImport) {
        return sources.stream()
                .filter(s -> s.layer().equals(layer))
                .flatMap(s -> s.imports().stream().filter(forbiddenImport).map(i -> s.name() + " -> " + i))
                .toList();
    }

    @Test
    @DisplayName("อ่านซอร์สโค้ดได้จริง (กันกรณี path ผิดแล้ว test ผ่านเพราะไม่มีไฟล์ให้ตรวจ)")
    void sourcesFound() {
        assertThat(sources).hasSizeGreaterThan(50);
        assertThat(sources).anyMatch(s -> s.layer().equals("controller"));
    }

    @Test
    @DisplayName("Controller ไม่เรียก Repository ตรง และไม่สร้าง/คืน Entity เอง")
    void controllersUseServicesAndDtosOnly() {
        assertThat(violations("controller", i -> i.startsWith("repository.") || i.startsWith("domain.entity.")))
                .isEmpty();
    }

    @Test
    @DisplayName("Controller รู้จักแค่ service interface — ไม่แตะ impl หรือคลาส Strategy/Pattern ภายใน")
    void controllersDependOnServiceInterfacesOnly() {
        assertThat(violations("controller", i -> i.startsWith("service.") && !i.matches("service\\.[A-Z]\\w*")))
                .isEmpty();
    }

    @Test
    @DisplayName("Scheduler (จุดเริ่มงานตามเวลา) เรียกผ่าน Service เท่านั้น")
    void schedulersUseServicesOnly() {
        assertThat(violations("scheduler", i -> i.startsWith("repository.") || i.startsWith("service.impl.")))
                .isEmpty();
    }

    @Test
    @DisplayName("Service ไม่ขึ้นกับชั้นบน (controller, DTO, mapper)")
    void servicesDoNotDependOnPresentation() {
        assertThat(violations("service",
                i -> i.startsWith("controller.") || i.startsWith("dto.") || i.startsWith("mapper.")))
                .isEmpty();
    }

    @Test
    @DisplayName("Repository ขึ้นกับ Domain เท่านั้น")
    void repositoriesDependOnDomainOnly() {
        assertThat(violations("repository", i -> !i.startsWith("domain."))).isEmpty();
    }

    @Test
    @DisplayName("Domain เป็นชั้นล่างสุด ไม่รู้จักชั้นอื่นเลย")
    void domainIsIndependent() {
        assertThat(violations("domain", i -> !i.startsWith("domain."))).isEmpty();
    }

    @Test
    @DisplayName("DTO ไม่รู้จัก Entity, Service หรือ Repository (เป็นแค่สัญญาของ API)")
    void dtosArePlainContracts() {
        assertThat(violations("dto", i -> !i.startsWith("domain.enums."))).isEmpty();
    }

    @Test
    @DisplayName("Mapper แปลงข้อมูลอย่างเดียว ไม่เรียก Repository")
    void mappersDoNotUseRepositories() {
        assertThat(violations("mapper", i -> i.startsWith("repository.") || i.startsWith("service.impl.")))
                .isEmpty();
    }

    @Test
    @DisplayName("ไม่มีชั้นใดเรียกขึ้นไปหา Controller")
    void nothingDependsOnControllers() {
        List<String> found = sources.stream()
                .filter(s -> !s.layer().equals("controller"))
                .flatMap(s -> s.imports().stream().filter(i -> i.startsWith("controller.")).map(i -> s.name() + " -> " + i))
                .toList();
        assertThat(found).isEmpty();
    }
}
