# 🎨 Territorial Paint Game - Real-time Multiplayer Game

> **Đồ án môn Lập trình mạng (Network Programming)**  
> **Hệ thống trò chơi tô màu chiếm lãnh thổ 2 người chơi thời gian thực (Real-time Authoritative Server)**

---

## 📋 MỤC LỤC
1. [Giới Thiệu Tổng Quan](#-giới-thiệu-tổng-quan)
2. [🚀 Hướng Dẫn Cài Đặt & Khởi Chạy (Usage Instructions)](#-hướng-dẫn-cài-đặt--khởi-chạy-usage-instructions)
3. [🕹 Hướng Dẫn Trải Nghiệm Trò Chơi (User Guide)](#-hướng-dẫn-trải-nghiệm-trò-chơi-user-guide)
4. [Kiến Trúc Hệ Thống (System Architecture)](#-kiến-trúc-hệ-thống-system-architecture)
5. [Công Nghệ Sử Dụng (Tech Stack)](#-công-nghệ-sử-dụng-tech-stack)
6. [Luật Chơi & Cơ Chế Game (Game Mechanics & Rules)](#-luật-chơi--cơ-chế-game-game-mechanics--rules)
7. [Chi Tiết Thiết Kế Cơ Sở Dữ Liệu (Database Schema)](#-chi-tiết-thiết-kế-cơ-sở-dữ-liệu-database-schema)
8. [Giao Thức Truyền Tin & An Toàn Dữ Liệu](#-giao-thức-truyền-tin--an-toàn-dữ-liệu)
9. [Yêu Cầu Môi Trường (Prerequisites)](#-yêu-cầu-môi-trường-prerequisites)
10. [Cấu Hình Nâng Cao (Advanced Configuration)](#-cấu-hình-nâng-cao-advanced-configuration)
11. [Cấu Trúc Thư Mục Dự Án (Directory Structure)](#-cấu-trúc-thư-mục-dự-án-directory-structure)

---

## 🎨 GIỚI THIỆU TỔNG QUAN

**Territorial Paint Game** là một ứng dụng trò chơi trực tuyến 2 người chơi theo thời gian thực (Real-time Online Multiplayer) được xây dựng dựa trên mô hình kiến trúc phân tán microservices/decoupled components. Người chơi tham gia vào một bàn cờ kích thước $20 \times 20$ (400 ô grid) với nhiệm vụ di chuyển để tô màu chiếm lĩnh lãnh thổ nhiều hơn đối thủ trong thời gian quy định 60 giây.

Dự án áp dụng mô hình **Authoritative TCP Game Server** kết hợp với **Spring Boot RESTful Backend** và giao diện đồ họa **JavaFX Desktop Client**, đảm bảo tính chính xác của trạng thái trò chơi (anti-cheat), chống mất mát dữ liệu trận đấu (Outbox Pattern) và tính sẵn sàng cao.

---

## 🚀 HƯỚNG DẪN CÀI ĐẶT & KHỞI CHẠY (USAGE INSTRUCTIONS)

### Bước 1: Khởi tạo Cơ sở dữ liệu MySQL

1. Mở MySQL Workbench hoặc Terminal MySQL Client.
2. Tạo database tên `territorial_paint`:
   ```sql
   CREATE DATABASE territorial_paint CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Chạy file khởi tạo schema từ đường dẫn: `game-backend/src/main/resources/schema.sql`

*(Hoặc dùng lệnh terminal)*:
```bash
mysql -u root -p territorial_paint < game-backend/src/main/resources/schema.sql
```

---

### Bước 2: Biên dịch toàn bộ dự án với Maven

Mở Terminal tại thư mục gốc dự án (`ltm`), chạy lệnh:

```bash
mvn clean install
```
> Lệnh này sẽ biên dịch cả 4 module theo thứ tự: `game-common` $\rightarrow$ `game-backend`, `game-server`, `game-client`.

---

### Bước 3: Khởi chạy Spring Boot Backend

* **Chạy trên IDE (VS Code / IntelliJ)**:
  Mở file `game-backend/src/main/java/com/territorypaint/backend/BackendApplication.java` $\rightarrow$ Click **Run ▶**
* **Chạy bằng Terminal**:
  ```bash
  mvn spring-boot:run -pl game-backend
  ```
*Tín hiệu sẵn sàng*: Console xuất hiện `Started BackendApplication in ... seconds`.

---

### Bước 4: Khởi chạy Game Server (TCP Socket Server)

* **Chạy trên IDE (VS Code / IntelliJ)**:
  Mở file `game-server/src/main/java/com/territorypaint/server/GameServer.java` $\rightarrow$ Click **Run ▶**
* **Chạy bằng Terminal**:
  ```bash
  mvn exec:java -pl game-server -Dexec.mainClass="com.territorypaint.server.GameServer"
  ```
*Tín hiệu sẵn sàng*: Console xuất hiện `Game Server started on port 8888`.

---

### Bước 5: Khởi chạy Game Client (JavaFX Application)

* **Chạy trên IDE (VS Code / IntelliJ)**:
  Mở file `game-client/src/main/java/com/territorypaint/client/MainLauncher.java` $\rightarrow$ Click **Run ▶**  
  *(Lưu ý: Chạy `MainLauncher` thay vì `GameApplication` để tránh lỗi missing JavaFX runtime)*.
* **Chạy bằng Terminal**:
  ```bash
  mvn javafx:run -pl game-client
  ```

> 💡 **Mẹo chạy tự động 1-Click trên Windows**:  
> Double-click vào file [run-all.bat](file:///d:/code%20btl/ltm/run-all.bat) ở thư mục gốc để tự động bật cả Backend, Server và 2 Client cùng lúc.

---

## 🕹 HƯỚNG DẪN TRẢI NGHIỆM TRÒ CHƠI (USER GUIDE)

### 1. Đăng ký & Đăng nhập
* Mở ứng dụng Client $\rightarrow$ Bấm **Register** nếu chưa có tài khoản.
* Điền **Username**, **Password**, **Full Name** $\rightarrow$ Đăng ký thành công.
* Tiến hành **Login** để vào **Sảnh chờ (Lobby)**.

### 2. Thách đấu hoặc Ghép trận tại Sảnh chờ (Lobby)
* **Danh sách Online**: Xem danh sách các người chơi đang sẵn sàng trong Lobby.
* **Thách đấu trực tiếp (Challenge)**: Click chọn một người chơi $\rightarrow$ Bấm **Challenge**. Đối phương sẽ nhận thông báo Popup để **Accept** hoặc **Reject**.
* **Ghép trận tự động (Quick Match)**: Bấm **Find Match** để tự động tìm và ghép cặp với người chơi khác.

### 3. Vào trận & Điều khiển Game (In-Game)
* Bàn cờ $20 \times 20$ (400 ô grid).
* **Điều khiển di chuyển**:
  * **W** hoặc **Mũi tên ĐI LÊN ($\uparrow$)**: Di chuyển Lên.
  * **S** hoặc **Mũi tên ĐI XUỐNG ($\downarrow$)**: Di chuyển Xuống.
  * **A** hoặc **Mũi tên SANG TRÁI ($\leftarrow$)**: Di chuyển Sang trái.
  * **D** hoặc **Mũi tên SANG PHẢI ($\rightarrow$)**: Di chuyển Sang phải.
* **Đồng hồ**: Đếm ngược từ 60s về 0s.
* **Kết thúc trận**: Hết giờ hoặc khi bấm **Forfeit** (Đầu hàng), màn hình kết quả sẽ hiển thị người thắng cuộc, số ô chiếm được và số điểm rank thay đổi.

### 4. Xem Bảng xếp hạng & Lịch sử trận đấu
* Tại Sảnh chờ, bấm nút **Leaderboard** để xem Top người chơi cao điểm nhất Server.
* Bấm **History** để xem lịch sử danh sách các trận đấu đã tham gia.

---

## 🏗 KIẾN TRÚC HỆ THỐNG (SYSTEM ARCHITECTURAL DESIGN)

Hệ thống được chia làm 4 module Maven độc lập:

```mermaid
graph TD
    Client1["🎮 Game Client (JavaFX UI) - Player 1"] <-->|TCP Socket / JSON (Port 8888)| GameServer["⚡ Authoritative Game Server (TCP)"]
    Client2["🎮 Game Client (JavaFX UI) - Player 2"] <-->|TCP Socket / JSON (Port 8888)| GameServer
    
    Client1 <-->|HTTP REST / JWT (Port 8080)| Backend["🛡 Spring Boot Backend (REST API)"]
    Client2 <-->|HTTP REST / JWT (Port 8080)| Backend
    
    GameServer -->|Async HTTP / Service Key & Outbox| Backend
    Backend <-->|JPA / JDBC Connection| Database[("🗄 MySQL Database")]
    
    subgraph Shared Common Module
        Common["📦 Game Common (DTOs, Protocol, Payload, Constants)"]
    end
    Client1 -.-> Common
    Client2 -.-> Common
    GameServer -.-> Common
    Backend -.-> Common
```

### 1. `game-common`
* Chứa toàn bộ các hằng số cấu hình (`GameConstants`), định dạng thông điệp Giao thức (`Message`, `ActionType`, `Direction`), các lớp truyền dữ liệu Payload (`CellUpdatePayload`, `ChallengePayload`...) và các DTO dùng chung cho cả Client, Server & Backend.

### 2. `game-backend` (Spring Boot REST API - Port 8080)
* **Xác thực & Ủy quyền**: Xử lý Đăng ký, Đăng nhập, cấp phát mã thông báo JWT (JSON Web Token).
* **Quản lý Dữ liệu**: Lưu trữ Người dùng, Thống kê cá nhân (Thắng/Thua/Hòa), Lịch sử trận đấu và Bảng xếp hạng (Leaderboard).
* **Bảo mật Nội bộ (Internal Security)**: Cung cấp API nội bộ cho Game Server lưu kết quả trận đấu bất đồng bộ (sử dụng Header `X-Service-Key`).

### 3. `game-server` (TCP Socket Authoritative Server - Port 8888)
* **Quản lý Kết nối & Session**: Tiếp nhận hàng trăm kết nối đồng thời qua `ServerSocket`, xác minh JWT token từ Client.
* **Lobby & Matchmaking**: Quản lý sảnh chờ trực tuyến, thách đấu trực tiếp 1v1 (`ChallengeManager`) hoặc xếp hàng ghép trận tự động (`LobbyCoordinator`).
* **Game Engine (Authoritative Loop)**: Tính toán di chuyển, cooldown (100ms), quy tắc đè màu ô, kiểm tra ranh giới, tính điểm và thời gian trận đấu (60 giây).
* **Outbox Pattern**: Ghi nhận kết quả trận đấu xuống file đệm (Outbox) trước khi gửi về Backend, đảm bảo không mất dữ liệu ngay cả khi Backend gián đoạn mạng.

### 4. `game-client` (JavaFX Desktop Client)
* Giao diện đồ họa sinh động: Login/Register, Sảnh chờ (Lobby), Bàn cờ Game ($20 \times 20$), Bảng xếp hạng (Leaderboard), Lịch sử trận đấu (Match History).
* Quản lý chuyển cảnh mượt mà (`SceneManager`) và lắng nghe sự kiện phím bàn phím (WASD / Mũi tên) phản hồi thời gian thực.

---

## 🛠 CÔNG NGHỆ SỬ DỤNG (TECH STACK)

* **Ngôn ngữ lập trình**: Java 21 (LTS)
* **Build Tool**: Maven (Multi-module project)
* **Backend**: Spring Boot 3.2.2, Spring Security, Spring Data JPA, Hibernate
* **Cơ sở dữ liệu**: MySQL 8.0+ / HikariCP Connection Pool
* **Bảo mật**: JWT (io.jsonwebtoken 0.12.5), Password Hash (BCrypt)
* **Game Server**: Core Java TCP Socket, Multi-threading (Executors, ScheduledExecutorService), Jackson JSON Serialization
* **Client App**: JavaFX 21.0.2 (Controls, FXML, Graphics), JavaFX Maven Plugin

---

## 🎮 LUẬT CHƠI & CƠ CHẾ GAME (GAME MECHANICS & RULES)

* **Kích thước bàn cờ**: $20 \times 20$ (Tổng cộng **400 ô**).
* **Vị trí xuất phát**:
  * **Người chơi 1 (Blue - `#2672D6`)**: Tọa độ $(0, 0)$ (Góc trên-trái).
  * **Người chơi 2 (Orange - `#F28C28`)**: Tọa độ $(19, 19)$ (Góc dưới-phải).
* **Thời gian trận đấu**: **60 giây** đếm ngược.
* **Tốc độ di chuyển**: Cooldown tối thiểu **100ms** giữa các bước di chuyển (tránh spam phím).
* **Cơ chế chiếm đất**: 
  * Khi di chuyển vào ô chưa tô (Trắng/Xám) $\rightarrow$ Ô đó đổi thành màu của bạn.
  * Khi di chuyển vào ô của đối thủ $\rightarrow$ Cướp ô và đổi ô đó thành màu của bạn.
* **Cơ chế tính điểm & Xếp hạng**:
  * Trận đấu kết thúc sau 60 giây hoặc khi có 1 người chơi đầu hàng / ngắt kết nối.
  * **Thắng (WIN)**: Điểm cá nhân $+10$.
  * **Hòa (DRAW)**: Điểm cá nhân $+3$.
  * **Thua (LOSS)**: Điểm cá nhân $+0$.

---

## 🗄 CHI TIẾT THIẾT KẾ CƠ SỞ DỮ LIỆU (DATABASE SCHEMA)

Cơ sở dữ liệu `territorial_paint` gồm 4 bảng chuẩn hóa:

```sql
USERS (id, username, password_hash, full_name, total_score, created_at)
USER_STATS (user_id, matches_played, matches_won, matches_lost, matches_drawn)
MATCHES (match_id, room_id, started_at, ended_at, end_reason, winner_user_id, forfeited_user_id, payload_hash)
MATCH_HISTORY (id, match_id, user_id, opponent_id, player_slot, cells_owned, outcome, rank_delta)
```

* **Chỉ mục tối ưu (Indexes)**:
  * `idx_users_username`: Tối ưu tìm kiếm đăng nhập.
  * `idx_users_leaderboard` (`total_score DESC, id ASC`): Tối ưu truy vấn Bảng xếp hạng Top Server.
  * `idx_match_history_lookup` (`user_id, match_id`): Tra cứu lịch sử đấu cá nhân cực nhanh.

---

## 📡 GIAO THỨC TRUYỀN TIN & AN TOÀN DỮ LIỆU

### 1. Cấu trúc Giao thức JSON (Socket Packet)
Tất cả gói tin trao đổi qua Socket giữa Client và Game Server tuân theo định dạng:
```json
{
  "actionType": "MOVE",
  "requestId": "req_123456789",
  "payload": {
    "direction": "UP"
  },
  "timestamp": 1727684900000
}
```

### 2. Cơ chế Heartbeat (Ping/Pong)
* Định kỳ **5 giây**, Client/Server gửi gói tin `PING`.
* Nếu quá **15 giây** không nhận được `PONG`, server chủ động ngắt kết nối và xử thua ngắt kết nối cho người chơi đó để bảo vệ phòng đấu.

### 3. Mẫu Outbox Pattern (Đảm bảo lưu trận đấu)
Khi trận đấu kết thúc:
1. Game Server ghi kết quả vào thư mục đệm đĩa local (`./outbox`).
2. Tiến trình đệm `BackendSubmitter` thực hiện retry gửi kết quả về Spring Boot Backend.
3. Chỉ khi Backend phản hồi `HTTP 200 OK`, file đệm mới được xóa. Tránh hoàn toàn rủi ro mất mát dữ liệu do nghẽn mạng hay Backend khởi động lại.

---

## 💻 YÊU CẦU MÔI TRƯỜNG (PREREQUISITES)

Trước khi khởi chạy dự án, hãy đảm bảo máy tính của bạn đã cài đặt:

1. **Java Development Kit (JDK)**: Phiên bản **21** trở lên (`java -version`).
2. **Apache Maven**: Phiên bản **3.8+** (`mvn -version`).
3. **MySQL Server**: Phiên bản **8.0+** tại `localhost:3306`.

---

## ⚙ CẤU HÌNH NÂNG CAO (ADVANCED CONFIGURATION)

Bạn có thể chỉnh sửa cấu hình hệ thống qua tham số Java System Properties hoặc file cấu hình:

### 1. Backend (`game-backend/src/main/resources/application.properties`)
* `server.port`: Port chạy Backend (Mặc định: `8080`).
* `spring.datasource.url`: Cấu hình kết nối MySQL.
* `app.jwt.secret`: Secret key mã hóa JWT Token.
* `app.service.key`: Khóa xác thực dành riêng cho Game Server khi gọi API nội bộ.

### 2. Game Server Command Options
Có thể tùy chỉnh thông số Server khi chạy bằng `-D`:
```bash
mvn exec:java -pl game-server -Dexec.mainClass="com.territorypaint.server.GameServer" \
  -Dbackend.url="http://localhost:8080" \
  -Dservice.key="dev_service_key_secret_change_me" \
  -Doutbox.dir="./outbox"
```

---

## 📁 CẤU TRÚC THƯ MỤC DỰ ÁN (DIRECTORY STRUCTURE)

```
ltm/
├── pom.xml                        # Maven Parent POM
├── README.md                      # Tài liệu hướng dẫn hệ thống
├── run-all.bat                    # Script tự động khởi chạy cả hệ thống
├── game-common/                   # Module dùng chung
│   └── src/main/java/com/territorypaint/common/
│       ├── GameConstants.java     # Thông số Hằng số game
│       ├── dto/                   # Data Transfer Objects
│       ├── payload/               # Các payload tin nhắn JSON
│       └── protocol/              # Enums (ActionType, Direction...)
├── game-backend/                  # Spring Boot REST API
│   └── src/main/resources/
│       ├── application.properties # Cấu hình Spring Boot & Database
│       └── schema.sql             # SQL Script tạo bảng dữ liệu
├── game-server/                   # Authoritative TCP Game Server
│   └── src/main/java/com/territorypaint/server/
│       ├── GameServer.java        # Main Server Socket Launcher
│       ├── game/                  # CellCaptureEngine, GameState
│       ├── lobby/                 # Lobby & Matchmaking Coordinator
│       ├── net/                   # ClientHandler, BackendClient
│       ├── outbox/                # ResultOutbox, BackendSubmitter
│       └── session/               # Session & Connection Manager
└── game-client/                   # JavaFX Application Client
    └── src/main/java/com/territorypaint/client/
        ├── GameApplication.java   # JavaFX Application Main Class
        ├── MainLauncher.java      # Wrapper Launcher cho IDE/VS Code
        ├── net/                   # ServerConnection & Socket Handler
        ├── state/                 # ClientState Store
        └── ui/                    # JavaFX Views (Login, Lobby, Game, Leaderboard, History)
```

---

### 🏆 Đánh giá & Giảng viên Hướng dẫn
* **Môn học**: Lập trình mạng (Network Programming)
* **Chủ đề**: Real-time Multiplayer Territory Paint Game
* **Kiến trúc**: Client-Server (Authoritative), RESTful API, Outbox Pattern, Multi-threaded IO.

---
*Chúc bạn có trải nghiệm chơi game và kiểm thử đồ án vui vẻ!* 🎉
