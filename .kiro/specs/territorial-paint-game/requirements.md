Requirements Document
Introduction
The Territorial Paint Game is a real-time online PvP game where two players compete to capture the most territory on a grid by moving and painting cells. The system consists of a JavaFX desktop client, a TCP-based game server for real-time gameplay, a Spring Boot REST backend for authentication and persistence, and a MySQL database for data storage.
The Game_Server is authoritative for player movement, cell ownership, match timing, and match results. The Backend_API is the only component permitted to access the Database. The Game_Client communicates with the Backend_API through REST and with the Game_Server through TCP.
This document defines the requirements shared by the client, game server, and backend teams, including game rules, communication contracts, state transitions, persistence, and error handling.
Glossary
- Game_Client: The JavaFX desktop application that provides the user interface for registration, login, lobby, gameplay, profiles, rankings, and match history.
- Game_Server: The TCP-based server that manages player connections, challenges, game rooms, and authoritative match logic.
- Backend_API: The Spring Boot REST API that handles authentication, user profiles, rankings, match history, and match result persistence.
- Database: The MySQL database that stores user accounts, statistics, and match history.
- Session_Manager: The component in Game_Server that tracks authenticated player connections and their current status.
- Challenge_Manager: The component in Game_Server that manages pending challenges, responses, cancellation, and expiration.
- Game_Room: A room containing two players that may host an initial match and subsequent rematches.
- Game_State: The authoritative grid, player positions, scores, and lifecycle information for a match.
- Cell_Capture_Engine: The component that applies valid movements, updates cell ownership, and maintains territory scores.
- Match_Timer: The component that enforces the 60-second match duration.
- Player: A registered user who can participate in matches.
- Player_Status: A player's availability state: IDLE, IN_ROOM, or PLAYING.
- Room_Status: A room's lifecycle state: WAITING_READY, PLAYING, FINISHING, WAITING_REMATCH, or CLOSED.
- Grid: The 20×20 playing field where territory is captured.
- Territory: Cells currently owned by a player.
- Territory_Score: The number of cells currently owned by a player within a match.
- Ranking_Score: The accumulated points awarded for completed matches.
- Challenge: A time-limited invitation from one player to another.
- Match: One game session between two players, identified by a unique match ID and lasting up to 60 seconds after gameplay begins.
- Rematch: A new match played by the same two players in the same room.
- JWT: JSON Web Token used to authenticate player requests.
- Service_Key: A server-side credential used to authenticate Game_Server requests to internal Backend_API endpoints.
- Result_Outbox: Persistent local storage used by Game_Server to retain match results until Backend_API confirms that they have been saved.
Requirements
Requirement 1: User Authentication
User Story: As a player, I want to register and log in to the game, so that I can access the game lobby and compete with other players.
Acceptance Criteria
1. WHEN a user submits valid registration data containing username, password, and full name, THE Backend_API SHALL create a user account with a BCrypt-hashed password.
2. THE Backend_API SHALL trim and normalize usernames to lowercase and require usernames to contain 3–30 characters consisting only of letters a–z, digits, and underscores.
3. THE Backend_API SHALL require a trimmed full name of 1–100 characters and a password of at least 8 characters and no more than 72 UTF-8 bytes.
4. THE Backend_API SHALL preserve password characters exactly without trimming or changing their case.
5. WHEN registration succeeds, THE Backend_API SHALL create the USERS record and its corresponding USER_STATS record within the same database transaction.
6. WHEN a username already exists, THE Backend_API SHALL return HTTP 409 with a USERNAME_ALREADY_EXISTS error.
7. WHEN a user submits valid login credentials, THE Backend_API SHALL verify the password using BCrypt and return a JWT token, its expiration time, and the player's profile.
8. WHEN login credentials are invalid, THE Backend_API SHALL return HTTP 401 with a generic authentication error that does not distinguish between an unknown username and an incorrect password.
9. THE Backend_API SHALL generate JWT tokens using JJWT with user ID in the sub claim, username, issuance time, and expiration time.
10. THE JWT token lifetime SHALL be 2 hours.
11. WHEN the Game_Client receives a JWT token, THE Game_Client SHALL keep it in memory and include it in subsequent authenticated REST requests and the TCP login message.
12. WHEN a player logs out, THE Game_Client SHALL clear the locally held JWT token and close its TCP session.
13. THE initial version SHALL not implement JWT refresh tokens or a server-side token revocation list; issued JWTs SHALL remain valid until their expiration time.
Requirement 2: Game Server Connection
User Story: As a player, I want to connect to the game server after login, so that I can see other online players and participate in matches.
Acceptance Criteria
1. WHEN a player successfully authenticates, THE Game_Client SHALL establish a TCP connection to the configured Game_Server host on port 8888 by default.
2. WITHIN 5 seconds of establishing a TCP connection, THE Game_Client SHALL send a LOGIN message containing the JWT token.
3. WHEN receiving a LOGIN message, THE Game_Server SHALL validate the token through the authenticated Backend_API session validation endpoint.
4. WHEN token validation succeeds, THE Backend_API SHALL return the player's user ID, username, current ranking score, and token expiration time.
5. WHEN authentication succeeds and no session already exists for the user, THE Game_Server SHALL register the player in the Session_Manager with IDLE status and send LOGIN_RESULT.
6. WHEN authentication fails, THE Game_Server SHALL send an ERROR message and close the connection.
7. IF the client does not submit LOGIN within the authentication timeout, THE Game_Server SHALL close the unauthenticated connection.
8. THE Session_Manager SHALL track session ID, user ID, username, ranking score, token expiration time, ClientHandler reference, Player_Status, and current room ID for each authenticated connection.
9. THE Game_Server SHALL permit only one active TCP session per user ID.
10. WHEN a second connection attempts to authenticate as an already connected user, THE Game_Server SHALL reject the new connection with ALREADY_CONNECTED without terminating the existing session.
11. THE Game_Server SHALL derive player identity from the authenticated session rather than trusting a user ID supplied in gameplay messages.
12. WHEN a player disconnects, THE Game_Server SHALL remove the corresponding session, cancel pending challenges, and notify the player's room if one exists.
13. WHEN removing a session, THE Game_Server SHALL verify that the session ID still matches the connection being cleaned up.
Requirement 3: Online Player List
User Story: As a player, I want to see a list of all online players, so that I can choose opponents to challenge.
Acceptance Criteria
1. WHEN a player successfully connects to the Game_Server, THE Game_Server SHALL broadcast an updated ONLINE_LIST to authenticated players.
2. WHEN a player disconnects, THE Game_Server SHALL broadcast an updated ONLINE_LIST to the remaining authenticated players.
3. WHEN a player's status, challenge availability, or confirmed ranking score changes, THE Game_Server SHALL broadcast an updated ONLINE_LIST.
4. THE ONLINE_LIST message SHALL include userId, username, totalScore, status, and canChallenge for each online player.
5. THE Player_Status SHALL use IDLE for players outside a room, IN_ROOM for players preparing a match or waiting for a rematch, and PLAYING for players in an active match.
6. THE Game_Client SHALL display “Available” for IDLE players, “In Room” for IN_ROOM players, and “Playing” for PLAYING players.
7. THE Game_Server SHALL set canChallenge to true only when the player is IDLE and has no pending incoming or outgoing challenge.
8. THE Game_Client SHALL disable the Challenge button for the current player, players with canChallenge=false, and all targets while the current player is unavailable.
9. WHEN receiving ONLINE_LIST, THE Game_Client SHALL update the lobby without interrupting the TCP connection or blocking the JavaFX application thread.
10. THE Game_Server SHALL obtain initial ranking scores from Backend_API during session validation and update them after successful match persistence.
Requirement 4: Player Challenge System
User Story: As a player, I want to challenge other available players to a match, so that I can compete for territory and increase my score.
Acceptance Criteria
1. WHEN a player selects an available opponent, THE Game_Client SHALL send CHALLENGE containing targetUserId.
2. WHEN receiving CHALLENGE, THE Game_Server SHALL verify that the challenger and target are different users, both are online and IDLE, and neither has a pending challenge.
3. WHEN a challenge is valid, THE Challenge_Manager SHALL create a unique challengeId with challenger ID, target ID, creation time, expiration time, and PENDING status.
4. THE challenge expiration time SHALL be 15 seconds after creation.
5. THE Game_Server SHALL send CHALLENGE_RESULT with PENDING status to the challenger and CHALLENGE_RECEIVED to the target.
6. THE CHALLENGE_RECEIVED message SHALL include challengeId, challenger information, and expiresAt.
7. WHEN receiving CHALLENGE_RECEIVED, THE Game_Client SHALL display an invitation dialog with Accept, Reject, and remaining response time.
8. WHEN the target accepts or rejects an invitation, THE Game_Client SHALL send CHALLENGE_ACCEPT or CHALLENGE_REJECT containing challengeId.
9. THE Game_Server SHALL accept an invitation response only from the target player associated with that challenge.
10. WHEN processing CHALLENGE_ACCEPT, THE Game_Server SHALL revalidate the challenge expiration, both players' availability, and their active sessions before creating a room.
11. WHEN an invitation is rejected, cancelled, or expired, THE Game_Server SHALL notify both connected participants using CHALLENGE_RESULT and release their challenge reservations.
12. WHEN the challenger sends CHALLENGE_CANCEL, THE Game_Server SHALL cancel the pending challenge if it belongs to that challenger.
13. WHEN either participant disconnects, THE Game_Server SHALL cancel their pending challenge and notify the other participant.
14. THE Challenge_Manager SHALL allow each player to participate in at most one pending incoming or outgoing challenge.
15. WHEN crossed challenges or simultaneous challenges target the same player, THE Game_Server SHALL process them sequentially and accept only the first valid challenge.
16. WHEN receiving a repeated acceptance or an acceptance for a completed or expired challenge, THE Game_Server SHALL reject the request without creating an additional room.
Requirement 5: Match Initialization
User Story: As a player, I want matches to start when my challenge is accepted and both clients are ready, so that neither player loses gameplay time while loading the board.
Acceptance Criteria
1. WHEN a valid challenge is accepted, THE Game_Server SHALL create exactly one Game_Room with a unique roomId and an initial match with a unique matchId.
2. THE Game_Server SHALL assign player slot 1 to the challenger and player slot 2 to the invited player.
3. WHEN creating the room, THE Game_Server SHALL set Room_Status to WAITING_READY and both players' statuses to IN_ROOM.
4. THE Game_Server SHALL initialize a 20×20 Grid using grid[y][x], where x increases to the right and y increases downward from the top-left origin (0,0).
5. THE Grid SHALL use 0 for unowned cells, 1 for cells owned by player slot 1, and 2 for cells owned by player slot 2.
6. THE Game_Server SHALL place player 1 at (0,0) and player 2 at (19,19).
7. THE Game_Server SHALL assign the two starting cells to their respective players and initialize each Territory_Score to 1.
8. THE Game_Server SHALL assign blue #2672D6 to player 1 and orange #F28C28 to player 2.
9. THE Game_Server SHALL send MATCH_START containing the initial snapshot and a readiness deadline 10 seconds after match preparation begins.
10. WHEN the board has been initialized, THE Game_Client SHALL send READY containing matchId.
11. WHEN both players are ready before the deadline, THE Game_Server SHALL change Room_Status and both Player_Status values to PLAYING, establish the 60-second deadline, and send MATCH_BEGIN.
12. THE Game_Server SHALL reject MOVE messages before MATCH_BEGIN.
13. IF either client fails to become ready within 10 seconds, leaves, or disconnects during WAITING_READY, THE Game_Server SHALL close the room and return remaining connected players to IDLE.
14. THE system SHALL not record match statistics or award ranking points for a match cancelled before PLAYING begins.
15. WHEN a repeated READY message is received for the current preparation phase, THE Game_Server SHALL treat it as already acknowledged without restarting the match.
Requirement 6: Player Movement and Cell Capture
User Story: As a player, I want to move my character using W/A/S/D keys and capture cells as I move, so that I can claim territory during the match.
Acceptance Criteria
1. WHEN a player uses W, A, S, or D, THE Game_Client SHALL map the input to UP, LEFT, DOWN, or RIGHT respectively.
2. WHILE a movement key remains held, THE Game_Client SHALL send movement requests at intervals of at least 100 milliseconds.
3. WHEN multiple movement keys are held, THE Game_Client SHALL use the most recently pressed key that remains held.
4. WHEN the application loses focus or leaves the gameplay screen, THE Game_Client SHALL clear held-key state and stop generating movement requests.
5. EACH MOVE message SHALL contain matchId, an increasing seq number, and direction.
6. THE Game_Server SHALL allow each player to complete at most one valid movement every 100 milliseconds, measured using server-side monotonic time.
7. WHEN receiving MOVE, THE Game_Server SHALL verify session identity, room membership, match identity, PLAYING status, sequence number, match deadline, movement cooldown, and grid boundaries.
8. EACH valid movement SHALL change the player's position by exactly one cell horizontally or vertically.
9. WHEN a movement would leave the grid, THE Game_Server SHALL retain the previous position and send MOVE_REJECTED with OUT_OF_BOUNDS.
10. WHEN a player moves into an unowned cell, THE Cell_Capture_Engine SHALL assign ownership to that player and increase their Territory_Score by 1.
11. WHEN a player moves into a cell they already own, THE Cell_Capture_Engine SHALL preserve ownership and leave both scores unchanged.
12. WHEN a player moves into an opponent-owned cell, THE Cell_Capture_Engine SHALL transfer ownership, increase the moving player's score by 1, and decrease the opponent's score by 1.
13. THE Game_Server SHALL permit players to occupy the same cell, pass through each other, and exchange positions.
14. WHEN players compete for the same cell, THE Game_Server SHALL process their movements in room event-queue order; the last valid movement SHALL determine ownership.
15. AFTER each valid movement, THE Game_Server SHALL increase stateVersion and broadcast MAP_UPDATE containing the complete grid, both positions, and both territory scores.
16. WHEN receiving a validly structured MOVE for the current match with a new sequence number, THE Game_Server SHALL record that sequence as seen even if the movement is subsequently rejected for cooldown or boundary reasons.
17. WHEN receiving an already seen sequence number, THE Game_Server SHALL reject it with STALE_SEQ; a message for another match SHALL be rejected with STALE_MATCH without changing the current match's sequence tracking.
18. THE Game_Client SHALL display authoritative positions and ownership received from the Game_Server and SHALL not independently apply gameplay results.
Requirement 7: Match Timer Synchronization
User Story: As a player, I want to see the remaining match time continuously updated, so that I can strategize my moves before time runs out.
Acceptance Criteria
1. WHEN both players become ready, THE Match_Timer SHALL begin a 60-second match duration.
2. THE Game_Server SHALL use a monotonic time source to enforce the duration and SHALL use UTC timestamps for communication and persistence.
3. THE Match_Timer SHALL send TIME_SYNC to both players every second with matchId, remainingMs, and deadlineAt.
4. WHEN receiving TIME_SYNC, THE Game_Client SHALL update its displayed remaining time and MAY interpolate the countdown between messages.
5. THE Game_Client SHALL display zero rather than a negative remaining time.
6. THE Game_Client SHALL not determine the winner or finalize the match when its local display reaches zero.
7. BEFORE processing any gameplay event, THE Game_Server SHALL check whether the authoritative deadline has been reached.
8. WHEN the deadline has been reached, THE Game_Server SHALL finalize the match before applying further movement, even if a scheduled timer task runs late.
9. A MOVE message SHALL be accepted only if its processing time is earlier than the authoritative deadline.
10. WHEN a match finishes or a room closes, THE Game_Server SHALL cancel its scheduled timer tasks or ignore any already queued timer events belonging to that finished match.
Requirement 8: Match Completion and Result Determination
User Story: As a player, I want the winner to be determined automatically when time expires, so that I know the match outcome and my score changes.
Acceptance Criteria
1. WHEN the match deadline is reached, THE Game_Server SHALL stop accepting movement and determine final Territory_Scores from grid ownership.
2. WHEN player 1 owns more cells, THE Game_Server SHALL declare player 1 the winner.
3. WHEN player 2 owns more cells, THE Game_Server SHALL declare player 2 the winner.
4. WHEN both players own the same number of cells, THE Game_Server SHALL declare a draw.
5. WHEN a match ends, THE Game_Server SHALL transition the room from PLAYING to FINISHING before initiating result delivery or persistence.
6. THE Game_Server SHALL finalize each matchId at most once, including when timeout, disconnection, and exit events occur concurrently.
7. WHEN a player disconnects or leaves during PLAYING before the deadline, THE Game_Server SHALL record a FORFEIT with LOSS for that player and WIN for the opponent, regardless of territory scores.
8. WHEN a disconnection or exit is processed at or after the deadline, THE Game_Server SHALL use the timeout result based on territory scores.
9. IF both players disconnect before a result is finalized, THE first disconnection processed by the room SHALL determine the forfeiting player; later events SHALL not replace the finalized result.
10. THE ranking point changes SHALL be +10 for WIN, +3 for DRAW, and +0 for LOSS.
11. THE same ranking point rules SHALL apply to matches ended by forfeiture.
12. THE Game_Server SHALL send MATCH_RESULT to connected participants with room ID, match ID, winner ID or null, final territory scores, per-player outcomes, ranking point changes, end reason, persistence status, and rematch deadline when applicable.
13. WHEN receiving MATCH_RESULT, THE Game_Client SHALL disable movement and display the result without locally incrementing the player's confirmed ranking score.
14. WHEN a normally completed match becomes eligible for rematch, THE Game_Server SHALL set Room_Status to WAITING_REMATCH and both players' statuses to IN_ROOM.
15. WHEN a player has left or disconnected, THE Game_Server SHALL deliver the result to remaining connected participants and close the room instead of waiting for rematch.
16. FOR timeout results, THE recorded end time SHALL equal the start time plus 60 seconds; for forfeits, it SHALL reflect the elapsed monotonic duration at finalization.
Requirement 9: Match Result Persistence
User Story: As a player, I want my match results automatically saved to my history, so that I can track my performance over time.
Acceptance Criteria
1. WHEN a match is finalized, THE Game_Server SHALL create an immutable result record and write it to Result_Outbox before reporting persistence as PENDING.
2. THE Result_Outbox SHALL store pending results on persistent local storage using one record per matchId.
3. THE Game_Server SHALL submit pending results through POST /api/matches/save without blocking the room's gameplay event executor on an HTTP request.
4. THE Backend_API SHALL permit /api/matches/save only for requests authenticated with the Game_Server's Service_Key.
5. THE save request SHALL contain matchId, roomId, startedAt, endedAt, endReason, forfeitedUserId, and exactly two player entries containing userId, slot, and cellsOwned.
6. THE Backend_API SHALL validate that the players are distinct existing users, slots are 1 and 2, cell counts are between 0 and 400, their sum does not exceed 400, and timestamps are consistent.
7. FOR FORFEIT results, THE forfeitedUserId SHALL identify one participant; for TIMEOUT results, it SHALL be null.
8. THE Backend_API SHALL derive outcomes, winner identity, and ranking point changes from the submitted result and the official rules rather than accepting arbitrary score increments.
9. WHEN saving a new result, THE Backend_API SHALL insert one MATCHES record and two MATCH_HISTORY records in a single transaction.
10. THE Backend_API SHALL increase matches_played for both participants and increment exactly one of matches_won, matches_lost, or matches_drawn for each participant.
11. THE Backend_API SHALL update each participant's total_score according to the ranking point rules.
12. THE Backend_API SHALL use row locking or equivalent atomic updates to prevent lost statistics updates when multiple results involving the same user are processed concurrently.
13. WHEN all writes succeed, THE Backend_API SHALL commit the transaction; if any write fails, it SHALL roll back the entire transaction.
14. THE Backend_API SHALL use matchId as the idempotency identifier and store a hash of the normalized result payload.
15. WHEN a previously saved matchId is submitted with the same normalized payload, THE Backend_API SHALL return the existing result without inserting history or changing scores again.
16. WHEN a previously saved matchId is submitted with a different normalized payload, THE Backend_API SHALL return HTTP 409 with MATCH_RESULT_CONFLICT.
17. WHEN a request fails because of a network error, HTTP 429, or HTTP 5xx, THE Game_Server SHALL retain the pending result and retry after 1, 2, 4, 8, 16, and 30 seconds, then every 30 seconds.
18. WHEN a request receives a permanent validation or conflict error, THE Game_Server SHALL retain the result for investigation, stop automatic retries for that record, and notify connected participants using RESULT_SAVE_FAILED.
19. WHEN service authentication fails, THE Game_Server SHALL retain pending results and pause submission until the service configuration is corrected.
20. WHEN the Backend_API confirms successful persistence, THE Game_Server SHALL remove the pending outbox record, update cached ranking information, and send RESULT_SAVED.
21. WHEN the Game_Server restarts, it SHALL retry pending outbox records using their original match IDs.
22. IF writing to Result_Outbox fails, THE Game_Server SHALL report persistence as FAILED, attempt immediate backend submission, and SHALL not claim that the result is durably saved.
23. THE Game_Client SHALL refresh profile, history, and ranking data after RESULT_SAVED and display pending or failed persistence honestly.
Requirement 10: Player Profile and Statistics
User Story: As a player, I want to view my profile and statistics, so that I can see my overall performance and ranking.
Acceptance Criteria
1. WHEN a player requests GET /api/users/profile with a valid JWT, THE Backend_API SHALL obtain the user ID from the token.
2. THE profile response SHALL include userId, username, fullName, totalScore, matchesPlayed, matchesWon, matchesLost, matchesDrawn, and winRate.
3. THE Backend_API SHALL calculate winRate as matchesWon × 100.0 / matchesPlayed, rounded to two decimal places.
4. WHEN matchesPlayed is zero, THE Backend_API SHALL return a win rate of 0.00.
5. WHEN a player views the lobby, THE Game_Client SHALL display username, confirmed ranking score, and win rate.
6. WHEN a player opens the Ranking page, THE Game_Client SHALL request GET /api/users/leaderboard.
7. THE Backend_API SHALL order leaderboard entries by total_score descending and then user ID ascending.
8. THE leaderboard response SHALL include sequential rank, user ID, username, total score, and win rate; equal scores SHALL not produce duplicate rank positions.
9. THE leaderboard endpoint SHALL support zero-based page and size parameters, with defaults of 0 and 20 and a maximum size of 100.
10. WHEN receiving leaderboard data, THE Game_Client SHALL display a ranked table and support page navigation.
Requirement 11: Match History
User Story: As a player, I want to view my past match results, so that I can review my performance against specific opponents.
Acceptance Criteria
1. WHEN a player opens the History page, THE Game_Client SHALL request GET /api/matches/history with a valid JWT.
2. THE Backend_API SHALL derive the requesting player's user ID from the JWT and return only that player's history.
3. THE Backend_API SHALL join MATCH_HISTORY with MATCHES and opponent user data.
4. THE Backend_API SHALL order history by match end time descending and then match ID descending.
5. THE history response SHALL include matchId, playedAt, opponentUserId, opponentUsername, myCells, opponentCells, outcome, rankDelta, and endReason.
6. THE playedAt response field SHALL represent MATCHES.ended_at; the Database SHALL not require a duplicate played_at column.
7. THE Game_Client SHALL display date/time, opponent, both territory scores, outcome, ranking point change, and whether the match ended by timeout or forfeiture.
8. THE history endpoint SHALL support zero-based pagination with default size 20 and maximum size 100.
9. WHEN no history exists, THE Game_Client SHALL display an empty-history state rather than an error.
10. Matches cancelled before entering PLAYING SHALL not appear in match history.
Requirement 12: Database Schema and Data Integrity
User Story: As a system administrator, I want the database to enforce data integrity constraints, so that the system maintains consistent and reliable data.
Acceptance Criteria
1. THE Database SHALL contain USERS, USER_STATS, MATCHES, and MATCH_HISTORY tables using InnoDB and utf8mb4.
2. THE USERS table SHALL contain:
   - id: BIGINT primary key with auto-increment.
   - username: VARCHAR(30), non-null and unique.
   - password_hash: VARCHAR(100), non-null.
   - full_name: VARCHAR(100), non-null.
   - total_score: BIGINT, non-null, default 0.
   - created_at: DATETIME(3), non-null.
