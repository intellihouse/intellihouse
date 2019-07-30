package house.intelli.jdo.model;

import static java.util.Objects.*;

import java.util.Date;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.jdo.Dao;

public class PvStatusHourDao extends Dao<PvStatusHourEntity, PvStatusHourDao> {

	private static final Logger logger = LoggerFactory.getLogger(PvStatusHourDao.class);

	public PvStatusHourEntity getPvStatusHourEntity(final String deviceName, final Date measured) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measured, "measured");
		final Query<PvStatusHourEntity> query = pm().newNamedQuery(getEntityClass(), "getPvStatusHourEntity_deviceName_measured");
		try {
			logger.debug("getPvStatusHourEntity: deviceName='{}', measured={}", deviceName, measured);
			final PvStatusHourEntity result = (PvStatusHourEntity) query.execute(deviceName, measured);
			logger.debug("getPvStatusHourEntity: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}
}
