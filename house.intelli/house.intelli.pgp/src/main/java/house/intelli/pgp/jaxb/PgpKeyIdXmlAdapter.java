package house.intelli.pgp.jaxb;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import house.intelli.pgp.PgpKeyId;

public class PgpKeyIdXmlAdapter extends XmlAdapter<String, PgpKeyId> {

	@Override
	public PgpKeyId unmarshal(final String v) throws Exception {
		return new PgpKeyId(v);
	}

	@Override
	public String marshal(final PgpKeyId v) throws Exception {
		return v.toString();
	}

}
