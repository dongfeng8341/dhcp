/*
 * Copyright 2009-2014 Jagornet Technologies, LLC.  All Rights Reserved.
 *
 * This software is the proprietary information of Jagornet Technologies, LLC. 
 * Use is subject to license terms.
 *
 */

/*
 *   This file DhcpV6StatusCodeOption.java is part of Jagornet DHCP.
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
package com.jagornet.dhcp.core.option.v6;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.jagornet.dhcp.core.option.base.BaseDhcpOption;
import com.jagornet.dhcp.core.util.DhcpConstants;
import com.jagornet.dhcp.core.util.Util;

/**
 * <p>Title: DhcpV6StatusCodeOption </p>
 * <p>Description: </p>.
 * 
 * @author A. Gregory Rabil
 */
public class DhcpV6StatusCodeOption extends BaseDhcpOption
{    
    private int statusCode;
    private String message;
    
    /**
     * Instantiates a new dhcp status code option.
     */
    public DhcpV6StatusCodeOption()
    {
        this((int)0, null);
    }
    
    /**
     * Instantiates a new dhcp status code option.
     * 
     * @param statusCodeOption the status code option
     */
    public DhcpV6StatusCodeOption(int statusCode, String message)
    {
        super();
        setStatusCode(statusCode);
        setMessage(message);
        setCode(DhcpConstants.V6OPTION_STATUS_CODE);
    }

    public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getMessage() {
		if (message == null) {
			return DhcpConstants.getV6StatusCodeString(statusCode);
		}
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	/* (non-Javadoc)
     * @see com.jagornet.dhcpv6.option.DhcpOption#getLength()
     */
    public int getLength()
    {
        int len = 2;	// status code
        if (message != null) {
        	len += message.length();
        }
        return len;
    }

    /* (non-Javadoc)
     * @see com.jagornet.dhcpv6.option.Encodable#encode()
     */
    public ByteBuffer encode() throws IOException
    {
        ByteBuffer buf = super.encodeCodeAndLength();
        buf.putShort((short)statusCode);
        if (message != null) {
        	buf.put(message.getBytes());
        }
        return (ByteBuffer) buf.flip();
    }

    /* (non-Javadoc)
     * @see com.jagornet.dhcpv6.option.Decodable#decode(java.nio.ByteBuffer)
     */
    public void decode(ByteBuffer buf) throws IOException
    {
    	int len = super.decodeLength(buf);
    	if ((len > 0) && (len <= buf.remaining())) {
            int eof = buf.position() + len;
            if (buf.position() < eof) {
	            statusCode = Util.getUnsignedShort(buf);
	            if (buf.position() < eof) {
	            	if (len > 2) {
		                byte[] data = new byte[len-2];  // minus 2 for the status code
		                buf.get(data);
		                message = new String(data);
	            	}
	            }
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder(Util.LINE_SEPARATOR);
        sb.append(super.getName());
        sb.append(": statusCode=");
        sb.append(getStatusCode());
        sb.append(" message=");
        sb.append(getMessage());
        return sb.toString();
    }
    
}
