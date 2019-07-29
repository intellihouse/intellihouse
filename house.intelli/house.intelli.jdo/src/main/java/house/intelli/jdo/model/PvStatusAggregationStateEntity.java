package house.intelli.jdo.model;

import java.util.Date;

import javax.jdo.annotations.Inheritance;
import javax.jdo.annotations.InheritanceStrategy;
import javax.jdo.annotations.PersistenceCapable;

import house.intelli.jdo.Entity;

@PersistenceCapable(table = "PvStatusAggregationState")
@Inheritance(strategy = InheritanceStrategy.NEW_TABLE)
public class PvStatusAggregationStateEntity extends Entity {

	private PvStatusEntity lastAggregatedPvStatusEntity;

	private Date deletedBefore;

	public PvStatusAggregationStateEntity() {
	}

	/**
	 * The newest {@link PvStatusEntity} written to the database which was already processed.
	 * Since the {@link PvStatusEntity#getId() PvStatusEntity.id} is incremented and data is not
	 * changed after it was initially written, this is determined by the {@code id} property,
	 * i.e. the greatest ID whose referenced object was already aggregated.
	 */
	public PvStatusEntity getLastAggregatedPvStatusEntity() {
		return lastAggregatedPvStatusEntity;
	}
	public void setLastAggregatedPvStatusEntity(PvStatusEntity lastAggregatedPvStatusEntity) {
		this.lastAggregatedPvStatusEntity = lastAggregatedPvStatusEntity;
	}

	public Date getDeletedBefore() {
		return deletedBefore;
	}
	public void setDeletedBefore(Date deletedBefore) {
		this.deletedBefore = deletedBefore;
	}
}
