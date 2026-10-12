Design Document: Territorial Paint Game
Overview
The Territorial Paint Game is a real-time multiplayer territory capture game built on a multi-tier Java architecture. The system consists of four Maven modules working together to provide a complete gaming experience:

game-common: Shared protocol definitions, DTOs, enums, and constants

game-client: JavaFX 21 desktop application with UI and client networking

game-server: TCP-based authoritative game server handling real-time gameplay

game-backend: Spring Boot REST API for authentication, persistence, and statistics

Key Architectural Principles
Authoritative Server Architecture: The Game_Server maintains authoritative game state. Clients render server updates rather than predicting outcomes.

Separation of Concerns:

Game_Server handles real-time gameplay, state validation, and match logic

Backend_API handles authentication, persistence, and historical data queries

Game_Client handles presentation and user input

Protocol-Driven Communication:

TCP with line-delimited JSON for real-time game events

REST with JWT authentication for account management and data retrieval

Concurrency Model:

Lobby operations serialized through a coordinator

Each game room processes events sequentially through a dedicated executor

Room executors backed by a shared thread pool

Resilience:

Result outbox pattern for durable match persistence

Idempotent result submission with conflict detection

Automatic retry with exponential backoff

Architecture
System Context
Đoạn mã
graph TB
    Client[Game Client<br/>JavaFX Desktop]
    Server[Game Server<br/>TCP Port 8888]
    Backend[Backend API<br/>Spring Boot REST]
    DB[(MySQL Database)]
    
    Client -->|TCP JSON Messages| Server
    Client -->|REST + JWT| Backend
    Server -->|REST + Service Key| Backend
    Backend -->|JDBC| DB
    
    style Client fill:#2672D6,color:#fff
    style Server fill:#F28C28,color:#fff
    style Backend fill:#124EA6,color:#fff
    style DB fill:#2E72D2,color:#fff
Module Dependencies
Đoạn mã
graph LR
    Common[game-common<br/>Protocol & DTOs]
    Client[game-client<br/>JavaFX UI]
    Server[game-server<br/>TCP Server]
    Backend[game-backend<br/>Spring Boot]
    
    Client --> Common
    Server --> Common
    Backend --> Common
    
    style Common fill:#EBF4FC
    style Client fill:#2672D6,color:#fff
    style Server fill:#F28C28,color:#fff
    style Backend fill:#124EA6,color:#fff
Game Server Architecture
Đoạn mã
graph TB
    subgraph "Game Server"
        Accept[Connection Acceptor<br/>ServerSocket:8888]
        Handler[ClientHandler Pool<br/>ExecutorService]
        Session[Session Manager<br/>ConcurrentHashMap]
        Lobby[Lobby Coordinator<br/>Sequential]
        Challenge[Challenge Manager]
        RoomReg[Room Registry<br/>ConcurrentHashMap]
        Room[Game Room<br/>Serial Event Queue]
        Outbox[Result Outbox<br/>Persistent Storage]
        
        Accept --> Handler
        Handler --> Session
        Handler --> Lobby
        Lobby --> Challenge
        Lobby --> RoomReg
        RoomReg --> Room
        Room --> Outbox
    end
    
    Backend[Backend API]
    Room -->|Persist Result| Backend
    Outbox -->|Retry Logic| Backend
    
    style Accept fill:#124EA6,color:#fff
    style Lobby fill:#F28C28,color:#fff
    style Room fill:#2672D6,color:#fff
Components and Interfaces
game-common Module
Protocol Enumerations
ActionType - All message types in the TCP protocol:

Java
public enum ActionType {
    // Authentication
    LOGIN, LOGIN_RESULT, LOGOUT,
    
    // Lobby
    ONLINE_LIST,
    
    // Challenge flow
    CHALLENGE, CHALLENGE_RECEIVED, CHALLENGE_ACCEPT, 
    CHALLENGE_REJECT, CHALLENGE_CANCEL, CHALLENGE_RESULT,
    
    // Match lifecycle
    MATCH_START, READY, MATCH_BEGIN,
    
    // Gameplay
    MOVE, MOVE_REJECTED, MAP_UPDATE, TIME_SYNC,
    
    // Match completion
    MATCH_RESULT, RESULT_SAVED, RESULT_SAVE_FAILED,
    
    // Rematch flow
    REMATCH, REMATCH_STATUS, EXIT_MATCH, ROOM_CLOSED,
    
    // Connection management
    PING, PONG, ERROR
}
PlayerStatus - Player availability states:

Java
public enum PlayerStatus {
    IDLE,       // Available for challenges
    IN_ROOM,    // In room, preparing or waiting for rematch
    PLAYING     // Currently in active match
}
RoomStatus - Room lifecycle states:

Java
public enum RoomStatus {
    WAITING_READY,    // Waiting for both players to send READY
    PLAYING,          // Active match in progress
    FINISHING,        // Match ended, persisting result
    WAITING_REMATCH,  // Waiting for rematch responses
    CLOSED            // Room terminated
}
Direction - Movement directions:

Java
public enum Direction {
    UP, DOWN, LEFT, RIGHT
}
MatchEndReason:

Java
public enum MatchEndReason {
    TIMEOUT,   // 60-second timer expired
    FORFEIT    // Player disconnected or left
}
MatchOutcome:

Java
public enum MatchOutcome {
    WIN, LOSS, DRAW
}
PersistenceStatus:

Java
public enum PersistenceStatus {
    PENDING,  // In outbox, will be submitted
    SAVED,    // Confirmed by backend
    FAILED    // Permanent error
}
Constants
Java
public final class GameConstants {
    // Grid dimensions
    public static final int GRID_WIDTH = 20;
    public static final int GRID_HEIGHT = 20;
    public static final int MAX_CELLS = 400;
    
    // Timing
    public static final long MATCH_DURATION_MS = 60_000;
    public static final long MOVEMENT_COOLDOWN_MS = 100;
    public static final long CHALLENGE_TIMEOUT_MS = 15_000;
    public static final long READY_TIMEOUT_MS = 10_000;
    public static final long REMATCH_TIMEOUT_MS = 15_000;
    
    // Connection
    public static final int DEFAULT_SERVER_PORT = 8888;
    public static final int PING_INTERVAL_MS = 5_000;
    public static final int PONG_TIMEOUT_MS = 15_000;
    
    // Rate limits
    public static final int MAX_MESSAGE_SIZE = 65_536;
    public static final int MAX_MESSAGES_PER_SECOND = 50;
    
    // Ranking points
    public static final int POINTS_WIN = 10;
    public static final int POINTS_DRAW = 3;
    public static final int POINTS_LOSS = 0;
    
    // Player colors
    public static final String PLAYER1_COLOR = "#2672D6";  // Blue
    public static final String PLAYER2_COLOR = "#F28C28";  // Orange
}
Core DTOs
Message - Envelope for all TCP communications:

Java
public class Message {
    private ActionType actionType;
    private UUID requestId;        // Client-provided for requests, null for events
    private String timestamp;      // UTC ISO-8601
    private Object payload;        // Specific DTO based on actionType
}
PlayerDTO - Player information in online list:

Java
public class PlayerDTO {
    private long userId;
    private String username;
    private long totalScore;
    private PlayerStatus status;
    private boolean canChallenge;
}
PositionDTO:

Java
public class PositionDTO {
    private int x;
    private int y;
}
PlayerSnapshotDTO - Player state within a match:

Java
public class PlayerSnapshotDTO {
    private long userId;
    private String username;
    private int slot;              // 1 or 2
    private String color;          // Hex color
    private PositionDTO position;
    private int cellsOwned;
}
SnapshotDTO - Complete game state:

Java
public class SnapshotDTO {
    private UUID roomId;
    private UUID matchId;
    private long stateVersion;
    private int[][] grid;          // [y][x], 0=unowned, 1=player1, 2=player2
    private List<PlayerSnapshotDTO> players;  // Ordered by slot
    private long remainingMs;
}
PlayerResultDTO - Per-player match outcome:

