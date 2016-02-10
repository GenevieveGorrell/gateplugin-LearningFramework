
package gate.plugin.learningframework.tests;

import gate.AnnotationSet;
import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.engines.Algorithm;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.data.CorpusRepresentationMallet;
import gate.plugin.learningframework.data.CorpusRepresentationMalletClass;
import gate.plugin.learningframework.engines.AlgorithmClassification;
import gate.plugin.learningframework.engines.Engine;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import gate.plugin.learningframework.features.TargetType;
import gate.util.GateException;
import java.io.File;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import static gate.plugin.learningframework.tests.Utils.*;
import java.net.MalformedURLException;

/**
 *
 * @author Johann Petrak
 */
public class TestEngineMalletClass {

  @BeforeClass
  public static void init() throws GateException {
    gate.Gate.init();
    // load the plugin
    gate.Utils.loadPlugin(new File("../LearningFramework"));
  }
  
  @Test
  public void testCreateEngine() throws MalformedURLException, ResourceInstantiationException {
    gate.Utils.loadPlugin(new File("../LearningFramework"));
    File configFile = new File("tests/cl-ionosphere/feats.xml");
    FeatureSpecification spec = new FeatureSpecification(configFile);
    FeatureInfo featureInfo = spec.getFeatureInfo();
    CorpusRepresentationMalletClass crm = new CorpusRepresentationMalletClass(featureInfo, ScalingMethod.NONE);
    Engine engine = Engine.createEngine(AlgorithmClassification.MALLET_CL_C45, "", crm);
    System.err.println("TESTS: have engine "+engine);
    
    // load a document and train the model
    Document doc = loadDocument(new File("tests/cl-ionosphere/ionosphere_gate.xml"));
    System.err.println("TESTS: have document");
    
    AnnotationSet instanceAS = doc.getAnnotations().get("Mention");
    AnnotationSet sequenceAS = null;
    AnnotationSet inputAS = doc.getAnnotations();
    AnnotationSet classAS = null;
    String targetFeature = "class";
    String nameFeature = null;
    crm.add(instanceAS, sequenceAS, inputAS, classAS, targetFeature, TargetType.NOMINAL, nameFeature);
    System.err.println("TESTS: added instances, number of instances now: "+crm.getRepresentationMallet().size());
    engine.trainModel("");
    System.err.println("TESTS: model trained");
    System.err.println("TESTS: engine before saving: "+engine);
    engine.saveEngine(new File("."));
    
    // Now check if we can restore the engine and thus the corpus representation
    Engine engine2 = Engine.loadEngine(new File("."), "");
    System.err.println("RESTORED engine is "+engine2);
    
  }
  
  // NOTE: other test annotations
  // @Test(expected = Exception.class) - fill if Exception not thrown
  // @Test(timeout = 100) - fail if method takes longer than 100 ms
  // @Before - execute before each test 
  // @BeforeClass - execute once
  // @After - execute after each test
  // @AfterClass
  // see http://www.vogella.com/tutorials/JUnit/article.html
  // http://www.javacodegeeks.com/2014/11/junit-tutorial-unit-testing.html
  // 
}
