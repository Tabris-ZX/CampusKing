package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 公告文件内容。
 */
@Setter
@Getter
public class NoticeResponse {

    private String name;
    private String displayTime;
    private String markdown;

    public NoticeResponse(String name, String displayTime, String markdown) {
        this.name = name;
        this.displayTime = displayTime;
        this.markdown = markdown;
    }
}
