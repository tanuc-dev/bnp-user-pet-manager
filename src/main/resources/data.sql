-- =========================
-- Addresses (canonicalized)
-- =========================
INSERT INTO address (city, type, address_name, number) VALUES
('paris',  'road',     'antoine lavoisier', '10'),   -- id=1
('mumbai', 'street',   'marine drive',      '200'),  -- id=2
('london', 'avenue',   'baker',             '221B'), -- id=3
('paris',  'boulevard','haussmann',         '12'),   -- id=4
('pune',   'road',     'fc',                '100'),  -- id=5
('mumbai', 'road',     'sv',                '50');   -- id=6

-- =========================
-- Users (with homonyms, genders, deceased)
-- =========================
-- Paris
INSERT INTO users (name, first_name, age, gender, address_id, is_deceased) VALUES
('Doe',   'John', 30, 'MALE',   1, FALSE),  -- id=1 (Paris)
('Smith', 'Anna', 24, 'FEMALE', 2, FALSE),  -- id=2 (Mumbai)
('Doe',   'Jane', 28, 'FEMALE', 1, FALSE),  -- id=3 (Paris)
('Doe',   'John', 40, 'MALE',   3, FALSE),  -- id=4 (London)  -- homonym, different city
('Patel', 'Priya',29, 'FEMALE', 2, FALSE),  -- id=5 (Mumbai)
('Mehta', 'Rahul',31, 'MALE',   2, FALSE),  -- id=6 (Mumbai)
('Curie', 'Marie',35, 'FEMALE', 4, FALSE),  -- id=7 (Paris, Haussmann)
('Rao',   'Arjun',33, 'MALE',   5, FALSE),  -- id=8 (Pune)
('Khan',  'Sara', 26, 'FEMALE', 6, FALSE),  -- id=9 (Mumbai, SV Road)
('Shah',  'Amit', 38, 'MALE',   6, TRUE ),  -- id=10 (Mumbai, SV Road) -- deceased user
('Maria', 'Ana',  32, 'FEMALE', 3, FALSE),  -- id=11 (London)
('Dupont','Jean', 45, 'MALE',   4, FALSE);  -- id=12 (Paris, Haussmann)

-- =========================
-- Pets (types, addresses, deceased)
-- =========================
INSERT INTO pet (name, age, type, is_deceased, address_id) VALUES
('Buddy',  5,  'DOG',    FALSE, 1),  -- id=1  Paris
('Kitty',  3,  'CAT',    FALSE, 2),  -- id=2  Mumbai (Marine)
('Sammy',  2,  'SNAKE',  FALSE, 3),  -- id=3  London
('Shadow', 4,  'CAT',    FALSE, 1),  -- id=4  Paris
('Bruno',  6,  'DOG',    FALSE, 2),  -- id=5  Mumbai (Marine)
('Coco',   1,  'SPIDER', FALSE, 4),  -- id=6  Paris (Haussmann)
('Nemo',   1,  'OTHER',  FALSE, 2),  -- id=7  Mumbai (Marine)
('Tiger',  7,  'DOG',    FALSE, 6),  -- id=8  Mumbai (SV Road)
('Luna',   2,  'CAT',    FALSE, 4),  -- id=9  Paris (Haussmann)
('Rocky',  9,  'DOG',    TRUE,  5),  -- id=10 Pune   -- deceased pet
('Kaa',    4,  'SNAKE',  FALSE, 2),  -- id=11 Mumbai (Marine)
('Pixie',  2,  'CAT',    FALSE, 1);  -- id=12 Paris

-- =========================
-- Ownerships (co-ownership only when user.address_id == pet.address_id)
-- =========================

-- Paris (address_id=1)
-- John Doe (Paris) owns Buddy, Shadow, Pixie
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (1, 1), (1, 4), (1, 12);
-- Jane Doe (Paris) co-owns Buddy (same address as pet → allowed)
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (3, 1);

-- Paris (Haussmann, address_id=4)
-- Marie Curie owns Coco and Luna
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (7, 6), (7, 9);
-- Jean Dupont co-owns Luna (same address → allowed)
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (12, 9);

-- Mumbai (Marine Drive, address_id=2)
-- Anna Smith owns Kitty, Bruno, Nemo
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (2, 2), (2, 5), (2, 7);
-- Priya Patel and Rahul Mehta co-own Bruno (same address → allowed)
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (5, 5), (6, 5);
-- Priya owns Kaa (same address)
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (5, 11);

-- Mumbai (SV Road, address_id=6)
-- Sara Khan owns Tiger; Amit Shah (deceased user) also owns Tiger (same address)
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (9, 8), (10, 8);

-- London (address_id=3)
-- John Doe (London) owns Sammy
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (4, 3);

-- Pune (address_id=5)
-- Arjun Rao owns Rocky (deceased pet)
INSERT INTO user_pet_ownership (user_id, pet_id) VALUES (8, 10);