Java
public class PlayerResultDTO {
    private long userId;
    private int slot;
    private int cellsOwned;
    private MatchOutcome outcome;
    private int rankDelta;
}
StandingsDTO - Current player statistics:

Java
public class StandingsDTO {
    private long userId;
    private long totalScore;
    private int matchesPlayed;
    private int matchesWon;
    private int matchesLost;
    private int matchesDrawn;
    private double winRate;
}
game-client Module
Architecture
Đoạn mã
graph TB
    subgraph "JavaFX Application"
        Main[GameApplication<br/>Main Entry]
        Scene[SceneManager<br/>Screen Navigation]
        
        Login[LoginController]
        Lobby[LobbyController]
        Game[GameController]
        Ranking[RankingController]
        History[HistoryController]
        
        Main --> Scene
        Scene --> Login
        Scene --> Lobby
        Scene --> Game
        Scene --> Ranking
        Scene --> History
    end
    
    subgraph "Network Layer"
        Rest[RestClient<br/>HTTP + JWT]
        Tcp[TcpClient<br/>JSON Messages]
        Msg[MessageHandler<br/>Dispatch]
    end
    
    subgraph "Services"
        Auth[AuthService]
        Player[PlayerService]
        Match[MatchService]
    end
    
    Lobby --> Player
    Game --> Match
    Login --> Auth
    
    Auth --> Rest
    Auth --> Tcp
    Player --> Tcp
    Match --> Tcp
    
    Tcp --> Msg
    Msg --> Lobby
    Msg --> Game
    
    style Main fill:#124EA6,color:#fff
    style Tcp fill:#F28C28,color:#fff
    style Game fill:#2672D6,color:#fff
Key Components
GameApplication - JavaFX entry point:

Initializes SceneManager

Loads FXML layouts

Applies CSS styling

Manages application lifecycle

SceneManager - Screen navigation:

Java
public class SceneManager {
    public void showLogin();
    public void showLobby(String jwt, PlayerProfile profile);
    public void showGameBoard(UUID roomId, SnapshotDTO snapshot);
    public void returnToLobby();
    public void cleanup();  // Remove listeners, stop loops
}
TcpClient - TCP connection management:

Java
public class TcpClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private MessageHandler handler;
    private ScheduledExecutorService heartbeat;
    
    public void connect(String host, int port);
    public void login(String jwt);
    public void send(Message message);
    public void disconnect();
    
    // Background reader thread
    private void readLoop();
    private void handleLine(String line);
}
MessageHandler - Dispatches received messages to controllers:

Java
public interface MessageHandler {
    void onLoginResult(LoginResultPayload payload);
    void onOnlineList(OnlineListPayload payload);
    void onChallengeReceived(ChallengeReceivedPayload payload);
    void onMatchStart(MatchStartPayload payload);
    void onMatchBegin(MatchBeginPayload payload);
    void onMapUpdate(MapUpdatePayload payload);
    void onTimeSync(TimeSyncPayload payload);
    void onMatchResult(MatchResultPayload payload);
    void onError(ErrorPayload payload);
    // ... other message types
}
RestClient - HTTP communication:

Java
public class RestClient {
    private final String baseUrl;
    private final HttpClient httpClient;
    private String jwt;
    
    public RegisterResponse register(String username, String password, String fullName);
    public LoginResponse login(String username, String password);
    public ProfileResponse getProfile();
    public LeaderboardResponse getLeaderboard(int page, int size);
    public HistoryResponse getHistory(int page, int size);
}
LobbyController - Main lobby UI:

TableView for online players

Profile card display

Challenge button handling

Challenge invitation dialog

Navigation to ranking, history, settings

GameController - Active gameplay:

Canvas rendering for 20×20 grid

KeyEvent handling (W/A/S/D)

Held-key state management

Movement throttling (100ms)

Timer display with interpolation

Match result dialog

Rematch UI

Input Management:

Java
public class InputManager {
    private final Set<KeyCode> heldKeys = new HashSet<>();
    private KeyCode activeKey;
    private long lastMoveTime;
    
    public void onKeyPressed(KeyEvent event);
    public void onKeyReleased(KeyEvent event);
    public Optional<Direction> getCurrentDirection();
    public boolean canSendMove();  // Check 100ms cooldown
    public void clear();  // On focus loss or screen change
}
game-server Module
Architecture
Đoạn mã
graph TB
    subgraph "Connection Layer"
        Accept[ServerSocket<br/>Port 8888]
        Pool[Connection Pool<br/>ExecutorService]
        Handler[ClientHandler<br/>Per Connection]
    end
    
    subgraph "Session Layer"
        Session[SessionManager<br/>ConcurrentHashMap]
        Auth[Backend Validator]
    end
    
    subgraph "Lobby Layer"
        Coord[LobbyCoordinator<br/>Sequential Executor]
        Challenge[ChallengeManager<br/>Pending Challenges]
    end
    
    subgraph "Game Layer"
        Registry[RoomRegistry<br/>ConcurrentHashMap]
        Room[GameRoom<br/>Sequential Event Queue]
        State[GameState<br/>Authoritative]
        Engine[CellCaptureEngine]
        Timer[MatchTimer]
    end
    
    subgraph "Persistence Layer"
        Outbox[ResultOutbox<br/>File-based Storage]
        Submit[BackendSubmitter<br/>HTTP Client]
    end
    
    Accept --> Pool
    Pool --> Handler
    Handler --> Session
    Handler --> Coord
    Handler --> Room
    
    Session --> Auth
    Coord --> Challenge
    Coord --> Registry
    Registry --> Room
    
    Room --> State
    Room --> Engine
    Room --> Timer
    Room --> Outbox
    
    Outbox --> Submit
    
    style Accept fill:#124EA6,color:#fff
    style Coord fill:#F28C28,color:#fff
    style Room fill:#2672D6,color:#fff
Key Components
GameServer - Main entry point:

Java
public class GameServer {
    private final ServerSocket serverSocket;
    private final ExecutorService connectionPool;
    private final SessionManager sessionManager;
    private final LobbyCoordinator lobbyCoordinator;
    private final RoomRegistry roomRegistry;
    private final ResultOutbox resultOutbox;
    private final BackendClient backendClient;
    
    public void start();
    public void shutdown();
    
    private void acceptLoop();
}
ClientHandler - Per-connection handler:

Java
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final SessionManager sessionManager;
    private final LobbyCoordinator lobbyCoordinator;
    private final RoomRegistry roomRegistry;
    
    private BufferedReader reader;
    private PrintWriter writer;
    private final BlockingQueue<Message> outboundQueue;
    private UUID sessionId;
    private long userId;
    
    @Override
    public void run() {
        authenticateWithTimeout();
        startWriter();
        startPingScheduler();
        messageLoop();
    }
    
    public void send(Message message);
    public void disconnect();
}
SessionManager - Authenticated connection tracking:

Java
public class SessionManager {
    private final ConcurrentHashMap<UUID, Session> sessionsById;
    private final ConcurrentHashMap<Long, UUID> sessionsByUserId;
    
    public CompletableFuture<Session> authenticate(String jwt, ClientHandler handler);
    public Session getSession(UUID sessionId);
    public Session getSessionByUserId(long userId);
    public void removeSession(UUID sessionId);
    public List<PlayerDTO> getOnlineList();
    public void broadcastOnlineList();
}

public class Session {
    private final UUID sessionId;
    private final long userId;
    private final String username;
    private final ClientHandler handler;
    private volatile long rankingScore;
    private volatile Instant tokenExpiry;
    private volatile PlayerStatus status;
    private volatile UUID currentRoomId;
}
LobbyCoordinator - Serialized lobby operations:

