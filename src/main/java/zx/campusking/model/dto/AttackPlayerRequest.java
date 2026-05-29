package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AttackPlayerRequest {

    private String playerId;
    private String attackerInstanceId;

}
