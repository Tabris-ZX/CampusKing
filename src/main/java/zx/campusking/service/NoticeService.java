package zx.campusking.service;

import zx.campusking.model.dto.NoticeResponse;
import zx.campusking.model.dto.SaveNoticeRequest;

import java.util.List;

/**
 * 公告接口。
 * 负责读取和保存 data/notice 下的公告文件。
 */
public interface NoticeService {

    /** 读取全部公告，按时间倒序返回。 */
    List<NoticeResponse> listNotices();

    /** 保存一条公告。 */
    NoticeResponse saveNotice(SaveNoticeRequest request);
}
