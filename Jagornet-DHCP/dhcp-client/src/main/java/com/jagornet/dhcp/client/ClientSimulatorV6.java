/*
 * Copyright 2009-2014 Jagornet Technologies, LLC.  All Rights Reserved.
 *
 * This software is the proprietary information of Jagornet Technologies, LLC. 
 * Use is subject to license terms.
 *
 */

/*
 *   This file ClientSimulatorV6.java is part of Jagornet DHCP.
 *
 *   Jagornet DHCP is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Jagornet DHCP is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Jagornet DHCP.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.jagornet.dhcp.client;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.DatagramChannel;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jagornet.dhcp.core.message.DhcpV6Message;
import com.jagornet.dhcp.core.option.v6.DhcpV6ClientFqdnOption;
import com.jagornet.dhcp.core.option.v6.DhcpV6ClientIdOption;
import com.jagornet.dhcp.core.option.v6.DhcpV6ElapsedTimeOption;
import com.jagornet.dhcp.core.option.v6.DhcpV6IaNaOption;
import com.jagornet.dhcp.core.option.v6.DhcpV6StatusCodeOption;
import com.jagornet.dhcp.core.util.DhcpConstants;
import com.jagornet.dhcp.core.util.Util;

/**
 * A test client that sends solict/request/release messages 
 * to a DHCPv6 server via multicast.
 * 
 * @author A. Gregory Rabil
 */
@ChannelHandler.Sharable
public class ClientSimulatorV6 extends SimpleChannelUpstreamHandler
{
	private static Logger log = LoggerFactory.getLogger(ClientSimulatorV6.class);

	protected Random random = new Random();
    protected Options options = new Options();
    protected CommandLineParser parser = new BasicParser();
    protected HelpFormatter formatter;

    protected NetworkInterface DEFAULT_NETIF = null;
    protected NetworkInterface mcastNetIf = null;
   	protected InetAddress DEFAULT_SERVER_ADDR;
    protected InetAddress serverAddr;
    protected InetAddress DEFAULT_CLIENT_ADDR;
    protected InetAddress clientAddr;
    protected int serverPort = DhcpConstants.V6_SERVER_PORT;
    protected int clientPort = DhcpConstants.V6_CLIENT_PORT;
    protected boolean rapidCommit = false;
    protected boolean sendRelease = false;
    protected boolean sendFqdn = false;
    protected int numRequests = 100;
    protected AtomicInteger solicitsSent = new AtomicInteger();
    protected AtomicInteger advertisementsReceived = new AtomicInteger();
    protected AtomicInteger requestsSent = new AtomicInteger();
    protected AtomicInteger requestRepliesReceived = new AtomicInteger();
    protected AtomicInteger releasesSent = new AtomicInteger();
    protected AtomicInteger releaseRepliesReceived = new AtomicInteger();
    protected int successCnt = 0;
    protected long startTime = 0;    
    protected long endTime = 0;
    protected long timeout = 0;
    protected int poolSize = 0;
    protected int threadPoolSize = 0;
    protected int requestRate = 0;
    protected CountDownLatch doneLatch = null;

    protected InetSocketAddress server = null;
    protected InetSocketAddress client = null;
    
    protected DatagramChannel channel = null;	
	//protected ExecutorService executor = Executors.newCachedThreadPool();
    protected ExecutorService executor = null;
	
	protected Map<String, ClientMachine> clientMap =
			Collections.synchronizedMap(new HashMap<String, ClientMachine>());