Java
public class LobbyCoordinator {
    private final ExecutorService serialExecutor;  // Single thread
    private final ChallengeManager challengeManager;
    private final RoomRegistry roomRegistry;
    private final SessionManager sessionManager;
    
    public void handleChallenge(UUID sessionId, long targetUserId);
    public void handleChallengeAccept(UUID sessionId, UUID challengeId);
    public void handleChallengeReject(UUID sessionId, UUID challengeId);
    public void handleChallengeCancel(UUID sessionId, UUID challengeId);
    public void handleDisconnect(UUID sessionId);
    
    // Atomically create room and assign both players
    private void acceptChallengeAndCreateRoom(Challenge challenge);
}
ChallengeManager - Challenge state tracking:

Java
public class ChallengeManager {
    private final ConcurrentHashMap<UUID, Challenge> challengesById;
    private final ConcurrentHashMap<Long, UUID> challengesByUser;
    private final ScheduledExecutorService expiryScheduler;
    
    public UUID createChallenge(long challengerId, long targetId);
    public Optional<Challenge> getChallenge(UUID challengeId);
    public void expireChallenge(UUID challengeId);
    public void removeChallenge(UUID challengeId);
    public boolean hasActiveChallenge(long userId);
}

public class Challenge {
    private final UUID challengeId;
    private final long challengerId;
    private final long targetId;
    private final Instant createdAt;
    private final Instant expiresAt;
    private volatile ChallengeStatus status;
}
RoomRegistry - Active room tracking:

Java
public class RoomRegistry {
    private final ConcurrentHashMap<UUID, GameRoom> rooms;
    private final ExecutorService roomEventPool;
    
    public GameRoom createRoom(UUID roomId, Session player1, Session player2);
    public GameRoom getRoom(UUID roomId);
    public void removeRoom(UUID roomId);
    public int getActiveRoomCount();
}
GameRoom - Authoritative match logic:

Java
public class GameRoom {
    private final UUID roomId;
    private final Executor eventExecutor;  // Serial event queue
    private final ResultOutbox resultOutbox;
    
    private volatile RoomStatus status;
    private UUID currentMatchId;
    private GameState gameState;
    private final Map<Long, PlayerSlot> playerSlots;  // userId -> slot
    private final Set<Integer> readyPlayers;
    private final Set<Long> rematchAcceptances;
    
    // Event handlers - all run on eventExecutor
    public void onReady(long userId, UUID matchId);
    public void onMove(long userId, UUID matchId, int seq, Direction direction);
    public void onTimerTick(UUID matchId);
    public void onMatchTimeout(UUID matchId);
    public void onRematch(long userId, boolean accept);
    public void onExit(long userId);
    public void onDisconnect(long userId);
    
    private void startMatch();
    private void finalizeMatch(MatchEndReason reason, Long forfeitedUserId);
    private void persistResult(MatchResult result);
    private void enterRematchWait();
    private void closeRoom(RoomCloseReason reason);
}
GameState - Authoritative match state:

Java
public class GameState {
    private final UUID matchId;
    private final int[][] grid;  // [y][x]
    private final Map<Integer, PlayerState> players;  // slot -> state
    private long stateVersion;
    private Instant startedAt;
    private Instant deadlineAt;
    
    public void initialize();
    public SnapshotDTO createSnapshot(long remainingMs);
    public boolean isWithinBounds(int x, int y);
    public long getStateVersion();
}

public class PlayerState {
    private final int slot;
    private final long userId;
    private final String username;
    private final String color;
    private int x;
    private int y;
    private int cellsOwned;
    private long lastMoveTime;  // Monotonic for cooldown
    private int lastSeq;
}
CellCaptureEngine - Cell ownership logic:

Java
public class CellCaptureEngine {
    public MoveResult applyMove(GameState state, int slot, Direction direction) {
        PlayerState player = state.getPlayers().get(slot);
        
        // Calculate new position
        int newX = player.getX() + direction.getDeltaX();
        int newY = player.getY() + direction.getDeltaY();
        
        // Boundary check
        if (!state.isWithinBounds(newX, newY)) {
            return MoveResult.outOfBounds(player.getX(), player.getY());
        }
        
        // Update position
        player.setX(newX);
        player.setY(newY);
        
        // Apply capture logic
        int[][] grid = state.getGrid();
        int currentOwner = grid[newY][newX];
        
        if (currentOwner == 0) {
            // Capture unowned cell
            grid[newY][newX] = slot;
            player.incrementCells();
        } else if (currentOwner != slot) {
            // Steal opponent cell
            grid[newY][newX] = slot;
            player.incrementCells();
            state.getPlayers().get(currentOwner).decrementCells();
        }
        // Same owner: no change
        
        state.incrementVersion();
        return MoveResult.success(newX, newY, state.getStateVersion());
    }
}
MatchTimer - Time enforcement:

Java
public class MatchTimer {
    private final ScheduledExecutorService scheduler;
    
    public void startMatch(UUID matchId, GameRoom room, Instant deadline) {
        // Schedule 1-second TIME_SYNC broadcasts
        ScheduledFuture<?> syncTask = scheduler.scheduleAtFixedRate(
            () -> room.onTimerTick(matchId),
            0, 1, TimeUnit.SECONDS
        );
        
        // Schedule timeout at deadline
        long delay = Duration.between(Instant.now(), deadline).toMillis();
        scheduler.schedule(
            () -> room.onMatchTimeout(matchId),
            delay, TimeUnit.MILLISECONDS
        );
    }
}
ResultOutbox - Durable result storage:

Java
public class ResultOutbox {
    private final Path outboxDir;
    private final ConcurrentHashMap<UUID, PendingResult> pending;
    
    public void save(MatchResult result);
    public void remove(UUID matchId);
    public List<PendingResult> loadPendingOnStartup();
    
    // File format: {matchId}.json containing result payload
}

public class PendingResult {
    private final UUID matchId;
    private final MatchResult result;
    private final Instant createdAt;
    private int attemptCount;
    private Instant nextRetryAt;
}
BackendSubmitter - Result submission with retry:

Java
public class BackendSubmitter {
    private final HttpClient httpClient;
    private final String backendUrl;
    private final String serviceKey;
    private final ScheduledExecutorService retryScheduler;
    
    public void submit(PendingResult pending, Consumer<SubmitResult> callback) {
        httpClient.sendAsync(createRequest(pending))
            .thenApply(this::handleResponse)
            .thenAccept(callback)
            .exceptionally(ex -> {
                scheduleRetry(pending, callback);
                return null;
            });
    }
    
    private void scheduleRetry(PendingResult pending, Consumer<SubmitResult> callback) {
        // Exponential backoff: 1, 2, 4, 8, 16, 30, 30, 30...
        long delaySeconds = calculateBackoff(pending.getAttemptCount());
        retryScheduler.schedule(
            () -> submit(pending, callback),
            delaySeconds, TimeUnit.SECONDS
        );
    }
}
game-backend Module
Architecture
Đoạn mã
graph TB
    subgraph "REST Layer"
        Auth[AuthController<br/>/api/auth]
        User[UserController<br/>/api/users]
        Match[MatchController<br/>/api/matches]
        Internal[InternalController<br/>/api/internal]
    end
    
    subgraph "Security"
        Jwt[JWT Filter]
        Service[ServiceKey Filter]
    end
    
    subgraph "Service Layer"
        AuthSvc[AuthService]
        UserSvc[UserService]
        MatchSvc[MatchService]
    end
    
    subgraph "Repository Layer"
        UserRepo[UserRepository]
        StatsRepo[UserStatsRepository]
        MatchRepo[MatchRepository]
        HistRepo[MatchHistoryRepository]
    end
    
    subgraph "Database"
        DB[(MySQL)]
    end
    
    Auth --> Jwt
    User --> Jwt
    Match --> Service
    Internal --> Service
    
    Jwt --> AuthSvc
    Jwt --> UserSvc
    Service --> MatchSvc
    
    AuthSvc --> UserRepo
    AuthSvc --> StatsRepo
    UserSvc --> UserRepo
    UserSvc --> StatsRepo
    MatchSvc --> MatchRepo
    MatchSvc --> HistRepo
    MatchSvc --> UserRepo
    MatchSvc --> StatsRepo
    
    UserRepo --> DB
    StatsRepo --> DB
    MatchRepo --> DB
    HistRepo --> DB
    
    style Auth fill:#124EA6,color:#fff
    style Service fill:#F28C28,color:#fff
    style DB fill:#2672D6,color:#fff
