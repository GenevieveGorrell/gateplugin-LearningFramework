package gate.learningframework.semanticmodeling;

import java.io.File;
import java.net.URL;

import org.apache.log4j.Logger;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.DoubleVector;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.ProcessingResource;
import gate.Resource;
import gate.Utils;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;

@CreoleResource(name = "Semantic Comparison PR", comment = "Allows comparison of arbitrary "
		+ "annotations using semantic vectors produced using a semantic model created using"
		+ " the Semantic Modeling PR.")
public class SemanticComparisonPR extends AbstractLanguageAnalyser implements
ProcessingResource{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	static final Logger logger = Logger.getLogger("SemanticComparisonPR");


	private java.net.URL saveDirectory;
	private String instanceASName;
	private String comparisonASName;
	private String instanceType;
	private String comparisonType;
	private String instanceFeature;
	private String comparisonFeature;
	private ComparisonSpecification comparisonSpecification;
	private Similarity.SimType similarityFunction;
	private String outputFeature;



	@RunTime
	@CreoleParameter(defaultValue = "FIRST_OVERLAPPING", comment = "How to find the comparison annotation.")
	public void setComparisonSpecification(ComparisonSpecification comparisonSpecification) {
		this.comparisonSpecification = comparisonSpecification;
	}

	public ComparisonSpecification getComparisonSpecification() {
		return this.comparisonSpecification;
	}

	@RunTime
	@Optional
	@CreoleParameter
	public void setInstanceASName(String iasn) {
		this.instanceASName = iasn;
	}

	public String getInstanceASName() {
		return this.instanceASName;
	}

	@RunTime
	@Optional
	@CreoleParameter(defaultValue = "")
	public void setComparisonASName(String casn) {
		this.comparisonASName = casn;
	}

	public String getComparisonASName() {
		return this.comparisonASName;
	}

	@RunTime
	@Optional
	@CreoleParameter
	public void setInstanceType(String it) {
		this.instanceType = it;
	}

	public String getInstanceType() {
		return this.instanceType;
	}

	@RunTime
	@Optional
	@CreoleParameter(defaultValue = "")
	public void setComparisonType(String ct) {
		this.comparisonType = ct;
	}

	public String getComparisonType() {
		return this.comparisonType;
	}

	@RunTime
	@Optional
	@CreoleParameter
	public void setInstanceFeature(String inf) {
		this.instanceFeature = inf;
	}

	public String getInstanceFeature() {
		return this.instanceFeature;
	}

	@RunTime
	@Optional
	@CreoleParameter(defaultValue = "")
	public void setComparisonFeature(String cf) {
		this.comparisonFeature = cf;
	}

	public String getComparisonFeature() {
		return this.comparisonFeature;
	}

	public Similarity.SimType getSimilarityFunction() {
		return this.similarityFunction;
	}

	@CreoleParameter(defaultValue = "COSINE")
	@RunTime
	@Optional
	public void setSimilarityFunction(Similarity.SimType sf) {
		this.similarityFunction = sf;
	}

	@RunTime
	@Optional
	@CreoleParameter
	public void setOutputFeature(String ouf) {
		this.outputFeature = ouf;
	}

	public String getOutputFeature() {
		return this.outputFeature;
	}

	private SemanticModel semanticModel = null;

	@RunTime
	@CreoleParameter(comment = "The location of the semantic model if any.")
	public void setSaveDirectory(URL sd) {
		this.saveDirectory = sd;
	}

	public URL getSaveDirectory() {
		return this.saveDirectory;
	}

	@Override
	public Resource init() throws ResourceInstantiationException {
		return this;
	}

	@Override
	public void execute() throws ExecutionException {

		//Do this once only on the first document
		if(corpus.indexOf(document)==0) {
			if(semanticModel==null){
				semanticModel = SemanticModel.restoreModel(new File(saveDirectory.getFile()));
			}
			if(semanticModel==null){
				logger.warn("Semantic Modeling: No semantic model available!");
			}
		}

		if(semanticModel!=null){
			Document doc = getDocument();

			AnnotationSet instances = doc.getAnnotations(instanceASName).get(instanceType);
			AnnotationSet comparisons = doc.getAnnotations(comparisonASName).get(comparisonType);

			for(Annotation instance: instances){
				Annotation comparisonAnnotation = null;
				AnnotationSet tmpas = null;

				switch(this.getComparisonSpecification()){
				case FIRST_CONTAINED:
					comparisonAnnotation = Utils.getContainedAnnotations(comparisons, instance)
					.inDocumentOrder().iterator().next();
					break;
				case FIRST_OVERLAPPING:
					comparisonAnnotation = Utils.getOverlappingAnnotations(comparisons, instance)
					.inDocumentOrder().iterator().next();
					break;
				case FIRST_COVERING:
					comparisonAnnotation = Utils.getCoveringAnnotations(comparisons, instance)
					.inDocumentOrder().iterator().next();
					break;
				case LAST_CONTAINED:
					tmpas = Utils.getContainedAnnotations(comparisons, instance);
					comparisonAnnotation = tmpas.inDocumentOrder().get(tmpas.size()-1);
					break;
				case LAST_OVERLAPPING:
					tmpas = Utils.getOverlappingAnnotations(comparisons, instance);
					comparisonAnnotation = tmpas.inDocumentOrder().get(tmpas.size()-1);
					break;
				case LAST_COVERING:
					tmpas = Utils.getCoveringAnnotations(comparisons, instance);
					comparisonAnnotation = tmpas.inDocumentOrder().get(tmpas.size()-1);
					break;
				}


				String instanceString = instance.getFeatures().get(instanceFeature).toString();
				String comparisonString = comparisonAnnotation.getFeatures().get(comparisonFeature).toString();

				DoubleVector instanceVector = semanticModel.transform(instanceString);
				DoubleVector comparisonVector = semanticModel.transform(comparisonString);

				if(instanceVector!=null && comparisonVector!=null){
					Double sim = Similarity.getSimilarity(similarityFunction, instanceVector, comparisonVector);
					instance.getFeatures().put(this.outputFeature, sim);
				}
			}
		}
	}
}
