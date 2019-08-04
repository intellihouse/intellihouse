package house.intelli.jdo.model;

import static java.util.Objects.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.jdo.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.TimeInterval;
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

	public Date getFirstMeasured() {
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getFirstMeasured");
		try {
			logger.debug("getFirstMeasured");
			final Date result = (Date) query.execute();
			logger.debug("getFirstMeasured: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public Date getFirstMeasuredAfter(final long lastAggregatedId) {
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getFirstMeasuredAfter_id");
		try {
			logger.debug("getFirstMeasuredAfter");
			final Date result = (Date) query.execute(lastAggregatedId);
			logger.debug("getFirstMeasuredAfter: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public List<PvStatusEntity> getPvStatusEntitiesMeasuredBetween(final TimeInterval interval) {
		requireNonNull(interval, "interval");
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getPvStatusEntitiesMeasuredBetween_fromIncl_toExcl");
		try {
			logger.debug("getPvStatusEntitiesMeasuredBetween: interval={}", interval);
			@SuppressWarnings("unchecked")
			final List<PvStatusEntity> result = (List<PvStatusEntity>) query.execute(interval.getFromIncl(), interval.getToExcl());
			return new ArrayList<>(result);
		} finally {
			query.closeAll();
		}
	}

	public Date getLastMeasuredBefore(final String deviceName, final Date measuredToExcl) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measuredToExcl, "measuredToExcl");
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getLastMeasuredBefore_deviceName_measuredToExcl");
		try {
			logger.debug("getLastMeasuredBefore: deviceName='{}', measuredToExcl={}", deviceName, measuredToExcl);
			final Date result = (Date) query.execute(deviceName, measuredToExcl);
			logger.debug("getLastMeasuredBefore: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public PvStatusEntity getLastPvStatusEntityMeasuredBefore(final String deviceName, final Date measuredToExcl) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measuredToExcl, "measuredToExcl");
		Date measured = getLastMeasuredBefore(deviceName, measuredToExcl);
		if (measured == null)
			return null;

		PvStatusEntity pvStatusEntity = getPvStatusEntity(deviceName, measured);
		requireNonNull(pvStatusEntity, "getPvStatusEntity('" + deviceName + "', " + measured + ")");
		return pvStatusEntity;
	}

	public Date getFirstMeasuredAfter(final String deviceName, final Date measuredFromExcl) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measuredFromExcl, "measuredFromExcl");
		final Query<PvStatusEntity> query = pm().newNamedQuery(getEntityClass(), "getFirstMeasuredAfter_deviceName_measuredFromExcl");
		try {
			logger.debug("getFirstMeasuredAfter: deviceName='{}', measuredFromExcl={}", deviceName, measuredFromExcl);
			final Date result = (Date) query.execute(deviceName, measuredFromExcl);
			logger.debug("getFirstMeasuredAfter: result={}", result);
			return result;
		} finally {
			query.closeAll();
		}
	}

	public PvStatusEntity getFirstPvStatusEntityMeasuredAfter(final String deviceName, final Date measuredFromExcl) {
		requireNonNull(deviceName, "deviceName");
		requireNonNull(measuredFromExcl, "measuredFromExcl");
		Date measured = getFirstMeasuredAfter(deviceName, measuredFromExcl);
		if (measured == null)
			return null;

		PvStatusEntity pvStatusEntity = getPvStatusEntity(deviceName, measured);
		requireNonNull(pvStatusEntity, "getPvStatusEntity('" + deviceName + "', " + measured + ")");
		return pvStatusEntity;
	}
}
