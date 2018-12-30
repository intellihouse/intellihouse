package house.intelli.jdo.model;

import static java.util.Objects.*;

import java.util.Date;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.jdo.Dao;

public class PvStatusDao extends Dao<PvStatusEntity, PvStatusDao> {

	private static final Logger logger = LoggerFactory.getLogger(PvStatusDao.class);

	public PvStatusEntity getPvStatusEntity(final String deviceName, final Date measured) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measured, "measured");
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getPvStatusEntity_deviceName_measured");
		try {
			logger.debug("getPvStatusEntity: deviceName='{}', measured={}", deviceName, measured);
			final PvStatusEntity result = (PvStatusEntity) query.execute(deviceName, measured);
			logger.debug("getPvStatusEntity: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}

}
