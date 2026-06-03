package zx.campusking.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import zx.campusking.config.AssetProperties;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AssetImageService {

    private static final List<Double> SCALE_STEPS = List.of(1.0, 0.92, 0.84, 0.76, 0.68, 0.6, 0.5, 0.4);
    private static final List<Float> JPEG_QUALITIES = List.of(0.9f, 0.82f, 0.74f, 0.66f, 0.58f, 0.5f);
    private static final Map<String, String> CARD_FOLDERS = Map.of(
      "characters", "characters",
      "skills", "skills"
    );

    private final AssetProperties assetProperties;
    private final Map<String, CachedImage> cache = new ConcurrentHashMap<>();

    public AssetImageService(AssetProperties assetProperties) {
        this.assetProperties = assetProperties;
    }

    public ImagePayload loadCardImage(String folder, String cardId) {
        String safeFolder = CARD_FOLDERS.get(folder);
        if (safeFolder == null) {
            throw new NoSuchElementException("未知图片目录: " + folder);
        }
        if (cardId == null || !cardId.matches("[a-zA-Z0-9_-]+")) {
            throw new NoSuchElementException("未知卡牌图片: " + cardId);
        }

        String relativePath = "images/texture/" + safeFolder + "/" + cardId + ".webp";
        SourceImage sourceImage = readSourceImage(relativePath);
        CachedImage cachedImage = cache.get(relativePath);
        if (cachedImage != null && cachedImage.matches(sourceImage.cacheTag())) {
            return cachedImage.payload();
        }

        ImagePayload payload = compressIfNeeded(sourceImage);
        cache.put(relativePath, new CachedImage(sourceImage.cacheTag(), payload));
        return payload;
    }

    public ImagePayload loadStaticAsset(String fileName) {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9_.-]+")) {
            throw new NoSuchElementException("未知静态资源: " + fileName);
        }

        SourceImage sourceImage = readSourceImage(fileName);
        CachedImage cachedImage = cache.get(fileName);
        if (cachedImage != null && cachedImage.matches(sourceImage.cacheTag())) {
            return cachedImage.payload();
        }

        ImagePayload payload = new ImagePayload(sourceImage.bytes(), sourceImage.mediaType());
        cache.put(fileName, new CachedImage(sourceImage.cacheTag(), payload));
        return payload;
    }

    public ImagePayload loadUiImage(String fileName) {
        if (fileName == null || !fileName.matches("[a-zA-Z0-9_.-]+")) {
            throw new NoSuchElementException("未知 UI 图片: " + fileName);
        }

        String relativePath = "images/ui/" + fileName;
        SourceImage sourceImage = readLocalSource(relativePath);
        CachedImage cachedImage = cache.get(relativePath);
        if (cachedImage != null && cachedImage.matches(sourceImage.cacheTag())) {
            return cachedImage.payload();
        }

        ImagePayload payload = new ImagePayload(sourceImage.bytes(), sourceImage.mediaType());
        cache.put(relativePath, new CachedImage(sourceImage.cacheTag(), payload));
        return payload;
    }

    public ImagePayload loadCardTexture(String folder, String textureId) {
        if (!"cards".equals(folder)) {
            throw new NoSuchElementException("未知贴图目录: " + folder);
        }
        if (textureId == null || !textureId.matches("[a-zA-Z0-9_-]+")) {
            throw new NoSuchElementException("未知卡牌贴图: " + textureId);
        }

        String relativePath = "images/texture/cards/" + textureId + ".webp";
        SourceImage sourceImage = readLocalSource(relativePath);
        CachedImage cachedImage = cache.get(relativePath);
        if (cachedImage != null && cachedImage.matches(sourceImage.cacheTag())) {
            return cachedImage.payload();
        }

        ImagePayload payload = new ImagePayload(sourceImage.bytes(), sourceImage.mediaType());
        cache.put(relativePath, new CachedImage(sourceImage.cacheTag(), payload));
        return payload;
    }

    private SourceImage readSourceImage(String relativePath) {
        if (!assetProperties.getBaseUrl().isBlank()) {
            return readRemoteSource(relativePath);
        }
        return readLocalSource(relativePath);
    }

    private SourceImage readLocalSource(String relativePath) {
        Path rootPath = Path.of(assetProperties.getLocalRoot());
        if (!rootPath.isAbsolute()) {
            rootPath = Path.of(System.getProperty("user.dir")).resolve(rootPath).normalize();
        }
        Path filePath = rootPath.resolve(relativePath).normalize();
        if (!filePath.startsWith(rootPath) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new NoSuchElementException("未找到图片资源: " + relativePath);
        }

        try {
            byte[] bytes = Files.readAllBytes(filePath);
            long lastModified = Files.getLastModifiedTime(filePath).toMillis();
            String contentType = Files.probeContentType(filePath);
            return new SourceImage(bytes, detectMediaType(contentType, relativePath), "local:" + filePath + ":" + lastModified + ":" + bytes.length);
        } catch (IOException exception) {
            throw new IllegalStateException("读取图片失败: " + relativePath, exception);
        }
    }

    private SourceImage readRemoteSource(String relativePath) {
        String normalizedBaseUrl = assetProperties.getBaseUrl().replaceAll("/+$", "");
        String sourceUrl = normalizedBaseUrl + "/" + relativePath;
        try {
            URLConnection connection = java.net.URI.create(sourceUrl).toURL().openConnection();
            connection.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
            connection.setReadTimeout((int) Duration.ofSeconds(10).toMillis());
            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = inputStream.readAllBytes();
                String contentType = connection.getContentType();
                long lastModified = connection.getLastModified();
                return new SourceImage(bytes, detectMediaType(contentType, sourceUrl), "remote:" + sourceUrl + ":" + lastModified + ":" + bytes.length);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("读取远程图片失败: " + sourceUrl, exception);
        }
    }

    private ImagePayload compressIfNeeded(SourceImage sourceImage) {
        int limitBytes = assetProperties.getMaxResponseBytes();
        if (sourceImage.bytes().length <= limitBytes) {
            return new ImagePayload(sourceImage.bytes(), sourceImage.mediaType());
        }

        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(sourceImage.bytes()));
            if (original == null) {
                return new ImagePayload(sourceImage.bytes(), sourceImage.mediaType());
            }

            EncodedImage encoded = encodeWithinLimit(original, sourceImage.mediaType(), limitBytes);
            return new ImagePayload(encoded.bytes(), encoded.mediaType());
        } catch (IOException exception) {
            throw new IllegalStateException("压缩图片失败", exception);
        }
    }

    private EncodedImage encodeWithinLimit(BufferedImage original, MediaType sourceMediaType, int limitBytes) throws IOException {
        boolean hasAlpha = original.getColorModel().hasAlpha();
        EncodedImage best = null;

        if (hasAlpha) {
            best = tryPngVariants(original, limitBytes, best);
            if (best != null && best.bytes().length <= limitBytes) {
                return best;
            }
        }

        best = tryJpegVariants(original, hasAlpha, limitBytes, best);
        if (best != null && best.bytes().length <= limitBytes) {
            return best;
        }

        if (!hasAlpha && MediaType.IMAGE_PNG.equals(sourceMediaType)) {
            best = tryPngVariants(original, limitBytes, best);
            if (best != null && best.bytes().length <= limitBytes) {
                return best;
            }
        }

        if (best != null) {
            return best;
        }

        return new EncodedImage(writePng(original), MediaType.IMAGE_PNG);
    }

    private EncodedImage tryPngVariants(BufferedImage original, int limitBytes, EncodedImage best) throws IOException {
        for (double scale : SCALE_STEPS) {
            BufferedImage variant = scaledImage(original, scale, true);
            EncodedImage encoded = new EncodedImage(writePng(variant), MediaType.IMAGE_PNG);
            best = smallerOf(best, encoded);
            if (encoded.bytes().length <= limitBytes) {
                return encoded;
            }
        }
        return best;
    }

    private EncodedImage tryJpegVariants(BufferedImage original, boolean hasAlpha, int limitBytes, EncodedImage best) throws IOException {
        for (double scale : SCALE_STEPS) {
            // JPEG does not support alpha; always flatten to RGB before encoding.
            BufferedImage variant = scaledImage(original, scale, false);
            for (float quality : JPEG_QUALITIES) {
                EncodedImage encoded = new EncodedImage(writeJpeg(variant, quality), MediaType.IMAGE_JPEG);
                best = smallerOf(best, encoded);
                if (encoded.bytes().length <= limitBytes) {
                    return encoded;
                }
            }
        }
        return best;
    }

    private EncodedImage smallerOf(EncodedImage left, EncodedImage right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return right.bytes().length < left.bytes().length ? right : left;
    }

    private BufferedImage scaledImage(BufferedImage original, double scale, boolean preserveAlpha) {
        if (scale >= 0.999d) {
            return preserveAlpha ? copyImage(original, BufferedImage.TYPE_INT_ARGB) : copyImage(original, BufferedImage.TYPE_INT_RGB);
        }

        int width = Math.max(1, (int) Math.round(original.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(original.getHeight() * scale));
        int type = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage scaled = new BufferedImage(width, height, type);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!preserveAlpha) {
                graphics.setBackground(java.awt.Color.WHITE);
                graphics.clearRect(0, 0, width, height);
            }
            graphics.drawImage(original, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    private BufferedImage copyImage(BufferedImage original, int type) {
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), type);
        Graphics2D graphics = copy.createGraphics();
        try {
            if (type == BufferedImage.TYPE_INT_RGB) {
                graphics.setBackground(java.awt.Color.WHITE);
                graphics.clearRect(0, 0, copy.getWidth(), copy.getHeight());
            }
            graphics.drawImage(original, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return copy;
    }

    private byte[] writePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        return outputStream.toByteArray();
    }

    private byte[] writeJpeg(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IllegalStateException("当前环境不支持 JPEG 编码");
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return outputStream.toByteArray();
    }

    private MediaType detectMediaType(String contentType, String sourcePath) {
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (IllegalArgumentException ignored) {
                // Fall back to extension sniffing below.
            }
        }

        if (sourcePath != null && sourcePath.toLowerCase().endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }

        String guessed = URLConnection.guessContentTypeFromName(sourcePath);
        if (guessed != null && !guessed.isBlank()) {
            return MediaType.parseMediaType(guessed);
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private record SourceImage(byte[] bytes, MediaType mediaType, String cacheTag) {
    }

    private record EncodedImage(byte[] bytes, MediaType mediaType) {
    }

    private record CachedImage(String cacheTag, ImagePayload payload) {
        private boolean matches(String otherTag) {
            return cacheTag.equals(otherTag);
        }
    }

    public record ImagePayload(byte[] bytes, MediaType mediaType) {
    }
}
