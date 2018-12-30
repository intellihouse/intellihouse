package house.intelli.jdo;

public interface DaoProvider {

	<D> D getDao(Class<D> daoClass);
}
