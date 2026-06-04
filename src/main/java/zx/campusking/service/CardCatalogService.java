package zx.campusking.service;

import zx.campusking.cards.GameCard;
import zx.campusking.model.CardDefinition;

import java.util.List;

/**
 * 卡牌目录接口。
 * 负责读取、保存和实例化当前可用的卡牌定义与卡牌规则对象。
 */
public interface CardCatalogService {

    /**
     * 返回当前全部可用卡牌定义。
     *
     * @return 卡牌定义列表
     */
    List<CardDefinition> listAll();

    /**
     * 按卡牌 id 读取静态定义，不存在时抛出异常。
     *
     * @param cardId 卡牌 id
     * @return 静态定义
     */
    CardDefinition require(String cardId);

    /**
     * 按卡牌 id 读取规则对象，不存在时抛出异常。
     *
     * @param cardId 卡牌 id
     * @return 卡牌规则对象
     */
    GameCard requireCard(String cardId);

    /**
     * 保存并规范化卡牌注册表。
     *
     * @param incomingCards 待保存卡牌定义
     * @return 保存后的全部可用卡牌定义
     */
    List<CardDefinition> saveRegistry(List<CardDefinition> incomingCards);
}
