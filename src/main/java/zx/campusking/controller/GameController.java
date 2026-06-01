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
import zx.campusking.model.dto.CreateRoomRequest;
import zx.campusking.model.dto.JoinRoomRequest;
import zx.campusking.model.dto.LeaveRoomRequest;
import zx.campusking.model.dto.PlayEffectRequest;
import zx.campusking.model.dto.RestoreSessionResponse;
import zx.campusking.model.dto.SummonRequest;
import zx.campusking.model.MatchState;
import zx.campusking.service.CardCatalogService;
import zx.campusking.service.GameService;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 游戏 REST API。
 * Controller 只负责 HTTP 入参/出参和异常映射，实际房间与对局规则委托给 service 层。
 */
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

    /**
     * 获取全部卡牌静态定义。
     *
     * @return 卡牌定义列表
     */
    @GetMapping("/cards")
    public Object listCards() {
        return cardCatalogService.listAll();
    }

    /**
     * 获取前端运行时配置。
     *
     * @return baseUrl 和 assetBaseUrl
     */
    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of(
                "baseUrl", assetProperties.getPublicBaseUrl(),
                "assetBaseUrl", assetProperties.getBaseUrl()
        );
    }

    /**
     * 创建房间。
     *
     * @param request 创建房间请求，可为空
     * @return 创建后的对局快照
     */
    @PostMapping("/rooms")
    public MatchState createRoom(@RequestBody(required = false) CreateRoomRequest request) {
        return gameService.createRoom(request == null ? new CreateRoomRequest() : request);
    }

    /**
     * 通过房间码加入房间。
     *
     * @param roomCode 房间码
     * @param request 加入房间请求，可为空
     * @return 加入后的对局快照
     */
    @PostMapping("/rooms/{roomCode}/join")
    public MatchState joinRoom(@PathVariable String roomCode, @RequestBody(required = false) JoinRoomRequest request) {
        // Join must preserve backend distinction between "room not found" and "room full".
        return gameService.joinRoom(roomCode, request == null ? new JoinRoomRequest() : request);
    }

    /**
     * 离开房间。
     *
     * @param roomCode 房间码
     * @param request 离开房间请求，可为空
     * @return 操作结果
     */
    @PostMapping("/rooms/{roomCode}/leave")
    public Map<String, String> leaveRoom(@PathVariable String roomCode, @RequestBody(required = false) LeaveRoomRequest request) {
        gameService.leaveRoom(roomCode, request == null ? new LeaveRoomRequest() : request);
        return Map.of("status", "ok");
    }

    /**
     * 获取房间当前对局快照。
     *
     * @param roomCode 房间码
     * @return 对局快照
     */
    @GetMapping("/rooms/{roomCode}")
    public MatchState getRoom(@PathVariable String roomCode) {
        return gameService.getMatchByRoomCode(roomCode);
    }

    /**
     * 通过浏览器会话 token 恢复玩家身份。
     *
     * @param roomCode 房间码
     * @param playerToken 浏览器会话 token
     * @return 对局快照和玩家 id
     */
    @GetMapping("/rooms/{roomCode}/session/{playerToken}")
    public RestoreSessionResponse restoreRoomSession(@PathVariable String roomCode, @PathVariable String playerToken) {
        // Browser refresh recovery is keyed by playerToken rather than a guessed seat id.
        return gameService.restoreRoomSession(roomCode, playerToken);
    }

    /**
     * 通过对局 id 获取对局快照。
     *
     * @param matchId 对局 id
     * @return 对局快照
     */
    @GetMapping("/matches/{matchId}")
    public MatchState getMatch(@PathVariable String matchId) {
        return gameService.getMatch(matchId);
    }

    /**
     * 进入抽牌阶段结算。
     *
     * @param matchId 对局 id
     * @param playerId 当前玩家 id
     * @return 结算后的对局快照
     */
    @PostMapping("/matches/{matchId}/draw")
    public MatchState draw(@PathVariable String matchId, @RequestParam String playerId) {
        return gameService.drawPhase(matchId, playerId);
    }

    /**
     * 召唤角色牌。
     *
     * @param matchId 对局 id
     * @param request 召唤请求
     * @return 召唤后的对局快照
     */
    @PostMapping("/matches/{matchId}/summon")
    public MatchState summon(@PathVariable String matchId, @RequestBody SummonRequest request) {
        return gameService.summon(matchId, request);
    }

    /**
     * 使用技能牌。
     *
     * @param matchId 对局 id
     * @param request 技能请求
     * @return 结算后的对局快照
     */
    @PostMapping("/matches/{matchId}/play-skill")
    public MatchState playSkill(@PathVariable String matchId, @RequestBody PlayEffectRequest request) {
        return gameService.playSkill(matchId, request);
    }

    /**
     * 使用角色攻击对方角色。
     *
     * @param matchId 对局 id
     * @param request 攻击请求
     * @return 结算后的对局快照
     */
    @PostMapping("/matches/{matchId}/attack-character")
    public MatchState attackCharacter(@PathVariable String matchId, @RequestBody AttackCharacterRequest request) {
        return gameService.attackCharacter(matchId, request);
    }

    /**
     * 使用角色直接攻击玩家。
     *
     * @param matchId 对局 id
     * @param request 攻击请求
     * @return 结算后的对局快照
     */
    @PostMapping("/matches/{matchId}/attack-player")
    public MatchState attackPlayer(@PathVariable String matchId, @RequestBody AttackPlayerRequest request) {
        return gameService.attackPlayer(matchId, request);
    }

    /**
     * 结束当前玩家回合。
     *
     * @param matchId 对局 id
     * @param playerId 当前玩家 id
     * @return 切换回合后的对局快照
     */
    @PostMapping("/matches/{matchId}/end-turn")
    public MatchState endTurn(@PathVariable String matchId, @RequestParam String playerId) {
        return gameService.endTurn(matchId, playerId);
    }

    /**
     * 将资源不存在或房间不存在转换为 404。
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(NoSuchElementException exception) {
        return Map.of("error", exception.getMessage());
    }

    /**
     * 将参数错误和规则非法转换为 400。
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(RuntimeException exception) {
        return Map.of("error", exception.getMessage());
    }
}
