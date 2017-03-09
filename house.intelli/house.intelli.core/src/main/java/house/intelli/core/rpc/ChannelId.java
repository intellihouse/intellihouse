//package house.intelli.core.rpc;
//
//import static house.intelli.core.util.AssertUtil.*;
//import static house.intelli.core.util.StringUtil.*;
//
//import java.util.List;
//import java.util.regex.Pattern;
//
//import javax.xml.bind.annotation.adapters.XmlAdapter;
//import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
//
//@XmlJavaTypeAdapter(type=ChannelId.class, value=ChannelId.ChannelIdXmlAdapter.class)
//public class ChannelId {
//	/**
//	 * Constant identifying the OpenHAB server being the center of the network.
//	 * <p>
//	 * Used as {@link #getClientChannelId() client} or {@link #getServerChannelId() server}.
//	 */
//	public static final String CENTER_SEGMENT = "center";
//
//	public static final char SEGMENT_SEPARATOR_CHAR = ':';
//
//	public static final Pattern HOSTID_PATTERN = Pattern.compile("[A-Za-z0-9_-]*");
//
//	public static class ChannelIdXmlAdapter extends XmlAdapter<String, ChannelId> {
//		@Override
//		public ChannelId unmarshal(final String v) throws Exception {
//			return new ChannelId(v);
//		}
//		@Override
//		public String marshal(final ChannelId v) throws Exception {
//			return v.toString();
//		}
//	}
//
//	private final String id;
//	private List<String> segments;
//
//	public ChannelId(String id) {
//		this.id = assertNotEmpty(id, "id");
//		getSegments(); // immediately create segments in order to test whether they're valid.
//	}
//
//	public List<String> getSegments() {
//		if (segments == null) {
//			segments = split(id, SEGMENT_SEPARATOR_CHAR);
//			for (String segment : segments) {
//				if (! HOSTID_PATTERN.matcher(segment).matches())
//					throw new IllegalArgumentException(String.format("Segment '%s' is not valid!", segment));
//			}
//		}
//		return segments;
//	}
//
//	@Override
//	public String toString() {
//		return id;
//	}
//
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + ((id == null) ? 0 : id.hashCode());
//		return result;
//	}
//
//	@Override
//	public boolean equals(final Object obj) {
//		if (this == obj) return true;
//
//		if (obj == null) return false;
//
//		if (getClass() != obj.getClass()) return false;
//
//		final ChannelId other = (ChannelId) obj;
//		return this.id.equals(other.id); // guaranteed to be never null!
//	}
//}
