package zx.campusking.service;

import org.springframework.stereotype.Service;
import zx.campusking.model.dto.NoticeResponse;
import zx.campusking.model.dto.SaveNoticeRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 读取和保存 resources/notices 下的公告文件。
 */
@Service
public class NoticeService {

    private static final Path NOTICE_DIR = Path.of("resources", "notices");
    private static final Pattern NOTICE_NAME_PATTERN = Pattern.compile("^(\\d{2})(\\d{2})(\\d{2})-(\\d{2})(\\d{2})\\.md$");
    private static final DateTimeFormatter NOTICE_FILE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd-HHmm");

    public List<NoticeResponse> listNotices() {
        if (!Files.isDirectory(NOTICE_DIR)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.list(NOTICE_DIR)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> NOTICE_NAME_PATTERN.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing((Path path) -> path.getFileName().toString()).reversed())
                    .map(this::toNotice)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("读取公告失败", exception);
        }
    }

    public NoticeResponse saveNotice(SaveNoticeRequest request) {
        String markdown = request == null ? "" : request.getMarkdown();
        if (markdown == null || markdown.isBlank()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }
        try {
            Files.createDirectories(NOTICE_DIR);
            String fileName = uniqueNoticeFileName(LocalDateTime.now());
            Path path = NOTICE_DIR.resolve(fileName);
            Files.writeString(path, markdown.trim() + System.lineSeparator(), StandardCharsets.UTF_8);
            return toNotice(path);
        } catch (IOException exception) {
            throw new UncheckedIOException("保存公告失败", exception);
        }
    }

    private String uniqueNoticeFileName(LocalDateTime time) {
        LocalDateTime candidate = time;
        while (true) {
            String fileName = NOTICE_FILE_FORMAT.format(candidate) + ".md";
            if (!Files.exists(NOTICE_DIR.resolve(fileName))) {
                return fileName;
            }
            candidate = candidate.plusMinutes(1);
        }
    }

    private NoticeResponse toNotice(Path path) {
        String name = path.getFileName().toString();
        try {
            return new NoticeResponse(name, displayTime(name), Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new UncheckedIOException("读取公告失败: " + name, exception);
        }
    }

    private String displayTime(String fileName) {
        Matcher matcher = NOTICE_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            return "未命名时间";
        }
        return "20" + matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3)
                + " " + matcher.group(4) + ":" + matcher.group(5);
    }
}
