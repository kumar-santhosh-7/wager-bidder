# Ledger Bid API

Java Spring Boot backend for the Ledger Bid app. Stores users, rounds, bids, ledger, and house settings in **XAMPP MySQL**.

## 1. Start MySQL in XAMPP

1. Open XAMPP Control Panel.
2. Start **MySQL** (Apache is optional; use it if you want phpMyAdmin at http://localhost/phpmyadmin).
3. Default login is `root` with an **empty password**.

The API creates the `ledger_bid` database on first start. To import tables yourself, run `db/schema.sql` in phpMyAdmin.

## 2. Run the API

Requires **JDK 17+**. Maven is optional — this repo includes the Maven wrapper.

```bash
cd c:\kwivra\vibuthar\ledger-bid-api
.\mvnw.cmd spring-boot:run
```

Listens on **http://localhost:8090** (8080 is often already in use). Health check: http://localhost:8090/api/health

If you set a MySQL password, edit `src/main/resources/application.properties`:

```
spring.datasource.password=YOUR_PASSWORD
```

## 3. Demo logins (seeded once)

| Role   | Username | Password  |
|--------|----------|-----------|
| Player | `arun`   | `1234`    |
| Admin  | `admin`  | `admin123`|

Also seeded: `meera`, `kiran`, `priya` / `1234`.

## 4. Point the app at this API

The Expo app calls `http://<dev-machine>:8090/api`.

- Android emulator: `http://10.0.2.2:8090/api`
- Phone on the same Wi-Fi: `http://YOUR_LAN_IP:8090/api` (set `EXPO_PUBLIC_API_URL` if auto-detect fails)

Keep this API running whenever you use the mobile app.