    /**
     * Instantiates a new test client.
     *
     * @param args the args
     * @throws Exception the exception
     */
    public ClientSimulatorV6(String[] args) throws Exception 
    {
    	DEFAULT_NETIF = NetworkInterface.getNetworkInterfaces().nextElement();
    	for (Enumeration<NetworkInterface> netIfs = NetworkInterface.getNetworkInterfaces();
    			netIfs.hasMoreElements();) {
    		NetworkInterface netIf = netIfs.nextElement();
    		if (netIf.isUp() && netIf.supportsMulticast() && !netIf.isLoopback()) {
    			DEFAULT_NETIF = netIf;
    		}
    	}
    	
    	DEFAULT_CLIENT_ADDR = DEFAULT_NETIF.getInetAddresses().nextElement();
    	for (Enumeration<InetAddress> inetAddrs = DEFAULT_NETIF.getInetAddresses(); 
    			inetAddrs.hasMoreElements();) {
    		InetAddress inetAddr = inetAddrs.nextElement();
    		if ((inetAddr instanceof Inet6Address) && inetAddr.isLinkLocalAddress()) {
    			DEFAULT_CLIENT_ADDR = inetAddr;
    		}
    	}
    	
    	DEFAULT_SERVER_ADDR = DhcpConstants.ALL_DHCP_RELAY_AGENTS_AND_SERVERS;
    	
        setupOptions();

        if(!parseOptions(args)) {
            formatter = new HelpFormatter();
            String cliName = this.getClass().getName();
            formatter.printHelp(cliName, options);
            System.exit(0);
        }
        
        log.info("Starting ClientSimulatorV6 with threadPoolSize=" + threadPoolSize);
        if (threadPoolSize <= 0) {
        	executor = Executors.newCachedThreadPool();
        }
        else {
        	executor = Executors.newFixedThreadPool(threadPoolSize);
        }
        
        try {
			start();
		} 
        catch (Exception ex) {
			ex.printStackTrace();
		}
    }
    
	/**
	 * Setup options.
	 */
	private void setupOptions()
    {
		Option numOption = new Option("n", "number", true,
										"Number of client requests to send" +
										" [" + numRequests + "]");
		options.addOption(numOption);
		
        Option miOption = new Option("mi", "multicastinterface", true,
        								"Multicast interface of the DHCPv6 Client" +
        								" [" + DEFAULT_NETIF.getName() + "]");
        options.addOption(miOption);
		
        Option caOption = new Option("ca", "clientaddress", true,
        								"Address of DHCPv6 Client" +
        								" [" + DEFAULT_CLIENT_ADDR + "]");		
        options.addOption(caOption);
		
        Option saOption = new Option("sa", "serveraddress", true,
        								"Address of DHCPv6 Server" +
        								" [" + DEFAULT_SERVER_ADDR + "]");		
        options.addOption(saOption);

        Option cpOption = new Option("cp", "clientport", true,
        							  "Client Port Number" +
        							  " [" + clientPort + "]");
        options.addOption(cpOption);

        Option spOption = new Option("sp", "serverport", true,
        							  "Server Port Number" +
        							  " [" + serverPort + "]");
        options.addOption(spOption);
        
        Option rOption = new Option("r", "rapidcommit", false,
        							"Send rapid-commit Solicit requests");
        options.addOption(rOption);
        
        Option toOption = new Option("to", "timeout", true,
        							"Timeout");
        options.addOption(toOption);
        
        Option psOption = new Option("ps", "poolsize", true,
        							"Size of the pool configured on the server; wait for release after this many requests");
        options.addOption(psOption);
        
        Option tpsOption = new Option("tps", "threadpoolsize", true,
        							"Size of the thread pool used by the client");
        options.addOption(tpsOption);
        
        Option xOption = new Option("x", "release", false,
        							"Send release");
        options.addOption(xOption);
        
        Option fOption = new Option("f", "fqdn", false,
        							"Send client FQDN");
        options.addOption(fOption);

        Option rrOption = new Option("rr","requestrate", true,
				"Request rate per second");
        options.addOption(rrOption);

        Option helpOption = new Option("?", "help", false, "Show this help page.");
        
        options.addOption(helpOption);
    }

	
	protected int parseIntegerOption(String opt, String str, int defval) {
    	int val = defval;
    	try {
    		val = Integer.parseInt(str);
    	}
    	catch (NumberFormatException ex) {
    		System.err.println("Invalid " + opt + " '" + str +
    							"' using default: " + defval +
    							" Exception=" + ex);
    		val = defval;
    	}
    	return val;
	}
	
