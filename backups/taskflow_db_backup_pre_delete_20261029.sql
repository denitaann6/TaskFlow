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
-- Table structure for table `completed_task_stack`
--

DROP TABLE IF EXISTS `completed_task_stack`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `completed_task_stack` (
  `id` int NOT NULL AUTO_INCREMENT,
  `task_id` int NOT NULL,
  `stack_order` bigint NOT NULL,
  `pushed_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_completed_stack_task_id` (`task_id`),
  KEY `idx_stack_order` (`stack_order` DESC),
  CONSTRAINT `fk_completed_stack_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `completed_task_stack`
--

LOCK TABLES `completed_task_stack` WRITE;
/*!40000 ALTER TABLE `completed_task_stack` DISABLE KEYS */;
INSERT INTO `completed_task_stack` VALUES (1,102,1,'2026-10-28 19:34:26'),(2,105,2,'2026-10-28 19:34:26');
/*!40000 ALTER TABLE `completed_task_stack` ENABLE KEYS */;
UNLOCK TABLES;

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

--
-- Table structure for table `task_queue`
--

DROP TABLE IF EXISTS `task_queue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_queue` (
  `id` int NOT NULL AUTO_INCREMENT,
  `task_id` int NOT NULL,
  `sequence_order` bigint NOT NULL,
  `enqueued_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_queue_task_id` (`task_id`),
  KEY `idx_queue_sequence` (`sequence_order`),
  CONSTRAINT `fk_task_queue_task` FOREIGN KEY (`task_id`) REFERENCES `tasks` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `task_queue`
--

LOCK TABLES `task_queue` WRITE;
/*!40000 ALTER TABLE `task_queue` DISABLE KEYS */;
INSERT INTO `task_queue` VALUES (3,121,3,'2026-10-28 19:34:26'),(4,118,4,'2026-10-28 19:35:06'),(5,119,5,'2026-10-28 19:44:19');
/*!40000 ALTER TABLE `task_queue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` int NOT NULL,
  `name` varchar(150) NOT NULL,
  `member` varchar(100) NOT NULL,
  `priority` varchar(20) NOT NULL DEFAULT 'Medium',
  `status` varchar(30) NOT NULL DEFAULT 'Pending',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
INSERT INTO `tasks` VALUES (101,'Database Design','John','High','In Progress'),(102,'trial','subu','High','Completed'),(103,'Bug Checking','Sheena','High','In Progress'),(104,'backend','Sebastian','High','In Progress'),(105,'Report Writing','Mary','Medium','Completed'),(106,'Documentation','Mathew','Medium','In Progress'),(108,'Testing Task','Test user','Medium','Completed'),(110,'backend testing','Amrith','High','Completed'),(116,'Prepare Report (Phase 1)','subu','High','In Progress'),(117,'Prepare Report (Phase 2)','Mathew','Medium','In Progress'),(118,'Prepare Database Documentation(overall)','Arjun','High','Pending'),(119,'Design User Interface','Priya','High','Pending'),(120,'Test Authentication Module','Rahul','Medium','In Progress'),(121,'Prepare Project Report','Sneha','Medium','Pending');
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `assigned_member_name` varchar(100) DEFAULT NULL,
  `display_name` varchar(100) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,_binary '','Administrator','System Administrator','$2a$10$1x5WoJ2T2.yVxYqu/DoPtuzBEqNCGgqHibwRQB0i0l9KEgJM6QfjO','ADMIN','admin'),(2,_binary '','John','John','$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um','MEMBER','john'),(3,_binary '','subu','Subu','$2a$10$FT1R20FU.6quc.m554W4KeQBjyghG23NArqT7j76Ni3j2rgTfy0r.','MEMBER','subu'),(4,_binary '','Sheena','Sheena','$2a$10$INF9sU5xkZAfoZmpGL3bJOHDKriviY7hB42HB7qfD/8ONFmlyq3D.','MEMBER','sheena'),(5,_binary '','Sebastian','Sebastian','$2a$10$zFvJdNfdwvh19mzOtYBN5.YbSw30Mfc8aB4bcfAXbbnlh7d8SM9sG','MEMBER','sebastian'),(6,_binary '','Mary','Mary','$2a$10$F7F6vrVm6FI/wJyok.CImuJ38qn.HCAgMyy5o6ynGrfGaRmk5lqtm','MEMBER','mary'),(7,_binary '','Mathew','Mathew','$2a$10$5qNstTGFW5pCg6vC8l48zubdD1cFQf7gbqU3mGHvidyDNnyfYI8Xe','MEMBER','mathew'),(8,_binary '','Amrith','Amrith','$2a$10$pQ6CDlUT/BFZgjUUg.U5SO.I7KzLyK8tanKg90FXo3NDxWwbSBgq2','MEMBER','amrith'),(9,_binary '','Test user','Test User','$2a$10$rT3LfozSxTi.QgXiPVoQK.nJrO9Cs2oJYWjM3h0R3XcOO4ju0lumW','MEMBER','testuser'),(10,_binary '','UserA','User A','$2a$10$/E/3rNgUvMXuy5E5mqEsWeArXtxtSif9QiRKexiqRf5UYGlQyPQgG','MEMBER','usera'),(11,_binary '','UserB','User B','$2a$10$lvKTIKlUOUs3lgmBr6DTIOpobbIBxBt1An.9NJOQjDEm/asGCbPrS','MEMBER','userb'),(12,_binary '','a','Member A','$2a$10$gAVT3PExDjqCNu8HfjGrzuM1idIbV3b9fmYtQOVjUDtvAgOztuQKS','MEMBER','member_a'),(13,_binary '','b','Member B','$2a$10$9YfDNhSq6lVqy5MmL7KzneMT.vkY34dkC.hMN8vdjvNBOAz.38BUS','MEMBER','member_b'),(14,_binary '','Arjun','Arjun','$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um','MEMBER','arjun'),(15,_binary '','Priya','Priya','$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um','MEMBER','priya'),(16,_binary '','Rahul','Rahul','$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um','MEMBER','rahul'),(17,_binary '','Sneha','Sneha','$2a$10$29V/TfRJhfsLNLWMf7VG/uW6kwROihSOlGsklbMyi/lechXuoZ0um','MEMBER','sneha');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
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
