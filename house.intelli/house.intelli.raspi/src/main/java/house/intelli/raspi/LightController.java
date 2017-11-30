package house.intelli.raspi;

public interface LightController extends DimmerActor {

	static enum PropertyEnum implements DimmerActor.Property {
		lightDimmerValuesIndex,
		lightOn,
		switchOffOnKeyButtonUp,
		dimDirection,
		state
	}

}
