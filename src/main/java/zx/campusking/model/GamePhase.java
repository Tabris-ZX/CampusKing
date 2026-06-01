package zx.campusking.model;

/**
 * 对局阶段。
 * 当前原型分为抽牌阶段、行动阶段和结束状态。
 */
public enum GamePhase {
    /** 抽牌阶段。 */
    DRAW,
    /** 行动阶段。 */
    ACTION,
    /** 对局已结束。 */
    FINISHED
}
