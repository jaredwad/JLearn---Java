package DataAccess;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;

/**
 * Created by jared on 12/3/15.
 */
public interface IDataAccessObject {
	Instance getInstance(String   id );
	Object   getLabel   (String   id );
	boolean  hasLabel   (String   id );

	Dataset  getDataset ();
	Dataset  getDataset (String[] ids);

	void saveInstance(String id, Instance instance);
}
