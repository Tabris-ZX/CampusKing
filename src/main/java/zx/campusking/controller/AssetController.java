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

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetImageService assetImageService;

    public AssetController(AssetImageService assetImageService) {
        this.assetImageService = assetImageService;
    }

    @GetMapping("/card-images/{folder}/{cardId}")
    public ResponseEntity<byte[]> cardImage(@PathVariable String folder, @PathVariable String cardId) {
        AssetImageService.ImagePayload payload = assetImageService.loadCardImage(folder, cardId);
        return ResponseEntity.ok()
          .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
          .contentType(payload.mediaType())
          .body(payload.bytes());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleServerError(IllegalStateException exception) {
        return Map.of("error", exception.getMessage());
    }
}
