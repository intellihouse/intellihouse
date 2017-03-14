/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link IntelliHouseBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marco Nguitragool - Initial contribution
 */
public class IntelliHouseBindingConstants {

    public static final String BINDING_ID = "intellihouse";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_DIMMER = new ThingTypeUID(BINDING_ID, "dimmer");
    public final static ThingTypeUID THING_TYPE_SWITCH = new ThingTypeUID(BINDING_ID, "switch");

    // // List of all Channel ids
    // public final static String CHANNEL_1 = "channel1";

}
