/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.tests;

import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.FeatureVector;
import cc.mallet.types.Instance;
import gate.Annotation;
import gate.Document;
import gate.Factory;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.features.Attribute;
import gate.plugin.learningframework.features.FeatureExtraction;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import static gate.plugin.learningframework.tests.Utils.*;
import gate.util.GateException;
import java.util.List;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;


/**
 * Tests for the FeatureSpecification parsing and creation of FeatureInfo.
 * 
 * @author Johann Petrak
 */
public class TestFeatureExtraction {
  
  private static final String specAttrNoFeature = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
  private static final String specAttrNominalAsNum = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><CODEAS>number</CODEAS></ATTRIBUTE>"+
            "</ROOT>";
  private static final String specAttrNumeric = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
  
  
  @BeforeClass
  public static void setup() throws ResourceInstantiationException, GateException {
    Gate.init();
  }
  
  private Document doc;
  
  @Before
  public void before() throws ResourceInstantiationException {
    doc = newDocument();
  }
  
  @After
  public void after() {
    if(doc != null)
      Factory.deleteResource(doc);
  }
  
  @Test
  public void extractNominal1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature2</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>numfeature1</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>numfeature2</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>boolfeature1</FEATURE><DATATYPE>boolean</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>boolfeature2</FEATURE><DATATYPE>bool</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE></ATTRIBUTE>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<Attribute> as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(7,as.size());

    Alphabet a = new Alphabet();
    AugmentableFeatureVector afv = new AugmentableFeatureVector(a);
    Instance inst = new Instance(afv,null,null,null);
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 0, "theType", gate.Utils.featureMap("theFeature","value1"));
    instAnn.getFeatures().put("feature2", "valOfFeature2");
    instAnn.getFeatures().put("numfeature1", 1.1);
    instAnn.getFeatures().put("numfeature2", "2.2");
    instAnn.getFeatures().put("boolfeature1", true);
    instAnn.getFeatures().put("boolfeature2", 3.3);
    
    // 1) the following all specify the same instance annotation type as is specified in the 
    // attribute so the instance annotation should directly get used.
    
    FeatureExtraction.extractFeature(inst, as.get(0), "theType", instAnn, doc);
    assertEquals(1,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:theFeature=value1"));
    assertEquals(1,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType:theFeature=value1"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(1), "theType", instAnn, doc);
    assertEquals(2,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:feature2=valOfFeature2"));
    assertEquals(2,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType:feature2=valOfFeature2"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(2), "theType", instAnn, doc);
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:numfeature1"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.1,((FeatureVector)inst.getData()).value("theType:numfeature1"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(3), "theType", instAnn, doc);
    assertEquals(4,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:numfeature2"));
    assertEquals(4,((FeatureVector)inst.getData()).numLocations());
    assertEquals(2.2,((FeatureVector)inst.getData()).value("theType:numfeature2"),EPS);
    
    FeatureExtraction.extractFeature(inst, as.get(4), "theType", instAnn, doc);
    assertEquals(5,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:boolfeature1"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType:boolfeature1"),EPS);
    
    FeatureExtraction.extractFeature(inst, as.get(5), "theType", instAnn, doc);
    assertEquals(6,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:boolfeature2"));
    assertEquals(6,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType:boolfeature2"),EPS);
    
    FeatureExtraction.extractFeature(inst, as.get(6), "theType", instAnn, doc);
    assertEquals(7,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("theType:boolfeature2"));
    assertEquals(7,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("theType::ISPRESENT"),EPS);
  }  
 
}