Key Components
AuthController - Authentication endpoints:

Java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request);
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request);
}
UserController - User data endpoints:

Java
@RestController
@RequestMapping("/api/users")
public class UserController {
    @GetMapping("/profile")
    public ResponseEntity<ProfileResponse> getProfile(@AuthenticationPrincipal JwtUser user);
    
    @GetMapping("/leaderboard")
    public ResponseEntity<PagedResponse<LeaderboardEntry>> getLeaderboard(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );
}
InternalController - Service-to-service endpoints:

Java
@RestController
@RequestMapping("/api/internal")
public class InternalController {
    @PostMapping("/sessions/validate")
    @ServiceKeyRequired
    public ResponseEntity<SessionValidationResponse> validateSession(
        @RequestBody SessionValidationRequest request
    );
}
MatchController - Match persistence and history:

Java
@RestController
@RequestMapping("/api/matches")
public class MatchController {
    @PostMapping("/save")
    @ServiceKeyRequired
    public ResponseEntity<SaveMatchResponse> saveMatch(@Valid @RequestBody SaveMatchRequest request);
    
    @GetMapping("/history")
    public ResponseEntity<PagedResponse<MatchHistoryEntry>> getHistory(
        @AuthenticationPrincipal JwtUser user,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    );
}
AuthService - Authentication logic:

Java
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final UserStatsRepository statsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    
    @Transactional
    public RegisterResponse register(String username, String password, String fullName) {
        // Normalize username
        // Validate constraints
        // Hash password with BCrypt
        // Create user and stats in single transaction
    }
    
    public LoginResponse login(String username, String password) {
        // Find user
        // Verify password
        // Generate JWT
        // Return token and profile
    }
    
    public SessionValidationResponse validateToken(String jwt) {
        // Parse and verify JWT
        // Extract user ID
        // Load current stats
        // Return session data
    }
}
MatchService - Match persistence with idempotency:

Java
@Service
public class MatchService {
    private final MatchRepository matchRepository;
    private final MatchHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final UserStatsRepository statsRepository;
    
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SaveMatchResponse saveMatch(SaveMatchRequest request) {
        // Check idempotency
        String payloadHash = computeHash(request);
        Optional<Match> existing = matchRepository.findById(request.getMatchId());
        
        if (existing.isPresent()) {
            if (existing.get().getPayloadHash().equals(payloadHash)) {
                // Duplicate submission - return existing
                return SaveMatchResponse.alreadySaved(existing.get());
            } else {
                // Conflicting payload
                throw new MatchResultConflictException(request.getMatchId());
            }
        }
        
        // Validate players, slots, cells, timestamps
        
        // Derive outcomes and rank deltas
        Long winnerUserId = determineWinner(request);
        Map<Long, MatchOutcome> outcomes = computeOutcomes(request, winnerUserId);
        Map<Long, Integer> rankDeltas = computeRankDeltas(outcomes);
        
        // Insert match record
        Match match = new Match(request, winnerUserId, payloadHash);
        matchRepository.save(match);
        
        // Insert history records for both players
        for (PlayerEntry entry : request.getPlayers()) {
            MatchHistory history = new MatchHistory(
                match.getMatchId(),
                entry.getUserId(),
                getOpponentId(request.getPlayers(), entry.getUserId()),
                entry.getSlot(),
                entry.getCellsOwned(),
                outcomes.get(entry.getUserId()),
                rankDeltas.get(entry.getUserId())
            );
            historyRepository.save(history);
        }
        
        // Update user stats with row locking
        for (PlayerEntry entry : request.getPlayers()) {
            UserStats stats = statsRepository.findByIdForUpdate(entry.getUserId());
            stats.incrementMatchesPlayed();
            stats.applyOutcome(outcomes.get(entry.getUserId()));
            
            User user = userRepository.findById(entry.getUserId()).orElseThrow();
            user.addScore(rankDeltas.get(entry.getUserId()));
        }
        
        // Return confirmation with updated standings
        List<StandingsDTO> standings = loadStandings(request.getPlayers());
        return SaveMatchResponse.newlySaved(match, standings);
    }
}
Data Models
Database Schema
Đoạn mã
erDiagram
    USERS ||--|| USER_STATS : has
    USERS ||--o{ MATCHES : "wins (nullable)"
    USERS ||--o{ MATCHES : "forfeits (nullable)"
    USERS ||--o{ MATCH_HISTORY : participates
    MATCHES ||--|{ MATCH_HISTORY : contains
    
    USERS {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR password_hash
        VARCHAR full_name
        BIGINT total_score
        DATETIME created_at
    }
    
    USER_STATS {
        BIGINT user_id PK,FK
        INT matches_played
        INT matches_won
        INT matches_lost
        INT matches_drawn
    }
    
    MATCHES {
        CHAR match_id PK
        CHAR room_id
        DATETIME started_at
        DATETIME ended_at
        VARCHAR end_reason
        BIGINT winner_user_id FK
        BIGINT forfeited_user_id FK
        CHAR payload_hash
    }
    
    MATCH_HISTORY {
        BIGINT id PK
        CHAR match_id FK
        BIGINT user_id FK
        BIGINT opponent_id FK
        TINYINT player_slot
        SMALLINT cells_owned
        VARCHAR outcome
        INT rank_delta
    }
Entity Classes
User entity:

Java
@Entity
@Table(name = "USERS")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 30)
    private String username;
    
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    
    @Column(name = "total_score", nullable = false)
    private Long totalScore = 0L;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
UserStats entity:

Java
@Entity
@Table(name = "USER_STATS")
public class UserStats {
    @Id
    @Column(name = "user_id")
    private Long userId;
    
    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "matches_played", nullable = false)
    private Integer matchesPlayed = 0;
    
    @Column(name = "matches_won", nullable = false)
    private Integer matchesWon = 0;
    
    @Column(name = "matches_lost", nullable = false)
    private Integer matchesLost = 0;
    
    @Column(name = "matches_drawn", nullable = false)
    private Integer matchesDrawn = 0;
    
    public void applyOutcome(MatchOutcome outcome) {
        matchesPlayed++;
        switch (outcome) {
            case WIN -> matchesWon++;
            case LOSS -> matchesLost++;
            case DRAW -> matchesDrawn++;
        }
    }
    
    public double calculateWinRate() {
        if (matchesPlayed == 0) return 0.0;
        return Math.round(matchesWon * 10000.0 / matchesPlayed) / 100.0;
    }
}
Match entity:

Java
@Entity
@Table(name = "MATCHES")
public class Match {
    @Id
    @Column(name = "match_id", length = 36)
    private String matchId;
    
    @Column(name = "room_id", nullable = false, length = 36)
    private String roomId;
    
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    
    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;
    
    @Column(name = "end_reason", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private MatchEndReason endReason;
    
    @Column(name = "winner_user_id")
    private Long winnerUserId;
    
    @Column(name = "forfeited_user_id")
    private Long forfeitedUserId;
    
    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;
}
MatchHistory entity:

Java
@Entity
@Table(
    name = "MATCH_HISTORY",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"match_id", "user_id"}),
        @UniqueConstraint(columnNames = {"match_id", "player_slot"})
    }
)
public class MatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "match_id", nullable = false, length = 36)
    private String matchId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "opponent_id", nullable = false)
    private Long opponentId;
    
    @Column(name = "player_slot", nullable = false)
    private Integer playerSlot;
    
    @Column(name = "cells_owned", nullable = false)
    private Integer cellsOwned;
    
    @Column(nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    private MatchOutcome outcome;
    
    @Column(name = "rank_delta", nullable = false)
    private Integer rankDelta;
}
Database Constraints and Indexes
SQL schema creation:

