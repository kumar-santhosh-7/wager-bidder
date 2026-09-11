-- Optional phpMyAdmin import. Spring Boot also creates these tables automatically.
CREATE DATABASE IF NOT EXISTS ledger_bid CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ledger_bid;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(40) PRIMARY KEY,
  username VARCHAR(64) NOT NULL UNIQUE,
  password_hash VARCHAR(120) NOT NULL,
  name VARCHAR(120) NOT NULL,
  role VARCHAR(16) NOT NULL,
  coins INT NOT NULL DEFAULT 0,
  wins INT NOT NULL DEFAULT 0,
  losses INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS rounds (
  id VARCHAR(40) PRIMARY KEY,
  option_a VARCHAR(80) NOT NULL,
  option_b VARCHAR(80) NOT NULL,
  pool_a INT NOT NULL DEFAULT 0,
  pool_b INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL,
  winner VARCHAR(8) NULL,
  created_at DATETIME(3) NOT NULL,
  ends_at DATETIME(3) NOT NULL,
  photo_a VARCHAR(500) NULL,
  photo_b VARCHAR(500) NULL
);

CREATE TABLE IF NOT EXISTS bids (
  id VARCHAR(40) PRIMARY KEY,
  user_id VARCHAR(40) NOT NULL,
  round_id VARCHAR(40) NOT NULL,
  side VARCHAR(8) NOT NULL,
  amount INT NOT NULL,
  payout INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_bids_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_bids_round FOREIGN KEY (round_id) REFERENCES rounds(id)
);

CREATE TABLE IF NOT EXISTS ledger_entries (
  id VARCHAR(40) PRIMARY KEY,
  user_id VARCHAR(40) NOT NULL,
  type VARCHAR(16) NOT NULL,
  amount INT NOT NULL,
  note VARCHAR(255) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_ledger_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS settings (
  id BIGINT PRIMARY KEY,
  house_edge DOUBLE NOT NULL,
  min_bet INT NOT NULL,
  max_bet INT NOT NULL,
  maintenance TINYINT(1) NOT NULL
);

CREATE TABLE IF NOT EXISTS auth_tokens (
  token VARCHAR(64) PRIMARY KEY,
  user_id VARCHAR(40) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  CONSTRAINT fk_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);
