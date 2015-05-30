package war.org.perfmon4j.restdatasource.util.aggregators;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface Aggregator {
	public void aggreagate(ResultSet rs) throws SQLException;
	public Number getResult();
}
