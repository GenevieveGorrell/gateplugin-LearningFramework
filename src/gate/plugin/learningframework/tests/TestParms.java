/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gate.plugin.learningframework.tests;

import gate.plugin.learningframework.engines.Info;
import gate.plugin.learningframework.engines.Parms;
import java.io.File;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tester for the Parms class.
 * @author Johann Petrak
 */
public class TestParms {
  @Test
  public void testParms1() {
    Map<String,String> myParms = Parms.getParameters("-toIgnore -maxDepth 3 -prune ", "m:maxDepth:1", "p:prune:0");
    assertEquals(2,myParms.size());
    assertEquals("3",myParms.get("maxDepth"));
    //assertEquals("true",myParms.get("prune"));
  }
}
