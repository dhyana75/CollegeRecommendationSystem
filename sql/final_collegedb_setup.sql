-- College Preference Recommender : full setup script


CREATE DATABASE IF NOT EXISTS collegedb;
USE collegedb;


-- 1. CITIES
CREATE TABLE CITIES (
    city_id INT AUTO_INCREMENT PRIMARY KEY,
    city_name VARCHAR(100) UNIQUE NOT NULL
);


-- 2. COLLEGES
CREATE TABLE COLLEGES (
    institute_code VARCHAR(50) PRIMARY KEY,
    institute_name VARCHAR(255) NOT NULL,
    city_id INT,
    FOREIGN KEY (city_id) REFERENCES CITIES(city_id)
);


-- 3. COURSES (course_id = last 5 digits of choice code, so NOT auto increment)
CREATE TABLE COURSES (
    course_id INT PRIMARY KEY,
    course_name VARCHAR(150) UNIQUE NOT NULL
);


-- 4. COLLEGE_COURSES
CREATE TABLE COLLEGE_COURSES (
    choice_code VARCHAR(50) PRIMARY KEY,
    institute_code VARCHAR(50),
    course_id INT,
    FOREIGN KEY (institute_code) REFERENCES COLLEGES(institute_code),
    FOREIGN KEY (course_id) REFERENCES COURSES(course_id)
);


-- 5. CATEGORY
CREATE TABLE CATEGORY (
    category_code VARCHAR(20) PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL
);


-- 6. CUTOFFS
CREATE TABLE CUTOFFS (
    cutoff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    choice_code VARCHAR(50),
    course_id INT,
    year INT NOT NULL,
    cap_round INT NOT NULL,
    category_code VARCHAR(20),
    merit_rank INT,
    percentile DECIMAL(7,4),
    FOREIGN KEY (choice_code) REFERENCES COLLEGE_COURSES(choice_code),
    FOREIGN KEY (course_id) REFERENCES COURSES(course_id),
    FOREIGN KEY (category_code) REFERENCES CATEGORY(category_code)
);


-- 7. mht_cet_cutoff
CREATE TABLE mht_cet_cutoff (
    cutoff_id BIGINT PRIMARY KEY,
    exam_type VARCHAR(50) NOT NULL,
    FOREIGN KEY (cutoff_id) REFERENCES CUTOFFS(cutoff_id)
);


-- 8. jee_cutoff
CREATE TABLE jee_cutoff (
    cutoff_id BIGINT PRIMARY KEY,
    exam_type VARCHAR(50) NOT NULL,
    FOREIGN KEY (cutoff_id) REFERENCES CUTOFFS(cutoff_id)
);


-- 9. RECOMMENDATION_SESSIONS
CREATE TABLE RECOMMENDATION_SESSIONS (
    session_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_percentile DECIMAL(7,4),
    student_rank INT,
    preferred_city VARCHAR(100),
    preferred_branch VARCHAR(100),
    category_code VARCHAR(20),
    search_metric ENUM('rank', 'percentile') NOT NULL,
    cutoff_year INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_code) REFERENCES CATEGORY(category_code)
);


-- 10. RECOMMENDED_COLLEGES
CREATE TABLE RECOMMENDED_COLLEGES (
    recommendation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT,
    choice_code VARCHAR(50),
    institute_name VARCHAR(255) NOT NULL,
    course_name VARCHAR(150) NOT NULL,
    city_name VARCHAR(100) NOT NULL,
    category_code VARCHAR(20) NOT NULL,
    lowest_cutoff DECIMAL(7,4),
    highest_cutoff DECIMAL(7,4),
    FOREIGN KEY (session_id) REFERENCES RECOMMENDATION_SESSIONS(session_id),
    FOREIGN KEY (choice_code) REFERENCES COLLEGE_COURSES(choice_code)
);




-- 11. STAGING TABLE (normal table, because triggers
--     are not allowed on TEMPORARY tables)


CREATE TABLE csv_cutoff_import (
    choice_code VARCHAR(30),
    institute_code INT,
    institute_name VARCHAR(200),
    city_name VARCHAR(100),
    course_name VARCHAR(150),
    cutoff_year SMALLINT,
    cap_round TINYINT,
    merit_rank INT,
    percentile DECIMAL(10,7),
    category_code VARCHAR(20)
);




-- 12. TRIGGER: every row inserted into the staging table
--     is automatically spread into the real tables


DELIMITER $$


