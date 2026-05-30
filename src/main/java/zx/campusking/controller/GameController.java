package zx.campusking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import zx.campusking.config.AssetProperties;
import zx.campusking.model.dto.AttackCharacterRequest;
import zx.campusking.model.dto.AttackPlayerRequest;
import zx.campusking.model.dto.CreateMatchRequest;
import zx.campusking.model.dto.CreateRoomRequest;
import zx.campusking.model.dto.JoinRoomRequest;
import zx.campusking.model.dto.LeaveRoomRequest;
import zx.campusking.model.dto.PlayEffectRequest;
import zx.campusking.model.dto.RestoreSessionResponse;
import zx.campusking.model.dto.SummonRequest;
import zx.campusking.model.MatchState;
import zx.campusking.service.CardCatalogService;
import zx.campusking.service.GameService;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;
    private final CardCatalogService cardCatalogService;
    private final AssetProperties assetProperties;

    public GameController(GameService gameService, CardCatalogService cardCatalogService, AssetProperties assetProperties) {
        this.gameService = gameService;
        this.cardCatalogService = cardCatalogService;
        this.assetProperties = assetProperties;
    }

    @GetMapping("/cards")
    public Object listCards() {
        return cardCatalogService.listAll();
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("assetBaseUrl", assetProperties.getBaseUrl());
    }

    @GetMapping("/matches")
    public List<MatchState> listMatches() {
        return gameService.listMatches();
    }

    @PostMapping("/rooms")
    public MatchState createRoom(@RequestBody(required = false) CreateRoomRequest request) {
        // Room creation supports custom room codes for repeatable local testing.
        return gameService.createRoom(request == null ? new CreateRoomRequest() : request);
    }

    @PostMapping("/rooms/{roomCode}/join")
    public MatchState joinRoom(@PathVariable String roomCode, @RequestBody(required = false) JoinRoomRequest request) {
        // Join must preserve backend distinction between "room not found" and "room full".
        return gameService.joinRoom(roomCode, request == null ? new JoinRoomRequest() : request);
    }

    @PostMapping("/rooms/{roomCode}/leave")
    public Map<String, String> leaveRoom(@PathVariable String roomCode, @RequestBody(required = false) LeaveRoomRequest request) {
        gameService.leaveRoom(roomCode, request == null ? new LeaveRoomRequest() : request);
        return Map.of("status", "ok");
    }

    @GetMapping("/rooms/{roomCode}")
    public MatchState getRoom(@PathVariable String roomCode) {
        return gameService.getMatchByRoomCode(roomCode);
    }

    @GetMapping("/rooms/{roomCode}/session/{playerToken}")
    public RestoreSessionResponse restoreRoomSession(@PathVariable String roomCode, @PathVariable String playerToken) {
        // Browser refresh recovery is keyed by playerToken rather than a guessed seat id.
        return gameService.restoreRoomSession(roomCode, playerToken);
    }

    @PostMapping("/matches")
    public MatchState createMatch(@RequestBody(required = false) CreateMatchRequest request) {
        return gameService.createMatch(request == null ? new CreateMatchRequest() : request);
    }

    @GetMapping("/matches/{matchId}")
    public MatchState getMatch(@PathVariable String matchId) {
        return gameService.getMatch(matchId);
    }

    @PostMapping("/matches/{matchId}/draw")
    public MatchState draw(@PathVariable String matchId, @RequestParam String playerId) {
        return gameService.drawPhase(matchId, playerId);
    }

    @PostMapping("/matches/{matchId}/summon")
    public MatchState summon(@PathVariable String matchId, @RequestBody SummonRequest request) {
        return gameService.summon(matchId, request);
    }

    @PostMapping("/matches/{matchId}/play-skill")
    public MatchState playSkill(@PathVariable String matchId, @RequestBody PlayEffectRequest request) {
        return gameService.playSkill(matchId, request);
    }

    @PostMapping("/matches/{matchId}/attack-character")
    public MatchState attackCharacter(@PathVariable String matchId, @RequestBody AttackCharacterRequest request) {
        return gameService.attackCharacter(matchId, request);
    }

    @PostMapping("/matches/{matchId}/attack-player")
    public MatchState attackPlayer(@PathVariable String matchId, @RequestBody AttackPlayerRequest request) {
        return gameService.attackPlayer(matchId, request);
    }

    @PostMapping("/matches/{matchId}/end-turn")
    public MatchState endTurn(@PathVariable String matchId, @RequestParam String playerId) {
        return gameService.endTurn(matchId, playerId);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }
}