	protected InetAddress parseIpAddressOption(String opt, String str, InetAddress defaddr) {
    	InetAddress addr = defaddr;
    	try {
    		addr = InetAddress.getByName(str);
    	}
    	catch (UnknownHostException ex) {
    		System.err.println("Invalid " + opt + " address: '" + str +
    							"' using default: " + defaddr +
    							" Exception=" + ex);
    		addr = defaddr;
    	}
    	return addr;
	}
	
    /**
     * Parses the options.
     * 
     * @param args the args
     * 
     * @return true, if successful
     */
    protected boolean parseOptions(String[] args)
    {
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("?")) {
                return false;
            }
            if (cmd.hasOption("n")) {
            	numRequests = 
            			parseIntegerOption("num requests", cmd.getOptionValue("n"), 100);
            }
            mcastNetIf = DEFAULT_NETIF;
            if (cmd.hasOption("mi")) {
            	try {
					mcastNetIf = NetworkInterface.getByName(cmd.getOptionValue("mi"));
				} 
            	catch (SocketException e) {
					e.printStackTrace();
					return false;
				}
            }
            clientAddr = DEFAULT_CLIENT_ADDR;
            if (cmd.hasOption("ca")) {
            	clientAddr = 
            			parseIpAddressOption("client addr", cmd.getOptionValue("ca"), DEFAULT_CLIENT_ADDR);
            }            
            serverAddr = DEFAULT_SERVER_ADDR;
            if (cmd.hasOption("sa")) {
            	serverAddr = 
            			parseIpAddressOption("server addr", cmd.getOptionValue("sa"), DEFAULT_SERVER_ADDR);
            }
            if (cmd.hasOption("cp")) {
            	clientPort = 
            			parseIntegerOption("client port", cmd.getOptionValue("cp"), 
            								DhcpConstants.V6_CLIENT_PORT);
            }
            if (cmd.hasOption("sp")) {
            	serverPort = 
            			parseIntegerOption("server port", cmd.getOptionValue("sp"), 
            								DhcpConstants.V6_SERVER_PORT);
            }
            if (cmd.hasOption("r")) {
            	rapidCommit = true;
            }
            if (cmd.hasOption("to")) {
            	timeout = 
            			parseIntegerOption("timeout", cmd.getOptionValue("to"), 0);
            }
            if (cmd.hasOption("ps")) {
            	poolSize = 
            			parseIntegerOption("address pool size configured on server", cmd.getOptionValue("ps"), 0);
            }
            if (cmd.hasOption("tps")) {
            	threadPoolSize = 
            			parseIntegerOption("thread pool size used by client", cmd.getOptionValue("tps"), 0);
            }
            if (cmd.hasOption("x")) {
            	sendRelease = true;
            }
            if (cmd.hasOption("f")) {
            	sendFqdn = true;
            }
            if (cmd.hasOption("rr")) {
            	requestRate = 
            			parseIntegerOption("request rate per second", cmd.getOptionValue("rr"), 0);
            }
            
