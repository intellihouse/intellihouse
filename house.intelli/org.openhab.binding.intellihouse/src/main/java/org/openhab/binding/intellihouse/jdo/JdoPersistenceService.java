package org.openhab.binding.intellihouse.jdo;

import javax.jdo.PersistenceManagerFactory;

import house.intelli.jdo.IntelliHouseTransaction;

public interface JdoPersistenceService {

    boolean isEnabled();

    PersistenceManagerFactory getPersistenceManagerFactory();

    IntelliHouseTransaction beginTransaction();

}
