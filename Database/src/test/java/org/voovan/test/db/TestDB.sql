/*
 Navicat Premium Data Transfer

 Source Server         : SZ
 Source Server Type    : MariaDB
 Source Server Version : 100033
 Source Host           : 192.168.4.72:3306
 Source Schema         : test

 Target Server Type    : MariaDB
 Target Server Version : 100033
 File Encoding         : 65001

 Date: 08/02/2018 15:25:43
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sc_script
-- ----------------------------
DROP TABLE IF EXISTS `sc_script`;
CREATE TABLE `sc_script` (
  `id` int(11) unsigned zerofill NOT NULL AUTO_INCREMENT,
  `packagePath` varchar(500) COLLATE utf8_bin DEFAULT NULL,
  `version` decimal(10,3) DEFAULT NULL,
  `sourcePath` varchar(500) COLLATE utf8_bin DEFAULT NULL,
  `sourceCode` varchar(500) COLLATE utf8_bin DEFAULT NULL,
  `fileDate` bigint(20) DEFAULT NULL,
  `canReload` int(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

-- ----------------------------
-- Records of sc_script
-- ----------------------------
BEGIN;
INSERT INTO `sc_script` VALUES (0, 'org.hocate.test', 2.000, '/Users/helyho/Work/BlockLink', 'test.js', 1518067120000, 1);
INSERT INTO `sc_script` VALUES (1, 'org.hocate.test', 1.000, '/Users/helyho/Work/BlockLink', 'test.js', 1518067120000, 1);
COMMIT;

-- ----------------------------
-- Procedure structure for test
-- ----------------------------
DROP PROCEDURE IF EXISTS `test`;
delimiter ;;
CREATE DEFINER=`root`@`%` PROCEDURE `test`(INOUT p_inout varchar(200))
BEGIN
	select now() into p_inout;
END;
;;
delimiter ;

SET FOREIGN_KEY_CHECKS = 1;