SQL
CREATE TABLE USERS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(30) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    total_score BIGINT NOT NULL DEFAULT 0 CHECK (total_score >= 0),
    created_at DATETIME(3) NOT NULL,
    INDEX idx_users_score (total_score DESC, id ASC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE USER_STATS (
    user_id BIGINT PRIMARY KEY,
    matches_played INT NOT NULL DEFAULT 0 CHECK (matches_played >= 0),
    matches_won INT NOT NULL DEFAULT 0 CHECK (matches_won >= 0),
    matches_lost INT NOT NULL DEFAULT 0 CHECK (matches_lost >= 0),
    matches_drawn INT NOT NULL DEFAULT 0 CHECK (matches_drawn >= 0),
    CONSTRAINT fk_stats_user FOREIGN KEY (user_id) REFERENCES USERS(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_stats_sum CHECK (
        matches_played = matches_won + matches_lost + matches_drawn
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE MATCHES (
    match_id CHAR(36) PRIMARY KEY,
    room_id CHAR(36) NOT NULL,
    started_at DATETIME(3) NOT NULL,
    ended_at DATETIME(3) NOT NULL,
    end_reason VARCHAR(16) NOT NULL CHECK (end_reason IN ('TIMEOUT', 'FORFEIT')),
    winner_user_id BIGINT,
    forfeited_user_id BIGINT,
    payload_hash CHAR(64) NOT NULL,
    CONSTRAINT fk_match_winner FOREIGN KEY (winner_user_id) REFERENCES USERS(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_match_forfeiter FOREIGN KEY (forfeited_user_id) REFERENCES USERS(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    INDEX idx_match_ended (ended_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE MATCH_HISTORY (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    opponent_id BIGINT NOT NULL,
    player_slot TINYINT NOT NULL CHECK (player_slot IN (1, 2)),
    cells_owned SMALLINT NOT NULL CHECK (cells_owned >= 0 AND cells_owned <= 400),
    outcome VARCHAR(8) NOT NULL CHECK (outcome IN ('WIN', 'LOSS', 'DRAW')),
    rank_delta INT NOT NULL,
    CONSTRAINT fk_history_match FOREIGN KEY (match_id) REFERENCES MATCHES(match_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES USERS(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_history_opponent FOREIGN KEY (opponent_id) REFERENCES USERS(id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    UNIQUE KEY uk_history_match_user (match_id, user_id),
    UNIQUE KEY uk_history_match_slot (match_id, player_slot),
    INDEX idx_history_user_time (user_id, match_id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
TCP Protocol and JSON Message Formats
Message Structure
All TCP messages follow this envelope format:

JSON
{
  "actionType": "MOVE",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-01-15T10:30:45.123Z",
  "payload": { /* action-specific */ }
}
Messages are sent as one JSON object per line, terminated by \n (LF). The receiver accumulates bytes until LF is found, then parses the complete JSON.

Authentication Flow
LOGIN (Client → Server):

JSON
{
  "actionType": "LOGIN",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "accessToken": "eyJhbGc..."
  }
}
LOGIN_RESULT (Server → Client):

JSON
{
  "actionType": "LOGIN_RESULT",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "success": true,
    "player": {
      "userId": 123,
      "username": "player1",
      "totalScore": 150,
      "status": "IDLE",
      "canChallenge": true
    },
    "expiresAt": "2024-01-15T12:30:45.123Z"
  }
}
Lobby Messages
ONLINE_LIST (Server → All Clients):

JSON
{
  "actionType": "ONLINE_LIST",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "players": [
      {
        "userId": 123,
        "username": "player1",
        "totalScore": 150,
        "status": "IDLE",
        "canChallenge": true
      },
      {
        "userId": 456,
        "username": "player2",
        "totalScore": 200,
        "status": "PLAYING",
        "canChallenge": false
      }
    ]
  }
}
Challenge Flow
CHALLENGE (Client → Server):

JSON
{
  "actionType": "CHALLENGE",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "targetUserId": 456
  }
}
CHALLENGE_RECEIVED (Server → Target):

JSON
{
  "actionType": "CHALLENGE_RECEIVED",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "challengeId": "abc-123",
    "challenger": {
      "userId": 123,
      "username": "player1",
      "totalScore": 150,
      "status": "IDLE",
      "canChallenge": false
    },
    "expiresAt": "2024-01-15T10:31:00.000Z"
  }
}
CHALLENGE_ACCEPT (Client → Server):

JSON
{
  "actionType": "CHALLENGE_ACCEPT",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "challengeId": "abc-123"
  }
}
CHALLENGE_RESULT (Server → Both):

JSON
{
  "actionType": "CHALLENGE_RESULT",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "challengeId": "abc-123",
    "status": "ACCEPTED",
    "reason": null
  }
}
Match Initialization
MATCH_START (Server → Both Players):

JSON
{
  "actionType": "MATCH_START",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "snapshot": {
      "roomId": "room-uuid",
      "matchId": "match-uuid",
      "stateVersion": 0,
      "grid": [[0, 0], []], 
      "players": [
        {
          "userId": 123,
          "username": "player1",
          "slot": 1,
          "color": "#2672D6",
          "position": {"x": 0, "y": 0},
          "cellsOwned": 1
        },
        {
          "userId": 456,
          "username": "player2",
          "slot": 2,
          "color": "#F28C28",
          "position": {"x": 19, "y": 19},
          "cellsOwned": 1
        }
      ],
      "remainingMs": 60000
    },
    "readyDeadlineAt": "2024-01-15T10:30:55.000Z"
  }
}
READY (Client → Server):

JSON
{
  "actionType": "READY",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "matchId": "match-uuid"
  }
}
MATCH_BEGIN (Server → Both):

JSON
{
  "actionType": "MATCH_BEGIN",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "matchId": "match-uuid",
    "startedAt": "2024-01-15T10:30:50.000Z",
    "deadlineAt": "2024-01-15T10:31:50.000Z",
    "remainingMs": 60000
  }
}
Gameplay Messages
MOVE (Client → Server):

JSON
{
  "actionType": "MOVE",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "matchId": "match-uuid",
    "seq": 42,
    "direction": "UP"
  }
}
MAP_UPDATE (Server → Both):

JSON
{
  "actionType": "MAP_UPDATE",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "snapshot": {
      "roomId": "room-uuid",
      "matchId": "match-uuid",
      "stateVersion": 127,
      "grid": [[1, 2, 0], []],
      "players": [
        {
          "userId": 123,
          "username": "player1",
          "slot": 1,
          "color": "#2672D6",
          "position": {"x": 5, "y": 8},
          "cellsOwned": 85
        },
        {
          "userId": 456,
          "username": "player2",
          "slot": 2,
          "color": "#F28C28",
          "position": {"x": 12, "y": 14},
          "cellsOwned": 92
        }
      ],
      "remainingMs": 43210
    },
    "actorUserId": 123,
    "acceptedSeq": 42
  }
}
MOVE_REJECTED (Server → Client):

JSON
{
  "actionType": "MOVE_REJECTED",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "matchId": "match-uuid",
    "seq": 42,
    "code": "OUT_OF_BOUNDS",
    "position": {"x": 5, "y": 8},
    "stateVersion": 127
  }
}
TIME_SYNC (Server → Both):

