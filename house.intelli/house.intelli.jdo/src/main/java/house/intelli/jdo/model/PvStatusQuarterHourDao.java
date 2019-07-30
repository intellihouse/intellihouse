package house.intelli.jdo.model;

import static java.util.Objects.*;

import java.util.Date;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.jdo.Dao;

public class PvStatusQuarterHourDao extends Dao<PvStatusQuarterHourEntity, PvStatusQuarterHourDao> {

	private static final Logger logger = LoggerFactory.getLogger(PvStatusQuarterHourDao.class);

	public PvStatusQuarterHourEntity getPvStatusQuarterHourEntity(final String deviceName, final Date measured) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measured, "measured");
		final Query<PvStatusQuarterHourEntity> query = pm().newNamedQuery(getEntityClass(), "getPvStatusQuarterHourEntity_deviceName_measured");
		try {
			logger.debug("getPvStatusQuarterHourEntity: deviceName='{}', measured={}", deviceName, measured);
			final PvStatusQuarterHourEntity result = (PvStatusQuarterHourEntity) query.execute(deviceName, measured);
			logger.debug("getPvStatusQuarterHourEntity: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}
}
