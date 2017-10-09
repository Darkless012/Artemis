/*
 * This file was automatically generated by EvoSuite
 * Fri Sep 29 03:11:10 GMT 2017
 */

package uk.ac.sanger.artemis.components.variant;

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.sshtools.j2ssh.io.ByteArrayReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PushbackInputStream;
import java.net.URL;
import net.sf.samtools.seekablestream.SeekableStream;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.evosuite.runtime.mock.java.net.MockURL;
import org.evosuite.runtime.testdata.EvoSuiteFile;
import org.evosuite.runtime.testdata.FileSystemHandling;
import org.junit.runner.RunWith;
import uk.ac.sanger.artemis.components.variant.TabixReader;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class TabixReader_ESTest extends TabixReader_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      byte[] byteArray0 = new byte[1];
      byteArray0[0] = (byte)73;
      ByteArrayReader byteArrayReader0 = new ByteArrayReader(byteArray0);
      String string0 = TabixReader.readLine((InputStream) byteArrayReader0);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      byte[] byteArray0 = new byte[3];
      byteArray0[0] = (byte)69;
      ByteArrayReader byteArrayReader0 = new ByteArrayReader(byteArray0);
      long long0 = TabixReader.readLong(byteArrayReader0);
      assertEquals(69L, long0);
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      byte[] byteArray0 = new byte[1];
      ByteArrayInputStream byteArrayInputStream0 = new ByteArrayInputStream(byteArray0, (byte) (-113), 0);
      int int0 = TabixReader.readInt(byteArrayInputStream0);
      assertEquals(0, int0);
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      // Undeclared exception!
      try { 
        TabixReader.readLong((InputStream) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("uk.ac.sanger.artemis.components.variant.TabixReader", e);
      }
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      // Undeclared exception!
      try { 
        TabixReader.readLine((InputStream) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("uk.ac.sanger.artemis.components.variant.TabixReader", e);
      }
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      PipedInputStream pipedInputStream0 = new PipedInputStream();
      DataInputStream dataInputStream0 = new DataInputStream(pipedInputStream0);
      try { 
        TabixReader.readLine((InputStream) dataInputStream0);
        fail("Expecting exception: IOException");
      
      } catch(IOException e) {
         //
         // Pipe not connected
         //
         verifyException("java.io.PipedInputStream", e);
      }
  }

  @Test(timeout = 4000)
  public void test06()  throws Throwable  {
      PushbackInputStream pushbackInputStream0 = new PushbackInputStream((InputStream) null, 1094);
      DataInputStream dataInputStream0 = new DataInputStream(pushbackInputStream0);
      try { 
        TabixReader.readInt(dataInputStream0);
        fail("Expecting exception: IOException");
      
      } catch(IOException e) {
         //
         // Stream closed
         //
         verifyException("java.io.PushbackInputStream", e);
      }
  }

  @Ignore("Not a cross-platform test")
  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      EvoSuiteFile evoSuiteFile0 = new EvoSuiteFile(CURRENT_DIR + "/.tbi");
      FileSystemHandling.createFolder(evoSuiteFile0);
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader("", (SeekableStream) null);
        fail("Expecting exception: IOException");
      
      } catch(Throwable e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("org.evosuite.runtime.mock.java.io.NativeMockedIO", e);
      }
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader("", (URL) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("org.evosuite.runtime.mock.java.net.MockURL", e);
      }
  }

  @Ignore("Not a cross-platform test")
  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      URL uRL0 = MockURL.getHttpExample();
      EvoSuiteFile evoSuiteFile0 = new EvoSuiteFile(CURRENT_DIR + "/You can not retrieve part of an empty symbol list.tbi");
      FileSystemHandling.createFolder(evoSuiteFile0);
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader("You can not retrieve part of an empty symbol list", uRL0);
        fail("Expecting exception: IOException");
      
      } catch(Throwable e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("org.evosuite.runtime.mock.java.io.NativeMockedIO", e);
      }
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader((String) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("java.io.File", e);
      }
  }

  @Ignore("Not a cross-platform test")
  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      EvoSuiteFile evoSuiteFile0 = new EvoSuiteFile(CURRENT_DIR + "/.tbi");
      FileSystemHandling.appendStringToFile(evoSuiteFile0, ";");
      TabixReader tabixReader0 = new TabixReader("", (SeekableStream) null);
      assertEquals(0, tabixReader0.getStartColumn());
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      byte[] byteArray0 = new byte[3];
      ByteArrayReader byteArrayReader0 = new ByteArrayReader(byteArray0);
      TabixReader.readLine((InputStream) byteArrayReader0);
      assertEquals(0, byteArrayReader0.available());
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader("");
        fail("Expecting exception: FileNotFoundException");
      
      } catch(Throwable e) {
         //
         // File does not exist, and RandomAccessFile is not open in write mode
         //
         verifyException("org.evosuite.runtime.mock.java.io.MockRandomAccessFile", e);
      }
  }

  @Test(timeout = 4000)
  public void test14()  throws Throwable  {
      URL uRL0 = MockURL.getHttpExample();
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader("You can not retrieve part of an empty symbol list", uRL0);
        fail("Expecting exception: FileNotFoundException");
      
      } catch(Throwable e) {
         //
         // File does not exist, and RandomAccessFile is not open in write mode
         //
         verifyException("org.evosuite.runtime.mock.java.io.MockRandomAccessFile", e);
      }
  }

  @Test(timeout = 4000)
  public void test15()  throws Throwable  {
      // Undeclared exception!
      try { 
        TabixReader.readInt((InputStream) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("uk.ac.sanger.artemis.components.variant.TabixReader", e);
      }
  }

  @Test(timeout = 4000)
  public void test16()  throws Throwable  {
      TabixReader tabixReader0 = null;
      try {
        tabixReader0 = new TabixReader("", (SeekableStream) null);
        fail("Expecting exception: FileNotFoundException");
      
      } catch(Throwable e) {
         //
         // File does not exist, and RandomAccessFile is not open in write mode
         //
         verifyException("org.evosuite.runtime.mock.java.io.MockRandomAccessFile", e);
      }
  }

  @Test(timeout = 4000)
  public void test17()  throws Throwable  {
      byte[] byteArray0 = new byte[3];
      ByteArrayReader byteArrayReader0 = new ByteArrayReader(byteArray0);
      long long0 = TabixReader.readLong(byteArrayReader0);
      assertEquals(0L, long0);
  }
}
