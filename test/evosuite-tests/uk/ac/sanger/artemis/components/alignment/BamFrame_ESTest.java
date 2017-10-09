/*
 * This file was automatically generated by EvoSuite
 * Fri Sep 29 01:09:56 GMT 2017
 */

package uk.ac.sanger.artemis.components.alignment;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import java.awt.HeadlessException;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;
import uk.ac.sanger.artemis.components.alignment.BamFrame;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class BamFrame_ESTest extends BamFrame_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      BamFrame bamFrame0 = null;
      try {
        bamFrame0 = new BamFrame();
        fail("Expecting exception: HeadlessException");
      
      } catch(HeadlessException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("java.awt.GraphicsEnvironment", e);
      }
  }

  @Test(timeout = 4000)
  public void test1()  throws Throwable  {
	  boolean result = (System.getProperty("mrj.version") != null ||
	             System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0);
      boolean boolean0 = BamFrame.isMac();
      assertEquals(result, boolean0);
  }
}
