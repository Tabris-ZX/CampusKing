package zx.campusking.service;

import zx.campusking.model.CardInstance;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;

import java.util.List;

/**
 * 牌堆接口。
 * 负责构筑公共牌堆、抽牌，以及在抽牌堆为空时处理墓地洗回。
 */
public interface DeckService {

    /**
     * 按当前单面玩法配置构筑公共牌堆。
     *
     * @param playerAId 玩家 A id
     * @param playerBId 玩家 B id
     * @return 新牌堆
     */
    List<CardInstance> buildDeck(String playerAId, String playerBId);

    /**
     * 从当前对局抽牌堆顶抽一张牌给玩家。
     *
     * @param match 当前对局
     * @param player 抽牌玩家
     */
    void drawOne(MatchState match, PlayerState player);
}
