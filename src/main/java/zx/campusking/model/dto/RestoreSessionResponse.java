package zx.campusking.model.dto;

import lombok.Getter;
import lombok.Setter;
import zx.campusking.model.MatchState;

@Setter
@Getter
public class RestoreSessionResponse {

    private MatchState match;
    private String playerId;

    public RestoreSessionResponse(MatchState match, String playerId) {
        this.match = match;
        this.playerId = playerId;
    }

}
