CREATE TABLE IF NOT EXISTS `sc_script` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `packagePath` varchar(250) NOT NULL,
  `version` float NOT NULL,
  `sourcePath` varchar(250) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `state` int(11) NOT NULL COMMENT '1 inuse ,0 unused',
  `canReload` int(11) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;


INSERT INTO `sc_script` (`id`, `packagePath`, `version`, `sourcePath`, `createDate`, `state`, `canReload`) VALUES
	(1, 'org.hocate.test', 1, '/Users/helyho/Work/Java/BuizPlatform/js/org/hocate/test.js', '2014-11-07 19:36:22', 1, 2),
	(2, 'org.hocate.test', 2, '/Users/helyho/Work/Java/BuizPlatform/js/org/hocate/test.js', '2014-11-07 19:36:22', 1, 1);
