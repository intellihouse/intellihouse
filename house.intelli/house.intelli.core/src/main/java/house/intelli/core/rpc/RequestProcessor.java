package house.intelli.core.rpc;

public interface RequestProcessor<REQ extends Request, RES extends Response> {

	Class<REQ> getRequestType();

	RES process(REQ request);

}
