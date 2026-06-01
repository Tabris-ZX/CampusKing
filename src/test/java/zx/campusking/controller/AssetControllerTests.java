package zx.campusking.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import zx.campusking.config.AssetProperties;
import zx.campusking.service.AssetImageService;

import java.lang.reflect.Field;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetControllerTests {

    @Test
    void oversizedCardImageIsCompressedBelowOneMegabyte() {
        AssetProperties properties = new AssetProperties();
        forceLocalAssets(properties);
        properties.setLocalRoot("resources");

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
        forceLocalAssets(properties);
        properties.setLocalRoot("resources");

        AssetImageService service = new AssetImageService(properties);
        AssetController controller = new AssetController(service);

        assertThrows(NoSuchElementException.class, () -> controller.cardImage("characters", "not-found"));
    }

    private void forceLocalAssets(AssetProperties properties) {
        try {
            Field field = AssetProperties.class.getDeclaredField("baseUrl");
            field.setAccessible(true);
            field.set(properties, "");
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("无法设置本地资源模式", exception);
        }
    }
}