JSON
{
  "actionType": "TIME_SYNC",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "matchId": "match-uuid",
    "remainingMs": 43000,
    "deadlineAt": "2024-01-15T10:31:50.000Z"
  }
}
Match Completion
MATCH_RESULT (Server → Both):

JSON
{
  "actionType": "MATCH_RESULT",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "roomId": "room-uuid",
    "matchId": "match-uuid",
    "winnerUserId": 456,
    "reason": "TIMEOUT",
    "players": [
      {
        "userId": 123,
        "slot": 1,
        "cellsOwned": 185,
        "outcome": "LOSS",
        "rankDelta": 0
      },
      {
        "userId": 456,
        "slot": 2,
        "cellsOwned": 215,
        "outcome": "WIN",
        "rankDelta": 10
      }
    ],
    "persistence": "PENDING",
    "rematchDeadlineAt": "2024-01-15T10:32:05.000Z"
  }
}
RESULT_SAVED (Server → Both):

JSON
{
  "actionType": "RESULT_SAVED",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "matchId": "match-uuid",
    "standings": [
      {
        "userId": 123,
        "totalScore": 150,
        "matchesPlayed": 10,
        "matchesWon": 4,
        "matchesLost": 5,
        "matchesDrawn": 1,
        "winRate": 40.0
      },
      {
        "userId": 456,
        "totalScore": 210,
        "matchesPlayed": 8,
        "matchesWon": 6,
        "matchesLost": 2,
        "matchesDrawn": 0,
        "winRate": 75.0
      }
    ]
  }
}
Rematch Flow
REMATCH (Client → Server):

JSON
{
  "actionType": "REMATCH",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "roomId": "room-uuid",
    "previousMatchId": "match-uuid-1",
    "accept": true
  }
}
REMATCH_STATUS (Server → Both):

JSON
{
  "actionType": "REMATCH_STATUS",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "roomId": "room-uuid",
    "previousMatchId": "match-uuid-1",
    "acceptedUserIds": [123],
    "expiresAt": "2024-01-15T10:32:05.000Z"
  }
}
ROOM_CLOSED (Server → Remaining):

JSON
{
  "actionType": "ROOM_CLOSED",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "roomId": "room-uuid",
    "reason": "DECLINED"
  }
}
Connection Management
PING (Server → Client):

JSON
{
  "actionType": "PING",
  "requestId": null,
  "timestamp": "...",
  "payload": {
    "nonce": "ping-uuid"
  }
}
PONG (Client → Server):

JSON
{
  "actionType": "PONG",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "nonce": "ping-uuid"
  }
}
ERROR (Server → Client):

JSON
{
  "actionType": "ERROR",
  "requestId": "...",
  "timestamp": "...",
  "payload": {
    "code": "INVALID_TOKEN",
    "message": "JWT token has expired",
    "details": {}
  }
}
Real-Time Game Flow and State Management
Challenge and Room Creation Flow
Đoạn mã
sequenceDiagram
    participant C1 as Client 1
    participant S as Server Lobby
    participant C2 as Client 2
    participant R as Game Room
    
    C1->>S: CHALLENGE(targetUserId: C2)
    S->>S: Validate both IDLE, no pending
    S->>S: Create Challenge
    S->>C1: CHALLENGE_RESULT(PENDING)
    S->>C2: CHALLENGE_RECEIVED
    S->>C1: ONLINE_LIST (both unavailable)
    S->>C2: ONLINE_LIST
    
    C2->>S: CHALLENGE_ACCEPT
    S->>S: Revalidate, create room atomically
    S->>C1: CHALLENGE_RESULT(ACCEPTED)
    S->>C2: CHALLENGE_RESULT(ACCEPTED)
    S->>R: Assign players, initialize grid
    R->>C1: MATCH_START
    R->>C2: MATCH_START
Match Initialization Flow
Đoạn mã
sequenceDiagram
    participant C1 as Client 1
    participant R as Game Room
    participant C2 as Client 2
    
    R->>C1: MATCH_START (snapshot, 10s deadline)
    R->>C2: MATCH_START
    
    C1->>C1: Load board UI
    C1->>R: READY(matchId)
    
    C2->>C2: Load board UI
    C2->>R: READY(matchId)
    
    R->>R: Both ready, start 60s timer
    R->>C1: MATCH_BEGIN (deadline)
    R->>C2: MATCH_BEGIN
    
    loop Every second
        R->>C1: TIME_SYNC (remainingMs)
        R->>C2: TIME_SYNC
    end
Gameplay Loop
Đoạn mã
sequenceDiagram
    participant C1 as Client 1
    participant R as Game Room
    participant C2 as Client 2
    
    loop During match
        C1->>C1: User presses W
        C1->>R: MOVE(seq: 1, direction: UP)
        R->>R: Validate cooldown, bounds
        R->>R: Apply move, update grid
        R->>R: Increment state version
        R->>C1: MAP_UPDATE (new snapshot)
        R->>C2: MAP_UPDATE
        
        C2->>R: MOVE(seq: 1, direction: LEFT)
        R->>R: Process movement
        R->>C1: MAP_UPDATE
        R->>C2: MAP_UPDATE
    end
    
    R->>R: Deadline reached
    R->>R: Finalize result
    R->>C1: MATCH_RESULT
    R->>C2: MATCH_RESULT
Match Completion and Persistence
Đoạn mã
sequenceDiagram
    participant R as Game Room
    participant O as Result Outbox
    participant B as Backend API
    participant C1 as Client 1
    participant C2 as Client 2
    
    R->>R: Finalize match
    R->>O: Save result to disk
    O-->>R: Saved
    R->>C1: MATCH_RESULT (PENDING)
    R->>C2: MATCH_RESULT (PENDING)
    
    R->>B: POST /api/matches/save
    B->>B: Validate, insert match + history
    B->>B: Update user stats (with locking)
    B-->>R: 201 Created (standings)
    
    R->>O: Remove from outbox
    R->>C1: RESULT_SAVED (standings)
    R->>C2: RESULT_SAVED
    R->>R: Enter WAITING_REMATCH
State Transitions
Player Status Transitions:

Đoạn mã
stateDiagram-v2
    [*] --> IDLE: Login
    IDLE --> IN_ROOM: Challenge Accepted
    IN_ROOM --> PLAYING: Both Ready
    PLAYING --> IN_ROOM: Match Ends (rematch available)
    IN_ROOM --> IDLE: Room Closed
    PLAYING --> IDLE: Match Ends (no rematch)
    IDLE --> [*]: Logout
Room Status Transitions:

Đoạn mã
stateDiagram-v2
    [*] --> WAITING_READY: Room Created
    WAITING_READY --> PLAYING: Both Ready
    WAITING_READY --> CLOSED: Ready Timeout / Leave
    PLAYING --> FINISHING: Match Ends
    FINISHING --> WAITING_REMATCH: Result Persisted
    FINISHING --> CLOSED: Persistence Failed
    WAITING_REMATCH --> WAITING_READY: Both Accept
    WAITING_REMATCH --> CLOSED: Declined / Timeout
    CLOSED --> [*]
Concurrency Model
Lobby Operations (Sequential):

Session registration

Challenge creation

Challenge acceptance → room assignment

Disconnect handling

All operations run through a single-threaded executor to ensure atomic state transitions.

Room Events (Sequential per room):

READY messages

MOVE messages

Timer ticks

Match timeout

Rematch votes

Exit/disconnect

Each room has a serial event queue backed by a shared worker pool.

Connection I/O (Per connection):

Each ClientHandler has dedicated reader thread

Outbound messages queued and written by dedicated writer task

No concurrent writes to same socket

Backend Submission (Async):

HTTP requests run outside room executor

Callbacks enqueue events back to room

Retry scheduler uses separate thread pool

