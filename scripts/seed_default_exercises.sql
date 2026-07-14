-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: fitjournaldb
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Dumping data for table `default_exercises`
--

LOCK TABLES `default_exercises` WRITE;
/*!40000 ALTER TABLE `default_exercises` DISABLE KEYS */;
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (1,'Back Squat','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (2,'Front Squat','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (3,'Squat Machine','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (4,'T-bar Squat','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (5,'T-bar Thrusters','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (6,'Leg press','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (7,'Lunges Backwards','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (8,'Lunges Forward','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (9,'Single Leg Step-uP','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (10,'Thrusters','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (11,'Leg Extension Machine','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (12,'Deadlift','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (13,'Seated Leg Curl','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (14,'Prone Leg Curl','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (15,'Kneeling Leg Curl','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (16,'Adductors','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (17,'Standing Calf Raise Machine','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (18,'Seated Calf Raise','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (19,'Calf Press Machine (MANHATTAN)','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (20,'Standing w/bar','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (21,'Seated Calf Extension','Legs','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (22,'Shoulder press machine','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (23,'Seated Military Press with DB','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (24,'Cable Y Raises','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (25,'Trapeze','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (26,'Shrinks','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (27,'Lateral Raise Machine','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (28,'Lateral Raise Cable','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (29,'Lateral Raise DB','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (30,'Frontal Raise DB','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (31,'Reverse cable crossover','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (32,'Posterior Raise DB','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (33,'Rear deltoid fly machine','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (34,'Exterior Rotation Pulley','Shoulders','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (35,'Chest press with Dumbells','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (36,'Chest press wtih bar','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (37,'Chest press machine','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (38,'Cable crossover','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (39,'Multi-plane chest: one arm usupported','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (40,'Incline press w/bar','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (41,'Incline cable fly','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (42,'Incline press machine','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (43,'High cable crossover','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (44,'Decline press w/bar','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (45,'Low cable crossover','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (46,'Fly machine','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (47,'Dumbell Pullover','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (48,'Wall Balls','Chest','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (49,'Glute Machine (patada de burro)','Glutes','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (50,'Abductores','Glutes','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (51,'Hip Thrust','Glutes','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (52,'Multihip','Glutes','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (53,'Bulgarian Split Squat','Glutes','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (54,'Machine: two-arm preacher curl','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (55,'Machine: one-arm seated curl','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (56,'Ez Bar Curl','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (57,'Biceps Curl Machine','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (58,'Biceps Curl w/bar','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (59,'Dumbell Biceps Curl','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (60,'Incline Dumbbell Inner Biceps Curl','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (61,'Cable Biceps Curl','Biceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (62,'Machine - standing on-arm, one leg extension','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (63,'Triceps Extension Machine','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (64,'Triceps Press','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (65,'Cable Bent Over Extension','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (66,'Arm extension (MANHATTAN)','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (67,'Cable Crossovers (external rotation)','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (68,'Cable Push Down','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (69,'Overhead Cable Triceps Extension','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (70,'Decline skull crushes','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (71,'Close Grip Bench Press','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (72,'French press','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (73,'Overhead Dumbell Extensions','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (74,'Reverse Grip Tricep Pushdowns','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (75,'Dip','Triceps','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (76,'Row (Machine)','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (77,'Barbell Overhead Press','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (78,'Duall Pulley Row Wide Grip','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (79,'Low Row','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (80,'Bent over barbell row','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (81,'Back Extension Machine','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (82,'Row Rear Deltoid Cable','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (83,'Row Rear Deltoid Machine','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (84,'Cable Upright Row','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (85,'Behind-Neck Barbell Press','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (86,'Overhand Barbell Row','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (87,'Vertical Traction','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (88,'T-bar row','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (89,'Seated twisting cable row (DUMBO)','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (90,'Close Grip Lat Pulldown','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (91,'Dual Pulley Row Narrow Grip','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (92,'Pulldown','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (93,'Lateral Pulldown machine','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (94,'Reverse-Grip Bent-Over Row','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (95,'One-Arm Lateral Pulldown (Lat machine)','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (96,'Pull-ups','Back','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (97,'Standing Calf Raise Machine','Calves','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (98,'Seated Calf Raise','Calves','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (99,'Calf Press Machine (MANHATTAN)','Calves','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (100,'Standing w/bar','Calves','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (101,'Seated Calf Extension','Calves','');
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (102,'Leg Raises','Abs',NULL);
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (103,'Knee Raises','Abs',NULL);
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (104,'Plank','Abs',NULL);
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (105,'Toes to Bar','Abs',NULL);
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (106,'Cross-Body Hammer Curls','Biceps',NULL);
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (107,'Behind-the-back Wrist Curls','Forearms',NULL);
INSERT INTO `default_exercises` (`default_exercise_id`, `exercise_name`, `exercise_muscle_group`, `exercise_link`) VALUES (108,'Reverse Biceps Curls','Forearms',NULL);
/*!40000 ALTER TABLE `default_exercises` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-14 18:56:05
