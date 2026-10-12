# Implementation Plan: Territorial Paint Game

## Overview

This implementation plan breaks down the Territorial Paint Game into discrete, testable coding tasks. The system is a multi-module Maven project with four modules: game-common (shared protocol), game-client (JavaFX UI), game-server (TCP game server), and game-backend (Spring Boot REST API with MySQL).

## Tasks

- [x] 1. Set up Maven multi-module project structure
  **Dependencies:** []
  **Requirements:** System Architecture
  
  Create parent pom.xml with Java 21, module definitions, and shared dependency management. Create game-common module with pom.xml and Jackson dependencies. Create game-client module with pom.xml, JavaFX 21, and Jackson dependencies. Create game-server module with pom.xml, Jackson, and Java HTTP Client support. Create game-backend module with pom.xml, Spring Boot 3.x, Spring Data JPA, Spring Security, MySQL connector, and JJWT dependencies. Configure Maven compiler plugin for Java 21 in the parent POM. Configure JJWT API, implementation, and Jackson integration dependencies. Set up .gitignore for Maven, IDE files, and build artifacts.

- [x] 2. Create protocol enumerations
  **Dependencies:** [1]
  **Requirements:** 13.1
  
  Implement ActionType enum with all message types: LOGIN, LOGIN_RESULT, CHALLENGE, CHALLENGE_ACCEPT, CHALLENGE_REJECT, CHALLENGE_CANCEL, MATCH_START, READY, MATCH_BEGIN, MOVE, MOVE_REJECTED, MAP_UPDATE, CELL_UPDATE, TIME_SYNC, MATCH_RESULT, RESULT_SAVED, REMATCH, REMATCH_STATUS, PING, PONG, ERROR. Implement PlayerStatus enum: IDLE, IN_ROOM, PLAYING. Implement RoomStatus enum: WAITING_READY, PLAYING, FINISHING, WAITING_REMATCH, CLOSED. Implement Direction enum: UP, DOWN, LEFT, RIGHT. Add movement delta values to Direction. Implement MatchEndReason enum: TIMEOUT, FORFEIT. Implement MatchOutcome enum: WIN, LOSS, DRAW. Implement PersistenceStatus enum: PENDING, SAVED, FAILED.

- [x] 3. Create GameConstants class
  **Dependencies:** [1]
  **Requirements:** 1.10, 4.4, 5.4, 6.6, 7.1, 8.10
  
  Define grid dimensions as 20x20. Define match duration as 60 seconds. Define move cooldown as 100ms. Define challenge timeout as 15 seconds. Define server port as 8888. Define heartbeat interval and timeout values. Define connection login timeout. Define connection rate limit as 50 messages/second. Define ranking points: WIN = 10, DRAW = 3, LOSS = 0. Define player colors: Blue = #2672D6, Orange = #F28C28.

- [x] 4. Create core DTO classes
  **Dependencies:** [2, 3]
  **Requirements:** 13.1, 13.2
  
  Implement Message envelope with actionType, requestId, timestamp, payload. Implement PlayerDTO with userId, username, totalScore, status, canChallenge. Implement PositionDTO with x, y. Implement PlayerSnapshotDTO with userId, username, slot, color, position, cellsOwned. Implement SnapshotDTO with roomId, matchId, stateVersion, grid, players, remainingMs. Implement PlayerResultDTO with userId, slot, cellsOwned, outcome, rankDelta. Implement StandingsDTO with userId, totalScore, matchesPlayed, matchesWon, matchesLost, matchesDrawn, winRate. Use Jackson annotations where required for JSON serialization and deserialization. Distinguish client sequenceNumber from server stateVersion.

- [x] 5. Create payload classes for all message types
  **Dependencies:** [2, 3]
  **Requirements:** 13.1, 13.2
  
  Implement LoginPayload, LoginResultPayload, OnlineListPayload, ChallengePayload, ChallengeReceivedPayload, ChallengeResultPayload, MatchStartPayload, ReadyPayload, MatchBeginPayload, MovePayload, MoveRejectedPayload, MapUpdatePayload, CellUpdatePayload, TimeSyncPayload, MatchResultPayload, ResultSavedPayload, RematchPayload, RematchStatusPayload, ErrorPayload. LoginPayload must contain the JWT obtained from the Backend. Do not send user passwords to the Game Server.

