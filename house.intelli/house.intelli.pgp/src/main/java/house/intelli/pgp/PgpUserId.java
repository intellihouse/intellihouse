package house.intelli.pgp;

import static house.intelli.core.util.StringUtil.*;

import java.io.Serializable;

import house.intelli.core.bean.AbstractBean;
import house.intelli.core.bean.PropertyBase;
import house.intelli.core.util.StringUtil;

public class PgpUserId extends AbstractBean<PgpUserId.Property> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static interface Property extends PropertyBase { }

	public static enum PropertyEnum implements Property {
		name,
		email
	}

	private String name;
	private String email;

	public PgpUserId() {
	}

	public PgpUserId(String userIdStr) {
		if (! StringUtil.isEmpty(userIdStr)) {
			userIdStr = userIdStr.trim();

			final int lastLt = userIdStr.lastIndexOf('<');
			final int lastGt = userIdStr.lastIndexOf('>');
			if (lastLt < 0) {
				final int lastSpace = userIdStr.lastIndexOf(' ');
				if (lastSpace < 0) {
					final String email = lastGt < 0 ? userIdStr : userIdStr.substring(0, lastGt);
					this.email = email.trim();
				}
				else {
					final String email = lastGt < 0 ? userIdStr.substring(lastSpace + 1) : userIdStr.substring(lastSpace + 1, lastGt);
					this.email = email.trim();
				}
			}
			else { // this should apply to most or even all
				final String email = lastGt < 0 ? userIdStr.substring(lastLt + 1) : userIdStr.substring(lastLt + 1, lastGt);
				final String fullName = userIdStr.substring(0, lastLt);

				this.email = email.trim();
				this.name = fullName.trim();
			}
		}
	}

	public PgpUserId(PgpUserId other) {
		if (other != null) {
			this.name = other.getName();
			this.email = other.getEmail();
		}
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		setPropertyValue(PropertyEnum.name, name);
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		setPropertyValue(PropertyEnum.email, email);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();

		final String name = trim(getName());
		final String email = trim(getEmail());

		if (! StringUtil.isEmpty(name))
			sb.append(name);

		if (! StringUtil.isEmpty(email)) {
			if (sb.length() > 0)
				sb.append(' ');

			sb.append('<').append(email).append('>');
		}
		return sb.toString();
	}

	public boolean isEmpty() {
		return StringUtil.isEmpty(trim(getName())) && StringUtil.isEmpty(trim(getEmail()));
	}
}
