package org.openhab.binding.intellihouse.rpc.echo;

import static house.intelli.core.util.AssertUtil.assertNotNull;

import org.eclipse.smarthome.core.items.ItemRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import house.intelli.core.rpc.echo.EchoRequest;
import house.intelli.core.rpc.echo.EchoResponse;
import house.intelli.core.rpc.echo.EchoRpcService;

public class OsgiEchoRpcService extends EchoRpcService {

    private final Logger logger = LoggerFactory.getLogger(OsgiEchoRpcService.class);

    protected ItemRegistry itemRegistry;

    public void setItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = itemRegistry;
    }

    public void unsetItemRegistry(ItemRegistry itemRegistry) {
        this.itemRegistry = null;
    }

    public OsgiEchoRpcService() {
        logger.debug("<init>");
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    @Override
    public EchoResponse process(EchoRequest request) throws Exception {
        logger.info("process");
        assertNotNull(itemRegistry, "itemRegistry");
        EchoResponse response = super.process(request);
        response.setPayload("OSGi: " + request.getPayload());
        return response;
    }

}
