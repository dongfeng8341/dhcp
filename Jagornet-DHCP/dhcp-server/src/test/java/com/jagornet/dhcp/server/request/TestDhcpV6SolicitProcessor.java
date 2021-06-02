/*
 * Copyright 2009-2014 Jagornet Technologies, LLC.  All Rights Reserved.
 *
 * This software is the proprietary information of Jagornet Technologies, LLC. 
 * Use is subject to license terms.
 *
 */

/*
 *   This file TestDhcpV6SolicitProcessor.java is part of Jagornet DHCP.
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
package com.jagornet.dhcp.server.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.InetAddress;

import org.junit.Test;

import com.jagornet.dhcp.core.message.DhcpV6Message;
import com.jagornet.dhcp.core.option.v6.DhcpV6ReconfigureAcceptOption;
import com.jagornet.dhcp.core.util.DhcpConstants;

/**
 * The Class TestDhcpV6SolicitProcessor.
 */
public class TestDhcpV6SolicitProcessor extends BaseTestDhcpV6Processor
{	
	/**
	 * Test solicit.
	 * 
	 * @throws Exception the exception
	 */
	@Test
	public void testNaSolicit() throws Exception
	{
		DhcpV6Message requestMsg = buildNaRequestMessage(firstPoolAddr);
		requestMsg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_SOLICIT);

		DhcpV6SolicitProcessor processor = 
			new DhcpV6SolicitProcessor(requestMsg, requestMsg.getRemoteAddress().getAddress());
		
		DhcpV6Message replyMsg = processor.processMessage();
		
		assertNotNull(replyMsg);
		assertEquals(requestMsg.getTransactionId(), replyMsg.getTransactionId());
		assertEquals(DhcpConstants.V6MESSAGE_TYPE_ADVERTISE, replyMsg.getMessageType());
		
		checkReply(replyMsg,
				InetAddress.getByName("2001:DB8:1::A"),
				InetAddress.getByName("2001:DB8:1::FF"));
	}
	
	@Test
	public void testNaPdSolicit() throws Exception
	{
		DhcpV6Message requestMsg = buildNaPdRequestMessage(firstPoolAddr, null,
									DhcpConstants.ZEROADDR_V6.getHostAddress(), 56);
		requestMsg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_SOLICIT);

		DhcpV6SolicitProcessor processor = 
			new DhcpV6SolicitProcessor(requestMsg, requestMsg.getRemoteAddress().getAddress());
		
		DhcpV6Message replyMsg = processor.processMessage();
		
		assertNotNull(replyMsg);
		assertEquals(requestMsg.getTransactionId(), replyMsg.getTransactionId());
		assertEquals(DhcpConstants.V6MESSAGE_TYPE_ADVERTISE, replyMsg.getMessageType());
		
		checkReply(replyMsg,
				InetAddress.getByName("2001:DB8:1::A"),
				InetAddress.getByName("2001:DB8:1::FF"),
				3600,
				InetAddress.getByName("2001:DB8:1:4000::"),
				(short)56);		
	}
	
	/**
	 * Test solicit request off link address.
	 * 
	 * @throws Exception the exception
	 */
	@Test
	public void testNaSolicitRequestOffLinkAddress() throws Exception
	{
		DhcpV6Message requestMsg = buildNaRequestMessage(firstPoolAddr,
													"2001:DB8:2::1");
		requestMsg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_SOLICIT);

		DhcpV6SolicitProcessor processor = 
			new DhcpV6SolicitProcessor(requestMsg, requestMsg.getRemoteAddress().getAddress());
		
		DhcpV6Message replyMsg = processor.processMessage();
		
		assertNotNull(replyMsg);
		assertEquals(requestMsg.getTransactionId(), replyMsg.getTransactionId());
		assertEquals(DhcpConstants.V6MESSAGE_TYPE_ADVERTISE, replyMsg.getMessageType());
		
		checkReply(replyMsg,
				InetAddress.getByName("2001:DB8:1::A"),
				InetAddress.getByName("2001:DB8:1::FF"));		
	}
	
	/**
	 * Test solicit reconfigure accept.
	 * 
	 * @throws Exception the exception
	 */
	@Test
	public void testNaSolicitReconfigureAccept() throws Exception
	{
		DhcpV6Message requestMsg = buildNaRequestMessage(firstPoolAddr);
		requestMsg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_SOLICIT);

		DhcpV6ReconfigureAcceptOption reconfigAcceptOption = new DhcpV6ReconfigureAcceptOption();
		requestMsg.putDhcpOption(reconfigAcceptOption);

		DhcpV6SolicitProcessor processor = 
			new DhcpV6SolicitProcessor(requestMsg, requestMsg.getRemoteAddress().getAddress());
		
		DhcpV6Message replyMsg = processor.processMessage();
		
		assertNotNull(replyMsg);
		assertEquals(requestMsg.getTransactionId(), replyMsg.getTransactionId());
		assertEquals(DhcpConstants.V6MESSAGE_TYPE_ADVERTISE, replyMsg.getMessageType());
		
		checkReply(replyMsg,
				InetAddress.getByName("2001:DB8:1::10A"),
				InetAddress.getByName("2001:DB8:1::1FF"));		
	}
	
	/**
	 * Test solicit no reconfigure accept.
	 * 
	 * @throws Exception the exception
	 */
	@Test
	public void testNaSolicitNoReconfigureAccept() throws Exception
	{
		DhcpV6Message requestMsg = buildNaRequestMessage(InetAddress.getByName("2001:DB8:2::1"));
		requestMsg.setMessageType(DhcpConstants.V6MESSAGE_TYPE_SOLICIT);

		DhcpV6SolicitProcessor processor = 
			new DhcpV6SolicitProcessor(requestMsg, requestMsg.getRemoteAddress().getAddress());
		
		DhcpV6Message replyMsg = processor.processMessage();
		
		assertNotNull(replyMsg);
		assertEquals(requestMsg.getTransactionId(), replyMsg.getTransactionId());
		assertEquals(DhcpConstants.V6MESSAGE_TYPE_ADVERTISE, replyMsg.getMessageType());
		
		// TAHI tests want the status at the message level, but it
		// should be safe to put it at both message and IA levels
		checkReplyIaNaStatus(replyMsg, DhcpConstants.V6STATUS_CODE_NOADDRSAVAIL);
		checkReplyMsgStatus(replyMsg, DhcpConstants.V6STATUS_CODE_NOADDRSAVAIL);
	}
	
	
}
