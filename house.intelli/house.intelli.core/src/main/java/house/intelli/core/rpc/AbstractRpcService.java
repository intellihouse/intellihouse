package house.intelli.core.rpc;

import static house.intelli.core.util.AssertUtil.*;
import static house.intelli.core.util.ReflectionUtil.*;

import java.lang.reflect.Type;

public abstract class AbstractRpcService<REQ extends Request<RES>, RES extends Response> implements RpcService<REQ, RES>, Cloneable {

	private Class<REQ> requestType;

	private Class<RES> responseType;

	private RpcContext rpcContext;

	@Override
	public Class<REQ> getRequestType() {
		if (requestType == null) {
			Type[] actualTypeArguments = resolveActualTypeArguments(AbstractRpcService.class, this);

			if (! (actualTypeArguments[0] instanceof Class))
				throw new IllegalStateException("Implementation error in class " + this.getClass().getName() + ": Generic type REQ is still undefined, i.e. not a concrete class, but: " + actualTypeArguments[0]);

			if (! (actualTypeArguments[1] instanceof Class))
				throw new IllegalStateException("Implementation error in class " + this.getClass().getName() + ": Generic type REQ is still undefined, i.e. not a concrete class, but: " + actualTypeArguments[0]);

			@SuppressWarnings("unchecked")
			Class<RES> rs = (Class<RES>) actualTypeArguments[1];
			responseType = rs;

			@SuppressWarnings("unchecked")
			Class<REQ> rq = (Class<REQ>) actualTypeArguments[0];
			requestType = rq;
		}
		return requestType;
	}

	@Override
	public Class<RES> getResponseType() {
		if (responseType == null) {
			getRequestType();
			assertNotNull(responseType, "responseType");
		}
		return responseType;
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public RpcContext getRpcContext() {
		return rpcContext;
	}
	@Override
	public void setRpcContext(final RpcContext rpcContext) {
		if (this.rpcContext != null)
			throw new IllegalStateException("this.rpcContext already assigned!");

		this.rpcContext = rpcContext;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RpcService<REQ, RES> clone() {
		try {
			return (RpcService<REQ, RES>) super.clone();
		} catch (CloneNotSupportedException e) { // Should really never happen, because we implement Cloneable!
			throw new RuntimeException(e);
		}
	}
}
