package house.intelli.pvagg;

import static java.util.Objects.*;

public class AggregateSource {

	private final String sourcePropertyName;

	private final AggregateType aggregateType;

	public AggregateSource(String sourcePropertyName, AggregateType aggregateType) {
		this.sourcePropertyName = requireNonNull(sourcePropertyName, "sourcePropertyName");
		this.aggregateType = requireNonNull(aggregateType, "aggregateType");
	}

	public String getSourcePropertyName() {
		return sourcePropertyName;
	}

	public AggregateType getAggregateType() {
		return aggregateType;
	}
}
