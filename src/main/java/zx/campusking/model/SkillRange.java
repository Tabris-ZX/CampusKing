package zx.campusking.model;

/**
 * 技能作用范围。
 */
public enum SkillRange {
    /** 单体目标，需要前端或机器人指定目标。 */
    SINGLE,
    /** 只作用于使用者。 */
    SELF,
    /** 作用于对手。 */
    ENEMY,
    /** 同时作用于双方。 */
    BOTH
}
