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
import gate.plugin.learningframework.features.SimpleAttribute;
import static gate.plugin.learningframework.tests.Utils.*;
import gate.util.GateException;
import gate.util.GateRuntimeException;
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
  public void extractSimple1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature2</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>numfeature1</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>numfeature2</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>boolfeature1</FEATURE><DATATYPE>boolean</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>boolfeature2</FEATURE><DATATYPE>bool</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>missing2nominal</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>missing3nominal</FEATURE><DATATYPE>nominal</DATATYPE><CODEAS>number</CODEAS></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>missing1bool</FEATURE><DATATYPE>bool</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>missing3numeric</FEATURE><DATATYPE>numeric</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<Attribute> as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(11,as.size());

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
    
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    assertEquals(1,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:theFeature=value1"));
    assertEquals(1,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:theFeature=value1"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(1), doc.getAnnotations(), instAnn);
    assertEquals(2,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:feature2=valOfFeature2"));
    assertEquals(2,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:feature2=valOfFeature2"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(2), doc.getAnnotations(), instAnn);
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:numfeature1"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.1,((FeatureVector)inst.getData()).value("A:theType:numfeature1"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(3), doc.getAnnotations(), instAnn);
    assertEquals(4,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:numfeature2"));
    assertEquals(4,((FeatureVector)inst.getData()).numLocations());
    assertEquals(2.2,((FeatureVector)inst.getData()).value("A:theType:numfeature2"),EPS);
    
    FeatureExtraction.extractFeature(inst, as.get(4), doc.getAnnotations(), instAnn);
    assertEquals(5,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:boolfeature1"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:boolfeature1"),EPS);
    
    FeatureExtraction.extractFeature(inst, as.get(5), doc.getAnnotations(), instAnn);
    assertEquals(6,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:boolfeature2"));
    assertEquals(6,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:boolfeature2"),EPS);
    
    FeatureExtraction.extractFeature(inst, as.get(6), doc.getAnnotations(), instAnn);
    assertEquals(7,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:boolfeature2"));
    assertEquals(7,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:=ISPRESENT"),EPS);
    
    // 2) check the kind of missing value we get by default
    
    // for a nominal, with the default one_of_k coding, and mvt "special value" 
    // a new special value is added
    FeatureExtraction.extractFeature(inst, as.get(7), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(7)+" FV="+inst.getData());
    assertEquals(8,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:missing2nominal=%%%NA%%%"));
    assertEquals(8,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:missing2nominal=%%%NA%%%"),EPS);
    
    // for a nominal coded as number, we should the special value -1
    FeatureExtraction.extractFeature(inst, as.get(8), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(8)+" FV="+inst.getData());
    assertEquals(9,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:missing3nominal"));
    assertEquals(9,((FeatureVector)inst.getData()).numLocations());
    assertEquals(-1.0,((FeatureVector)inst.getData()).value("A:theType:missing3nominal"),EPS);
    
    // for a boolean we should get 0.5
    FeatureExtraction.extractFeature(inst, as.get(9), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(9)+" FV="+inst.getData());
    assertEquals(10,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:missing1bool"));
    assertEquals(10,((FeatureVector)inst.getData()).numLocations());
    assertEquals(0.5,((FeatureVector)inst.getData()).value("A:theType:missing1bool"),EPS);

    FeatureExtraction.extractFeature(inst, as.get(10), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(10)+" FV="+inst.getData());
    assertEquals(11,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:missing3numeric"));
    assertEquals(11,((FeatureVector)inst.getData()).numLocations());
    assertEquals(-1.0,((FeatureVector)inst.getData()).value("A:theType:missing3numeric"),EPS);
    
    // 3) it does not matter where the attribute comes from, we can just as well get it from 
    // a different specification.
    // Test this, than do it once more, but after locking the alphabet and check if the new
    // feature is indeed ignored!
    
    spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>nomFeat1</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>nomFeat2</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>nomFeat3</FEATURE><DATATYPE>nominal</DATATYPE><CODEAS>number</CODEAS></ATTRIBUTE>"+
            "</ROOT>";
    instAnn.getFeatures().put("nomFeat1", 7.7);
    instAnn.getFeatures().put("nomFeat2", "xxxx");
    instAnn.getFeatures().put("nomFeat3", "xxxx");
    
    List<Attribute> as2 = new FeatureSpecification(spec).getFeatureInfo().getAttributes();

    FeatureExtraction.extractFeature(inst, as2.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as2.get(0)+" FV="+inst.getData());
    assertEquals(12,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:nomFeat1=7.7"));
    assertEquals(12,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:nomFeat1=7.7"),EPS);
    
    inst.getAlphabet().stopGrowth();
    as2.get(1).stopGrowth();
    FeatureExtraction.extractFeature(inst, as2.get(1), doc.getAnnotations(), instAnn);
    System.err.println("After "+as2.get(1)+" FV="+inst.getData());
    assertEquals(12,inst.getAlphabet().size());
    //assertTrue(inst.getAlphabet().contains("theType:nomFeat1=7.7"));
    assertEquals(12,((FeatureVector)inst.getData()).numLocations());
    //assertEquals(1.0,((FeatureVector)inst.getData()).value("theType:nomFeat1=7.7"),EPS);
    
    // unlock and try again
    inst.getAlphabet().startGrowth();
    as2.get(1).startGrowth();
    FeatureExtraction.extractFeature(inst, as2.get(1), doc.getAnnotations(), instAnn);
    System.err.println("After "+as2.get(1)+" unlocked FV="+inst.getData());
    assertEquals(13,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:nomFeat2=xxxx"));
    assertEquals(13,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:nomFeat2=xxxx"),EPS);

    //nominal feature coded as number
    FeatureExtraction.extractFeature(inst, as2.get(2), doc.getAnnotations(), instAnn);
    System.err.println("After "+as2.get(2)+" unlocked FV="+inst.getData());
    assertEquals(14,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:nomFeat3"));
    assertEquals(14,((FeatureVector)inst.getData()).numLocations());
    assertEquals(0.0,((FeatureVector)inst.getData()).value("A:theType:nomFeat3"),EPS);
    
    // add another annotation, so we get another value for nomFeat3. The number of 
    // entries in the feature name alphabet and the feature vector has to remain the same!
    instAnn = addAnn(doc, "", 2, 2, "theType", gate.Utils.featureMap("nomFeat3","yyyy"));
    FeatureExtraction.extractFeature(inst, as2.get(2), doc.getAnnotations(), instAnn);
    System.err.println("After "+as2.get(2)+" unlocked FV="+inst.getData());
    assertEquals(14,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("A:theType:nomFeat3"));
    assertEquals(14,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("A:theType:nomFeat3"),EPS);
    
    // check if we do have the proper value alphabet
    assertTrue(as2.get(2) instanceof SimpleAttribute);
    SimpleAttribute att2 = (SimpleAttribute)as2.get(2);
    Alphabet att2a = att2.alphabet;
    System.err.println("Alphabet for nomFeat3="+att2a);
    assertEquals(2,att2a.size());
    assertEquals(0,att2a.lookupIndex("xxxx"));
    assertEquals(1,att2a.lookupIndex("yyyy"));
  }  

  @Test(expected = GateRuntimeException.class)
  public void extractSimple2() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature1</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    List<Attribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 10, "instanceType", gate.Utils.featureMap());
    Annotation tok1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","f1v1"));
    tok1.getFeatures().put("feature2", "valOfFeature2");
    Annotation tok2 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","f1v2"));
    tok2.getFeatures().put("feature2", "valOfFeature2B");
    
    // We do not allow more than one overlapping annotation of the given type for ATTRIBUTE
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
  }  

  @Test
  public void extractSimple3() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><FEATURE>feature1</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    List<Attribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 10, "instanceType", gate.Utils.featureMap());
    Annotation tok1 = addAnn(doc, "", 0, 5, "theType", gate.Utils.featureMap("feature1","f1v1"));
    tok1.getFeatures().put("feature2", "valOfFeature2");
    
    // We do not allow more than one overlapping annotation of the given type for ATTRIBUTE
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (overlapping) FV="+inst.getData());
  }  


  
  @Test
  public void extractNgram1() {
    String spec = "<ROOT>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>1</NUMBER></NGRAM>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>2</NUMBER></NGRAM>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>3</NUMBER></NGRAM>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<Attribute> as = fi.getAttributes();

    Alphabet a = new Alphabet();
    AugmentableFeatureVector afv = new AugmentableFeatureVector(a);
    Instance inst = new Instance(afv,null,null,null);
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 20, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (one-grams) FV="+inst.getData());
    assertEquals(5,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("N1:theType:theFeature=tok1"));
    assertTrue(inst.getAlphabet().contains("N1:theType:theFeature=tok2"));
    assertTrue(inst.getAlphabet().contains("N1:theType:theFeature=tok3"));
    assertTrue(inst.getAlphabet().contains("N1:theType:theFeature=tok4"));
    assertTrue(inst.getAlphabet().contains("N1:theType:theFeature=tok5"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N1:theType:theFeature=tok1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N1:theType:theFeature=tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N1:theType:theFeature=tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N1:theType:theFeature=tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N1:theType:theFeature=tok5"),EPS);
    
    // now the bigrams
    inst = newInstance();
    FeatureExtraction.extractFeature(inst, as.get(1), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(1)+" (bi-grams) FV="+inst.getData());
    assertEquals(4,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("N2:theType:theFeature=tok1_tok2"));
    assertTrue(inst.getAlphabet().contains("N2:theType:theFeature=tok2_tok3"));
    assertTrue(inst.getAlphabet().contains("N2:theType:theFeature=tok3_tok4"));
    assertTrue(inst.getAlphabet().contains("N2:theType:theFeature=tok4_tok5"));
    assertEquals(4,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N2:theType:theFeature=tok1_tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N2:theType:theFeature=tok2_tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N2:theType:theFeature=tok3_tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N2:theType:theFeature=tok4_tok5"),EPS);

    // and the 3-grams
    inst = newInstance();
    FeatureExtraction.extractFeature(inst, as.get(2), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(2)+" (bi-grams) FV="+inst.getData());
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("N3:theType:theFeature=tok1_tok2_tok3"));
    assertTrue(inst.getAlphabet().contains("N3:theType:theFeature=tok2_tok3_tok4"));
    assertTrue(inst.getAlphabet().contains("N3:theType:theFeature=tok3_tok4_tok5"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N3:theType:theFeature=tok1_tok2_tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N3:theType:theFeature=tok2_tok3_tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("N3:theType:theFeature=tok3_tok4_tok5"),EPS);
  }

  @Test
  public void extractNgram2() {
    // essentially the same as extractNgram1 but explicitly specifies the name to use as internal
    // feature name
    String spec = "<ROOT>"+
            "<NGRAM><NAME>ng1</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>1</NUMBER></NGRAM>"+
            "<NGRAM><NAME>ngram2</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>2</NUMBER></NGRAM>"+
            "<NGRAM><NAME>someName</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>3</NUMBER></NGRAM>"+
            "</ROOT>";
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    List<Attribute> as = fi.getAttributes();
    System.err.println("NGRAMS with explicitly specified name!!");
    Alphabet a = new Alphabet();
    AugmentableFeatureVector afv = new AugmentableFeatureVector(a);
    Instance inst = new Instance(afv,null,null,null);
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 0, 20, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (one-grams) FV="+inst.getData());
    assertEquals(5,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("ng1=tok1"));
    assertTrue(inst.getAlphabet().contains("ng1=tok2"));
    assertTrue(inst.getAlphabet().contains("ng1=tok3"));
    assertTrue(inst.getAlphabet().contains("ng1=tok4"));
    assertTrue(inst.getAlphabet().contains("ng1=tok5"));
    assertEquals(5,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1=tok1"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1=tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1=tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1=tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ng1=tok5"),EPS);
    
    // now the bigrams
    inst = newInstance();
    FeatureExtraction.extractFeature(inst, as.get(1), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(1)+" (bi-grams) FV="+inst.getData());
    assertEquals(4,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("ngram2=tok1_tok2"));
    assertTrue(inst.getAlphabet().contains("ngram2=tok2_tok3"));
    assertTrue(inst.getAlphabet().contains("ngram2=tok3_tok4"));
    assertTrue(inst.getAlphabet().contains("ngram2=tok4_tok5"));
    assertEquals(4,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2=tok1_tok2"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2=tok2_tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2=tok3_tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("ngram2=tok4_tok5"),EPS);

    // and the 3-grams
    inst = newInstance();
    FeatureExtraction.extractFeature(inst, as.get(2), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(2)+" (bi-grams) FV="+inst.getData());
    assertEquals(3,inst.getAlphabet().size());
    assertTrue(inst.getAlphabet().contains("someName=tok1_tok2_tok3"));
    assertTrue(inst.getAlphabet().contains("someName=tok2_tok3_tok4"));
    assertTrue(inst.getAlphabet().contains("someName=tok3_tok4_tok5"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("someName=tok1_tok2_tok3"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("someName=tok2_tok3_tok4"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("someName=tok3_tok4_tok5"),EPS);
  }

  
  @Test
  public void extractList1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTELIST><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-1</FROM><TO>1</TO></ATTRIBUTELIST>"+
            "</ROOT>";
    List<Attribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 10, 10, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    addAnn(doc,"",10,12,"theType",gate.Utils.featureMap("theFeature","tok6"));
    addAnn(doc,"",12,14,"theType",gate.Utils.featureMap("theFeature","tok7"));
    addAnn(doc,"",14,16,"theType",gate.Utils.featureMap("theFeature","tok8"));
    addAnn(doc,"",16,18,"theType",gate.Utils.featureMap("theFeature","tok9"));
    addAnn(doc,"",18,20,"theType",gate.Utils.featureMap("theFeature","tok10"));
    
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (list -1to1) FV="+inst.getData());
    assertEquals(3,inst.getAlphabet().size());
    System.err.println("Alphabet is "+inst.getAlphabet());
    assertTrue(inst.getAlphabet().contains("L-1:theType:theFeature=tok5"));
    assertTrue(inst.getAlphabet().contains("L0:theType:theFeature=tok6"));
    assertTrue(inst.getAlphabet().contains("L1:theType:theFeature=tok7"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("L-1:theType:theFeature=tok5"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("L0:theType:theFeature=tok6"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("L1:theType:theFeature=tok7"),EPS);
  }
  
  @Test
  public void extractList2() {
    // same as extractList2, but with explicitly specified name
    String spec = "<ROOT>"+
            "<ATTRIBUTELIST><NAME>myAttList</NAME><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-1</FROM><TO>1</TO></ATTRIBUTELIST>"+
            "</ROOT>";
    List<Attribute> as = new FeatureSpecification(spec).getFeatureInfo().getAttributes();
    Instance inst = newInstance();
    
    // prepare the document
    Annotation instAnn = addAnn(doc, "", 10, 10, "instanceType", gate.Utils.featureMap());
    addAnn(doc,"",0,2,"theType",gate.Utils.featureMap("theFeature","tok1"));
    addAnn(doc,"",2,4,"theType",gate.Utils.featureMap("theFeature","tok2"));
    addAnn(doc,"",4,6,"theType",gate.Utils.featureMap("theFeature","tok3"));
    addAnn(doc,"",6,8,"theType",gate.Utils.featureMap("theFeature","tok4"));
    addAnn(doc,"",8,10,"theType",gate.Utils.featureMap("theFeature","tok5"));
    addAnn(doc,"",10,12,"theType",gate.Utils.featureMap("theFeature","tok6"));
    addAnn(doc,"",12,14,"theType",gate.Utils.featureMap("theFeature","tok7"));
    addAnn(doc,"",14,16,"theType",gate.Utils.featureMap("theFeature","tok8"));
    addAnn(doc,"",16,18,"theType",gate.Utils.featureMap("theFeature","tok9"));
    addAnn(doc,"",18,20,"theType",gate.Utils.featureMap("theFeature","tok10"));
    
    FeatureExtraction.extractFeature(inst, as.get(0), doc.getAnnotations(), instAnn);
    System.err.println("After "+as.get(0)+" (list -1to1) FV="+inst.getData());
    assertEquals(3,inst.getAlphabet().size());
    System.err.println("Alphabet is "+inst.getAlphabet());
    assertTrue(inst.getAlphabet().contains("myAttList-1=tok5"));
    assertTrue(inst.getAlphabet().contains("myAttList0=tok6"));
    assertTrue(inst.getAlphabet().contains("myAttList1=tok7"));
    assertEquals(3,((FeatureVector)inst.getData()).numLocations());
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList-1=tok5"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList0=tok6"),EPS);
    assertEquals(1.0,((FeatureVector)inst.getData()).value("myAttList1=tok7"),EPS);
  }
 
}