            if (poolSize > 0 && !sendRelease) {
            	System.err.println("Must specify -x/--release when using -ps/--poolsize");
            	return false;
            }
        }
        catch (ParseException pe) {
            System.err.println("Command line option parsing failure: " + pe);
            return false;
		}
        return true;
    }
    
    /**
     * Start sending DHCPv6 SOLICITs.
     */
    public void start()
    {
    	DatagramChannelFactory factory = 
    		new NioDatagramChannelFactory(Executors.newCachedThreadPool());
    	
    	server = new InetSocketAddress(serverAddr, serverPort);
    	client = new InetSocketAddress(clientAddr, clientPort);
    	
		ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("logger", new LoggingHandler());
        pipeline.addLast("encoder", new DhcpV6ChannelEncoder());
        pipeline.addLast("decoder", new DhcpV6ChannelDecoder(client, false));
        pipeline.addLast("executor", new ExecutionHandler(
        		new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));
        pipeline.addLast("handler", this);
    	
        channel = factory.newChannel(pipeline);
		channel.getConfig().setNetworkInterface(mcastNetIf);
    	channel.bind(client);

    	
    	if (requestRate <= 0) {
    		// spin up requests as fast as possible
	    	for (int i=1; i<=numRequests; i++) {
	    		log.debug("Executing client " + i);
	    		executor.execute(new ClientMachine(i));
	    	}
    	}
    	else {
	    	// spin up requests at the given rate per second
	    	Thread requestThread = new Thread() {
	    		@Override
	    		public void run() {
	    	    	long before = System.currentTimeMillis();
	    	    	log.debug("Spinning up " + numRequests + " clients at " + before);
	    	    	for (int i=1; i<=numRequests; i++) {
	    	    		log.debug("Executing client " + i);
	    	    		executor.execute(new ClientMachine(i));
	    	    		// if not the last request, see if on request rate boundary
	    	    		if ((i < numRequests) && (i % requestRate == 0)) {
	    	    			long now = System.currentTimeMillis();
	    	    			long diff = now - before;
	    	    			// if less than one second since starting last batch
	    	    			if (diff < 1000) {
	    	    				long wait = 1000 - diff;
	    	    				try {
	    	    					log.debug("Waiting " + wait + "ms to honor requestRate=" + requestRate);
	    							Thread.sleep(wait);
	    							before = System.currentTimeMillis();
	    						} catch (InterruptedException e) {
	    							// TODO Auto-generated catch block
	    							e.printStackTrace();
	    						}
	    	    			}
	    	    		}
	    	    	}
	    		}
	    	};
	    	requestThread.start();
    	}

    	doneLatch = new CountDownLatch(numRequests);
    	try {
    		if (timeout <= 0) {
    			log.info("Waiting for completion");
    			doneLatch.await();
    		}
    		else {
	    		log.info("Waiting total of " + timeout + " seconds for completion");
				doneLatch.await(timeout, TimeUnit.SECONDS);
    		}
		} catch (InterruptedException e) {
			log.warn("Waiting interrupted");
			System.err.println("Interrupted");
		}
    	
		endTime = System.currentTimeMillis();

		log.info("Complete: solicitsSent=" + solicitsSent +
				" advertisementsReceived=" + advertisementsReceived +
				" requestsSent=" + requestsSent +
				" requestRepliesReceived=" + requestRepliesReceived +
				" releasesSent=" + releasesSent +
				" releaseRepliesReceived=" + releaseRepliesReceived +
				" elapsedTime=" + (endTime - startTime) + "ms");

    	log.info("Shutting down executor...");
    	executor.shutdownNow();
    	log.info("Closing channel...");
    	channel.close();
    	log.info("Done.");
    	if ((solicitsSent.get() == advertisementsReceived.get()) &&
    			(requestsSent.get() == requestRepliesReceived.get()) &&
    			(releasesSent.get() == releaseRepliesReceived.get())) {
    		
    		System.exit(0);
    	}
    	else {
    		System.exit(1);
    	}
    }

    /**
     * The Class ClientMachine.
     */
    class ClientMachine implements Runnable, ChannelFutureListener
    {
    	DhcpV6Message msg;
    	int id;
    	String duid;
    	boolean released;
    	Semaphore replySemaphore;
    	DhcpV6Message advertiseMsg;
    	DhcpV6Message requestReplyMsg;
    	boolean retry;
    	
    	/**
	     * Instantiates a new client machine.
	     *
	     * @param msg the msg
	     * @param server the server
	     */
	    public ClientMachine(int id) {
    		this.id = id;
    		this.duid = buildDuid(id);
    		this.released = false;
    		this.replySemaphore = new Semaphore(1);
    		this.retry = true;	// configure to retry
    	}
		
		/* (non-Javadoc)
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			if (poolSize > 0) {
				synchronized (clientMap) {
					if (poolSize <= clientMap.size()) {
						try {
							log.info("Waiting for release...");
							clientMap.wait();
						} 
						catch (InterruptedException ex) {
							log.error("Interrupted", ex);
						}
					}
					clientMap.put(duid, this);
				}
			}
			else {
				clientMap.put(duid, this);
			}
			solicit();
		}
		
		/**
		 * This client is done running.
		 */
		public void doneRun() {
			clientMap.remove(duid);
			doneLatch.countDown();			
		}
		
		public void solicit() {
			msg = buildSolicitMessage(duid); 
			ChannelFuture future = channel.write(msg, server);
			future.addListener(this);
			replySemaphore.drainPermits();
			waitForAdvertise();
		}
	    
	    public void waitForAdvertise() {
	    	try {
				if (!replySemaphore.tryAcquire(2, TimeUnit.SECONDS)) {
					if (retry) {
						retry = false;
						log.warn("Solicit timeout after 2 seconds, retrying...");
						solicit();
					}
				}
				else {
					// A Status Code option may appear in the options field of a DHCP
					// message and/or in the options field of another option.  If the Status
					// Code option does not appear in a message in which the option could
					// appear, the status of the message is assumed to be Success.
					DhcpV6StatusCodeOption statusCodeOption = advertiseMsg.getStatusCodeOption();
					if ((statusCodeOption != null) &&
						(statusCodeOption.getStatusCode() != DhcpConstants.V6STATUS_CODE_SUCCESS)) {
						log.error("Received Advertise message with unsuccessful status code=" +
								statusCodeOption.getStatusCode() + " msg=" +
								statusCodeOption.getMessage());
						doneRun();
					}
					else {
						List<DhcpV6IaNaOption> iaNaOptions = advertiseMsg.getIaNaOptions();
						if ((iaNaOptions != null) && !iaNaOptions.isEmpty()) {
							statusCodeOption = iaNaOptions.get(0).getStatusCodeOption();
							if ((statusCodeOption != null) &&
								(statusCodeOption.getStatusCode() != DhcpConstants.V6STATUS_CODE_SUCCESS)) {
								log.error("Received Advertise IA_NA with unsuccessful status code=" +
										statusCodeOption.getStatusCode() + " msg=" +
										statusCodeOption.getMessage());
								doneRun();
							}
							else {
								request();
							}
						}
						else {
							log.error("No IA_NA options in Advertise message");
							doneRun();
						}
					}
				}
			} catch (InterruptedException e) {
				log.warn(e.getMessage());
			}
	    }
	    
	    public void advertiseReceived(DhcpV6Message advertiseMsg) {
	    	this.advertiseMsg = advertiseMsg;
	    	replySemaphore.release();
	    }
		
		public void request() {
			if (advertiseMsg != null) {
	        	msg = buildRequestMessage(advertiseMsg);
				ChannelFuture future = channel.write(msg, server);
				future.addListener(this);
				waitForRequestReply();
			}
			else {
				log.error("No advertise to request!");
			}
		}
	    
	    public void waitForRequestReply() {
	    	try {
	    		if (!replySemaphore.tryAcquire(2, TimeUnit.SECONDS)) {
	    			if (retry) {
	    				retry = false;
						log.warn("Request timeout after 2 seconds, retrying...");
	    				request();
	    			}
	    		}
	    		else {
					// A Status Code option may appear in the options field of a DHCP
					// message and/or in the options field of another option.  If the Status
					// Code option does not appear in a message in which the option could
					// appear, the status of the message is assumed to be Success.
					DhcpV6StatusCodeOption statusCodeOption = requestReplyMsg.getStatusCodeOption();
					if ((statusCodeOption != null) &&
						(statusCodeOption.getStatusCode() != DhcpConstants.V6STATUS_CODE_SUCCESS)) {
						log.error("Received RequestReply message with unsuccessful status code=" +
								statusCodeOption.getStatusCode() + " msg=" +
								statusCodeOption.getMessage());
						doneRun();
					}
					else {
						List<DhcpV6IaNaOption> iaNaOptions = requestReplyMsg.getIaNaOptions();
						if ((iaNaOptions != null) && !iaNaOptions.isEmpty()) {
							statusCodeOption = iaNaOptions.get(0).getStatusCodeOption();
							if ((statusCodeOption != null) &&
								(statusCodeOption.getStatusCode() != DhcpConstants.V6STATUS_CODE_SUCCESS)) {
								log.error("Received RequestReply IA_NA with unsuccessful status code=" +
										statusCodeOption.getStatusCode() + " msg=" +
										statusCodeOption.getMessage());
								doneRun();
							}
							else {
								release();
							}
						}
						else {
							log.error("No IA_NA options in RequestReply message");
							doneRun();
						}
					}
	    		}
			} catch (InterruptedException e) {
				log.warn(e.getMessage());
			}
	    }
	    
	    public void requestReplyReceived(DhcpV6Message requestReplyMsg) {
	    	this.requestReplyMsg = requestReplyMsg;
	    	replySemaphore.release();
	    }
		
		public void release() {
			if (sendRelease) {
				if (requestReplyMsg != null) {
		        	msg = buildReleaseMessage(requestReplyMsg);
					ChannelFuture future = channel.write(msg, server);
					future.addListener(this);
				}
				else {
					log.error("No request reply to release!");
				}
			}
			else {
				doneRun();
			}
		}
		
		/* (non-Javadoc)
		 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
		 */
		@Override
		public void operationComplete(ChannelFuture future) throws Exception
		{
			if (future.isSuccess()) {
				if (startTime == 0) {
					startTime = System.currentTimeMillis();
					log.info("Starting at: " + startTime);
				}
				if (msg.getMessageType() == DhcpConstants.V6MESSAGE_TYPE_SOLICIT) {
					solicitsSent.getAndIncrement();
					log.info("Successfully sent solicit message duid=" + duid +
							" cnt=" + solicitsSent);
				}
				else if (msg.getMessageType() == DhcpConstants.V6MESSAGE_TYPE_REQUEST) {
					requestsSent.getAndIncrement();
					log.info("Successfully sent request message duid=" + duid +
							" cnt=" + requestsSent);
				}
				else if (msg.getMessageType() == DhcpConstants.V6MESSAGE_TYPE_RELEASE) {
					released = true;
					releasesSent.getAndIncrement();
					log.info("Successfully sent release message duid=" + duid +
							" cnt=" + releasesSent);
				}
			}
			else {
				log.error("Failed to send message id=" + msg.getTransactionId() +
						  ": " + future.getCause());
			}
		}
    }

    private String buildDuid(long id) {
        byte[] bid = BigInteger.valueOf(id).toByteArray();
        byte[] chAddr = new byte[6];
        chAddr[0] = (byte)0xde;
        chAddr[1] = (byte)0xb1;
        if (bid.length == 4) {
            chAddr[2] = bid[0];
            chAddr[3] = bid[1];
            chAddr[4] = bid[2];
            chAddr[5] = bid[3];
        }
        else if (bid.length == 3) {
	        chAddr[2] = 0;
	        chAddr[3] = bid[0];
	        chAddr[4] = bid[1];
	        chAddr[5] = bid[2];
        }
        else if (bid.length == 2) {
	        chAddr[2] = 0;
	        chAddr[3] = 0;
	        chAddr[4] = bid[0];
	        chAddr[5] = bid[1];
        }
        else if (bid.length == 1) {
	        chAddr[2] = 0;
	        chAddr[3] = 0;
	        chAddr[4] = 0;
	        chAddr[5] = bid[0];
        }
        return "clientid-" + Util.toHexString(chAddr);
    }
    
    /**
     * Builds the solict message.
     * 
     * @return the  dhcp message
     */
    private DhcpV6Message buildSolicitMessage(String duid)
    {
        DhcpV6Message msg = new DhcpV6Message(null, new InetSocketAddress(serverAddr, serverPort));

        msg.setTransactionId(random.nextInt());
        DhcpV6ClientIdOption dhcpClientId = new DhcpV6ClientIdOption();
        dhcpClientId.getOpaqueData().setAscii(duid);
        
        msg.putDhcpOption(dhcpClientId);
        
        DhcpV6ElapsedTimeOption dhcpElapsedTime = new DhcpV6ElapsedTimeOption();
        dhcpElapsedTime.setUnsignedShort(1);
        msg.putDhcpOption(dhcpElapsedTime);
        
    	msg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_SOLICIT);
        DhcpV6IaNaOption dhcpIaNa = new DhcpV6IaNaOption();
        dhcpIaNa.setIaId(1);
        msg.putDhcpOption(dhcpIaNa);

        if (sendFqdn) {
        	DhcpV6ClientFqdnOption fqdnOption = new DhcpV6ClientFqdnOption();
        	fqdnOption.setDomainName("jagornet-clientsimv6-" + duid);
        	msg.putDhcpOption(fqdnOption);
        }
        
        return msg;
    }
    
    private DhcpV6Message buildRequestMessage(DhcpV6Message advertisement) {
    	
        DhcpV6Message msg = new DhcpV6Message(null, new InetSocketAddress(serverAddr, serverPort));

        msg.setTransactionId(advertisement.getTransactionId());
        msg.putDhcpOption(advertisement.getDhcpClientIdOption());
        msg.putDhcpOption(advertisement.getDhcpServerIdOption());
        msg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_REQUEST);
        msg.putDhcpOption(advertisement.getIaNaOptions().get(0));

        if (sendFqdn) {
        	DhcpV6ClientFqdnOption fqdnOption = new DhcpV6ClientFqdnOption();
        	fqdnOption.setDomainName("jagornet-clientsimv6-" + 
        							 advertisement.getDhcpClientIdOption().getOpaqueData().getAscii());
        	msg.putDhcpOption(fqdnOption);
        }
        
        return msg;
    }
    
    private DhcpV6Message buildReleaseMessage(DhcpV6Message reply) {
    	
        DhcpV6Message msg = new DhcpV6Message(null, new InetSocketAddress(serverAddr, serverPort));

        msg.setTransactionId(reply.getTransactionId());
        msg.putDhcpOption(reply.getDhcpClientIdOption());
        msg.putDhcpOption(reply.getDhcpServerIdOption());
        msg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_RELEASE);
        msg.putDhcpOption(reply.getIaNaOptions().get(0));
        
        return msg;
    }

	/*
	 * (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception
    {
    	Object message = e.getMessage();
        if (message instanceof DhcpV6Message) {
            
            DhcpV6Message dhcpMessage = (DhcpV6Message) message;
            if (log.isDebugEnabled())
            	log.debug("Received: " + dhcpMessage.toStringWithOptions());
            else
            	log.info("Received: " + dhcpMessage.toString());
            
            if (dhcpMessage.getMessageType() == DhcpConstants.V6MESSAGE_TYPE_ADVERTISE) {
	            ClientMachine client = 
	            		clientMap.get(dhcpMessage.getDhcpClientIdOption().getOpaqueData().getAscii());
	            if (client != null) {
	            	advertisementsReceived.getAndIncrement();
	            	client.advertiseReceived(dhcpMessage);
	            }
	            else {
	            	log.error("Received advertise for client not found in map: duid=" + 
	            			dhcpMessage.getDhcpClientIdOption().getOpaqueData().getAscii());
	            }
            }
            else if (dhcpMessage.getMessageType() == DhcpConstants.V6MESSAGE_TYPE_REPLY) {
            	String key = dhcpMessage.getDhcpClientIdOption().getOpaqueData().getAscii();
	            ClientMachine client = clientMap.get(key);
	            if (client != null) {
	            	if (!client.released) {
		            	requestRepliesReceived.getAndIncrement();
		            	client.requestReplyReceived(dhcpMessage);
	            	}
	            	else {
	            		releaseRepliesReceived.getAndIncrement();
						clientMap.remove(key);
						if (poolSize > 0) {
							synchronized (clientMap) {
								clientMap.notify();
							}
						}
						doneLatch.countDown();
	            	}
	            }
	            else {
	            	log.error("Received reply for client not found in map: duid=" + 
	            			dhcpMessage.getDhcpClientIdOption().getOpaqueData().getAscii());
	            }
            }
            else {
            	log.warn("Received unhandled message type: " + dhcpMessage.getMessageType());
            }
        }
        else {
            // Note: in theory, we can't get here, because the
            // codec would have thrown an exception beforehand
            log.error("Received unknown message object: " + message.getClass());
        }
    }
	 
	/* (non-Javadoc)
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception
	{
    	log.error("Exception caught: ", e.getCause());
    	e.getChannel().close();
	}
    
    /**
     * The main method.
     * 
     * @param args the arguments
     */
    public static void main(String[] args) {
        try {
			new ClientSimulatorV6(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

}
