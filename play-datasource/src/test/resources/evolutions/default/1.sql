-- !Ups

CREATE TABLE IF NOT EXISTS `test_db`.`my_sql_local_data_time_override_profile_tests`
(
    `id`         INTEGER     NOT NULL AUTO_INCREMENT,
    `created_at` DATETIME(6) NOT NULL DEFAULT 0,
    `updated_at` DATETIME(6) NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

-- !Downs

DROP TABLE IF EXISTS `test_db`.`my_sql_local_data_time_override_profile_tests`;
