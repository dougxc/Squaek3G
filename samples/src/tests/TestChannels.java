/*
 * TestChannels.java
 *
 * Created on March 3, 2006, 1:16 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package tests;

import com.sun.squawk.*;
import com.sun.squawk.util.Assert;
import com.sun.squawk.io.mailboxes.*;

import java.io.*;
import javax.microedition.io.*;



/**
 *
 * @author dw29446
 */
public class TestChannels {
    public static final String msg1 = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    public final static int NUM_MESSAGES = 1000;
    
    
    public static void main(String[] args) {
        Runtime runtime = Runtime.getRuntime();
        
       /* System.out.println("Sanity tests:");
        x1();*/
        
        System.out.println("\n cleanup GC:");
        System.gc();
        System.gc();
        System.out.println("\nTime test of new IIC (3 runs):");
        try {
            long free1 = runtime.freeMemory();
            NewChannelTimeTest.main(new String[0]);
            long free2 = runtime.freeMemory();
            NewChannelTimeTest.main(new String[0]);
            NewChannelTimeTest.main(new String[0]);
            System.out.println("at least " + (free1 - free2) + " bytes used in one run");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
       /* System.out.println("\n cleanup GC:");
        System.gc();
        System.gc();
        System.out.println("\nTime test of old IIC (3 runs):");
        try {
            long free1 = runtime.freeMemory();
            OldMsgTimeTest.main(new String[0]);
            long free2 = runtime.freeMemory();
            OldMsgTimeTest.main(new String[0]);
            OldMsgTimeTest.main(new String[0]);
            System.out.println("at least " + (free1 - free2) + " bytes used in one run");
        } catch (Exception ex) {
            ex.printStackTrace();
        }*/
        
        
        System.exit(0);
    }
}


/**
 *===============================================================================
 * Time new inter-isolate mechanism.
 * Time client sending NUM_MESSAGES messages, where server responds with error code.
 */
class NewChannelTimeTest {
    public final static String MAILBOX_NAME = "NewChannelTimeTest";
    

    public static void main(String[] args) throws Exception {
        Client client =new Client();
        
        Server server = new Server();
        server.start();
        
        client.start();
        client.join();
        
        server.join();
    }
    
    /**
     * The client thread class.
     */
    static class Client extends Thread {
        public void run() {
             try {
                byte[] data = TestMailboxes.msg1.getBytes();
                
                long start = System.currentTimeMillis();
                Channel testChan = Channel.lookup(MAILBOX_NAME);
                
                for (int i = 0; i < TestMailboxes.NUM_MESSAGES; i++) {
                    Envelope cmdEnv = new ByteArrayEnvelope(data);
                    testChan.send(cmdEnv);
                    ByteArrayEnvelope replyEnv = (ByteArrayEnvelope)testChan.receive();
                    byte[] replyData = replyEnv.getData();
                    if (replyData.length != 1 || (replyData[0] != 0)) {
                        System.err.println("Reply not OK");
                    }
                }
                long time = System.currentTimeMillis() - start;
                
                System.err.println("Client sent " + TestMailboxes.NUM_MESSAGES + " messages of " + data.length + " bytes in " + time + "ms");
                testChan.close();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * The server thread class.
     */
    static class Server extends Thread {
        
        public void run() {
            byte[] replyData = new byte[1];
            ServerChannel serverChannel = null;
            Channel aChannel = null;
            try {
                serverChannel = ServerChannel.create(MAILBOX_NAME);
            } catch (MailboxInUseException ex) {
                throw new RuntimeException(ex.toString());
            }
            
            try {
                aChannel = serverChannel.accept();

                // handle messages:
                while (true) {
                    Envelope msg;
                    try {
                        msg = aChannel.receive();
                    } catch (MailboxClosedException e) {
                        System.out.println("Server seems to have gone away. Oh well. " + aChannel);
                        break;
                    }
                    
                    if (msg instanceof ByteArrayEnvelope) {
                        ByteArrayEnvelope dataEnv = (ByteArrayEnvelope)msg;
                        byte[] data = dataEnv.getData();
                        
                        replyData[0] = 0;
                        Envelope replyEnv = new ByteArrayEnvelope(replyData);
                        try {
                            msg.getReplyAddress().send(replyEnv);
                        } catch (AddressClosedException ex) {
                            System.out.println("Client seems to have gone away. Oh well. " + msg.getToAddress());
                        }
                    }
                }
            } catch (IOException ex) {
                // ok, just close server.
            } finally {
                // no way to get here:
                System.out.println("Closing server...");
                aChannel.close();
                serverChannel.close();
            }
        }
    }
}

class IDEnvelope extends Envelope {
    private String value;
    
    public IDEnvelope(String value) {
        this.value = new String(value);
    }
    
    public Object getContents() {
        return value;
    }
}// IDEnvelope

class ChildIsolateTestRunner {

	public static final String CHANNEL_NAME = "INTER_ISOLATE_TEST_RESULTS";

	public void run(String[] classesToRun) throws MailboxInUseException, AddressClosedException, MailboxClosedException, Exception {
		ServerChannel serverChannel = ServerChannel.create(CHANNEL_NAME);
		String uri = VM.getCurrentIsolate().getParentSuiteSourceURI();
		Isolate[] isolate = new Isolate[classesToRun.length];
		
		for (int i = 0; i < classesToRun.length; i++) {
			isolate[i] = new Isolate("tests.ChildIsolateTestHelper", new String[] {""+i}, null, uri);
			isolate[i].start();
		}

		for (int i = 0; i < isolate.length; i++) {
			Channel aChannel = serverChannel.accept();
			IDEnvelope msg = (IDEnvelope)aChannel.receive();
			System.out.println("Received: " + msg.getContents());
			aChannel.close();
		}
		
		for (int i = 0; i < isolate.length; i++) {
			isolate[i].join();
			if (isolate[i].getExitCode() != 0)
				throw new Exception("exception in " + classesToRun[i] + " isolate");			
		}
		
        serverChannel.close();
	}
}

class ChildIsolateTestHelper {
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
 
        try {
            Thread.currentThread().sleep(5);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
		Channel aChannel = null;
		try {

			aChannel = Channel.lookup(ChildIsolateTestRunner.CHANNEL_NAME);
System.err.println("isolate # " + args[0] + " opened " + aChannel);
			Thread.yield();
			aChannel.send(new IDEnvelope(args[0]));
		} catch (NoSuchMailboxException e) {
			e.printStackTrace();
		} catch (AddressClosedException e) {
			e.printStackTrace();
		} finally {
			if (aChannel != null) {
                System.err.println("isolate # " + args[0] + "is closing channel");
				aChannel.close();
			}
		}
	
	}
}

class MultiIsolateTest {

	public static void main(String[] args) throws Exception {
		System.out.println("Testing child isolates can communicate with remote slave");
		
		String[] classesToRun = new String[] {
				"tests.ChildIsolateTest",
				"tests.ChildIsolateTest",
                "tests.ChildIsolateTest",
                "tests.ChildIsolateTest",
                "tests.ChildIsolateTest",
                "tests.ChildIsolateTest",
                "tests.ChildIsolateTest",
                "tests.ChildIsolateTest",
				"tests.ChildIsolateTest"
		};
		ChildIsolateTestRunner childIsolateTestRunner = new ChildIsolateTestRunner();
		childIsolateTestRunner.run(classesToRun);
	}
}