- [x] 6. Checkpoint - Verify game-common
  **Dependencies:** [4, 5]
  
  Compile the game-common module successfully. Verify all protocol enums and DTOs compile. Verify JSON serialization and deserialization for all protocol messages. Verify required fields and payload structures.

- [x] 7. Create database schema
  **Dependencies:** [6]
  **Requirements:** 12.1, 12.2, 12.3, 12.5, 12.6, 12.7, 12.8, 12.12
  
  Create USERS table with id, username, password_hash, full_name, total_score, created_at. Create USER_STATS table with user_id, matches_played, matches_won, matches_lost, matches_drawn. Create MATCHES table with match_id, room_id, started_at, ended_at, end_reason, winner_user_id, forfeited_user_id, payload_hash. Create MATCH_HISTORY table with id, match_id, user_id, opponent_id, player_slot, cells_owned, outcome, rank_delta. Add UNIQUE constraint on USERS.username. Add indexes for username lookup, leaderboard ordering by total_score DESC and id ASC, match history lookup by user and match end time. Add unique constraints for (match_id, user_id) and (match_id, player_slot). Add foreign keys between users, user statistics, matches, and match history. Allow winner_user_id to be NULL for draws. Allow forfeited_user_id to be NULL for non-forfeit matches. Use InnoDB engine and utf8mb4 charset. Use a consistent UUID/String representation for match_id and room_id.

- [x] 8. Create JPA entity classes
  **Dependencies:** [7]
  **Requirements:** 12.2, 12.3, 12.5, 12.6, 12.7, 12.9
  
  Implement User entity with id, username, passwordHash, fullName, totalScore, createdAt. Implement UserStats entity with userId, matchesPlayed, matchesWon, matchesLost, matchesDrawn. Implement Match entity with matchId, roomId, startedAt, endedAt, endReason, winnerUserId, forfeitedUserId, payloadHash. Implement MatchHistory entity with id, matchId, userId, opponentId, playerSlot, cellsOwned, outcome, rankDelta. Add applyOutcome() to UserStats to increment matchesPlayed and the corresponding outcome counter. Add calculateWinRate() to UserStats, returning 0.00 when matchesPlayed is zero. Calculate win rate from stored statistics instead of persisting a separate win_rate column.

- [x] 9. Configure Spring Security and JWT
  **Dependencies:** [8]
  **Requirements:** 1.9, 1.10, 2.3, 9.4
  
  Create JwtTokenProvider with generateToken(), validateToken(), getUserIdFromToken(). Configure JJWT using HS256 signing. Configure JWT expiration to two hours. Use the user ID as the JWT subject. Create JwtAuthenticationFilter extending OncePerRequestFilter. Validate JWT from the Authorization: Bearer header. Create ServiceKeyFilter to authenticate Game Server requests using X-Service-Key. Configure SecurityFilterChain for public and protected endpoints. Configure BCryptPasswordEncoder.

- [x] 10. Create Spring Data JPA repositories
  **Dependencies:** [8, 9]
  **Requirements:** 10.6, 10.7, 11.2, 11.3, 11.4, 12.12
  
  Create UserRepository extending JpaRepository. Add findByUsername(). Add a paginated leaderboard query ordered by totalScore DESC, id ASC. Create UserStatsRepository extending JpaRepository. Add a custom locking query for deterministic row locking. Create MatchRepository extending JpaRepository. Create MatchHistoryRepository extending JpaRepository. Add a history query joining MATCHES and MATCH_HISTORY, ordered by MATCHES.ended_at DESC.

- [x] 11. Implement AuthController and AuthService
  **Dependencies:** [10]
  **Requirements:** 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8
  
  Create RegisterRequest, LoginRequest, RegisterResponse, LoginResponse. Return token, expiration information, and profile information after successful login. Implement AuthService.register() to normalize username, check username uniqueness, hash password with BCrypt, create User, create UserStats, execute creation in a transaction. Implement AuthService.login() to find user, verify password, generate JWT, return authenticated profile. Create POST /api/auth/register and POST /api/auth/login.

- [x] 12. Implement UserController
  **Dependencies:** [10]
  **Requirements:** 10.1, 10.2, 10.3, 10.4, 10.6, 10.7
  
  Implement UserService.getProfile(). Implement UserService.getLeaderboard(). Create GET /api/users/profile and GET /api/users/leaderboard. Return profile data with statistics and calculated win rate. Support paginated leaderboard results.