CREATE TRIGGER trg_import_cutoff
AFTER INSERT ON csv_cutoff_import
FOR EACH ROW
BEGIN
    DECLARE v_city_id   INT;
    DECLARE v_course_id INT;
    DECLARE v_cutoff_id BIGINT;


    -- a) City (ignored if it already exists)
    INSERT IGNORE INTO CITIES (city_name) VALUES (NEW.city_name);
    SELECT city_id INTO v_city_id FROM CITIES WHERE city_name = NEW.city_name;


    -- b) College
    INSERT IGNORE INTO COLLEGES (institute_code, institute_name, city_id)
    VALUES (NEW.institute_code, NEW.institute_name, v_city_id);


    -- c) Reuse the existing course ID when the course name already exists.
    --    This is necessary because COURSES.course_name is UNIQUE, while the
    --    supplied sample choice code 628524550F yields 24550 but the same
    --    course name already uses ID 24510. For a new course, use the last
    --    five digits of the choice code as originally intended.
    SET v_course_id = NULL;
    SELECT MAX(course_id) INTO v_course_id
    FROM COURSES
    WHERE course_name = NEW.course_name;

    IF v_course_id IS NULL THEN
        SET v_course_id = CAST(
            RIGHT(REGEXP_REPLACE(NEW.choice_code, '[^0-9]', ''), 5) AS UNSIGNED);

        INSERT IGNORE INTO COURSES (course_id, course_name)
        VALUES (v_course_id, NEW.course_name);

        -- If INSERT IGNORE encountered a duplicate course name, use its ID.
        SELECT MAX(course_id) INTO v_course_id
        FROM COURSES
        WHERE course_name = NEW.course_name;
    END IF;


    -- d) College + course combination
    INSERT IGNORE INTO COLLEGE_COURSES (choice_code, institute_code, course_id)
    VALUES (NEW.choice_code, NEW.institute_code, v_course_id);


    -- e) Category (no full name in the data, so name = code)
    INSERT IGNORE INTO CATEGORY (category_code, category_name)
    VALUES (NEW.category_code, NEW.category_code);


    -- f) The cutoff itself
    INSERT INTO CUTOFFS (choice_code, course_id, year, cap_round,
                         category_code, merit_rank, percentile)
    VALUES (NEW.choice_code, v_course_id, NEW.cutoff_year, NEW.cap_round,
            NEW.category_code, NEW.merit_rank, NEW.percentile);


    SET v_cutoff_id = LAST_INSERT_ID();


    -- g) Sub-entity routing: Insert into jee_cutoff if category is 'AI', else mht_cet_cutoff
    IF NEW.category_code = 'AI' THEN
        INSERT INTO jee_cutoff (cutoff_id, exam_type)
        VALUES (v_cutoff_id, 'JEE(Main)');
    ELSE
        INSERT INTO mht_cet_cutoff (cutoff_id, exam_type)
        VALUES (v_cutoff_id, 'MHT-CET');
    END IF;
END$$


DELIMITER ;






-- 13. SAMPLE DATA (the trigger fills all other tables)




SET FOREIGN_KEY_CHECKS = 0;


INSERT INTO csv_cutoff_import VALUES
('620799510',6207,'Dr. D. Y. Patil Unitech Society''s Dr. D. Y. Patil Institute of Technology, Pimpri, Pune','Pune','Artificial Intelligence and Data Science',2023,1,8690,89.7341513,'AI'),
('319624510',3196,'Lokmanya Tilak College of Engineering, Kopar Khairane, Navi Mumbai','Navi Mumbai','Computer Engineering',2023,1,8736,89.6887549,'AI'),
('615624510',6156,'Marathwada Mitra Mandal''s College of Engineering, Karvenagar, Pune','Pune','Computer Engineering',2023,1,8754,89.6767767,'AI'),
('628524550F',6285,'Bharati Vidyapeeth''s College of Engineering for Women, Katraj, Dhankawadi, Pune','Pune','Computer Engineering',2023,1,8762,89.6767767,'AI'),
('617724610',6177,'Sinhgad College of Engineering, Vadgaon (BK), Pune','Pune','Information Technology',2023,1,8805,89.5981928,'AI'),
('314824510',3148,'Mahavir Education Trust''s Shah & Anchor Kutchhi Engineering College, Mumbai','Mumbai','Computer Engineering',2023,1,8848,89.5285591,'AI'),
('613924610',6139,'Progressive Education Society''s Modern College of Engineering, Pune','Pune','Information Technology',2023,1,8910,89.4796474,'AI'),
('416791110',4167,'Yeshwantrao Chavan College of Engineering,Wanadongri, Nagpur','Nagpur','Computer Science and Engineering(Artificial Intelligence)',2023,1,9012,89.3805067,'AI'),
('317661210',3176,'Thakur College of Engineering and Technology, Kandivali, Mumbai','Mumbai','Mechanical Engineering',2023,1,9093,89.2567087,'AI'),
('110124210',1101,'Shri Sant Gajanan Maharaj College of Engineering,Shegaon','Shegaon','Computer Science and Engineering',2023,1,9124,89.2371456,'AI'),
('411624210',4116,'Ankush Shikshan Sanstha''s G.H.Raisoni College of Engineering, Nagpur','Nagpur','Computer Science and Engineering',2023,1,9243,89.1355125,'AI'),
('313524610',3135,'Manjara Charitable Trust''s Rajiv Gandhi Institute of Technology, Mumbai','Mumbai','Information Technology',2023,1,9278,89.1045003,'AI'),
('512124510',5121,'K. K. Wagh Institute of Engineering Education and Research, Nashik','Nashik','Computer Engineering',2023,1,9374,88.9644925,'AI'),
('321119210',3211,'S.I.E.S. Graduate School of Technology, Nerul, Navi Mumbai','Navi Mumbai','Artificial Intelligence and Machine Learning',2023,1,9474,88.9057917,'AI'),
('615624610',6156,'Marathwada Mitra Mandal''s College of Engineering, Karvenagar, Pune','Pune','Information Technology',2023,1,9482,88.8699805,'AI');


