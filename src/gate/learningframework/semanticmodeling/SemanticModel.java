package gate.learningframework.semanticmodeling;

import edu.ucla.sspace.vector.DoubleVector;
import gate.creole.ResourceInstantiationException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Date;
import java.util.List;

public abstract class SemanticModel {
	
	private String semanticmodel;
	
	public static String info = "info";
	
	private File outputDirectory;
	
	private static String modelFileName = "my.model";
		
	public abstract ModelType whatIsIt();
	
	public abstract void add(String stringToAdd);
	
	public abstract DoubleVector transform(String toTransform);

	public abstract void conclude();
	
	private ModelType modeltype;
	
	public void writeInfo(){
		try {
			FileWriter fw = new FileWriter(new File(outputDirectory, info));
			fw.write(this.whatIsIt() + "\n" + new Date() + "\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Use the info file to correctly restore the model from the saved
	 * model directory.
	 * @param savedModelDirectoryFile
	 * @return
	 */
	public static SemanticModel restoreModel(File savedModelDirectoryFile){
		//Restore previously trained model
		File infofile = new File(savedModelDirectoryFile, SemanticModel.info);
		SemanticModel model = null;

		if(infofile.exists()){

			List<String> infostrings = null;
			try {
				infostrings = Files.readAllLines(
						infofile.toPath(), Charset.forName("UTF-8"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(infostrings!=null && infostrings.size()>2){
				String engine = infostrings.get(0).trim();
				ModelType modeltype = ModelType.valueOf(engine);
				switch(modeltype){
				case MALLET_LDA:
					//model = new SemanticModelMallet(savedModelDirectoryFile, modelFileName, true);
					break;
				case SEMSPACE_TFIDF:
					try {
						model = new SemanticModelSspace(savedModelDirectoryFile, modelFileName, true);
					} catch (ResourceInstantiationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
		} else {
			//No learner to restore
		}
		
		return model;
	}

	public String getInfo() {
		return info;
	}

	public void setOutputDirectory(File outputDirectory){
		this.outputDirectory = outputDirectory;
	}
	
	public File getOutputDirectory(){
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}
		return this.outputDirectory;
	}
	
	public String getModelFileName() {
		return modelFileName;
	}

	public void setModelFileName(String modelFileName) {
		this.modelFileName = modelFileName;
	}

	public String getSemanticModel() {
		return semanticmodel;
	}

	public void setSemanticModel(String semanticmodel) {
		this.semanticmodel = semanticmodel;
	}

	public ModelType getModelType() {
		return modeltype;
	}

	public void setModelType(ModelType modeltype) {
		this.modeltype = modeltype;
	}
}