- [x] 13. Implement InternalController for session validation
  **Dependencies:** [10]
  **Requirements:** 2.3, 2.4
  
  Create SessionValidationRequest and SessionValidationResponse. Implement AuthService.validateSession(). Create POST /api/internal/sessions/validate. Authenticate requests using X-Service-Key. Validate the JWT received from the Game Server. Return the authenticated user information and token expiration.

- [x] 14. Create match persistence DTOs and validation
  **Dependencies:** [10]
  **Requirements:** 9.5, 9.6, 9.7
  
  Create SaveMatchRequest, PlayerEntry, SaveMatchResponse. Add Bean Validation annotations. Add custom validators where required. Validate exactly two players, unique players, valid player slots, valid cell counts, valid outcomes, valid match identifiers, valid end reason, valid rank delta.

- [x] 15. Implement MatchService saveMatch with idempotency
  **Dependencies:** [14]
  **Requirements:** 8.1, 8.2, 8.3, 8.4, 8.10, 9.7, 9.8, 9.14, 9.15
  
  Normalize the incoming match result. Compute a SHA-256 hash of the normalized payload. Check whether the match has already been persisted. Return the existing result for duplicate submissions. Validate that both players exist. Validate that final cell counts are consistent with the submitted result. Derive WIN, LOSS, or DRAW from final cell counts for timeout results. Apply forfeit rules for FORFEIT results. Compute rank delta: WIN = +10, DRAW = +3, LOSS = +0.

- [x] 16. Implement transactional match persistence
  **Dependencies:** [15]
  **Requirements:** 9.9, 9.10, 9.11, 9.12, 9.13
  
  Start the persistence transaction using READ_COMMITTED. Insert the Match entity. Insert two MatchHistory entities. Lock both UserStats rows in deterministic user ID order. Update matchesPlayed, matchesWon, matchesLost, matchesDrawn. Update User.totalScore using the calculated rank delta. Commit the transaction. Load the updated standings after successful persistence.

- [x] 17. Implement MatchController and match history endpoint
  **Dependencies:** [16]
  **Requirements:** 9.4, 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 11.7, 11.8, 11.9
  
  Create MatchController. Create POST /api/matches/save. Authenticate Game Server requests using X-Service-Key. Create GET /api/matches/history. Authenticate history requests using JWT. Return match history ordered by match end time descending. Support pagination.

- [x] 18. Create GameServer and connection acceptor
  **Dependencies:** [16]
  **Requirements:** 2.1
  
  Implement GameServer. Create ServerSocket on port 8888. Create the connection executor. Implement acceptLoop(). Implement graceful shutdown hook.

- [x] 19. Implement ClientHandler
  **Dependencies:** [18]
  **Requirements:** 2.1, 2.2, 13.4, 13.5, 13.6
  
  Create ClientHandler with Socket, BufferedReader, PrintWriter, outbound BlockingQueue. Implement line-delimited JSON communication. Parse incoming JSON into Message. Dispatch messages according to ActionType. Handle EOF and socket exceptions through a common disconnect cleanup flow. Implement per-connection rate limiting of 50 messages per second.

- [x] 20. Implement SessionManager
  **Dependencies:** [18]
  **Requirements:** 2.3, 2.4, 2.5, 2.8, 2.9, 2.10, 2.11, 3.1, 3.2, 3.3, 3.4, 3.10
  
  Create Session with sessionId, userId, username, rankingScore, tokenExpiry. Implement authenticate(), getSession(), getOnlineList(), broadcastOnlineList(). Enforce one active session per user. Remove disconnected users from online tracking. Maintain player status transitions.

- [x] 21. Implement LOGIN handling
  **Dependencies:** [19, 20]
  **Requirements:** 2.2, 2.3, 2.5, 2.6, 2.7, 2.12, 2.13
  
  Require LOGIN within five seconds after connection. Receive the JWT from LoginPayload. Validate the JWT with the Backend internal session endpoint. Create an authenticated server-side session. Send LOGIN_RESULT. Broadcast updated ONLINE_LIST. Reject invalid, expired, or missing tokens.

- [x] 22. Implement heartbeat
  **Dependencies:** [19, 20]
  **Requirements:** 13.7
  
  Schedule PING every five seconds. Track the latest PONG. Disconnect clients that fail to respond within fifteen seconds. Clean up the corresponding session.

