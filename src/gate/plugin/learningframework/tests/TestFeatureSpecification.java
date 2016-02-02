/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.tests;

import gate.Document;
import gate.Gate;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.features.Attribute;
import gate.plugin.learningframework.features.FeatureInfo;
import gate.plugin.learningframework.features.FeatureSpecification;
import static gate.plugin.learningframework.tests.Utils.*;
import gate.util.GateException;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;


/**
 * Tests for the FeatureSpecification parsing and creation of FeatureInfo.
 * 
 * @author Johann Petrak
 */
public class TestFeatureSpecification {
  
  @Test
  public void basicSpecParsing1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE></ATTRIBUTE>"+
            "</ROOT>";
    FeatureSpecification fs;    
    FeatureInfo fi;
    List<Attribute> as;
    fs = new FeatureSpecification(spec);
    fi = fs.getFeatureInfo();
    as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(1,as.size());
    assertEquals("SimpleAttribute(type=theType,feature=,datatype=bool,missingvaluetreatment=special_value,codeas=one_of_k",as.get(0).toString());
    
    spec = "<ROOT>"+
            "<ATTRIBUTELIST><TYPE>theType</TYPE><FEATURE>string</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-2</FROM><TO>1</TO></ATTRIBUTELIST>"+
            "</ROOT>";    
    fs = new FeatureSpecification(spec);
    fi = fs.getFeatureInfo();
    as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(1,as.size());
    assertEquals("AttributeList(type=theType,feature=string,datatype=nominal,missingvaluetreatment=special_value,codeas=one_of_k,from=-2,to=1",as.get(0).toString());

    spec = "<ROOT>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>3</NUMBER></NGRAM>"+
            "</ROOT>";    
    fs = new FeatureSpecification(spec);
    fi = fs.getFeatureInfo();
    as = fi.getAttributes();
    assertNotNull(as);
    assertEquals(1,as.size());
    assertEquals("NgramAttribute(type=theType,feature=theFeature,number=3",as.get(0).toString());
    
    // make sure that the feature info object we get from the feature specification is a clone
    FeatureInfo fi2 = fs.getFeatureInfo();
    assertFalse(fi == fi2);
    
  }  
 
}
