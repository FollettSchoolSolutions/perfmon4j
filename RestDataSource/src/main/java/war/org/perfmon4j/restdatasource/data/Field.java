package war.org.perfmon4j.restdatasource.data;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Field {
	private String name;
	private AggregationMethod[] aggregationMethods;
	private AggregationMethod defaultAggregationMethod;
	
	public Field() {
		super();
	}

	public Field(String name) {
		super();
		this.name = name;
	}
	
	public Field(String name, AggregationMethod[] aggregationMethod) {
		super();
		this.name = name;
		this.aggregationMethods = aggregationMethod;
		this.defaultAggregationMethod = AggregationMethod.NATURAL;
	}

	public Field(String name, AggregationMethod[] aggregationMethods, AggregationMethod defaultAggregationMethod) {
		super();
		this.name = name;
		this.aggregationMethods = aggregationMethods;
		this.defaultAggregationMethod = defaultAggregationMethod;
	}
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public AggregationMethod[] getAggregationMethods() {
		return aggregationMethods;
	}
	public void setAggregationMethods(AggregationMethod[] aggregationMethods) {
		this.aggregationMethods = aggregationMethods;
	}
	
	public AggregationMethod getDefaultAggregationMethod() {
		return defaultAggregationMethod;
	}

	public void setDefaultAggregationMethod(AggregationMethod defaultAggregationMethod) {
		this.defaultAggregationMethod = defaultAggregationMethod;
	}

	@Override
	public String toString() {
		return "Field [name=" + name + ", aggregationMethods="
				+ Arrays.toString(aggregationMethods)
				+ ", defaultAggregationMethod=" + defaultAggregationMethod
				+ "]";
	}
}
