package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JoinRoomRequest {

    private String playerName;
    private String playerToken;

}