3. THE USER_STATS table SHALL contain:
   - user_id: BIGINT primary key and foreign key to USERS.id.
   - matches_played: INT, non-null, default 0.
   - matches_won: INT, non-null, default 0.
   - matches_lost: INT, non-null, default 0.
   - matches_drawn: INT, non-null, default 0.
4. THE Backend_API SHALL calculate win rate from USER_STATS rather than store a redundant win_rate column.
5. THE MATCHES table SHALL contain:
   - match_id: CHAR(36) primary key supplied by Game_Server.
   - room_id: CHAR(36), non-null.
   - started_at and ended_at: DATETIME(3), non-null.
   - end_reason: VARCHAR(16), non-null.
   - winner_user_id: nullable BIGINT foreign key to USERS.id.
   - forfeited_user_id: nullable BIGINT foreign key to USERS.id.
   - payload_hash: CHAR(64), non-null.
6. THE MATCH_HISTORY table SHALL contain:
   - id: BIGINT primary key with auto-increment.
   - match_id: CHAR(36), non-null foreign key to MATCHES.match_id.
   - user_id and opponent_id: non-null BIGINT foreign keys to USERS.id.
   - player_slot: TINYINT, non-null.
   - cells_owned: SMALLINT, non-null.
   - outcome: VARCHAR(8), non-null.
   - rank_delta: INT, non-null.
