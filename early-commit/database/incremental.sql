DROP SCHEMA IF EXISTS jaudio;
CREATE SCHEMA jaudio;
USE jaudio;

DROP TABLE IF EXISTS accounts;
DROP TABLE IF EXISTS similarities;

CREATE TABLE accounts (
	username VARCHAR(100) NOT NULL,
	password VARCHAR(100) NOT NULL,
	firstname VARCHAR(100) NOT NULL,
	lastname VARCHAR(100) NOT NULL,
	msisdn VARCHAR(100) NOT NULL,
	email VARCHAR(100) NOT NULL,
	creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	last_usage TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status BIT(1) DEFAULT 1,
  admin BIT(1) DEFAULT 0,
  region VARCHAR(4000),
  PRIMARY KEY (username)
);

CREATE TABLE similarities (
	id BIGINT NOT NULL AUTO_INCREMENT,
	producer VARCHAR(256) NOT NULL,
	creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	similarity INT NOT NULL,
  PRIMARY KEY (id)
);

COMMIT;

INSERT INTO accounts VALUES ('gtsat','202cb962ac59075b964b07152d234b70','George','Tsatsanifos','6949290888','gtsatsanifos@gmail.com','16384','500','8','16384','500','8',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1,1,NULL,0);
COMMIT;