- [x] 23. Create LobbyCoordinator
  **Dependencies:** [21, 22]
  **Requirements:** 4.1, 4.2, 4.11, 4.12, 4.13, 4.14, 4.15
  
  Create a single-threaded executor for serialized lobby operations. Implement challenge operations. Validate player status before creating or accepting challenges. Ensure lobby state transitions are processed sequentially.

- [x] 24. Create ChallengeManager
  **Dependencies:** [21, 22]
  **Requirements:** 4.3, 4.4, 4.5, 4.6, 4.11, 4.14
  
  Create Challenge with challengeId, challengerId, targetId, createdAt, expiresAt, status. Implement creation, lookup, acceptance, rejection, cancellation, expiration. Expire challenges after fifteen seconds.

- [x] 25. Implement challenge message handlers
  **Dependencies:** [23, 24]
  **Requirements:** 4.1, 4.2, 4.5, 4.6, 4.7, 4.8, 4.9, 4.10, 4.15, 4.16
  
  Implement CHALLENGE, CHALLENGE_ACCEPT, CHALLENGE_REJECT, CHALLENGE_CANCEL. Prevent cross-challenge race conditions through serialized lobby processing. Reject challenges when either player is not IDLE. Transition both players to IN_ROOM when a challenge is accepted. Create a room only after successful challenge acceptance.

- [x] 26. Create RoomRegistry
  **Dependencies:** [23, 24]
  **Requirements:** 5.1, 5.2
  
  Create RoomRegistry using ConcurrentHashMap. Track active GameRoom instances by room ID. Implement createRoom(), getRoom(), removeRoom(). Use unique room IDs.

- [x] 27. Create GameRoom
  **Dependencies:** [25, 26]
  **Requirements:** 5.1, 5.2, 5.3
  
  Create GameRoom with a dedicated serial executor. Initialize the room in WAITING_READY. Route all room state mutations through the serial executor. Prevent concurrent modification of authoritative match state. Shutdown the room executor when the room reaches CLOSED.

- [x] 28. Implement match initialization
  **Dependencies:** [27]
  **Requirements:** 5.4, 5.5, 5.6, 5.7, 5.8, 5.9
  
  Generate a unique matchId. Create a unique roomId. Create GameState with a 20x20 grid. Initialize all cells as unowned. Assign player slots. Assign starting positions. Initialize stateVersion to zero. Send MATCH_START to both players.

- [x] 29. Create GameState
  **Dependencies:** [28]
  **Requirements:** 5.4, 5.5, 5.6, 5.7, 6.15
  
  Track 20x20 grid, player positions, player cell counts, stateVersion, match deadline, room status. Keep the Game Server authoritative over gameplay state.

- [x] 30. Implement READY and MATCH_BEGIN flow
  **Dependencies:** [28, 29]
  **Requirements:** 5.10, 5.11, 5.12, 5.13, 5.14, 5.15
  
  Process READY through the room executor. Require both players to be ready. Transition the room to PLAYING. Set the authoritative match deadline. Start the match timer from the authoritative deadline. Send MATCH_BEGIN with the deadline.

- [x] 31. Implement CellCaptureEngine
  **Dependencies:** [29]
  **Requirements:** 6.8, 6.9, 6.10, 6.11, 6.12, 6.13, 6.14
  
  Calculate the new position from the requested direction. Reject positions outside the 20x20 grid. Update ownership: unowned cell to current player's color, current player's cell unchanged, opponent's cell to current player's color. Update both players' cell ownership counters when ownership changes. Return a MoveResult.

- [x] 32. Implement MOVE processing
  **Dependencies:** [30, 31]
  **Requirements:** 6.5, 6.6, 6.7, 6.8, 6.9, 6.15, 6.16, 6.17, 6.18
  
  Submit MOVE processing to the GameRoom serial executor. Validate authenticated session, player is in the room, room is PLAYING, valid direction, valid client sequence number, valid position, 100ms move cooldown. Resolve simultaneous moves according to serialized server processing order. Increment stateVersion after each accepted state change. Send CELL_UPDATE for changed cells. Send updated player position information. Send MOVE_REJECTED for invalid moves. Maintain first-come, first-served ownership resolution.

- [x] 33. Implement MatchTimer
  **Dependencies:** [32]
  **Requirements:** 7.1, 7.2, 7.3, 7.8, 7.9, 7.10
  
  Schedule TIME_SYNC broadcasts every second. Calculate remaining time from the authoritative match deadline. Schedule timeout using the authoritative deadline. Ensure timeout processing is executed through the GameRoom serial executor.