7. THE MATCH_HISTORY table SHALL enforce unique constraints on (match_id, user_id) and (match_id, player_slot).
8. THE Database SHALL enforce nonnegative scores and counters, player slots of 1 or 2, cell counts of 0–400, and outcomes of WIN, LOSS, or DRAW.
9. THE USER_STATS counters SHALL satisfy matches_played = matches_won + matches_lost + matches_drawn.
10. THE Backend_API SHALL enforce exactly two mutually consistent history records per saved match within one transaction.
11. Foreign keys SHALL use restrictive deletion behavior; the initial version SHALL not provide hard deletion of accounts or recorded matches.
12. THE Database SHALL index leaderboard ordering, player-history lookup, and match end time.
13. THE Backend_API SHALL store timestamps using a UTC convention and configure its database connection timezone accordingly.
14. THE Backend_API SHALL use HikariCP for connection pooling.
15. THE Game_Server and Game_Client SHALL not directly access the Database.
Requirement 13: Protocol Message Handling
User Story: As a system architect, I want all network communication to follow a standardized protocol, so that client and server can reliably exchange game data.
Acceptance Criteria
1. THE game-common module SHALL define ActionType values:
   LOGIN, LOGIN_RESULT, ONLINE_LIST, CHALLENGE, CHALLENGE_RECEIVED, CHALLENGE_ACCEPT, CHALLENGE_REJECT, CHALLENGE_CANCEL, CHALLENGE_RESULT, MATCH_START, READY, MATCH_BEGIN, MOVE, MOVE_REJECTED, MAP_UPDATE, TIME_SYNC, MATCH_RESULT, RESULT_SAVED, RESULT_SAVE_FAILED, REMATCH, REMATCH_STATUS, EXIT_MATCH, ROOM_CLOSED, PING, PONG, LOGOUT, and ERROR.
