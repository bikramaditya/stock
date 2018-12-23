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
-- Table structure for table `ind_niftynext50list`
--

DROP TABLE IF EXISTS `ind_niftynext50list`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
 SET character_set_client = utf8mb4 ;
CREATE TABLE `ind_niftynext50list` (
  `NAME` text,
  `Industry` text,
  `Symbol` text
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ind_niftynext50list`
--

LOCK TABLES `ind_niftynext50list` WRITE;
/*!40000 ALTER TABLE `ind_niftynext50list` DISABLE KEYS */;
INSERT INTO `ind_niftynext50list` VALUES ('ABB India Ltd.','INDUSTRIAL MANUFACTURING','ABB'),('ACC Ltd.','CEMENT & CEMENT PRODUCTS','ACC'),('Aditya Birla Capital Ltd.','FINANCIAL SERVICES','ABCAPITAL'),('Ambuja Cements Ltd.','CEMENT & CEMENT PRODUCTS','AMBUJACEM'),('Ashok Leyland Ltd.','AUTOMOBILE','ASHOKLEY'),('Aurobindo Pharma Ltd.','PHARMA','AUROPHARMA'),('Avenue Supermarts Ltd.','CONSUMER GOODS','DMART'),('Bandhan Bank Ltd.','FINANCIAL SERVICES','BANDHANBNK'),('Bank of Baroda','FINANCIAL SERVICES','BANKBARODA'),('Bharat Electronics Ltd.','INDUSTRIAL MANUFACTURING','BEL'),('Bharat Heavy Electricals Ltd.','INDUSTRIAL MANUFACTURING','BHEL'),('Biocon Ltd.','PHARMA','BIOCON'),('Bosch Ltd.','AUTOMOBILE','BOSCHLTD'),('Britannia Industries Ltd.','CONSUMER GOODS','BRITANNIA'),('Cadila Healthcare Ltd.','PHARMA','CADILAHC'),('Colgate Palmolive (India) Ltd.','CONSUMER GOODS','COLPAL'),('Container Corporation of India Ltd.','SERVICES','CONCOR'),('DLF Ltd.','CONSTRUCTION','DLF'),('Dabur India Ltd.','CONSUMER GOODS','DABUR'),('General Insurance Corporation of India','FINANCIAL SERVICES','GICRE'),('Godrej Consumer Products Ltd.','CONSUMER GOODS','GODREJCP'),('HDFC Standard Life Insurance Company Ltd.','FINANCIAL SERVICES','HDFCLIFE'),('Havells India Ltd.','CONSUMER GOODS','HAVELLS'),('Hindustan Zinc Ltd.','METALS','HINDZINC'),('ICICI Lombard General Insurance Company Ltd.','FINANCIAL SERVICES','ICICIGI'),('ICICI Prudential Life Insurance Company Ltd.','FINANCIAL SERVICES','ICICIPRULI'),('InterGlobe Aviation Ltd.','SERVICES','INDIGO'),('L&T Finance Holdings Ltd.','FINANCIAL SERVICES','L&TFH'),('LIC Housing Finance Ltd.','FINANCIAL SERVICES','LICHSGFIN'),('Lupin Ltd.','PHARMA','LUPIN'),('MRF Ltd.','AUTOMOBILE','MRF'),('Marico Ltd.','CONSUMER GOODS','MARICO'),('Motherson Sumi Systems Ltd.','AUTOMOBILE','MOTHERSUMI'),('NHPC Ltd.','ENERGY','NHPC'),('NMDC Ltd.','METALS','NMDC'),('Oil India Ltd.','ENERGY','OIL'),('Oracle Financial Services Software Ltd.','IT','OFSS'),('Petronet LNG Ltd.','ENERGY','PETRONET'),('Pidilite Industries Ltd.','CHEMICALS','PIDILITIND'),('Piramal Enterprises Ltd.','PHARMA','PEL'),('Procter & Gamble Hygiene & Health Care Ltd.','CONSUMER GOODS','PGHH'),('SBI Life Insurance Company Ltd.','FINANCIAL SERVICES','SBILIFE'),('Shree Cement Ltd.','CEMENT & CEMENT PRODUCTS','SHREECEM'),('Shriram Transport Finance Co. Ltd.','FINANCIAL SERVICES','SRTRANSFIN'),('Siemens Ltd.','INDUSTRIAL MANUFACTURING','SIEMENS'),('Steel Authority of India Ltd.','METALS','SAIL'),('Sun TV Network Ltd.','MEDIA & ENTERTAINMENT','SUNTV'),('The New India Assurance Company Ltd.','FINANCIAL SERVICES','NIACL'),('United Spirits Ltd.','CONSUMER GOODS','MCDOWELL-N'),('Vodafone Idea Ltd.','TELECOM','IDEA');
/*!40000 ALTER TABLE `ind_niftynext50list` ENABLE KEYS */;
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
