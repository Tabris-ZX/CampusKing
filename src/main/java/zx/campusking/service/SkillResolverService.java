package zx.campusking.service;

import zx.campusking.model.CardDefinition;
import zx.campusking.model.MatchState;
import zx.campusking.model.PlayerState;
import zx.campusking.model.dto.PlayEffectRequest;

/**
 * 技能结算接口。
 * 负责技能合法性判断、技能反制消耗和技能效果派发。
 */
public interface SkillResolverService {

    /** 尝试消耗防御方的下一张技能反制效果。 */
    boolean consumeSkillPrevention(PlayerState defender, MatchState match, CardDefinition definition);

    /** 判断技能在当前上下文中是否可结算。 */
    boolean canResolveEffect(MatchState match, PlayerState player, PlayerState enemy, CardDefinition definition, PlayEffectRequest request);

    /** 结算技能效果。 */
    void resolveEffect(MatchState match, PlayerState player, PlayerState enemy, CardDefinition definition, PlayEffectRequest request);
}