SET FOREIGN_KEY_CHECKS = 1;




-- For future imports: add more rows to csv_cutoff_import, then run
-- TRUNCATE TABLE csv_cutoff_import;   (the real tables keep their data)


-- Run this AFTER collegedb_setup.sql




DELIMITER $$




-- PROCEDURE 1: get cutoffs of a college by (part of) its name
-- Rows are grouped by college + branch + category + year,
-- so every category of the same branch sits together.


DROP PROCEDURE IF EXISTS get_college_cutoffs$$


CREATE PROCEDURE get_college_cutoffs(IN p_college_name VARCHAR(255))
BEGIN
    SELECT c.institute_name,
           co.course_name,
           cu.category_code,
           cu.year,
           MIN(cu.merit_rank) AS lowest_rank,
           MAX(cu.merit_rank) AS highest_rank,
           MIN(cu.percentile) AS lowest_percentile,
           MAX(cu.percentile) AS highest_percentile
    FROM CUTOFFS cu
    JOIN COLLEGE_COURSES cc ON cu.choice_code = cc.choice_code
    JOIN COLLEGES c         ON cc.institute_code = c.institute_code
    JOIN COURSES co         ON cc.course_id = co.course_id
    WHERE c.institute_name LIKE CONCAT('%', p_college_name, '%')
    GROUP BY c.institute_name, co.course_name, cu.category_code, cu.year
    ORDER BY c.institute_name, co.course_name, cu.category_code;
END$$
DELIMITER ;




DELIMITER $$
-- PROCEDURE 2: recommend 3 colleges
-- Any parameter can be NULL (NULL = "don't filter on this").
-- Saves the search in RECOMMENDATION_SESSIONS and the
-- 3 results in RECOMMENDED_COLLEGES, then shows them.


DROP PROCEDURE IF EXISTS recommend_colleges$$


CREATE PROCEDURE recommend_colleges(
    IN p_city     VARCHAR(100),
    IN p_course   VARCHAR(150),
    IN p_rank     INT,
    IN p_category VARCHAR(20)
)
BEGIN
    DECLARE v_year    INT;
    DECLARE v_session BIGINT;


    -- use the latest year available in the data
    SELECT MAX(year) INTO v_year FROM CUTOFFS;


    -- 1) save the search
    INSERT INTO RECOMMENDATION_SESSIONS
        (student_rank, preferred_city, preferred_branch,
         category_code, search_metric, cutoff_year)
    VALUES
        (p_rank, p_city, p_course, p_category, 'rank', v_year);


    SET v_session = LAST_INSERT_ID();


    -- 2) find and save top 3 colleges
    -- A student can get a seat if his rank is <= the closing (highest) rank,
    -- so we keep those and pick the 3 with the closest closing rank.
    INSERT INTO RECOMMENDED_COLLEGES
        (session_id, choice_code, institute_name, course_name,
         city_name, category_code, lowest_cutoff, highest_cutoff)
    SELECT v_session, cc.choice_code, c.institute_name, co.course_name,
           ci.city_name, cu.category_code,
           MIN(cu.percentile), MAX(cu.percentile)
    FROM CUTOFFS cu
    JOIN COLLEGE_COURSES cc ON cu.choice_code = cc.choice_code
    JOIN COLLEGES c         ON cc.institute_code = c.institute_code
    JOIN CITIES ci          ON c.city_id = ci.city_id
    JOIN COURSES co         ON cc.course_id = co.course_id
    WHERE cu.year = v_year
      AND (p_city     IS NULL OR ci.city_name = p_city)
      AND (p_course   IS NULL OR co.course_name LIKE CONCAT('%', p_course, '%'))
      AND (p_category IS NULL OR cu.category_code = p_category)
    GROUP BY cc.choice_code, c.institute_name, co.course_name,
             ci.city_name, cu.category_code
    HAVING (p_rank IS NULL OR MAX(cu.merit_rank) >= p_rank)
    ORDER BY MAX(cu.merit_rank) ASC
    LIMIT 3;


    -- 3) show the result to the user
    SELECT * FROM RECOMMENDED_COLLEGES WHERE session_id = v_session;
END$$


DELIMITER ;