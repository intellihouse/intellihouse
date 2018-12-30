package house.intelli.jdo.model;

import static java.util.Objects.*;

import java.util.Date;

import javax.jdo.Query;

import house.intelli.jdo.Dao;

public class PvStatusDao extends Dao<PvStatusEntity, PvStatusDao> {

	public PvStatusEntity getPvStatusEntity(final String deviceName, final Date measured) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measured, "measured");
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getPvStatusEntity_deviceName_measured");
		try {
			final PvStatusEntity result = (PvStatusEntity) query.execute(deviceName, measured);
			return result;
		} finally {
			query.closeAll();
		}
	}

}
