# ========================================================
# Dockerfile: MySQL Database cho Territorial Paint Game
# Tự động nạp sẵn cơ sở dữ liệu và cấu trúc bảng từ schema.sql
# ========================================================

FROM mysql:8.0

# Thiết lập thông tin môi trường mặc định khớp với application.properties
ENV MYSQL_ROOT_PASSWORD=123456
ENV MYSQL_DATABASE=territorial_paint

# Sao chép file schema.sql vào thư mục init của container MySQL
# MySQL image tự động thực thi các file .sql trong thư mục này khi tạo mới container
COPY game-backend/src/main/resources/schema.sql /docker-entrypoint-initdb.d/01-schema.sql

EXPOSE 3306
