import java.util.Comparator;
import java.util.SortedSet;
import java.util.Map.Entry;

public class ValueComparator implements Comparator<Object> {

	@Override
	public int compare(Object arg0, Object arg1) {
		double diff = ((Entry<Integer, Double>)arg0).getValue()-((Entry<Integer, Double>)arg1).getValue();
		if(diff < 0)
			return -1;
		if(diff > 0)
			return 1;
		return -1;
	}

}
