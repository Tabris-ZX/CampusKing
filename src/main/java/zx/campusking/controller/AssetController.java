package zx.campusking.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zx.campusking.service.AssetImageService;

import java.time.Duration;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 静态资源接口。
 * 负责按当前资源配置读取卡牌图片，并在需要时压缩到前端可接受的大小。
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetImageService assetImageService;

    public AssetController(AssetImageService assetImageService) {
        this.assetImageService = assetImageService;
    }

    /**
     * 读取卡牌图片。
     *
     * @param folder 图片分类，只允许 characters 或 skills
     * @param cardId 卡牌 id
     * @return 图片二进制响应
     */
    @GetMapping("/card-images/{folder}/{cardId}")
    public ResponseEntity<byte[]> cardImage(@PathVariable String folder, @PathVariable String cardId) {
        AssetImageService.ImagePayload payload = assetImageService.loadCardImage(folder, cardId);
        return ResponseEntity.ok()
          .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
          .contentType(payload.mediaType())
          .body(payload.bytes());
    }

    /**
     * 读取 resources 根目录下的 favicon。
     *
     * @return favicon 二进制响应
     */
    @GetMapping("/favicon.ico")
    public ResponseEntity<byte[]> favicon() {
        AssetImageService.ImagePayload payload = assetImageService.loadStaticAsset("favicon.ico");
        return ResponseEntity.ok()
          .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
          .contentType(payload.mediaType())
          .body(payload.bytes());
    }

    /**
     * 将资源不存在转换为 404。
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException exception) {
        return Map.of("error", exception.getMessage());
    }

    /**
     * 将资源读取或压缩失败转换为 500。
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(IllegalStateException exception) {
        return Map.of("error", exception.getMessage());
    }
}
