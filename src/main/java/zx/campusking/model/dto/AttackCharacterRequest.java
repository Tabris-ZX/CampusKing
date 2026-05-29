package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AttackCharacterRequest {

    private String playerId;
    private String attackerInstanceId;
    private String defenderInstanceId;

}