- [x] 34. Implement match completion
  **Dependencies:** [32]
  **Requirements:** 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.16
  
  Implement onMatchTimeout(matchId). Count final cells owned by each player. Determine WIN, LOSS, DRAW. Create the authoritative match result. Finalize the room state exactly once. Pass the result to the persistence subsystem.

- [x] 35. Implement disconnect and forfeit handling
  **Dependencies:** [32]
  **Requirements:** 8.6, 8.7, 8.8, 8.9, 8.11
  
  Detect player disconnection during PLAYING. Finalize the match according to forfeit rules. Mark the disconnected player as forfeited. Determine the remaining player's outcome. Create the final match result. Pass the result to the persistence subsystem. Ensure disconnect and timeout cannot finalize the same match twice.

- [x] 36. Create ResultOutbox
  **Dependencies:** [33, 34, 35]
  **Requirements:** 9.1, 9.2, 9.21, 9.22
  
  Serialize pending match results to local JSON files. Write files atomically using temporary files followed by rename. Mark unsaved results as PENDING. Keep result data available until successful Backend persistence.

- [x] 37. Implement BackendSubmitter
  **Dependencies:** [37]
  **Requirements:** 9.3, 9.17, 9.18, 9.19
  
  POST results to /api/matches/save. Authenticate using X-Service-Key. Handle successful persistence, duplicate result, validation failure, server error. Retry transient failures using exponential backoff.

- [x] 38. Implement result persistence coordination
  **Dependencies:** [37]
  **Requirements:** 9.1, 9.2, 9.12, 9.20, 9.21, 9.22, 9.23
  
  Create the final authoritative result. Write it to the outbox. Submit the result to the Backend asynchronously. Send MATCH_RESULT to both players without waiting for Backend persistence. Send RESULT_SAVED after successful persistence. Mark the outbox entry as SAVED after successful persistence. Keep failed entries for later retry.

- [x] 39. Implement rematch flow
  **Dependencies:** [38]
  **Requirements:** 8.12, 8.14, 8.15
  
  Process REMATCH requests through the GameRoom serial executor. Track each player's rematch decision. Transition the room to WAITING_READY when both players accept. Create a new match while preserving the room. Reset match-specific state. Send MATCH_START. Require both players to send READY again. Close the room when rematch is declined or the rematch timeout expires.

- [x] 40. Create TcpClient
  **Dependencies:** [21, 22]
  **Requirements:** 2.1, 2.2, 13.3, 13.5
  
  Connect to the Game Server on the configured host and port. Implement login(jwt). Send the JWT obtained from the Backend. Implement send(Message). Create a reader thread for incoming messages. Implement clean connection shutdown.

- [x] 41. Create MessageHandler and dispatcher
  **Dependencies:** [21, 22]
  **Requirements:** 13.3, 13.9
  
  Define handlers for all server message types. Parse JSON using game-common DTOs. Support both MAP_UPDATE and CELL_UPDATE. Validate stateVersion before applying server updates. Route UI updates to the JavaFX Application Thread.

- [x] 42. Create RestClient
  **Dependencies:** [21, 22]
  **Requirements:** 1.7, 1.11, 10.1, 10.6, 11.1
  
  Implement REST communication for registration, authentication, profile, leaderboard, match history. Store and attach JWT to protected REST requests. Handle HTTP errors and authentication expiration.

- [x] 43. Implement client heartbeat
  **Dependencies:** [40, 41, 42]
  **Requirements:** 13.7
  
  Respond to PING with PONG. Track connection health. Detect server-side disconnect. Trigger client cleanup when the TCP connection closes.

- [x] 44. Create GameApplication and SceneManager
  **Dependencies:** [43]
  **Requirements:** System Architecture
  
  Initialize JavaFX scenes for Login, Lobby, Game Board, Result. Implement scene transitions. Preserve authenticated user session while navigating between scenes.

- [x] 45. Create LoginController
  **Dependencies:** [44]
  **Requirements:** 1.1, 1.2, 1.3, 1.7, 1.11
  
  Implement login action. Implement registration action. Validate username and password input. Display authentication and validation errors. Store JWT after successful authentication. Connect the authenticated client to the Game Server using the JWT.

- [x] 46. Create LobbyController
  **Dependencies:** [44]
  **Requirements:** 3.1, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9
  
  Display the received online player list. Display IDLE, IN_ROOM, PLAYING. Implement challenge button action. Disable challenge actions when the target player cannot be challenged. Update the lobby when ONLINE_LIST changes.

