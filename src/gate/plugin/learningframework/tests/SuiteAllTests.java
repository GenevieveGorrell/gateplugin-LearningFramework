
package gate.plugin.learningframework.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestFeatureSpecification.class,
  TestFeatureExtraction.class,
  EngineWekaTest.class,
  TestPipeSerialization.class
})
public class SuiteAllTests {
  // so we can run this test from the command line 
  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main(SuiteAllTests.class.getCanonicalName());
  }  
  
}
