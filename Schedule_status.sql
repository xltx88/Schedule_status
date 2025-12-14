-- MySQL dump 10.13  Distrib 8.0.42, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: Multi_functiona_assistant
-- ------------------------------------------------------
-- Server version	5.7.44

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
-- Table structure for table `tasks`
--

DROP TABLE IF EXISTS `tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tasks` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `user_id` bigint(20) DEFAULT NULL COMMENT '所属用户ID，NULL表示系统默认',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否显示',
  `deleted` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_name` (`user_id`,`name`)
) ENGINE=InnoDB AUTO_INCREMENT=18 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tasks`
--

LOCK TABLES `tasks` WRITE;
/*!40000 ALTER TABLE `tasks` DISABLE KEYS */;
INSERT INTO `tasks` VALUES (1,'离岗',1,1,_binary '\0'),(13,'资料分析',1,1,_binary '\0'),(14,'玩手机',1,1,_binary ''),(15,'11',2,1,_binary ''),(16,'111',1,1,_binary ''),(17,'111111',3,1,_binary '\0');
/*!40000 ALTER TABLE `tasks` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `time_records`
--

DROP TABLE IF EXISTS `time_records`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `time_records` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL,
  `task_id` bigint(20) NOT NULL,
  `start_time` bigint(20) DEFAULT NULL,
  `end_time` bigint(20) DEFAULT NULL,
  `duration` bigint(20) DEFAULT NULL,
  `record_date` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `time_records`
--

LOCK TABLES `time_records` WRITE;
/*!40000 ALTER TABLE `time_records` DISABLE KEYS */;
INSERT INTO `time_records` VALUES (1,1,1,1765694110438,1765701312711,7202273,'2025-12-14','2025-12-14 08:35:13'),(2,1,13,1765701312711,1765701318677,5966,'2025-12-14','2025-12-14 08:35:19'),(3,1,1,1765701318677,1765701320689,2012,'2025-12-14','2025-12-14 08:35:21'),(4,1,13,1765701320689,1765701383719,63030,'2025-12-14','2025-12-14 08:36:24'),(5,1,14,1765701383719,1765701413368,29649,'2025-12-14','2025-12-14 08:36:53'),(6,1,13,1765701413368,1765701417955,4587,'2025-12-14','2025-12-14 08:36:58'),(7,1,14,1765701417955,1765702509679,1091724,'2025-12-14','2025-12-14 08:55:10'),(8,1,1,1765702509679,1765702591160,81481,'2025-12-14','2025-12-14 08:56:31'),(9,1,13,1765702591160,1765702596112,4952,'2025-12-14','2025-12-14 08:56:36'),(10,1,16,1765702596112,1765703142840,546728,'2025-12-14','2025-12-14 09:05:43'),(11,3,1,1765703340004,1765703381886,41882,'2025-12-14','2025-12-14 17:09:42');
/*!40000 ALTER TABLE `time_records` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `current_task_id` bigint(20) DEFAULT NULL,
  `current_task_start_time` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'xltx','123456','2025-12-13 22:36:20',13,1765703142840),(2,'11','22','2025-12-14 08:39:25',1,1765701580533),(3,'12','12','2025-12-14 17:08:37',17,1765703381886);
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

-- Dump completed on 2025-12-14 17:17:21
