package gate.learningframework.semanticmodeling;

import edu.ucla.sspace.common.DocumentVectorBuilder;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vsm.VectorSpaceModel;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.common.SemanticSpaceWriter;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.util.SemanticSpaceMatrix;
import edu.ucla.sspace.lsa.LatentSemanticAnalysis;
import edu.ucla.sspace.ri.RandomIndexing;
import edu.ucla.sspace.matrix.SVD.Algorithm;
import gate.creole.ResourceInstantiationException;
import gate.learningframework.corpora.CorpusWriterSspace;
import gate.util.Benchmark;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class SemanticModelSspace extends SemanticModel {

	private SemanticSpace sspace = null;
	
	private DocumentVectorBuilder vectorBuilder;
	
	private Properties config = new Properties();
	
	public SemanticModelSspace(File savedModelDirectoryFile, String modelFileName, boolean restore) 
			throws ResourceInstantiationException {
		
		this.setOutputDirectory(savedModelDirectoryFile);

		this.setModelFileName(modelFileName);
		
		// TODO Restore the properties file too
		
		if(restore){
			File sspacefile = new File(this.getOutputDirectory(), this.getModelFileName());
			if(sspacefile.exists()){
				try {
					sspace = SemanticSpaceIO.load(sspacefile);
				} catch (Exception e) {
					throw new ResourceInstantiationException("Could not load semantic space file " + sspacefile, e);
				}
			}
			vectorBuilder = new DocumentVectorBuilder(sspace, config);
		} else {
			try {
				sspace = new VectorSpaceModel();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}


	public void add(String stringToAdd){
		try {
			sspace.processDocument(new BufferedReader(new StringReader(stringToAdd)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void conclude(){
		config.put(DocumentVectorBuilder.USE_TERM_FREQUENCIES_PROPERTY, true);
		//config.put(VectorSpaceModel.MATRIX_TRANSFORM_PROPERTY,
		//          "edu.ucla.sspace.matrix.TfIdfDocStripedTransform");
		sspace.processSpace(config);
		vectorBuilder = new DocumentVectorBuilder(sspace, config);
		File outputFile = new File(this.getOutputDirectory(), this.getModelFileName());
		try {
			SemanticSpaceIO.save(sspace, outputFile, SemanticSpaceIO.SSpaceFormat.SPARSE_BINARY);
			writeInfo();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public DoubleVector transform(String toTransform){
		return vectorBuilder.buildVector(
				new BufferedReader(new StringReader(toTransform)),
				new DenseVector(this.sspace.getVectorLength()));
	}

	public ModelType whatIsIt(){
		return ModelType.valueOf("SEMSPACE_TFIDF");
	}
}
