/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.tests;

import cc.mallet.types.Alphabet;
import cc.mallet.types.AugmentableFeatureVector;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import gate.Annotation;
import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.Mode;
import gate.plugin.learningframework.ScalingMethod;
import gate.plugin.learningframework.corpora.CorpusRepresentationMallet;
import gate.plugin.learningframework.corpora.CorpusWriterMallet;
import gate.plugin.learningframework.features.Attribute;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import gate.plugin.learningframework.features.TargetType;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.*;
import static gate.plugin.learningframework.tests.Utils.*;
import java.io.File;

/**
 * Temporary tester class for comparing old and new code.
 * This tries to make sure that the new code produces the same results as the old code.
 * @author Johann Petrak
 */
public class TestNewVersusOld {

  /**
   * Check that the instances we create are the same.
   * Note: since we have changed the way feature names are generated and also the order in 
   * which features get created, we only check a few things fully here, the rest gets printed
   * for manual inspection!
   */
  @Test
  public void testInstances1() throws ResourceInstantiationException {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>type1</TYPE><FEATURE>feat</FEATURE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "<NGRAM><TYPE>type2</TYPE><FEATURE>feat</FEATURE><NUMBER>2</NUMBER></NGRAM>"+
            "</ROOT>";
    
    Document doc = newDocument();
    Annotation instAnn1 = addAnn(doc, "",  0, 10, "type1", gate.Utils.featureMap("feat","value1"));
    // 2 contained annotations for the ngrams
    addAnn(doc,"",2,4,"type2",gate.Utils.featureMap("feat","ngram1"));
    addAnn(doc,"",6,8,"type2",gate.Utils.featureMap("feat","ngram2"));
    
    Annotation instAnn2 = addAnn(doc, "", 20, 40, "type1", gate.Utils.featureMap("feat","value2"));
    // 3 contained annotations for the ngrams
    addAnn(doc,"",22,24,"type2",gate.Utils.featureMap("feat","ngram3"));
    addAnn(doc,"",26,28,"type2",gate.Utils.featureMap("feat","ngram4"));
    addAnn(doc,"",30,32,"type2",gate.Utils.featureMap("feat","ngram5"));
    
    // 1) Create the instances the old way.
    gate.plugin.learningframework.corpora.FeatureSpecification fs_old = new gate.plugin.learningframework.corpora.FeatureSpecification(spec);
    CorpusWriterMallet cwm = new CorpusWriterMallet(
            fs_old, 
            "type1",  // instance annotation type?
            "",   // input annotation set name?
            new File("."), // output directory
            Mode.CLASSIFICATION, 
            "type1",  // class type name
            "feat",  // class feature name
            null,  // identifier feature name
            ScalingMethod.NONE);
    cwm.add(doc);
    InstanceList instances_old = cwm.getInstances();
    assertNotNull(instances_old);
    assertEquals(2,instances_old.size());
    System.err.println("INSTANCE OLD 1 data="+instances_old.get(0).getData());
    System.err.println("INSTANCE OLD 2 data="+instances_old.get(1).getData());
    System.err.println("INSTANCE OLD 1 target="+instances_old.get(0).getTarget());
    System.err.println("INSTANCE OLD 2 target="+instances_old.get(1).getTarget());
    
    // 2) Create the instances the new way.
    FeatureInfo fi = new FeatureSpecification(spec).getFeatureInfo();
    CorpusRepresentationMallet crm = new CorpusRepresentationMallet(fi, ScalingMethod.NONE);
    crm.add(
            doc.getAnnotations().get("type1"), 
            null, 
            doc.getAnnotations(),
            null, 
            "feat", 
            TargetType.NOMINAL, 
            null);
    InstanceList instances_new = crm.getInstances();
    assertNotNull(instances_new);
    assertEquals(2,instances_new.size());
    System.err.println("INSTANCE NEW 1 data="+instances_new.get(0).getData());
    System.err.println("INSTANCE NEW 2 data="+instances_new.get(1).getData());
    System.err.println("INSTANCE NEW 1 target="+instances_new.get(0).getTarget());
    System.err.println("INSTANCE NEW 2 target="+instances_new.get(1).getTarget());
    
  }
}