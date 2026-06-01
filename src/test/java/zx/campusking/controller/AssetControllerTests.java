package zx.campusking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import zx.campusking.config.AssetProperties;
import zx.campusking.service.AssetImageService;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetControllerTests {

    @Test
    void oversizedCardImageIsCompressedBelowOneMegabyte() {
        AssetProperties properties = new AssetProperties();
        properties.setLocalRoot("webui/public");

        AssetImageService service = new AssetImageService(properties);
        AssetController controller = new AssetController(service);

        ResponseEntity<byte[]> response = controller.cardImage("characters", "dragon");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertTrue(response.getHeaders().getContentType() != null);
        assertTrue(response.getBody() != null && response.getBody().length <= 1024 * 1024);
    }

    @Test
    void missingCardImageThrowsNotFound() {
        AssetProperties properties = new AssetProperties();
        properties.setLocalRoot("webui/public");

        AssetImageService service = new AssetImageService(properties);
        AssetController controller = new AssetController(service);

        assertThrows(NoSuchElementException.class, () -> controller.cardImage("characters", "not-found"));
    }
}