JavaFX UI Component Structure
Screen Hierarchy
Plaintext
GameApplication
├── LoginScene
│   └── LoginController
│       ├── Username TextField
│       ├── Password PasswordField
│       ├── Login Button
│       └── Register Link
├── RegisterScene
│   └── RegisterController
│       ├── Username TextField
│       ├── Full Name TextField
│       ├── Password PasswordField
│       ├── Confirm Password PasswordField
│       └── Register Button
└── MainScene (after login)
    ├── Sidebar
    │   ├── App Logo / Branding
    │   ├── Lobby Button
    │   ├── Ranking Button
    │   ├── Match History Button
    │   ├── Settings Button
    │   └── Logout Button
    └── ContentPane
        ├── LobbyView
        │   ├── Online Players Section (Main Area)
        │   │   ├── Online Count Indicator
        │   │   └── Online Players Table
        │   │       ├── Index Column (#)
        │   │       ├── Player Name Column (Avatar + Username)
        │   │       ├── Total Score Column
        │   │       ├── Status Column (Badge: RẢNH / ĐANG CHƠI)
        │   │       └── Action Column (Challenge Button)
        │   └── Right Sidebar (Profile & Info)
        │       ├── Player Profile Card
        │       │   ├── Avatar ImageView
        │       │   ├── Username Label & Online Status Indicator
        │       │   ├── Total Score Label
        │       │   ├── Total Matches Label
        │       │   └── Win Rate Label
        │       ├── Quick Match Button
        │       └── Game Information Card
        │           ├── Game Mode Label (1 vs 1)
        │           ├── Map Size Label (20 x 20)
        │           ├── Time Limit Label (60 seconds)
        │           └── Information Tip Box
        ├── GameView
        │   ├── Game Board Canvas (20×20 grid)
        │   ├── Player 1 Info Panel
        │   │   ├── Name Label
        │   │   └── Territory Score Label
        │   ├── Player 2 Info Panel
        │   ├── Timer Display
        │   └── Match Result Dialog
        │       ├── Winner Announcement
        │       ├── Final Scores
        │       ├── Rank Changes
        │       ├── Rematch Button
        │       └── Return to Lobby Button
        ├── RankingView
        │   ├── Leaderboard Table
        │   │   ├── Rank Column
        │   │   ├── Username Column
        │   │   ├── Score Column
        │   │   └── Win Rate Column
        │   └── Pagination Controls
        ├── HistoryView
        │   ├── Match History Table
        │   │   ├── Date Column
        │   │   ├── Opponent Column
        │   │   ├── My Score Column
        │   │   ├── Opponent Score Column
        │   │   ├── Result Column
        │   │   └── Points Column
        │   └── Pagination Controls
        └── SettingsView
            ├── Backend URL TextField
            ├── Server Host TextField
            ├── Server Port TextField
            └── Save Button
Canvas Rendering
GameBoardRenderer - Renders the 20×20 grid:

Java
public class GameBoardRenderer {
    private final Canvas canvas;
    private final GraphicsContext gc;
    private static final int CELL_SIZE = 30;  // pixels
    private static final int GRID_SIZE = 20;
    
    public void render(SnapshotDTO snapshot) {
        clearCanvas();
        drawGrid(snapshot.getGrid());
        drawPlayers(snapshot.getPlayers());
    }
    
    private void drawGrid(int[][] grid) {
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                int owner = grid[y][x];
                Color color = getCellColor(owner);
                gc.setFill(color);
                gc.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                
                // Draw grid lines
                gc.setStroke(Color.web("#DDDDDD"));
                gc.strokeRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
    }
    
    private void drawPlayers(List<PlayerSnapshotDTO> players) {
        for (PlayerSnapshotDTO player : players) {
            int x = player.getPosition().getX();
            int y = player.getPosition().getY();
            
            // Draw player marker (circle)
            gc.setFill(Color.web(player.getColor()).darker());
            gc.fillOval(
                x * CELL_SIZE + 5,
                y * CELL_SIZE + 5,
                CELL_SIZE - 10,
                CELL_SIZE - 10
            );
        }
    }
    
    private Color getCellColor(int owner) {
        return switch (owner) {
            case 0 -> Color.web("#FFFFFF");  // Unowned
            case 1 -> Color.web("#2672D6");  // Player 1 blue
            case 2 -> Color.web("#F28C28");  // Player 2 orange
            default -> Color.GRAY;
        };
    }
}
Threading Model
All UI updates must occur on the JavaFX Application Thread:

Java
// Network callback on background thread
tcpClient.onMapUpdate(snapshot -> {
    Platform.runLater(() -> {
        gameController.updateBoard(snapshot);
    });
});

// REST call on background thread
CompletableFuture.supplyAsync(() -> {
    return restClient.getLeaderboard(page, size);
}).thenAccept(leaderboard -> {
    Platform.runLater(() -> {
        rankingController.displayLeaderboard(leaderboard);
    });
});
CSS Styling
Theme colors applied through CSS:

CSS
/* Main theme colors */
.root {
    -fx-primary: #124EA6;
    -fx-primary-light: #2672D6;
    -fx-accent: #F28C28;
    -fx-background: #FFFFFF;
    -fx-background-light: #EBF4FC;
}

/* Buttons */
.button-primary {
    -fx-background-color: -fx-primary;
    -fx-text-fill: white;
    -fx-font-size: 14px;
    -fx-padding: 10px 20px;
    -fx-background-radius: 5px;
}

.button-primary:hover {
    -fx-background-color: -fx-primary-light;
}

/* Grid cells */
.game-canvas {
    -fx-border-color: #DDDDDD;
    -fx-border-width: 1px;
}

/* Online player table */
.table-view {
    -fx-background-color: -fx-background;
}

.table-row-cell:selected {
    -fx-background-color: -fx-background-light;
}

/* Status Badges */
.badge-idle {
    -fx-background-color: #EAF2FF;
    -fx-text-fill: #2563EB;
    -fx-font-weight: bold;
    -fx-padding: 4 12;
    -fx-background-radius: 12px;
}

.badge-playing {
    -fx-background-color: #D6E7FF;
    -fx-text-fill: #124EA6;
    -fx-font-weight: bold;
    -fx-padding: 4 10;
    -fx-background-radius: 12px;
}
Security and Authentication Flow
JWT Authentication Flow
Đoạn mã
sequenceDiagram
    participant C as Client
    participant BA as Backend API
    participant DB as Database
    participant GS as Game Server
    
    C->>BA: POST /api/auth/login
    BA->>DB: Query user by username
    DB-->>BA: User record
    BA->>BA: Verify BCrypt password
    BA->>BA: Generate JWT (2hr exp)
    BA-->>C: {accessToken, profile}
    
    C->>GS: TCP connect + LOGIN(token)
    GS->>BA: POST /api/internal/sessions/validate
    BA->>BA: Verify JWT signature
    BA->>BA: Check expiration
    BA->>DB: Load current stats
    BA-->>GS: {userId, username, score, expiresAt}
    GS->>GS: Create session
    GS-->>C: LOGIN_RESULT(success)
JWT Token Structure
Claims:

sub: User ID (long)

username: Username (string)

iat: Issued at (Unix timestamp)

exp: Expiration (Unix timestamp, iat + 2 hours)

Generation:

Java
public String generateToken(User user) {
    Instant now = Instant.now();
    Instant expiry = now.plus(2, ChronoUnit.HOURS);
    
    return Jwts.builder()
        .setSubject(String.valueOf(user.getId()))
        .claim("username", user.getUsername())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiry))
        .signWith(secretKey, SignatureAlgorithm.HS256)
        .compact();
}
Validation:

Java
public Claims validateToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(secretKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
}
Service Key Authentication
The Game_Server authenticates to internal Backend_API endpoints using a shared secret:

Request:

Plaintext
POST /api/internal/sessions/validate
X-Service-Key: <configured-secret>
Content-Type: application/json

{
  "accessToken": "eyJhbGc..."
}
Filter:

