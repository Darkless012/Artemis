/*
 * This file was automatically generated by EvoSuite
 * Fri Sep 29 03:18:41 GMT 2017
 */

package uk.ac.sanger.artemis.editor;

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;
import uk.ac.sanger.artemis.editor.BrowserControl;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class BrowserControl_ESTest extends BrowserControl_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      boolean boolean0 = BrowserControl.isWindowsPlatform();
  }

  @Test(timeout = 4000)
  public void test1()  throws Throwable  {
	  if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0) {
		  BrowserControl.displayURL("g;]4HJos}P!EYor|");
	  }
  }

  @Test(timeout = 4000)
  public void test2()  throws Throwable  {
      BrowserControl browserControl0 = new BrowserControl();
  }
}