- [x] 47. Implement challenge invitation dialog
  **Dependencies:** [45, 46]
  **Requirements:** 4.6, 4.7, 4.8, 4.11
  
  Display incoming challenge information. Show a 15-second countdown. Implement Accept and Reject actions. Send CHALLENGE_ACCEPT or CHALLENGE_REJECT. Automatically expire the invitation when the countdown ends.

- [x] 48. Create GameController
  **Dependencies:** [47]
  **Requirements:** 5.9, 5.10, 6.18, 7.4
  
  Render the 20x20 game grid. Render player positions. Render owned cells. Display current cell counts. Display match status. Display remaining time. Display final result when the match ends.

- [x] 49. Implement movement input
  **Dependencies:** [47]
  **Requirements:** 6.1, 6.2, 6.3, 6.4, 6.5
  
  Register W/A/S/D keyboard input. Convert keyboard input into MOVE messages. Apply client-side 100ms input throttling. Attach an incrementing sequenceNumber to MOVE messages. Do not rely on client-side validation as the authoritative game rule.

- [x] 50. Implement server state synchronization
  **Dependencies:** [48, 49]
  **Requirements:** 6.15, 6.18, 7.3, 7.4, 7.5, 7.6
  
  Handle MAP_UPDATE. Handle CELL_UPDATE. Ignore stale stateVersion updates. Apply valid authoritative updates. Request or perform full synchronization when required. Re-render the Canvas from authoritative server state. Update timer display using TIME_SYNC.

- [x] 51. Create RankingController
  **Dependencies:** [50]
  **Requirements:** 10.6, 10.7, 10.8, 10.9, 10.10
  
  Fetch paginated leaderboard data. Display username, total score, matches played, wins, losses, draws, win rate. Implement pagination.

- [x] 52. Create HistoryController
  **Dependencies:** [50]
  **Requirements:** 11.1, 11.2, 11.5, 11.6, 11.7, 11.8, 11.9
  
  Fetch match history for the authenticated user. Display opponent, player slot, cells owned, outcome, rank delta, match time. Implement pagination.

- [x] 53. Implement result and rematch UI
  **Dependencies:** [50, 51, 52]
  **Requirements:** 8.12, 8.13, 9.12, 9.18, 9.20, 9.23
  
  Display WIN, LOSS, DRAW. Display final cell counts. Display rank delta. Display persistence status. Provide Rematch and Exit actions. Handle RESULT_SAVED.

- [x] 54. Implement profile refresh
  **Dependencies:** [53]
  **Requirements:** 9.23, 10.5
  
  Refresh profile data after RESULT_SAVED. Update displayed total score and statistics. Refresh leaderboard when the ranking view is opened. Refresh match history when the history view is opened.

- [x] 55. Configure application properties
  **Dependencies:** [53]
  **Requirements:** System Configuration
  
  Configure Backend URL. Configure Game Server host and port. Configure JWT settings. Configure Game Server service key. Configure database connection. Configure environment-specific values without hardcoding secrets into source code.

- [x] 56. Verify database initialization
  **Dependencies:** [53]
  **Requirements:** 12.1
  
  Create and configure schema.sql or the selected migration mechanism. Verify USERS, USER_STATS, MATCHES, MATCH_HISTORY. Verify all foreign keys. Verify all indexes. Verify all unique constraints. Verify that the schema matches the JPA mappings.

- [x] 57. Wire GameRoom subsystems
  **Dependencies:** [54, 55, 56]
  **Requirements:** Full Room Lifecycle
  
  Connect GameState, CellCaptureEngine, MatchTimer, ResultOutbox, BackendSubmitter. Ensure timeout and disconnect callbacks finalize the room exactly once. Ensure closed rooms are removed from RoomRegistry. Ensure all room mutations execute through the room executor.

- [x] 58. Wire LobbyCoordinator
  **Dependencies:** [54, 55, 56]
  **Requirements:** Full Challenge Flow
  
  Connect challenge creation, acceptance, rejection, cancellation, expiration, room creation. Ensure both players transition from IDLE to IN_ROOM only after successful challenge acceptance.

- [x] 59. Wire GameController
  **Dependencies:** [54, 55, 56]
  **Requirements:** Full Gameplay Flow
  
  Connect keyboard input to MOVE messages. Connect MAP_UPDATE and CELL_UPDATE to Canvas rendering. Connect TIME_SYNC to timer display. Connect MATCH_RESULT to the result screen. Connect RESULT_SAVED to persistence status. Connect REMATCH to the rematch flow.

