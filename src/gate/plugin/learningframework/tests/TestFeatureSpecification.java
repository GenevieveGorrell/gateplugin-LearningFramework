/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.tests;

import gate.Document;
import gate.creole.ResourceInstantiationException;
import gate.plugin.learningframework.features.FeatureSpecification;
import static gate.plugin.learningframework.tests.Utils.*;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;


/**
 * Tests for the FeatureSpecification parsing and creation of FeatureInfo.
 * 
 * @author Johann Petrak
 */
public class TestFeatureSpecification {
  
  Document doc;
  
  @BeforeClass
  public void setup() throws ResourceInstantiationException {
    doc = newDocument();
  }
  
  @Test
  public void basicSpecParsing1() {
    String spec = "<ROOT>"+
            "<ATTRIBUTE><TYPE>theType</TYPE><DATATYPE>nominal</DATATYPE></ATTRIBUTE>"+
            "</ROOT>";
    FeatureSpecification fs;    
    fs = new FeatureSpecification(spec);
    assertNotNull(fs.getAttributes());
    assertEquals(1,fs.getAttributes().size());
    assertEquals("SimpleAttribute(type=theType,feature=,datatype=nominal,missingvaluetreatment=special_value,codeas=one_of_k",fs.getAttributes().get(0).toString());
    
    spec = "<ROOT>"+
            "<ATTRIBUTELIST><TYPE>theType</TYPE><FEATURE>string</FEATURE><DATATYPE>nominal</DATATYPE><FROM>-2</FROM><TO>1</TO></ATTRIBUTELIST>"+
            "</ROOT>";    
    fs = new FeatureSpecification(spec);
    assertNotNull(fs.getAttributes());
    assertEquals(1,fs.getAttributes().size());
    assertEquals("AttributeList(type=theType,feature=string,datatype=nominal,missingvaluetreatment=special_value,codeas=one_of_k,from=-2,to=1",fs.getAttributes().get(0).toString());

    spec = "<ROOT>"+
            "<NGRAM><TYPE>theType</TYPE><FEATURE>theFeature</FEATURE><NUMBER>3</NUMBER></NGRAM>"+
            "</ROOT>";    
    fs = new FeatureSpecification(spec);
    assertNotNull(fs.getAttributes());
    assertEquals(1,fs.getAttributes().size());
    assertEquals("NgramAttribute(type=theType,feature=theFeature,number=3",fs.getAttributes().get(0).toString());
    
  }  
 
}
