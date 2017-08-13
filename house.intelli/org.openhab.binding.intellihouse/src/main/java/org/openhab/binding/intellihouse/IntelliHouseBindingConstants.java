/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.intellihouse;

import java.util.Date;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

import house.intelli.core.rpc.RpcConst;

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

    /**
     * {@link String}-representation of the thing's Host-ID. Must be configured in the
     * *.thing file.
     */
    public static final String THING_CONFIG_KEY_HOST_ID = "hostId";
    /**
     * {@link Date} encoded as {@linkplain DateUtil#toString() ISO8601 string} of the last time this thing was seen
     * online. This is not configuration, but state written by the {@code RpcServlet}.
     */
    public static final String THING_CONFIG_KEY_LAST_SEEN_DATE = "lastSeenDate";
    /**
     * {@link Date} encoded as {@linkplain DateUtil#toString() ISO8601 string} of the first time this thing was
     * considered offline. This is not configuration, but state written by the {@code RpcServlet}.
     * <p>
     * Important: The presence of this date doesn't mean that the device is offline. This date is only
     * used, if the thing does not have any {@link #THING_CONFIG_KEY_LAST_SEEN_DATE lastSeenDate} assigned.
     */
    public static final String THING_CONFIG_KEY_MAYBE_OFFLINE_SINCE_DATE = "maybeOfflineSinceDate";

    /**
     * Period in milliseconds how often to check, whether a thing was last seen longer ago than the
     * {@link #THING_OFFLINE_TIMEOUT}.
     */
    public static final long THING_OFFLINE_CHECK_PERIOD = RpcConst.LOW_LEVEL_TIMEOUT;

    /**
     * Timeout in milliseconds after which a thing is considered offline. As soon as the
     * {@link #THING_CONFIG_KEY_LAST_SEEN_DATE lastSeenDate} is longer in the past as this
     * timeout, the thing is marked as being offline.
     * <p>
     * If there is no {@code lastSeenDate}, the
     * {@link #THING_CONFIG_KEY_MAYBE_OFFLINE_SINCE_DATE maybeOfflineSinceDate} is used instead.
     */
    public static final long THING_OFFLINE_TIMEOUT = RpcConst.LOW_LEVEL_TIMEOUT * 2;
}