- [x] 60. Configure graceful shutdown
  **Dependencies:** [57, 58, 59]
  **Requirements:** System Shutdown
  
  Stop accepting new TCP connections. Stop lobby processing. Notify or terminate active rooms according to shutdown policy. Stop room and timer executors. Flush pending result outbox operations. Close network resources. Close application resources.

- [x] 61. Checkpoint - Verify backend
  **Dependencies:** [17, 39]
  
  Run unit tests for authentication, JWT, repositories, services, controllers, match validation. Verify database transactions. Verify duplicate match persistence is idempotent. Verify JWT authentication. Verify service-key authentication. Verify leaderboard ordering. Verify match history ordering.

- [x] 62. Checkpoint - Verify game-server
  **Dependencies:** [39]
  
  Run unit tests for networking, sessions, authentication, lobby, challenges, rooms, movement, timers, result processing, outbox persistence. Verify concurrent room operations do not corrupt authoritative state. Verify simultaneous MOVE processing. Verify disconnect and timeout cannot finalize the same match twice. Verify Backend retry behavior.

- [x] 63. Final checkpoint - System integration validation
  **Dependencies:** [60, 61, 62]
  
  Verify complete registration and login flow. Verify JWT authentication. Verify Game Server session validation. Verify online player synchronization. Verify challenge creation, acceptance, rejection, cancellation, expiration. Verify room creation. Verify READY flow. Verify MATCH_BEGIN. Verify 60-second gameplay. Verify authoritative movement processing. Verify cell capture. Verify stateVersion synchronization. Verify timeout results, draw results, forfeit results. Verify match persistence. Verify idempotent result submission. Verify Backend retry behavior. Verify profile updates, leaderboard updates, match history updates. Verify rematch flow. Verify room closure. Run full integration tests. Run regression tests. Verify graceful shutdown and recovery behavior.

## Task Dependency Graph

```json
{
  "waves": [
    {
      "wave": 1,
      "tasks": [1]
    },
    {
      "wave": 2,
      "tasks": [2, 3]
    },
    {
      "wave": 3,
      "tasks": [4, 5]
    },
    {
      "wave": 4,
      "tasks": [6]
    },
    {
      "wave": 5,
      "tasks": [7]
    },
    {
      "wave": 6,
      "tasks": [8]
    },
    {
      "wave": 7,
      "tasks": [9]
    },
    {
      "wave": 8,
      "tasks": [10]
    },
    {
      "wave": 9,
      "tasks": [11, 12, 13, 14]
    },
    {
      "wave": 10,
      "tasks": [15]
    },
    {
      "wave": 11,
      "tasks": [16]
    },
    {
      "wave": 12,
      "tasks": [17, 18]
    },
    {
      "wave": 13,
      "tasks": [19, 20]
    },
    {
      "wave": 14,
      "tasks": [21, 22, 40, 41, 42]
    },
    {
      "wave": 15,
      "tasks": [23, 24, 43]
    },
    {
      "wave": 16,
      "tasks": [25, 26, 44]
    },
    {
      "wave": 17,
      "tasks": [27, 45, 46]
    },
    {
      "wave": 18,
      "tasks": [28, 47]
    },
    {
      "wave": 19,
      "tasks": [29, 48, 49]
    },
    {
      "wave": 20,
      "tasks": [30, 31, 50]
    },
    {
      "wave": 21,
      "tasks": [32, 51, 52]
    },
    {
      "wave": 22,
      "tasks": [33, 34, 35, 53]
    },
    {
      "wave": 23,
      "tasks": [36, 54, 55, 56]
    },
    {
      "wave": 24,
      "tasks": [37]
    },
    {
      "wave": 25,
      "tasks": [38]
    },
    {
      "wave": 26,
      "tasks": [39]
    },
    {
      "wave": 27,
      "tasks": [57, 58, 59]
    },
    {
      "wave": 28,
      "tasks": [60, 61, 62]
    },
    {
      "wave": 29,
      "tasks": [63]
    }
  ]
}
```

## Notes

This implementation uses a flat task structure for the orchestration system. The Game Server uses serial executors for lobby and room operations to ensure thread-safe state management. Match persistence uses an outbox pattern with idempotent backend submission for reliability.
