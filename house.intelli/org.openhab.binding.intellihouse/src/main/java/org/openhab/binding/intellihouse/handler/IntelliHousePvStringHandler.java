package org.openhab.binding.intellihouse.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntelliHousePvStringHandler extends IntelliHousePvHandler {

    private Logger logger = LoggerFactory.getLogger(IntelliHousePvStringHandler.class);

    public IntelliHousePvStringHandler(Thing thing) {
        super(thing);
    }

    @Override
    protected String getAcceptedItemType() {
        return "String";
    }
}