2. THE game-common module SHALL define PlayerStatus values IDLE, IN_ROOM, and PLAYING.
3. THE game-common module SHALL define RoomStatus values WAITING_READY, PLAYING, FINISHING, WAITING_REMATCH, and CLOSED.
4. THE game-common module SHALL define shared constants for map dimensions, match duration, movement interval, challenge timeout, readiness timeout, rematch timeout, and default server port.
5. EACH message SHALL contain actionType, requestId, timestamp, and payload.
6. Client requests SHALL contain a UUID requestId; direct responses SHALL preserve it, while unsolicited server events SHALL use null.
7. Message timestamps SHALL use UTC ISO-8601 strings and SHALL not be trusted as authoritative gameplay time when supplied by the client.
8. THE Game_Client and Game_Server SHALL use Jackson with UTF-8 JSON encoded as one message per line, terminated by LF.
9. THE sender SHALL escape newline characters inside JSON string values and SHALL not pretty-print messages on the TCP connection.
10. THE receiver SHALL handle both partial messages and multiple messages arriving in a single TCP read.
11. THE receiver SHALL limit a message to 65,536 bytes before its LF delimiter and close connections that exceed this limit.
12. THE game-common module SHALL define typed payload DTOs for every ActionType rather than require client and server modules to define independent payload formats.
13. THE protocol SHALL use a complete-grid MAP_UPDATE in this version; CELL_UPDATE SHALL not be used.
14. THE receiver SHALL reject missing required fields, invalid field types, and unsupported actions with a controlled error.
15. THE receiver MAY ignore unknown additional fields when all required fields remain valid.
16. THE Game_Client SHALL ignore MAP_UPDATE messages for another match or with a stateVersion no greater than the last applied version.
17. THE Game_Client SHALL stop applying gameplay updates for a match after MATCH_RESULT.
18. THE protocol SHALL distinguish EXIT_MATCH, which leaves a room, from LOGOUT, which terminates the authenticated connection.
Requirement 14: Concurrent Client Handling
User Story: As a system architect, I want the game server to handle multiple concurrent connections safely, so that simultaneous players and matches do not corrupt one another's state.
Acceptance Criteria
1. THE Game_Server SHALL use ServerSocket on the configured port, defaulting to 8888.
2. WHEN accepting a connection, THE Game_Server SHALL create a ClientHandler and execute it using an ExecutorService with sufficient connection-handling capacity for the configured limit.
3. THE Session_Manager and room registry SHALL use ConcurrentHashMap for concurrent lookup.
4. THE Game_Server SHALL use a sequential lobby coordinator for session registration, challenge reservation, challenge acceptance, and assignment of players to rooms.
5. THE acceptance of a challenge and assignment of both players to the resulting room SHALL be one atomic lobby operation.
6. EACH Game_Room SHALL process READY, MOVE, timer, exit, disconnect, and rematch events sequentially through a room-specific serial executor.
7. ONLY the room's event executor SHALL modify its Game_State.
8. THE Game_Server SHALL use a shared worker pool for room event processing rather than require a dedicated permanently running platform thread for each room.
9. Timer tasks SHALL enqueue room events rather than modify Game_State directly.
10. EACH TCP connection SHALL have one serialized outbound writer; concurrent components SHALL not write directly to the same socket output stream.
11. THE Game_Server SHALL create immutable snapshots or defensive copies before placing state updates in outbound queues.
12. Backend HTTP requests and result persistence operations SHALL run outside the room's event executor and return through callbacks or queued events.
13. THE default limits SHALL be 100 TCP connections, 50 rooms, 200 queued gameplay events per room, and 256 outbound messages per connection.
14. WHEN gameplay queues are full, THE Game_Server SHALL reject additional movement requests with SERVER_BUSY while preserving a bounded processing path for timer and cleanup events.
15. WHEN an outbound queue is full, THE Game_Server SHALL disconnect the slow client and apply the normal disconnect rules.
16. WHEN a client exceeds 50 messages per second in two consecutive one-second windows, THE Game_Server SHALL close that connection.
17. Connection cleanup SHALL be idempotent and SHALL not remove a newer session belonging to the same user.
18. Updates in one room SHALL not modify another room's grid, participants, scores, deadline, or result.
Requirement 15: User Interface Design
User Story: As a player, I want an intuitive interface, so that I can navigate the game and understand its current state.
Acceptance Criteria
1. THE Game_Client SHALL use JavaFX 21, FXML layouts, and CSS styling.
2. THE Game_Client SHALL use a blue-and-white interface theme with primary colors #124EA6, #2672D6, #2E72D2, #FFFFFF, and #EBF4FC.
3. THE Game_Client SHALL provide Registration, Login, Lobby, Game, Ranking, History, and Settings screens or dialogs.
4. THE Lobby sidebar SHALL contain Lobby, Ranking, History, Settings, and Logout actions.
5. THE Settings interface SHALL allow configuration of Backend_API base URL, Game_Server host, and Game_Server port for the next login; connection settings SHALL not be changed during an active authenticated session.
6. THE Lobby SHALL display an Online Players table with username, ranking score, status, and Challenge button.
7. THE Lobby SHALL display a profile card containing username, confirmed ranking score, and win rate.
8. THE Game Board SHALL display the 20×20 grid, distinguishable player markers, player names, territory scores, and remaining time.
9. WHEN a match is waiting for READY, THE Game_Client SHALL display a preparation state and disable movement.
10. THE Game_Client SHALL distinguish Territory_Score from Ranking_Score through explicit labels.
11. WHEN a match ends, THE Game_Client SHALL show the outcome, territory scores, ranking point changes, persistence status, and available rematch actions.
12. WHEN a player accepts a rematch, THE Game_Client SHALL display that it is waiting for the opponent without enabling movement prematurely.
13. WHEN a room closes, THE Game_Client SHALL remove the room UI and return to the lobby unless authentication has expired or the connection has ended.
14. THE Game_Client SHALL perform REST and TCP operations outside the JavaFX application thread and dispatch control updates through Platform.runLater or an equivalent mechanism.
15. WHEN changing scenes, THE Game_Client SHALL remove obsolete listeners, stop previous input loops, and clear held keys.
16. WHEN a player attempts to log out or close the application during PLAYING, THE Game_Client SHALL request confirmation that leaving causes forfeiture.
17. THE Game_Client SHALL not display repeated modal dialogs for expected movement rejections such as OUT_OF_BOUNDS or MOVE_TOO_FAST.
Requirement 16: Error Handling and Recovery
User Story: As a player, I want the system to handle errors gracefully, so that I receive clear feedback when something goes wrong.
Acceptance Criteria
1. WHEN a TCP network error occurs, THE Game_Client SHALL disable gameplay input and display a connection error.
2. IF the JWT remains valid, THE Game_Client SHALL attempt reconnection up to three times after delays of 1, 2, and 4 seconds.
3. EACH successful new TCP connection SHALL require a new LOGIN exchange.
4. WHEN automatic reconnection fails, THE Game_Client SHALL offer a manual reconnect action.
5. Reconnection SHALL return the player to the lobby and SHALL not restore a forfeited or completed match.
6. THE Game_Server SHALL send PING every 5 seconds and require a PONG containing the corresponding nonce.
7. IF no valid new PONG is received for 15 seconds after authentication or the last valid PONG, THE Game_Server SHALL close the session.
8. WHEN EOF or IOException detects a disconnect earlier, THE Game_Server SHALL clean up immediately.
9. WHEN a disconnected player belongs to an active match, THE Game_Server SHALL apply the match completion rules in Requirement 8.
10. WHEN the Backend_API receives malformed JSON or invalid request fields, it SHALL return HTTP 400 with structured validation errors.
11. WHEN authentication fails, THE Backend_API SHALL return HTTP 401; authenticated requests without sufficient permission SHALL receive HTTP 403.
12. WHEN the Database is temporarily unavailable, THE Backend_API SHALL return HTTP 503; unexpected internal failures SHALL return HTTP 500.
13. THE Backend_API SHALL not expose stack traces, SQL statements, password hashes, tokens, or service credentials in error responses.
14. WHEN the Game_Server receives malformed JSON, it SHALL send ERROR when possible and close the connection after three consecutive malformed messages.
15. Invalid UTF-8, oversized messages, and authentication timeout SHALL cause connection closure.
16. WHEN a JWT expires while the player is IDLE or waiting in a room, THE Game_Server SHALL cancel pending activity and terminate the session with SESSION_EXPIRED.
17. WHEN a JWT expires during PLAYING, THE Game_Server SHALL permit that match to finish, disable rematch, then terminate the expired session.
18. WHEN the Game_Server restarts, matches without a durably recorded final result SHALL be treated as interrupted and SHALL not award points; pending outbox results SHALL still be retried.
Requirement 17: Maven Project Structure
User Story: As a developer, I want a well-organized multi-module Maven project, so that I can build, test, and maintain the system efficiently.
Acceptance Criteria
1. THE project SHALL use a parent Maven POM and four child modules: game-common, game-client, game-server, and game-backend.
2. THE project SHALL target Java 21.
3. THE game-common module SHALL contain shared protocol classes, enums, constants, and DTOs.
4. THE game-client module SHALL depend on game-common and contain JavaFX application and client networking code.
5. THE game-server module SHALL depend on game-common and contain TCP networking, sessions, challenges, rooms, game logic, timers, and result-outbox implementation.
6. THE game-backend module SHALL depend on game-common and contain Spring Boot controllers, services, repositories, entities, security, and database migrations.
7. THE application modules SHALL not depend directly on one another.
8. THE game-common module SHALL not contain JPA entities, database credentials, JWT signing secrets, or service keys.
9. THE parent POM SHALL centralize compatible dependency versions for Jackson, Spring Boot, JavaFX, MySQL Connector, JJWT, BCrypt support, and JUnit.
10. THE game-client module SHALL use JavaFX Maven Plugin, and the game-backend module SHALL use Spring Boot Maven Plugin.
11. THE project SHALL provide example configuration files for hostnames, ports, database connection settings, and outbox location without committing real secrets.
12. THE project SHALL support building and executing automated tests through mvn clean verify from the parent directory.
Requirement 18: Rematch and Room Lifecycle
User Story: As a player, I want to play another match with the same opponent or return to the lobby, so that I can continue playing without unnecessary reconnection.
Acceptance Criteria
1. AFTER finalizing a normally completed match, THE Game_Room SHALL remain FINISHING until the result has been written to Result_Outbox or its local persistence failure has been handled.
2. DURING FINISHING, THE Game_Server SHALL reject MOVE and REMATCH; EXIT_MATCH SHALL remain available and SHALL not cancel persistence of an already finalized result.
3. WHEN the result has been durably queued and both participants remain connected with valid sessions, THE Game_Server SHALL enter WAITING_REMATCH and establish a 15-second response deadline.
4. IF local outbox persistence fails, THE Game_Server SHALL deliver the result with FAILED status and close the room after initiating the immediate backend submission attempt.
5. THE MATCH_RESULT message SHALL contain rematchDeadlineAt when rematch is available and null otherwise.
6. WHEN a player selects Rematch, THE Game_Client SHALL send REMATCH containing roomId, previousMatchId, and accept=true.
7. WHEN a player declines rematch, THE Game_Client SHALL send REMATCH with accept=false or EXIT_MATCH.
8. THE Game_Server SHALL accept rematch responses only from the room's participants for the most recently completed match.
9. WHEN one participant accepts, THE Game_Server SHALL send REMATCH_STATUS containing accepted user IDs and the unchanged response deadline.
10. WHEN both participants accept before the deadline, THE Game_Server SHALL create a new matchId within the same roomId.
11. THE new match SHALL reset the grid, positions, scores, state version, movement sequences, cooldowns, readiness flags, and rematch votes while retaining player slots and colors.
12. THE new match SHALL follow the same WAITING_READY and MATCH_BEGIN process as an initial match.
13. WHEN either participant declines, leaves, disconnects, or fails to respond before the deadline, THE Game_Server SHALL send ROOM_CLOSED and return remaining valid connected participants to IDLE.
14. Duplicate rematch acceptance SHALL not create additional matches or extend the response deadline.
15. WHEN a room is closed, THE Game_Server SHALL remove it from the active registry and ignore obsolete callbacks associated with that room or its previous matches.
Requirement 19: REST API Contracts
User Story: As a developer, I want consistent REST request and response contracts, so that the client and game server can integrate with the backend without ambiguity.
Acceptance Criteria
1. THE Backend_API SHALL expose POST /api/auth/register accepting username, password, and fullName, and returning HTTP 201 with userId, username, and fullName.
2. THE Backend_API SHALL expose POST /api/auth/login accepting username and password, and returning HTTP 200 with accessToken, expiresAt, and profile.
3. THE Backend_API SHALL expose GET /api/users/profile, GET /api/users/leaderboard, and GET /api/matches/history using Authorization: Bearer <token>.
4. THE Backend_API SHALL expose POST /api/internal/sessions/validate accepting accessToken and returning userId, username, totalScore, and expiresAt.
5. THE Backend_API SHALL require X-Service-Key authentication for /api/internal/sessions/validate and /api/matches/save.
6. THE Backend_API SHALL return HTTP 201 for a newly saved match and HTTP 200 for an identical result that was already saved.
7. A successful save response SHALL contain matchId, alreadySaved, and the current statistics of both participants in standings.
8. EACH standings entry SHALL contain userId, totalScore, matchesPlayed, matchesWon, matchesLost, matchesDrawn, and winRate.
9. Paginated responses SHALL contain items, page, size, totalElements, and totalPages.
10. Invalid pagination parameters SHALL produce HTTP 400.
11. Error responses SHALL contain code, message, fieldErrors, and requestId; fieldErrors SHALL be an empty object when no field-specific errors apply.
12. Invalid match-result business rules SHALL return HTTP 422, while conflicting existing match content SHALL return HTTP 409.
13. THE Backend_API SHALL use camelCase in JSON and SHALL map database snake_case names within its persistence layer.
14. THE Service_Key SHALL be available only to Backend_API and Game_Server through server configuration and SHALL never be packaged into Game_Client.
15. WHEN deployed outside a trusted lab network, THE system SHALL protect HTTP and TCP credentials in transit using appropriate TLS connections and restrict access to service-only endpoints.
Requirement 20: TCP Payload Contracts
User Story: As a developer, I want each TCP action to have an explicit payload, so that client and server implementations use the same fields and types.
Acceptance Criteria
1. LOGIN SHALL contain accessToken; LOGIN_RESULT SHALL contain success=true, player, and expiresAt. Authentication failure SHALL use ERROR followed by connection closure.
2. PlayerDTO SHALL contain userId:long, username:string, totalScore:long, status:PlayerStatus, and canChallenge:boolean.
3. ONLINE_LIST SHALL contain players:PlayerDTO[].
4. CHALLENGE SHALL contain targetUserId:long; CHALLENGE_RECEIVED SHALL contain challengeId:UUID, challenger:PlayerDTO, and expiresAt:string.
5. CHALLENGE_ACCEPT, CHALLENGE_REJECT, and CHALLENGE_CANCEL SHALL each contain challengeId:UUID.
6. CHALLENGE_RESULT SHALL contain challengeId, status, and nullable reason; status SHALL be PENDING, ACCEPTED, REJECTED, CANCELLED, or EXPIRED.
7. SnapshotDTO SHALL contain roomId, matchId, stateVersion, grid, players, and remainingMs.
8. SnapshotDTO.players SHALL contain exactly two entries ordered by slot, each containing userId, username, slot, color, position:{x,y}, and cellsOwned.
9. MATCH_START SHALL contain snapshot and readyDeadlineAt; READY SHALL contain matchId.
10. MATCH_BEGIN SHALL contain matchId, startedAt, deadlineAt, and remainingMs=60000.
11. MOVE SHALL contain matchId, seq, and direction; direction SHALL be UP, DOWN, LEFT, or RIGHT.
12. MAP_UPDATE SHALL contain snapshot, actorUserId, and acceptedSeq.
13. MOVE_REJECTED SHALL contain matchId, seq, code, position, and stateVersion.
14. TIME_SYNC SHALL contain matchId, remainingMs, and deadlineAt.
15. MATCH_RESULT SHALL contain roomId, matchId, nullable winnerUserId, reason, two player result entries, persistence, and nullable rematchDeadlineAt.
16. EACH player result entry SHALL contain userId, slot, cellsOwned, outcome, and rankDelta; reason SHALL be TIMEOUT or FORFEIT, and initial persistence SHALL be PENDING or FAILED.
17. RESULT_SAVED SHALL contain matchId and standings; RESULT_SAVE_FAILED SHALL contain matchId and code.
18. REMATCH SHALL contain roomId, previousMatchId, and accept; REMATCH_STATUS SHALL contain roomId, previousMatchId, acceptedUserIds, and expiresAt.
19. EXIT_MATCH SHALL contain roomId; ROOM_CLOSED SHALL contain roomId and a reason of DECLINED, TIMEOUT, LEFT, DISCONNECTED, READY_TIMEOUT, SESSION_EXPIRED, or PERSISTENCE_FAILED.
20. PING and PONG SHALL contain a matching UUID nonce; LOGOUT SHALL contain an empty payload object.
21. ERROR SHALL contain code, message, and details; details SHALL be an empty object when no additional information is required.
22. UUID values SHALL be serialized as strings, timestamps as UTC ISO-8601 strings, and nullable fields as explicit JSON null values.
23. THE shared error codes SHALL include AUTH_REQUIRED, INVALID_TOKEN, SESSION_EXPIRED, ALREADY_CONNECTED, PLAYER_UNAVAILABLE, INVALID_TARGET, CHALLENGE_NOT_FOUND, CHALLENGE_EXPIRED, INVALID_STATE, NOT_IN_ROOM, STALE_MATCH, OUT_OF_BOUNDS, MOVE_TOO_FAST, STALE_SEQ, INVALID_MESSAGE, UNSUPPORTED_ACTION, BACKEND_UNAVAILABLE, and SERVER_BUSY.
24. WHEN applying asynchronous standings updates, THE Game_Client SHALL ignore a standings snapshot whose matchesPlayed is lower than the currently displayed confirmed value, so that delayed responses do not replace newer statistics.
Requirement 21: Integration and Acceptance Testing
User Story: As a development team, we want to verify critical flows and failure cases, so that the integrated system behaves consistently across all modules.
Acceptance Criteria
1. THE project SHALL include automated tests for capturing an unowned cell, entering an owned cell, capturing an opponent's cell, and rejecting out-of-bounds movement.
2. THE project SHALL verify movement cooldown enforcement, stale sequence rejection, and rejection of messages belonging to an earlier match.
3. THE project SHALL verify that crossed challenges, simultaneous invitations, repeated acceptance, and expired invitations never create multiple rooms for one player.
4. THE project SHALL verify that preparation timeout cancels a match without saving history or changing ranking scores.
5. THE project SHALL verify that simultaneous timeout, exit, and disconnect events produce only one finalized result.
6. THE project SHALL verify WIN, LOSS, DRAW, and FORFEIT outcomes and their corresponding ranking point changes.
7. THE project SHALL verify rematch acceptance, rejection, timeout, state reset, and generation of a new match ID.
8. THE project SHALL verify that resubmitting the same saved result does not duplicate history or increment statistics twice.
9. THE project SHALL verify transaction rollback and concurrent result updates involving the same player.
10. THE project SHALL verify that pending outbox records survive a Game_Server restart and are retried after Backend_API becomes available.
11. THE project SHALL verify heartbeat timeout, reconnect-to-lobby behavior, duplicate login rejection, and JWT expiration handling.
12. THE team SHALL run an integration scenario with at least two separate client instances and a load scenario with 10 simultaneous matches, recording observed errors, latency, and resource usage.
13. THE team SHALL treat configured connection limits as limits to validate rather than claim measured capacity without test results.