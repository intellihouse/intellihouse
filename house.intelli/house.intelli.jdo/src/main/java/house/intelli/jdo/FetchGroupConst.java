package house.intelli.jdo;

import javax.jdo.FetchPlan;

public interface FetchGroupConst {

	/**
	 * Fetch-group to fetch the ID only. There is a bug in DN preventing it from loading the ID, if no fetch-group at all is specified.
	 * Therefore, we temporarily use this constant. Once we updated to a new DN which does not require this anymore, we
	 * can easily find all references and refactor them to use no fetch-group at all.
	 */
	String OBJECT_ID = FetchPlan.DEFAULT; // "ObjectId";

//	/**
//	 * Functional fetch-group used to load all data needed for a {@link ChangeSetDto}.
//	 * <p>
//	 * Specified by {@link ChangeSetDtoBuilder#buildChangeSetDto(Long)}.
//	 */
//	String CHANGE_SET_DTO = "ChangeSetDto";

}
