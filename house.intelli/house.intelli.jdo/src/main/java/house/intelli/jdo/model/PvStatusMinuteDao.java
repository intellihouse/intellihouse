package house.intelli.jdo.model;

import static java.util.Objects.*;

import java.util.Date;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.jdo.Dao;

public class PvStatusMinuteDao extends Dao<PvStatusMinuteEntity, PvStatusMinuteDao> {

	private static final Logger logger = LoggerFactory.getLogger(PvStatusMinuteDao.class);

	public PvStatusMinuteEntity getPvStatusMinuteEntity(final String deviceName, final Date measured) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measured, "measured");
		final Query<PvStatusMinuteEntity> query = pm().newNamedQuery(getEntityClass(), "getPvStatusMinuteEntity_deviceName_measured");
		try {
			logger.debug("getPvStatusMinuteEntity: deviceName='{}', measured={}", deviceName, measured);
			final PvStatusMinuteEntity result = (PvStatusMinuteEntity) query.execute(deviceName, measured);
			logger.debug("getPvStatusMinuteEntity: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}
}
