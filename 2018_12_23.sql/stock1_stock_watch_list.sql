-- MySQL dump 10.13  Distrib 8.0.13, for Win64 (x86_64)
--
-- Host: localhost    Database: stock1
-- ------------------------------------------------------
-- Server version	8.0.13

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
 SET NAMES utf8 ;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `stock_watch_list`
--

DROP TABLE IF EXISTS `stock_watch_list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `stock_watch_list` (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `MKT` varchar(10) NOT NULL,
  `NAME` varchar(300) NOT NULL,
  `INDUSTRY` varchar(45) DEFAULT NULL,
  `SYMBOL` varchar(10) NOT NULL,
  `IS_ACTIVE` int(11) DEFAULT NULL,
  `SECTOR` varchar(150) DEFAULT NULL,
  `instrument_token` varchar(45) DEFAULT NULL,
  `IS_FRESH_DATA` int(11) DEFAULT '0',
  PRIMARY KEY (`ID`)
) ENGINE=InnoDB AUTO_INCREMENT=101 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_watch_list`
--

LOCK TABLES `stock_watch_list` WRITE;
/*!40000 ALTER TABLE `stock_watch_list` DISABLE KEYS */;
INSERT INTO `stock_watch_list` VALUES (1,'NSE','Adani Ports and Special Economic Zone Ltd.','SERVICES','ADANIPORTS',1,'Infrastructure','3861249',0),(2,'NSE','Asian Paints Ltd.','CONSUMER GOODS','ASIANPAINT',1,'Paints','60417',0),(3,'NSE','Axis Bank Ltd.','FINANCIAL SERVICES','AXISBANK',1,'Banks','1510401',0),(7,'NSE','Bharat Petroleum Corporation Ltd.','ENERGY','BPCL',1,'Oil','134657',0),(8,'NSE','Bharti Airtel Ltd.','TELECOM','BHARTIARTL',1,'Telecommunications','2714625',0),(9,'NSE','Bharti Infratel Ltd.','TELECOM','INFRATEL',1,'Telecommunications','7458561',0),(10,'NSE','Cipla Ltd.','PHARMA','CIPLA',1,'Pharma','177665',0),(11,'NSE','Coal India Ltd.','METALS','COALINDIA',1,'Power','5215745',0),(14,'NSE','GAIL (India) Ltd.','ENERGY','GAIL',1,'Oil','1207553',0),(15,'NSE','Grasim Industries Ltd.','CEMENT & CEMENT PRODUCTS','GRASIM',1,'Cement','315393',0),(16,'NSE','HCL Technologies Ltd.','IT','HCLTECH',1,'Information','1850625',0),(19,'NSE','Hindalco Industries Ltd.','METALS','HINDALCO',1,'Metals','348929',0),(20,'NSE','Hindustan Petroleum Corporation Ltd.','ENERGY','HINDPETRO',1,'Oil','359937',0),(23,'NSE','I T C Ltd.','CONSUMER GOODS','ITC',1,'Consumer','424961',0),(24,'NSE','ICICI Bank Ltd.','FINANCIAL SERVICES','ICICIBANK',1,'Banks','1270529',0),(25,'NSE','Indiabulls Housing Finance Ltd.','FINANCIAL SERVICES','IBULHSGFIN',1,'Finance','7712001',0),(26,'NSE','Indian Oil Corporation Ltd.','ENERGY','IOC',1,'Oil','415745',0),(27,'NSE','IndusInd Bank Ltd.','FINANCIAL SERVICES','INDUSINDBK',1,'Banks','1346049',0),(28,'NSE','Infosys Ltd.','IT','INFY',1,'Information','408065',0),(29,'NSE','JSW Steel Ltd.','METALS','JSWSTEEL',1,'Metals','3001089',0),(30,'NSE','Kotak Mahindra Bank Ltd.','FINANCIAL SERVICES','KOTAKBANK',1,'Banks','492033',0),(31,'NSE','Larsen & Toubro Ltd.','CONSTRUCTION','LT',1,'Construction','2939649',0),(34,'NSE','Oil & Natural Gas Corporation Ltd.','ENERGY','ONGC',1,'Oil','633601',0),(35,'NSE','Power Grid Corporation of India Ltd.','ENERGY','POWERGRID',1,'Power','3834113',0),(36,'NSE','Reliance Industries Ltd.','ENERGY','RELIANCE',1,'Power','738561',0),(37,'NSE','State Bank of India','FINANCIAL SERVICES','SBIN',1,'Banks','779521',0),(38,'NSE','Sun Pharmaceutical Industries Ltd.','PHARMA','SUNPHARMA',1,'Pharma','857857',0),(40,'NSE','Tata Motors Ltd.','AUTOMOBILE','TATAMOTORS',1,'Auto','884737',0),(41,'NSE','Tata Steel Ltd.','METALS','TATASTEEL',1,'Metals','895745',0),(42,'NSE','Tech Mahindra Ltd.','IT','TECHM',1,'Information','3465729',0),(43,'NSE','Titan Company Ltd.','CONSUMER GOODS','TITAN',1,'Durables','897537',0),(44,'NSE','UPL Ltd.','FERTILISERS & PESTICIDES','UPL',1,'Fertilisers','2889473',0),(46,'NSE','Vedanta Ltd.','METALS','VEDL',1,'Metals','784129',0),(47,'NSE','Wipro Ltd.','IT','WIPRO',1,'Information','969473',0),(48,'NSE','Yes Bank Ltd.','FINANCIAL SERVICES','YESBANK',1,'Bank','3050241',0),(50,'NSE','NIFTY 50','NIFTY 50','NIFTY 50',0,'Index',NULL,0),(51,'NSE','ABB India Ltd.','INDUSTRIAL MANUFACTURING','ABB',1,'Manufacturing',NULL,0),(52,'NSE','ACC Ltd.','CEMENT & CEMENT PRODUCTS','ACC',1,'Cement',NULL,0),(54,'NSE','Ambuja Cements Ltd.','CEMENT & CEMENT PRODUCTS','AMBUJACEM',1,'Cement',NULL,0),(55,'NSE','Ashok Leyland Ltd.','AUTOMOBILE','ASHOKLEY',1,'Auto',NULL,0),(56,'NSE','Aurobindo Pharma Ltd.','PHARMA','AUROPHARMA',1,'Pharma',NULL,0),(57,'NSE','Avenue Supermarts Ltd.','CONSUMER GOODS','DMART',1,'Consumer',NULL,0),(58,'NSE','Bandhan Bank Ltd.','FINANCIAL SERVICES','BANDHANBNK',1,'Banks',NULL,0),(59,'NSE','Bank of Baroda','FINANCIAL SERVICES','BANKBARODA',1,'Banks',NULL,0),(60,'NSE','Bharat Electronics Ltd.','INDUSTRIAL MANUFACTURING','BEL',1,'Manufacturing',NULL,0),(61,'NSE','Bharat Heavy Electricals Ltd.','INDUSTRIAL MANUFACTURING','BHEL',1,'Manufacturing',NULL,0),(62,'NSE','Biocon Ltd.','PHARMA','BIOCON',1,'Pharma',NULL,0),(65,'NSE','Cadila Healthcare Ltd.','PHARMA','CADILAHC',1,'Pharma',NULL,0),(66,'NSE','Colgate Palmolive (India) Ltd.','CONSUMER GOODS','COLPAL',1,'Consumer',NULL,0),(68,'NSE','DLF Ltd.','CONSTRUCTION','DLF',1,'Construction',NULL,0),(69,'NSE','Dabur India Ltd.','CONSUMER GOODS','DABUR',1,'Consumer',NULL,0),(70,'NSE','General Insurance Corporation of India','FINANCIAL SERVICES','GICRE',1,'Finance',NULL,0),(71,'NSE','Godrej Consumer Products Ltd.','CONSUMER GOODS','GODREJCP',1,'Consumer',NULL,0),(72,'NSE','HDFC Standard Life Insurance Company Ltd.','FINANCIAL SERVICES','HDFCLIFE',1,'Finance',NULL,0),(74,'NSE','Hindustan Zinc Ltd.','METALS','HINDZINC',1,'Metals',NULL,0),(75,'NSE','ICICI Lombard General Insurance Company Ltd.','FINANCIAL SERVICES','ICICIGI',1,'Finance',NULL,0),(76,'NSE','ICICI Prudential Life Insurance Company Ltd.','FINANCIAL SERVICES','ICICIPRULI',1,'Finance',NULL,0),(77,'NSE','InterGlobe Aviation Ltd.','SERVICES','INDIGO',1,'Telecom',NULL,0),(79,'NSE','LIC Housing Finance Ltd.','FINANCIAL SERVICES','LICHSGFIN',1,'Finance',NULL,0),(80,'NSE','Lupin Ltd.','PHARMA','LUPIN',1,'Pharma',NULL,0),(82,'NSE','Marico Ltd.','CONSUMER GOODS','MARICO',1,'Consumer',NULL,0),(83,'NSE','Motherson Sumi Systems Ltd.','AUTOMOBILE','MOTHERSUMI',1,'Auto',NULL,0),(85,'NSE','NMDC Ltd.','METALS','NMDC',1,'Metals',NULL,0),(86,'NSE','Oil India Ltd.','ENERGY','OIL',1,'Oil',NULL,0),(88,'NSE','Petronet LNG Ltd.','ENERGY','PETRONET',1,'Oil',NULL,0),(92,'NSE','SBI Life Insurance Company Ltd.','FINANCIAL SERVICES','SBILIFE',1,'Finance',NULL,0),(95,'NSE','Siemens Ltd.','INDUSTRIAL MANUFACTURING','SIEMENS',1,'Manufacturing',NULL,0),(96,'NSE','Steel Authority of India Ltd.','METALS','SAIL',1,'Metals',NULL,0),(97,'NSE','Sun TV Network Ltd.','MEDIA & ENTERTAINMENT','SUNTV',1,'Media',NULL,0),(100,'NSE','Vodafone Idea Ltd.','TELECOM','IDEA',1,'Telecom',NULL,0);
/*!40000 ALTER TABLE `stock_watch_list` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2018-12-23 20:00:49
