-- MySQL dump 10.13  Distrib 8.0.46, for Win64 (x86_64)
--
-- Host: localhost    Database: taskflow_db
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
-- Table structure for table `popped_tasks_history`
--

DROP TABLE IF EXISTS `popped_tasks_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `popped_tasks_history` (
  `id` int NOT NULL AUTO_INCREMENT,
  `task_id` int NOT NULL,
  `task_name` varchar(150) NOT NULL,
  `member` varchar(100) NOT NULL,
  `priority` varchar(20) NOT NULL,
  `status` varchar(30) NOT NULL DEFAULT 'Completed',
  `popped_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `popped_by` varchar(100) DEFAULT 'Administrator',
  `original_stack_order` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_popped_history_task` (`task_id`),
  KEY `idx_popped_at` (`popped_at` DESC),
  CONSTRAINT `fk_popped_history_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `popped_tasks_history`
--

LOCK TABLES `popped_tasks_history` WRITE;
/*!40000 ALTER TABLE `popped_tasks_history` DISABLE KEYS */;
INSERT INTO `popped_tasks_history` VALUES (1,110,'backend testing','Amrith','High','Completed','2026-10-28 19:44:20','System Administrator',4),(2,110,'backend testing','Amrith','High','Completed','2026-10-28 19:45:51','System Administrator',4),(3,110,'backend testing','Amrith','High','Completed','2026-10-28 20:03:27','System Administrator',4),(5,108,'Testing Task','Test user','Medium','Completed','2026-10-28 22:21:19','System Administrator',3);
/*!40000 ALTER TABLE `popped_tasks_history` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-10-29  9:16:17
