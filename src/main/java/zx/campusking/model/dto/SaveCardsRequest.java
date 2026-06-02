package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;
import zx.campusking.model.CardDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * 保存卡牌注册表请求。
 */
@Setter
@Getter
public class SaveCardsRequest {

    private List<CardDefinition> cards = new ArrayList<>();

}
