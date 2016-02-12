
package gate.plugin.learningframework.tests;

import cc.mallet.pipe.Pipe;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.GateClassification;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.data.CorpusRepresentationMallet;
import gate.plugin.learningframework.data.CorpusRepresentationMalletClass;
import gate.plugin.learningframework.data.CorpusRepresentationMalletSeq;
import gate.plugin.learningframework.engines.AlgorithmClassification;
import gate.plugin.learningframework.engines.Engine;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import gate.plugin.learningframework.features.TargetType;
import gate.plugin.learningframework.mallet.LFPipe;
import gate.util.GateException;
import java.io.File;
import org.junit.Test;
import org.junit.BeforeClass;
import static gate.plugin.learningframework.tests.Utils.*;
import java.io.FileFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import static org.junit.Assert.*;

/**
 *
 * @author Johann Petrak
 */
public class TestEngineMalletSeq {

  @BeforeClass
  public static void init() throws GateException {
    gate.Gate.init();
    // load the plugin
    gate.Utils.loadPlugin(new File("../LearningFramework"));
  }
  
  @Test
  public void testEngineMalletSeq1() throws MalformedURLException, ResourceInstantiationException, IOException {
    gate.Utils.loadPlugin(new File("../LearningFramework"));
    File configFile = new File("testing/sequence-features.xml");
    FeatureSpecification spec = new FeatureSpecification(configFile);
    FeatureInfo featureInfo = spec.getFeatureInfo();
    CorpusRepresentationMalletSeq crm = new CorpusRepresentationMalletSeq(featureInfo, ScalingMethod.NONE);
    Engine engine = Engine.createEngine(AlgorithmClassification.MALLET_SEQ_CRF, "", crm);
    System.err.println("TESTS: have engine "+engine);
    
    // for this we need to go through a number of documents and train on all of them
    // The directory testing/trainingset_prepared contains the prepared documents in XML format
    // They have:
    // class annotations Mention
    // instance annotations Token
    // sequence annotations Sentence
    
    // To do this efficiently we create a corpus and populate it from the directory
    Corpus corpus = (Corpus)Factory.createResource("gate.corpora.CorpusImpl");
    corpus.populate(new File("testing/trainingset_prepared").toURI().toURL(), 
            new gate.util.ExtensionFileFilter("","xml"), "UTF-8", true);
    
    for(Document doc : corpus) {
      System.err.println("Processing document "+doc.getName());
      AnnotationSet instanceAS = doc.getAnnotations().get("Token");
      AnnotationSet sequenceAS = doc.getAnnotations().get("Sentence");
      AnnotationSet inputAS = doc.getAnnotations();
      AnnotationSet classAS = doc.getAnnotations().get("Mention");
      String targetFeature = null;
      String nameFeature = null;
      crm.add(instanceAS, sequenceAS, inputAS, classAS, targetFeature, TargetType.NOMINAL, nameFeature);
    }
    
    System.err.println("TESTS: added instances, number of instances now: "+crm.getRepresentationMallet().size());
    
    engine.trainModel("");
    System.err.println("TESTS: model trained");
    System.err.println("TESTS: engine before saving: "+engine);
    engine.saveEngine(new File("."));
    
    // Now check if we can restore the engine and thus the corpus representation
    Engine engine2 = Engine.loadEngine(new File("."), "");
    System.err.println("RESTORED engine is "+engine2);
    
    // check if the corpusRepresentation has been restored correctly
    CorpusRepresentationMallet crm2 = engine2.getCorpusRepresentationMallet();
    assertNotNull(crm2);
    assertTrue(crm2 instanceof CorpusRepresentationMalletSeq);
    CorpusRepresentationMalletSeq crmc2 = (CorpusRepresentationMalletSeq)crm2;
    Pipe pipe = crmc2.getPipe();
    assertNotNull(pipe);
    assertTrue(pipe instanceof LFPipe);
    LFPipe lfpipe = (LFPipe)pipe;
    FeatureInfo fi = lfpipe.getFeatureInfo();
    assertNotNull(fi);
    
    // For the application, first remove the class annotations from the default set. This is 
    // not strictly necessary but just so we are sure no cheating is possible
    for(Document doc : corpus) {
      doc.getAnnotations().removeAll(doc.getAnnotations().get("Mention"));
    }
    
    // now go through all the documents and create Mention annotations in the LF set
    for(Document doc : corpus) {
      
    }
    
    /*
    AnnotationSet lfAS = doc.getAnnotations("LF");
    String parms = "";
    List<GateClassification> gcs = engine2.classify(instanceAS, inputAS, sequenceAS, parms);
    System.err.println("Number of classifications: "+gcs.size());
    GateClassification.applyClassification(doc, gcs, "target", lfAS);
    
    System.err.println("Original instances: "+instanceAS.size()+", classification: "+lfAS.size());
    
    // quick and dirty evaluation: go through all the original annotations, get the 
    // co-extensive annotations from LF, and compare the values from the "class" feature
    int total = 0;
    int correct = 0;
    for(Annotation orig : instanceAS) {
      total++;
      Annotation lf = gate.Utils.getOnlyAnn(gate.Utils.getCoextensiveAnnotations(lfAS, orig));
      //System.err.println("ORIG="+orig+", lf="+lf);
      if(orig.getFeatures().get("class").equals(lf.getFeatures().get("target"))) {
        correct++;
      }
    }
    
    double acc = (double)correct / (double)total;
    System.err.println("Got total="+total+", correct="+correct+", acc="+acc);
    assertEquals(0.9630, acc, 0.01);
    */
  }
  
}
