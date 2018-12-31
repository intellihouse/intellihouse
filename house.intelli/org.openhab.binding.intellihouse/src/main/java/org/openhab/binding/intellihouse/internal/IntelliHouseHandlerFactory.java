/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse.internal;

import static org.openhab.binding.intellihouse.IntelliHouseBindingConstants.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.intellihouse.handler.IntelliHouseDimmerHandler;
import org.openhab.binding.intellihouse.handler.IntelliHousePvDateTimeHandler;
import org.openhab.binding.intellihouse.handler.IntelliHousePvNumberHandler;
import org.openhab.binding.intellihouse.handler.IntelliHousePvStringHandler;
import org.openhab.binding.intellihouse.handler.IntelliHouseSwitchHandler;

/**
 * The {@link IntelliHouseHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Marco Nguitragool - Initial contribution
 */
public class IntelliHouseHandlerFactory extends BaseThingHandlerFactory {

    private final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.<ThingTypeUID> asList(THING_TYPE_DIMMER, THING_TYPE_SWITCH)));

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_DIMMER)) {
            return new IntelliHouseDimmerHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_SWITCH)) {
            return new IntelliHouseSwitchHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_PV_DATE_TIME)) {
            return new IntelliHousePvDateTimeHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_PV_NUMBER)) {
            return new IntelliHousePvNumberHandler(thing);
        }
        if (thingTypeUID.equals(THING_TYPE_PV_STRING)) {
            return new IntelliHousePvStringHandler(thing);
        }
        return null;
    }
}
