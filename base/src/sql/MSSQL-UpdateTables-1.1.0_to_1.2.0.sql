/* SQL files are obsolete. Use perfmon4j-dbupgrader.jar to create or update perfmon4j tables.
 * Example:
 * java -jar perfmon4j-dbupgrade.jar driverJarFile=~/JDBCDrivers/sqljdbc4.jar /
 * 		driverClass=com.microsoft.sqlserver.jdbc.SQLServerDriver /
 * 		jdbcURL=jdbc:sqlserver://dbhost;DatabaseName=perfmon4j 
 * 		userName=perfmon4j / 
 * 		password=mypassword
 */ 