Java
@Component
public class ServiceKeyFilter extends OncePerRequestFilter {
    @Value("${security.service-key}")
    private String configuredServiceKey;
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        if (isInternalEndpoint(request)) {
            String providedKey = request.getHeader("X-Service-Key");
            
            if (!configuredServiceKey.equals(providedKey)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid service key");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean isInternalEndpoint(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/internal");
    }
}
Password Hashing
BCrypt with automatic salt generation:

Java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);  // 2^12 rounds
}

// Registration
String hash = passwordEncoder.encode(plainPassword);
user.setPasswordHash(hash);

// Login
boolean matches = passwordEncoder.matches(plainPassword, user.getPasswordHash());
Security Configuration
Java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // JWT-based, no session cookies
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/internal/**").hasRole("SERVICE")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(serviceKeyFilter, JwtAuthenticationFilter.class);
        
        return http.build();
    }
}
Token Expiration Handling
Client-side:

Store token and expiry time from login response

Check expiry before each request

Display session expiry warning when < 5 minutes remain

Server-side:

Game_Server checks session expiry during idle/waiting states

Permits active matches to complete even if token expires

Sends SESSION_EXPIRED and closes connection after match

Java
// In GameRoom during match finalization
if (session.isTokenExpired()) {
    room.setRematchDisabled();
    deliverResultAndClose();
} else {
    enterRematchWait();
}
Concurrency Handling Strategy
Thread Pools and Executors
Connection Acceptor Pool - Handles new TCP connections:

Java
private final ExecutorService connectionPool = 
    Executors.newFixedThreadPool(100);  // Max 100 concurrent connections
Lobby Coordinator - Sequential processing:

Java
private final ExecutorService lobbyExecutor = 
    Executors.newSingleThreadExecutor();
Room Event Pool - Shared worker pool for room event queues:

Java
private final ExecutorService roomEventPool = 
    Executors.newFixedThreadPool(20);  // Supports 50 rooms with burst capacity
Backend Submission Pool - Async HTTP requests:

Java
private final ExecutorService httpPool = 
    Executors.newFixedThreadPool(10);
Retry Scheduler - Delayed retry tasks:

Java
private final ScheduledExecutorService retryScheduler = 
    Executors.newScheduledThreadPool(2);
Timer Scheduler - Match timers and pings:

Java
private final ScheduledExecutorService timerScheduler = 
    Executors.newScheduledThreadPool(4);
Room Event Serialization
Each room has a serial event queue to ensure thread-safe state access:

Java
public class GameRoom {
    private final Executor eventExecutor;
    
    public GameRoom(UUID roomId, ExecutorService pool) {
        this.roomId = roomId;
        
        // Create serial executor backed by shared pool
        BlockingQueue<Runnable> eventQueue = new LinkedBlockingQueue<>(200);
        this.eventExecutor = new ThreadPoolExecutor(
            1, 1,  // Single thread
            0L, TimeUnit.MILLISECONDS,
            eventQueue,
            pool.getThreadFactory()
        );
    }
    
    public void onMove(long userId, UUID matchId, int seq, Direction direction) {
        eventExecutor.execute(() -> {
            handleMoveInternal(userId, matchId, seq, direction);
        });
    }
    
    private void handleMoveInternal(long userId, UUID matchId, int seq, Direction direction) {
        // All state access happens here, single-threaded
        if (!this.currentMatchId.equals(matchId)) {
            sendError(userId, "STALE_MATCH");
            return;
        }
        
        PlayerState player = gameState.getPlayers().get(getSlot(userId));
        
        // Check cooldown
        long now = System.nanoTime();
        if (now - player.getLastMoveTime() < COOLDOWN_NS) {
            sendMoveRejected(userId, seq, "MOVE_TOO_FAST");
            return;
        }
        
        // Apply move
        MoveResult result = cellCaptureEngine.applyMove(gameState, getSlot(userId), direction);
        if (result.isSuccess()) {
            player.setLastMoveTime(now);
            player.setLastSeq(seq);
            broadcastMapUpdate(userId, seq);
        } else {
            sendMoveRejected(userId, seq, result.getReason());
        }
    }
}
Concurrent Data Structures
SessionManager:

Java
private final ConcurrentHashMap<UUID, Session> sessionsById = new ConcurrentHashMap<>();
private final ConcurrentHashMap<Long, UUID> sessionsByUserId = new ConcurrentHashMap<>();
Safe for concurrent reads and single-key updates. Complex operations (like register + broadcast) go through lobby coordinator.

RoomRegistry:

Java
private final ConcurrentHashMap<UUID, GameRoom> rooms = new ConcurrentHashMap<>();
ResultOutbox:

Java
private final ConcurrentHashMap<UUID, PendingResult> pending = new ConcurrentHashMap<>();
Outbound Message Queuing
Each ClientHandler has a bounded outbound queue:

Java
public class ClientHandler {
    private final BlockingQueue<Message> outboundQueue = 
        new LinkedBlockingQueue<>(256);
    
    private final AtomicBoolean writerRunning = new AtomicBoolean(false);
    
    public void send(Message message) {
        if (!outboundQueue.offer(message)) {
            // Queue full - disconnect slow client
            disconnect();
        }
    }
    
    private void startWriter() {
        if (writerRunning.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    while (connected && !Thread.interrupted()) {
                        Message msg = outboundQueue.poll(1, TimeUnit.SECONDS);
                        if (msg != null) {
                            String json = objectMapper.writeValueAsString(msg);
                            writer.write(json);
                            writer.write('\n');
                            writer.flush();
                        }
                    }
                } catch (Exception e) {
                    disconnect();
                } finally {
                    writerRunning.set(false);
                }
            });
        }
    }
}
Rate Limiting
Java
public class RateLimiter {
    private final AtomicInteger firstWindowCount = new AtomicInteger(0);
    private final AtomicInteger secondWindowCount = new AtomicInteger(0);
    private volatile long currentWindowStart = System.currentTimeMillis();
    
    public boolean allowMessage() {
        long now = System.currentTimeMillis();
        long elapsed = now - currentWindowStart;
        
        if (elapsed > 2000) {
            // Rotate windows
            firstWindowCount.set(secondWindowCount.getAndSet(0));
            currentWindowStart = now;
        }
        
        secondWindowCount.incrementAndGet();
        
        // Check if exceeded limit in both consecutive windows
        return firstWindowCount.get() <= 50 && secondWindowCount.get() <= 50;
    }
}
Database Concurrency
Row Locking for Score Updates:

Java
@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserStats s WHERE s.userId = :userId")
    UserStats findByIdForUpdate(@Param("userId") Long userId);
}
Transaction Isolation:

Java
@Transactional(isolation = Isolation.READ_COMMITTED)
public SaveMatchResponse saveMatch(SaveMatchRequest request) {
    // Acquire locks when reading stats
    UserStats stats1 = statsRepository.findByIdForUpdate(player1Id);
    UserStats stats2 = statsRepository.findByIdForUpdate(player2Id);
    
    // Perform updates
    stats1.applyOutcome(outcome1);
    stats2.applyOutcome(outcome2);
    
    // Commit atomically
}
Shutdown Sequence
Java
public void shutdown() {
    // 1. Stop accepting new connections
    serverSocket.close();
    
    // 2. Stop new lobby operations
    lobbyExecutor.shutdown();
    
    // 3. Let active matches complete (30s grace period)
    roomEventPool.shutdown();
    roomEventPool.awaitTermination(30, TimeUnit.SECONDS);
    
    // 4. Disconnect remaining clients
    sessionManager.disconnectAll();
    
    // 5. Final outbox flush attempt
    resultOutbox.flushPending();
    
    // 6. Shutdown schedulers
    timerScheduler.shutdownNow();
    retryScheduler.shutdownNow();
    httpPool.shutdown();
    connectionPool.shutdown();
}