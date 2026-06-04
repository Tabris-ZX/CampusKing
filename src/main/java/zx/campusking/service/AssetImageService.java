package zx.campusking.service;

import org.springframework.http.MediaType;

/**
 * 图片资源接口。
 * 负责读取卡牌图片、卡牌贴图、UI 图片和静态资源，并返回可直接写入 HTTP 响应的二进制载荷。
 */
public interface AssetImageService {

    /**
     * 读取指定卡牌图片。
     *
     * @param folder 卡牌图片目录，只允许 characters 或 skills
     * @param cardId 卡牌 id
     * @return 图片载荷
     */
    ImagePayload loadCardImage(String folder, String cardId);

    /**
     * 读取指定卡牌贴图。
     *
     * @param folder 贴图目录，只允许 cards
     * @param textureId 贴图 id
     * @return 图片载荷
     */
    ImagePayload loadCardTexture(String folder, String textureId);

    /**
     * 读取 UI 图片。
     *
     * @param fileName UI 图片文件名
     * @return 图片载荷
     */
    ImagePayload loadUiImage(String fileName);

    /**
     * 读取静态资源文件。
     *
     * @param fileName 静态资源文件名
     * @return 图片载荷
     */
    ImagePayload loadStaticAsset(String fileName);

    /**
     * 图片二进制载荷和媒体类型。
     */
    record ImagePayload(byte[] bytes, MediaType mediaType) {
    }
}
