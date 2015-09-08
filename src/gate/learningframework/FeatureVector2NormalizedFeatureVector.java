package gate.learningframework;

import java.io.Serializable;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;

public class FeatureVector2NormalizedFeatureVector extends Pipe implements Serializable {
	double[] means;
	double[] variances;
	
	public FeatureVector2NormalizedFeatureVector (double[] means, double[] variances, 
			Alphabet alphabet) {
		super(alphabet, null);
		this.means = means;
		this.variances = variances;
	}

	public Instance pipe(Instance carrier) {
		if (! (carrier.getData() instanceof FeatureVector)) {
			System.out.println(carrier.getData().getClass());
			throw new IllegalArgumentException("Data must be of type FeatureVector");
		}
		
		if(this.means.length!=this.getDataAlphabet().size() || 
				this.variances.length!=this.getDataAlphabet().size()){
			System.err.println("Normalize failed due to dimensionality mismatch!");
		} else {
			FeatureVector fv = (FeatureVector)carrier.getData();
			int[] indices = fv.getIndices();
			double[] values = fv.getValues();
			for(int i=0;i<indices.length;i++){
				int index = indices[i];
				double value = values[i];
				double mean = means[index];
				double variance = variances[index];
				double newvalue = (value-mean)/Math.sqrt(variance);
				fv.setValue(index, newvalue);
			}
			carrier.unLock();
			carrier.setData(fv);
			carrier.lock();
		}
		
		return carrier;
	}

	private static final long serialVersionUID = 1;
	private static final int CURRENT_SERIAL_VERSION = 0;
}

